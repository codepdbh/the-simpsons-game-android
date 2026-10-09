#include "vulkan_context.h"
#include "android_logging.h"
#include "gpu_quirks.h"
#include <SDL3/SDL_vulkan.h>
#include <algorithm>
#include <cstring>
#include <limits>
#include <stdexcept>
#include <string>

namespace {
void check(VkResult result, const char* operation) {
    if (result != VK_SUCCESS) throw std::runtime_error(std::string(operation) + " VkResult=" + std::to_string(result));
}
bool has(const std::vector<VkExtensionProperties>& extensions, const char* name) {
    return std::any_of(extensions.begin(), extensions.end(), [name](const auto& e) { return strcmp(e.extensionName, name) == 0; });
}
VKAPI_ATTR VkBool32 VKAPI_CALL validation_message(VkDebugUtilsMessageSeverityFlagBitsEXT severity,
    VkDebugUtilsMessageTypeFlagsEXT, const VkDebugUtilsMessengerCallbackDataEXT* data, void*) {
    log_message("VULKAN", "VALIDATION %s: %s", severity & VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT ? "ERROR" : "WARNING", data->pMessage);
    return VK_FALSE;
}
}
VulkanContext::VulkanContext(SDL_Window* window) : window_(window) {
    try { initialize(); } catch (...) { cleanup(); throw; }
}
VulkanContext::~VulkanContext() { cleanup(); }
void VulkanContext::initialize() {
    if (!SDL_Vulkan_LoadLibrary(nullptr)) throw std::runtime_error(SDL_GetError());
    log_message("VULKAN", "Loader initialized (system driver)");
    auto getProc = reinterpret_cast<PFN_vkGetInstanceProcAddr>(SDL_Vulkan_GetVkGetInstanceProcAddr());
    auto enumerateVersion = reinterpret_cast<PFN_vkEnumerateInstanceVersion>(getProc(VK_NULL_HANDLE, "vkEnumerateInstanceVersion"));
    uint32_t version = VK_API_VERSION_1_0;
    if (enumerateVersion) check(enumerateVersion(&version), "vkEnumerateInstanceVersion");
    if (version < VK_API_VERSION_1_1) throw std::runtime_error("Vulkan 1.1 or newer is required.");
    uint32_t count = 0;
    check(vkEnumerateInstanceExtensionProperties(nullptr, &count, nullptr), "instance extensions");
    std::vector<VkExtensionProperties> available(count);
    check(vkEnumerateInstanceExtensionProperties(nullptr, &count, available.data()), "instance extensions");
    const char* const* required = SDL_Vulkan_GetInstanceExtensions(&count);
    if (!required) throw std::runtime_error(SDL_GetError());
    for (uint32_t i = 0; i < count; ++i) {
        if (!has(available, required[i])) throw std::runtime_error(std::string("Missing instance extension: ") + required[i]);
        log_message("VULKAN", "Required instance extension: %s", required[i]);
    }
    std::vector<const char*> enabled(required, required + count);
    const char* validation = "VK_LAYER_KHRONOS_validation";
    bool validationEnabled = false;
#ifndef NDEBUG
    uint32_t layerCount = 0;
    check(vkEnumerateInstanceLayerProperties(&layerCount, nullptr), "layers");
    std::vector<VkLayerProperties> layers(layerCount);
    check(vkEnumerateInstanceLayerProperties(&layerCount, layers.data()), "layers");
    for (const auto& layer : layers) if (strcmp(layer.layerName, validation) == 0) validationEnabled = true;
#endif
    bool debugUtils = false;
    if (validationEnabled) {
        uint32_t n = 0;
        check(vkEnumerateInstanceExtensionProperties(validation, &n, nullptr), "validation extensions");
        std::vector<VkExtensionProperties> ext(n);
        check(vkEnumerateInstanceExtensionProperties(validation, &n, ext.data()), "validation extensions");
        debugUtils = has(available, VK_EXT_DEBUG_UTILS_EXTENSION_NAME) || has(ext, VK_EXT_DEBUG_UTILS_EXTENSION_NAME);
        if (debugUtils) enabled.push_back(VK_EXT_DEBUG_UTILS_EXTENSION_NAME);
    }
    log_message("VULKAN", "Validation layer: %s", validationEnabled ? "enabled" : "unavailable or release build");
    VkDebugUtilsMessengerCreateInfoEXT debug{VK_STRUCTURE_TYPE_DEBUG_UTILS_MESSENGER_CREATE_INFO_EXT};
    debug.messageSeverity = VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT | VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT;
    debug.messageType = VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT | VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT | VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT;
    debug.pfnUserCallback = validation_message;
    VkApplicationInfo app{VK_STRUCTURE_TYPE_APPLICATION_INFO};
    app.pApplicationName = "The Simpsons Game Android Milestone 1";
    app.apiVersion = VK_API_VERSION_1_1;
    VkInstanceCreateInfo info{VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO};
    info.pApplicationInfo = &app; info.enabledExtensionCount = static_cast<uint32_t>(enabled.size()); info.ppEnabledExtensionNames = enabled.data();
    if (validationEnabled) { info.enabledLayerCount = 1; info.ppEnabledLayerNames = &validation; }
    if (debugUtils) info.pNext = &debug;
    check(vkCreateInstance(&info, nullptr, &instance_), "vkCreateInstance");
    if (debugUtils) {
        auto create = reinterpret_cast<PFN_vkCreateDebugUtilsMessengerEXT>(vkGetInstanceProcAddr(instance_, "vkCreateDebugUtilsMessengerEXT"));
        if (create) check(create(instance_, &debug, nullptr, &messenger_), "validation messenger");
    }
    log_message("VULKAN", "Compatibility baseline: Vulkan 1.1");
    if (!SDL_Vulkan_CreateSurface(window_, instance_, nullptr, &surface_)) throw std::runtime_error(SDL_GetError());
    log_message("VULKAN", "Android surface created");
    check(vkEnumeratePhysicalDevices(instance_, &count, nullptr), "physical devices");
    std::vector<VkPhysicalDevice> devices(count);
    check(vkEnumeratePhysicalDevices(instance_, &count, devices.data()), "physical devices");
    for (auto candidate : devices) {
        VkPhysicalDeviceProperties p{}; vkGetPhysicalDeviceProperties(candidate, &p);
        log_message("GPU", "Candidate: %s API %u.%u.%u vendor=0x%x device=0x%x driver=%u", p.deviceName,
            VK_VERSION_MAJOR(p.apiVersion), VK_VERSION_MINOR(p.apiVersion), VK_VERSION_PATCH(p.apiVersion), p.vendorID, p.deviceID, p.driverVersion);
        if (p.apiVersion < VK_API_VERSION_1_1) continue;
        uint32_t n = 0;
        check(vkEnumerateDeviceExtensionProperties(candidate, nullptr, &n, nullptr), "device extensions");
        std::vector<VkExtensionProperties> ext(n);
        check(vkEnumerateDeviceExtensionProperties(candidate, nullptr, &n, ext.data()), "device extensions");
        for (const auto& e : ext) log_message("GPU", "Extension: %s", e.extensionName);
        if (!has(ext, VK_KHR_SWAPCHAIN_EXTENSION_NAME)) continue;
        vkGetPhysicalDeviceQueueFamilyProperties(candidate, &n, nullptr);
        std::vector<VkQueueFamilyProperties> queues(n); vkGetPhysicalDeviceQueueFamilyProperties(candidate, &n, queues.data());
        uint32_t g = UINT32_MAX, pr = UINT32_MAX;
        for (uint32_t i = 0; i < n; ++i) {
            VkBool32 supported = VK_FALSE;
            check(vkGetPhysicalDeviceSurfaceSupportKHR(candidate, i, surface_, &supported), "present support");
            log_message("GPU", "Queue %u flags=0x%x count=%u present=%u", i, queues[i].queueFlags, queues[i].queueCount, supported);
            if (queues[i].queueCount && (queues[i].queueFlags & VK_QUEUE_GRAPHICS_BIT)) g = i;
            if (queues[i].queueCount && supported) pr = i;
            if (queues[i].queueCount && supported && (queues[i].queueFlags & VK_QUEUE_GRAPHICS_BIT)) { g = pr = i; break; }
        }
        uint32_t formats = 0, modes = 0;
        check(vkGetPhysicalDeviceSurfaceFormatsKHR(candidate, surface_, &formats, nullptr), "surface formats");
        check(vkGetPhysicalDeviceSurfacePresentModesKHR(candidate, surface_, &modes, nullptr), "present modes");
        VkSurfaceCapabilitiesKHR caps{};
        check(vkGetPhysicalDeviceSurfaceCapabilitiesKHR(candidate, surface_, &caps), "surface capabilities");
        if (g == UINT32_MAX || pr == UINT32_MAX || !formats || !modes || !(caps.supportedUsageFlags & VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT)) continue;
        gpu_ = candidate; graphicsFamily_ = g; presentFamily_ = pr;
        (void)identify_gpu_quirks(p);
        log_message("VULKAN", "GPU selected: %s; graphics queue=%u; present queue=%u", p.deviceName, g, pr);
        log_message("GPU", "Limits: maxImageDimension2D=%u maxMemoryAllocationCount=%u maxUniformBufferRange=%u nonCoherentAtomSize=%llu",
            p.limits.maxImageDimension2D, p.limits.maxMemoryAllocationCount, p.limits.maxUniformBufferRange,
            static_cast<unsigned long long>(p.limits.nonCoherentAtomSize));
        VkPhysicalDeviceMemoryProperties memory{}; vkGetPhysicalDeviceMemoryProperties(gpu_, &memory);
        for (uint32_t i = 0; i < memory.memoryHeapCount; ++i) log_message("MEMORY", "Heap %u bytes=%llu flags=0x%x", i, static_cast<unsigned long long>(memory.memoryHeaps[i].size), memory.memoryHeaps[i].flags);
        for (uint32_t i = 0; i < memory.memoryTypeCount; ++i) log_message("MEMORY", "Type %u heap=%u flags=0x%x", i, memory.memoryTypes[i].heapIndex, memory.memoryTypes[i].propertyFlags);
        for (auto format : {VK_FORMAT_R8G8B8A8_UNORM, VK_FORMAT_B8G8R8A8_UNORM, VK_FORMAT_D16_UNORM, VK_FORMAT_D24_UNORM_S8_UINT, VK_FORMAT_D32_SFLOAT, VK_FORMAT_D32_SFLOAT_S8_UINT}) {
            VkFormatProperties fp{}; vkGetPhysicalDeviceFormatProperties(gpu_, format, &fp);
            log_message("GPU", "Format %d linear=0x%x optimal=0x%x buffer=0x%x", format, fp.linearTilingFeatures, fp.optimalTilingFeatures, fp.bufferFeatures);
        }
        break;
    }
    if (!gpu_) throw std::runtime_error("No Vulkan 1.1 device with graphics, presentation and swapchain support.");
    float priority = 1.0f;
    VkDeviceQueueCreateInfo q{VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO}; q.queueCount = 1; q.pQueuePriorities = &priority;
    q.queueFamilyIndex = graphicsFamily_;
    std::vector<VkDeviceQueueCreateInfo> queues{q};
    if (presentFamily_ != graphicsFamily_) { q.queueFamilyIndex = presentFamily_; queues.push_back(q); }
    const char* extension = VK_KHR_SWAPCHAIN_EXTENSION_NAME;
    VkDeviceCreateInfo deviceInfo{VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO};
    deviceInfo.queueCreateInfoCount = static_cast<uint32_t>(queues.size()); deviceInfo.pQueueCreateInfos = queues.data();
    deviceInfo.enabledExtensionCount = 1; deviceInfo.ppEnabledExtensionNames = &extension;
    check(vkCreateDevice(gpu_, &deviceInfo, nullptr, &device_), "vkCreateDevice");
    vkGetDeviceQueue(device_, graphicsFamily_, 0, &graphics_); vkGetDeviceQueue(device_, presentFamily_, 0, &present_);
    VkCommandPoolCreateInfo poolInfo{VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO};
    poolInfo.queueFamilyIndex = graphicsFamily_; poolInfo.flags = VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT;
    check(vkCreateCommandPool(device_, &poolInfo, nullptr, &pool_), "command pool");
    VkSemaphoreCreateInfo si{VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO};
    check(vkCreateSemaphore(device_, &si, nullptr, &acquired_), "acquire semaphore");
    VkFenceCreateInfo fi{VK_STRUCTURE_TYPE_FENCE_CREATE_INFO}; fi.flags = VK_FENCE_CREATE_SIGNALED_BIT;
    check(vkCreateFence(device_, &fi, nullptr, &fence_), "fence");
    create_swapchain();
}
void VulkanContext::create_swapchain() {
    VkSurfaceCapabilitiesKHR caps{}; check(vkGetPhysicalDeviceSurfaceCapabilitiesKHR(gpu_, surface_, &caps), "surface caps");
    int width = 0, height = 0; SDL_GetWindowSizeInPixels(window_, &width, &height);
    if (width <= 0 || height <= 0) return;
    extent_ = caps.currentExtent;
    if (extent_.width == UINT32_MAX) {
        extent_.width = std::clamp(static_cast<uint32_t>(width), caps.minImageExtent.width, caps.maxImageExtent.width);
        extent_.height = std::clamp(static_cast<uint32_t>(height), caps.minImageExtent.height, caps.maxImageExtent.height);
    }
    if (!extent_.width || !extent_.height) return;
    uint32_t count = 0; check(vkGetPhysicalDeviceSurfaceFormatsKHR(gpu_, surface_, &count, nullptr), "formats");
    std::vector<VkSurfaceFormatKHR> formats(count); check(vkGetPhysicalDeviceSurfaceFormatsKHR(gpu_, surface_, &count, formats.data()), "formats");
    if (formats.empty()) throw std::runtime_error("No surface formats");
    auto chosen = formats.front();
    if (formats.size() == 1 && chosen.format == VK_FORMAT_UNDEFINED) chosen.format = VK_FORMAT_B8G8R8A8_UNORM;
    for (auto f : formats) if (f.format == VK_FORMAT_B8G8R8A8_UNORM && f.colorSpace == VK_COLOR_SPACE_SRGB_NONLINEAR_KHR) chosen = f;
    VkFormatProperties fp{}; vkGetPhysicalDeviceFormatProperties(gpu_, chosen.format, &fp);
    if (!(fp.optimalTilingFeatures & VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT)) throw std::runtime_error("Surface format lacks color attachment support");
    uint32_t images = caps.minImageCount + 1;
    if (caps.maxImageCount) images = std::min(images, caps.maxImageCount);
    VkCompositeAlphaFlagBitsKHR alpha = VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR;
    for (auto a : {VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR, VK_COMPOSITE_ALPHA_PRE_MULTIPLIED_BIT_KHR, VK_COMPOSITE_ALPHA_POST_MULTIPLIED_BIT_KHR, VK_COMPOSITE_ALPHA_INHERIT_BIT_KHR}) {
        if (caps.supportedCompositeAlpha & static_cast<VkCompositeAlphaFlagsKHR>(a)) { alpha = a; break; }
    }
    uint32_t families[] = {graphicsFamily_, presentFamily_};
    VkSwapchainCreateInfoKHR sc{VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR};
    sc.surface = surface_; sc.minImageCount = images; sc.imageFormat = chosen.format; sc.imageColorSpace = chosen.colorSpace;
    sc.imageExtent = extent_; sc.imageArrayLayers = 1; sc.imageUsage = VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT;
    sc.imageSharingMode = graphicsFamily_ == presentFamily_ ? VK_SHARING_MODE_EXCLUSIVE : VK_SHARING_MODE_CONCURRENT;
    if (sc.imageSharingMode == VK_SHARING_MODE_CONCURRENT) { sc.queueFamilyIndexCount = 2; sc.pQueueFamilyIndices = families; }
    sc.preTransform = caps.currentTransform; sc.compositeAlpha = alpha; sc.presentMode = VK_PRESENT_MODE_FIFO_KHR; sc.clipped = VK_TRUE;
    check(vkCreateSwapchainKHR(device_, &sc, nullptr, &swapchain_), "swapchain");
    check(vkGetSwapchainImagesKHR(device_, swapchain_, &count, nullptr), "swapchain images");
    std::vector<VkImage> handles(count); check(vkGetSwapchainImagesKHR(device_, swapchain_, &count, handles.data()), "swapchain images");
    VkAttachmentDescription attachment{};
    attachment.format = chosen.format; attachment.samples = VK_SAMPLE_COUNT_1_BIT;
    attachment.loadOp = VK_ATTACHMENT_LOAD_OP_CLEAR; attachment.storeOp = VK_ATTACHMENT_STORE_OP_STORE;
    attachment.stencilLoadOp = VK_ATTACHMENT_LOAD_OP_DONT_CARE; attachment.stencilStoreOp = VK_ATTACHMENT_STORE_OP_DONT_CARE;
    attachment.initialLayout = VK_IMAGE_LAYOUT_UNDEFINED; attachment.finalLayout = VK_IMAGE_LAYOUT_PRESENT_SRC_KHR;
    VkAttachmentReference ref{0, VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL};
    VkSubpassDescription sub{}; sub.pipelineBindPoint = VK_PIPELINE_BIND_POINT_GRAPHICS; sub.colorAttachmentCount = 1; sub.pColorAttachments = &ref;
    VkSubpassDependency dep{}; dep.srcSubpass = VK_SUBPASS_EXTERNAL; dep.dstSubpass = 0;
    dep.srcStageMask = VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT; dep.dstStageMask = VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT;
    dep.dstAccessMask = VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT;
    VkRenderPassCreateInfo rp{VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO};
    rp.attachmentCount = 1; rp.pAttachments = &attachment; rp.subpassCount = 1; rp.pSubpasses = &sub; rp.dependencyCount = 1; rp.pDependencies = &dep;
    check(vkCreateRenderPass(device_, &rp, nullptr, &renderPass_), "render pass");
    for (auto image : handles) {
        VkSemaphoreCreateInfo si{VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO};
        VkSemaphore semaphore = VK_NULL_HANDLE;
        check(vkCreateSemaphore(device_, &si, nullptr, &semaphore), "render semaphore");
        rendered_.push_back(semaphore);
        VkImageViewCreateInfo vi{VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO}; vi.image = image; vi.viewType = VK_IMAGE_VIEW_TYPE_2D; vi.format = chosen.format;
        vi.subresourceRange.aspectMask = VK_IMAGE_ASPECT_COLOR_BIT; vi.subresourceRange.levelCount = 1; vi.subresourceRange.layerCount = 1;
        VkImageView view = VK_NULL_HANDLE; check(vkCreateImageView(device_, &vi, nullptr, &view), "image view"); views_.push_back(view);
        VkFramebufferCreateInfo fb{VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO}; fb.renderPass = renderPass_; fb.attachmentCount = 1; fb.pAttachments = &views_.back();
        fb.width = extent_.width; fb.height = extent_.height; fb.layers = 1;
        VkFramebuffer frame = VK_NULL_HANDLE; check(vkCreateFramebuffer(device_, &fb, nullptr, &frame), "framebuffer"); frames_.push_back(frame);
    }
    commands_.resize(count);
    VkCommandBufferAllocateInfo ai{VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO}; ai.commandPool = pool_; ai.level = VK_COMMAND_BUFFER_LEVEL_PRIMARY; ai.commandBufferCount = count;
    check(vkAllocateCommandBuffers(device_, &ai, commands_.data()), "command buffers");
    for (uint32_t i = 0; i < count; ++i) {
        VkCommandBufferBeginInfo begin{VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO}; check(vkBeginCommandBuffer(commands_[i], &begin), "begin command buffer");
        VkClearValue clear{}; clear.color = {{0.12f, 0.28f, 0.55f, 1.0f}};
        VkRenderPassBeginInfo pass{VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO}; pass.renderPass = renderPass_; pass.framebuffer = frames_[i]; pass.renderArea.extent = extent_; pass.clearValueCount = 1; pass.pClearValues = &clear;
        vkCmdBeginRenderPass(commands_[i], &pass, VK_SUBPASS_CONTENTS_INLINE); vkCmdEndRenderPass(commands_[i]);
        check(vkEndCommandBuffer(commands_[i]), "end command buffer");
    }
    log_message("VULKAN", "Swapchain created: %ux%u images=%u format=%d colorspace=%d FIFO transform=0x%x", extent_.width, extent_.height, count, chosen.format, chosen.colorSpace, caps.currentTransform);
    log_message("VULKAN", "Rendering initialized");
}
bool VulkanContext::draw() {
    if (!swapchain_) { create_swapchain(); if (!swapchain_) return false; }
    check(vkWaitForFences(device_, 1, &fence_, VK_TRUE, UINT64_MAX), "wait fence");
    uint32_t image = 0;
    VkResult acquired = vkAcquireNextImageKHR(device_, swapchain_, 1000000000, acquired_, VK_NULL_HANDLE, &image);
    if (acquired == VK_TIMEOUT || acquired == VK_NOT_READY) return false;
    if (acquired == VK_ERROR_SURFACE_LOST_KHR) { cleanup(); initialize(); return false; }
    if (acquired == VK_ERROR_OUT_OF_DATE_KHR) { recreate(); return false; }
    if (acquired != VK_SUBOPTIMAL_KHR) check(acquired, "acquire image");
    check(vkResetFences(device_, 1, &fence_), "reset fence");
    VkPipelineStageFlags stage = VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT;
    VkSubmitInfo submit{VK_STRUCTURE_TYPE_SUBMIT_INFO}; submit.waitSemaphoreCount = 1; submit.pWaitSemaphores = &acquired_; submit.pWaitDstStageMask = &stage;
    submit.commandBufferCount = 1; submit.pCommandBuffers = &commands_[image]; submit.signalSemaphoreCount = 1; submit.pSignalSemaphores = &rendered_[image];
    check(vkQueueSubmit(graphics_, 1, &submit, fence_), "queue submit");
    VkPresentInfoKHR present{VK_STRUCTURE_TYPE_PRESENT_INFO_KHR}; present.waitSemaphoreCount = 1; present.pWaitSemaphores = &rendered_[image];
    present.swapchainCount = 1; present.pSwapchains = &swapchain_; present.pImageIndices = &image;
    VkResult result = vkQueuePresentKHR(present_, &present);
    // Present semaphores are indexed by acquired image, as required for safe reuse.
    check(vkQueueWaitIdle(present_), "present queue idle");
    if (result == VK_ERROR_SURFACE_LOST_KHR) { cleanup(); initialize(); return false; }
    if (result == VK_ERROR_OUT_OF_DATE_KHR || result == VK_SUBOPTIMAL_KHR || acquired == VK_SUBOPTIMAL_KHR) recreate();
    else check(result, "queue present");
    if (++presented_ == 1 || presented_ % 300 == 0) log_message("VULKAN", "Frames presented: %llu", static_cast<unsigned long long>(presented_));
    return true;
}
void VulkanContext::destroy_swapchain() {
    if (!device_) return;
    if (!commands_.empty()) vkFreeCommandBuffers(device_, pool_, static_cast<uint32_t>(commands_.size()), commands_.data()); commands_.clear();
    for (auto f : frames_) vkDestroyFramebuffer(device_, f, nullptr); frames_.clear();
    for (auto v : views_) vkDestroyImageView(device_, v, nullptr); views_.clear();
    for (auto s : rendered_) vkDestroySemaphore(device_, s, nullptr); rendered_.clear();
    if (renderPass_) vkDestroyRenderPass(device_, renderPass_, nullptr); renderPass_ = VK_NULL_HANDLE;
    if (swapchain_) vkDestroySwapchainKHR(device_, swapchain_, nullptr); swapchain_ = VK_NULL_HANDLE;
}
void VulkanContext::recreate() { check(vkDeviceWaitIdle(device_), "device idle"); destroy_swapchain(); create_swapchain(); }
void VulkanContext::cleanup() {
    if (device_) {
        vkDeviceWaitIdle(device_); destroy_swapchain();
        if (fence_) vkDestroyFence(device_, fence_, nullptr);
        if (acquired_) vkDestroySemaphore(device_, acquired_, nullptr);
        if (pool_) vkDestroyCommandPool(device_, pool_, nullptr);
        vkDestroyDevice(device_, nullptr);
    }
    device_ = VK_NULL_HANDLE; fence_ = VK_NULL_HANDLE; acquired_ = VK_NULL_HANDLE; pool_ = VK_NULL_HANDLE;
    if (surface_) vkDestroySurfaceKHR(instance_, surface_, nullptr); surface_ = VK_NULL_HANDLE;
    if (messenger_) {
        auto destroy = reinterpret_cast<PFN_vkDestroyDebugUtilsMessengerEXT>(vkGetInstanceProcAddr(instance_, "vkDestroyDebugUtilsMessengerEXT"));
        if (destroy) destroy(instance_, messenger_, nullptr);
        messenger_ = VK_NULL_HANDLE;
    }
    if (instance_) vkDestroyInstance(instance_, nullptr); instance_ = VK_NULL_HANDLE;
    gpu_ = VK_NULL_HANDLE; SDL_Vulkan_UnloadLibrary();
}

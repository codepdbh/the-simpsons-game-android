#pragma once
#include <SDL3/SDL.h>
#include <vulkan/vulkan.h>
#include <vector>
class VulkanContext {
public:
    explicit VulkanContext(SDL_Window* window);
    ~VulkanContext();
    VulkanContext(const VulkanContext&) = delete;
    VulkanContext& operator=(const VulkanContext&) = delete;
    bool draw();
    void recreate();
private:
    void initialize();
    void create_swapchain();
    void destroy_swapchain();
    void cleanup();
    SDL_Window* window_;
    VkInstance instance_ = VK_NULL_HANDLE;
    VkDebugUtilsMessengerEXT messenger_ = VK_NULL_HANDLE;
    VkSurfaceKHR surface_ = VK_NULL_HANDLE;
    VkPhysicalDevice gpu_ = VK_NULL_HANDLE;
    VkDevice device_ = VK_NULL_HANDLE;
    VkQueue graphics_ = VK_NULL_HANDLE, present_ = VK_NULL_HANDLE;
    uint32_t graphicsFamily_ = 0, presentFamily_ = 0;
    VkSwapchainKHR swapchain_ = VK_NULL_HANDLE;
    VkExtent2D extent_{};
    VkRenderPass renderPass_ = VK_NULL_HANDLE;
    VkCommandPool pool_ = VK_NULL_HANDLE;
    VkSemaphore acquired_ = VK_NULL_HANDLE;
    std::vector<VkSemaphore> rendered_;
    VkFence fence_ = VK_NULL_HANDLE;
    std::vector<VkImageView> views_;
    std::vector<VkFramebuffer> frames_;
    std::vector<VkCommandBuffer> commands_;
    uint64_t presented_ = 0;
};

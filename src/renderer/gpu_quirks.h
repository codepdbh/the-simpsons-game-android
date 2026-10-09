#pragma once
#include <vulkan/vulkan.h>
struct GPUQuirks { bool forceSafeResolvePath = false; };
GPUQuirks identify_gpu_quirks(const VkPhysicalDeviceProperties& properties);

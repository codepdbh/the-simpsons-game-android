#include "gpu_quirks.h"
GPUQuirks identify_gpu_quirks(const VkPhysicalDeviceProperties&) {
    // No evidence-based device workarounds are needed by the clear-screen renderer.
    return {};
}

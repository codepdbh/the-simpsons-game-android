#include <SDL3/SDL.h>
#include <SDL3/SDL_main.h>
#include <sys/system_properties.h>
#include <unistd.h>
#include <chrono>
#include <exception>
#include <memory>
#include <stdexcept>
#include <thread>
#include "android_logging.h"
#include "vulkan_context.h"
namespace {
struct Lifecycle {
    std::unique_ptr<VulkanContext>* renderer;
    bool paused = false;
    bool resume = false;
};
bool SDLCALL lifecycle_watch(void* userdata, SDL_Event* event) {
    auto& state = *static_cast<Lifecycle*>(userdata);
    // SDL Android lifecycle events are synchronous watcher events, not poll events.
    if (event->type == SDL_EVENT_WILL_ENTER_BACKGROUND) {
        state.renderer->reset(); state.paused = true;
        log_message("ANDROID", "Paused; Vulkan surface released");
    } else if (event->type == SDL_EVENT_DID_ENTER_FOREGROUND) {
        state.paused = false; state.resume = true;
    }
    return true;
}
}
int main(int, char**) {
    if (!SDL_Init(SDL_INIT_VIDEO | SDL_INIT_GAMEPAD)) return 1;
    open_log();
    log_message("ANDROID", "Starting The Simpsons Game Android; ABI: arm64-v8a; host page size=%ld", sysconf(_SC_PAGESIZE));
    for (const char* name : {"ro.build.version.release", "ro.product.model", "ro.soc.model", "ro.product.cpu.abi"}) {
        char value[PROP_VALUE_MAX]{}; __system_property_get(name, value); log_message("ANDROID", "%s: %s", name, value);
    }
    SDL_Window* window = SDL_CreateWindow("The Simpsons Game — Vulkan 1.1 test", 1280, 720, SDL_WINDOW_VULKAN | SDL_WINDOW_FULLSCREEN);
    int code = 0;
    try {
        if (!window) throw std::runtime_error(SDL_GetError());
        auto renderer = std::make_unique<VulkanContext>(window);
        Lifecycle lifecycle{&renderer};
        // Unregister with the same userdata before renderer/lifecycle destruction.
        struct Watch {
            Lifecycle* state;
            ~Watch() { SDL_RemoveEventWatch(lifecycle_watch, state); }
        } watch{&lifecycle};
        SDL_AddEventWatch(lifecycle_watch, &lifecycle);
        bool running = true;
        log_message("ANDROID", "Application running; target 30 FPS; game runtime NOT integrated");
        while (running) {
            const auto start = std::chrono::steady_clock::now();
            SDL_Event event;
            auto handle = [&](const SDL_Event& e) {
                switch (e.type) {
                case SDL_EVENT_QUIT: running = false; break;
                case SDL_EVENT_WINDOW_PIXEL_SIZE_CHANGED:
                    if (renderer && !lifecycle.paused) renderer->recreate(); break;
                case SDL_EVENT_GAMEPAD_ADDED:
                    log_message("INPUT", "Gamepad available: %u", e.gdevice.which); break;
                default: break;
                }
            };
            if (lifecycle.paused) { if (SDL_WaitEventTimeout(&event, 100)) handle(event); }
            while (SDL_PollEvent(&event)) handle(event);
            if (lifecycle.resume && running) {
                if (!renderer) renderer = std::make_unique<VulkanContext>(window);
                lifecycle.resume = false; log_message("ANDROID", "Resumed; Vulkan surface recreated");
            }
            if (running && !lifecycle.paused && renderer) renderer->draw();
            std::this_thread::sleep_until(start + std::chrono::nanoseconds(33333333));
        }
    } catch (const std::exception& e) {
        log_message("VULKAN", "ERROR: %s", e.what());
        SDL_ShowSimpleMessageBox(SDL_MESSAGEBOX_ERROR, "The Simpsons Game Android", e.what(), window); code = 1;
    }
    if (window) SDL_DestroyWindow(window); close_log(); SDL_Quit(); return code;
}

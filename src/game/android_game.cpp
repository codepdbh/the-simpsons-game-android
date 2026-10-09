#include "generated/default/simpsons_init.h"
#include <rex/rex_app.h>
#include <rex/runtime.h>
#include <SDL3/SDL.h>
#include <SDL3/SDL_main.h>
#include <rex/cvar.h>
#include <rex/logging.h>
#include <rex/ui/windowed_app_context_sdl.h>
#include <android/log.h>
#include <filesystem>
#include <map>

void ApplyInkOutlineOptions(rex::memory::Memory* memory);
void ApplyEyeShadingOptions(rex::memory::Memory* memory);
void ApplyPhysicsStepOptions(rex::memory::Memory* memory);
class AndroidSimpsonsApp : public rex::ReXApp {
public:
    AndroidSimpsonsApp(rex::ui::WindowedAppContext& context) : rex::ReXApp(context, "simpsons", PPCImageConfig) {}
protected:
    void OnConfigurePaths(rex::PathConfig& paths) override {
        const auto root = std::filesystem::path(SDL_GetAndroidInternalStoragePath());
        paths.game_data_root = root / "game";
        paths.user_data_root = root / "user";
        paths.cache_root = root / "cache";
        paths.config_path = root / "simpsons.toml";
    }
    void OnPreSetup(rex::RuntimeConfig& config) override {
        config.gpu_plugin = "xenos";
        __android_log_print(ANDROID_LOG_INFO, "TSGAndroid", "[REX] Constructing runtime with statically recompiled Simpsons functions");
    }
    void OnPostLoadXexImage() override {
        ApplyInkOutlineOptions(runtime()->memory());
        ApplyEyeShadingOptions(runtime()->memory());
        ApplyPhysicsStepOptions(runtime()->memory());
        REXLOG_INFO("[GAME] XEX loaded; static PPC mapping installed");
    }
};
int main(int argc, char** argv) {
    try {
    rex::cvar::Init(argc, argv);
    rex::cvar::ApplyEnvironment();
    rex::InitLoggingEarly();
    rex::ui::SDLWindowedAppContext context;
    if (!context.Initialize()) return 1;
    AndroidSimpsonsApp app(context);
    app.SetParsedArguments({});
    const bool initialized = static_cast<rex::ui::WindowedApp&>(app).OnInitialize();
    const int result = initialized ? context.RunMainMessageLoop() : 1;
    app.InvokeOnDestroy();
    return result;
    } catch (const std::exception& error) {
        __android_log_print(ANDROID_LOG_ERROR, "TSGAndroid", "[REX] Initialization failed: %s", error.what());
        return 1;
    }
}

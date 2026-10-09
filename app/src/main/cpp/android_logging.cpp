#include "android_logging.h"
#include <SDL3/SDL.h>
#include <android/log.h>
#include <cstdarg>
#include <cstdio>
#include <mutex>
#include <string>
namespace { FILE* file = nullptr; std::mutex mutex; }
void open_log() {
    const char* path = SDL_GetAndroidInternalStoragePath();
    if (path) file = fopen((std::string(path) + "/game.log").c_str(), "a");
}
void close_log() { std::lock_guard lock(mutex); if (file) fclose(file); file = nullptr; }
void log_message(const char* category, const char* format, ...) {
    char buffer[2048]; va_list args; va_start(args, format);
    vsnprintf(buffer, sizeof(buffer), format, args); va_end(args);
    std::lock_guard lock(mutex);
    __android_log_print(ANDROID_LOG_INFO, "TSGAndroid", "[%s] %s", category, buffer);
    if (file) { fprintf(file, "[%s] %s\n", category, buffer); fflush(file); }
}

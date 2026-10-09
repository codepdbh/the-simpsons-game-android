cmake_minimum_required(VERSION 3.25)
project(the_simpsons_game_android LANGUAGES C CXX)
if(NOT ANDROID OR NOT ANDROID_ABI STREQUAL "arm64-v8a")
    message(FATAL_ERROR "This milestone requires Android NDK arm64-v8a")
endif()
set(SDL_SHARED ON CACHE BOOL "" FORCE)
set(SDL_STATIC OFF CACHE BOOL "" FORCE)
set(SDL_TEST_LIBRARY OFF CACHE BOOL "" FORCE)
add_subdirectory(extern/SDL EXCLUDE_FROM_ALL)
add_library(main SHARED app/src/main/cpp/android_main.cpp
    app/src/main/cpp/android_logging.cpp src/renderer/vulkan_context.cpp
    src/renderer/gpu_quirks.cpp)
target_compile_features(main PRIVATE cxx_std_20)
target_compile_options(main PRIVATE -Wall -Wextra -Wconversion -Wsign-conversion
    -Wno-missing-field-initializers -Werror)
target_include_directories(main PRIVATE app/src/main/cpp src/renderer)
target_link_libraries(main PRIVATE SDL3::SDL3 vulkan android log)

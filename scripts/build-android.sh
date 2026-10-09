#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
export ANDROID_HOME="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Android/Sdk}}"
test -d "$ANDROID_HOME/ndk/28.2.13676358" || { echo 'Install Android NDK 28.2.13676358'; exit 1; }
if [ ! -f extern/SDL/CMakeLists.txt ]; then
    git clone --depth 1 --branch release-3.2.28 https://github.com/libsdl-org/SDL.git extern/SDL
fi
bash ./gradlew "assemble${1:-Debug}"

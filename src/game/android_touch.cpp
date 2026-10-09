#include <jni.h>
#include <rex/input/sdl/sdl_input_driver.h>
extern "C" JNIEXPORT void JNICALL
Java_org_tsg_android_TouchControlsView_nativeSetTouchState(JNIEnv*, jclass, jint buttons,
    jint lx, jint ly, jint rx, jint ry, jint lt, jint rt) {
  rex_sdl_set_touch_gamepad_state(static_cast<uint16_t>(buttons),
      static_cast<int16_t>(lx), static_cast<int16_t>(ly), static_cast<int16_t>(rx),
      static_cast<int16_t>(ry), static_cast<uint8_t>(lt), static_cast<uint8_t>(rt));
}

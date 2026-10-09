# Dependencies

- SDL3 3.2.28, https://github.com/libsdl-org/SDL,
  commit 7f3ae3d57459e59943a4ecfefc8f6277ec6bf540, zlib license in extern/SDL/LICENSE.txt.
- Gradle wrapper from SDL android-project, Apache-2.0 (Gradle).
- Optional Debug Vulkan validation layer, KhronosGroup/Vulkan-ValidationLayers
  vulkan-sdk-1.4.363.0; Apache-2.0, upstream release archive/source licenses.
- Android Vulkan loader supplied by the device, not distributed here.

The default diagnostic build does not package ReXGlue or FFmpeg. The optional
experimental game build uses TheSimpsonsGameRecomp (GPL-3.0, license in
extern/TheSimpsonsGameRecomp/LICENSE) and the Android ReXGlue SDK fork from
extern/android-reference (see its LICENSE and THIRD_PARTY_NOTICES.md).
Its SDK dependency sources are fetched at the pinned ReXGlue 0.10 revision by
tools/fetch_thirdparty.py. These include FFmpeg, imgui, Vulkan tooling and SIMD
support; their upstream notices and licenses remain in the source checkout.
No Xbox executable, game assets or additional GPU driver binaries are included.

The Android driver ZIP importer and its host tests are adapted from
codepdbh/nfsmw-android (GPL-3.0). Their original source is available in
app/src/main/java/com/nfsmw/android and tools/tests of that project. The launcher
layout follows the same project, with Simpsons-specific options and artwork.

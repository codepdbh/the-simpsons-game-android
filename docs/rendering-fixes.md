# Rendering fixes / Correcciones del renderer

## Empty optional streams / Flujos opcionales vacíos

Device logs from 0.3.1 show more than 10,000 character draw failures referring to
vertex fetch slot 89 with descriptor words `00000001 00000000`. Offline inspection
of six affected vertex shaders confirms a zero-stride fetch from that slot.

The newer Simpsons SDK source includes a fix for absent blend-shape streams.
This Android backport combines both required parts:

- SPIR-V fetches with size zero return zero words through a control-flow branch,
  without loading unbacked shared memory at address zero. Existing format decode,
  endian conversion and destination swizzles still apply.
- The Vulkan CPU validation admits an empty invalid-vertex descriptor only when
  no used binding on that slot has a nonzero stride. Missing regular vertex
  streams and other invalid descriptor types remain rejected. The broad
  `gpu_allow_invalid_fetch_constants` bypass stays disabled.

Shader metadata also accepts legal zero strides, fixing the assertion that closed
the game after the introduction. Mini-fetch metadata uses the inherited full
fetch slot, matching the parser and the actual shader address calculation.

Los logs de 0.3.1 muestran más de 10.000 dibujos descartados por la constante de
vértices 89 vacía. Se adapta la corrección del SDK de Simpsons más reciente:
devolver ceros para flujos opcionales ausentes sin leer memoria sin respaldo y
permitir esos dibujos. Los buffers principales inválidos siguen rechazándose;
no se activa el bypass general de validación.

Aceptar stride cero en los metadatos también corrige el assert que cerraba el
juego tras la introducción. Los mini-fetches heredan correctamente la referencia
del fetch completo anterior.

The patch is tracked in `patches/android-sdk.patch`. No game shaders or game
assets are committed as test fixtures.

### Regression check

`tsg_shader_metadata_test` is an explicit CMake executable target, excluded from
normal APK builds. It uses synthetic microcode to check zero-stride full fetches,
mini-fetch inheritance despite a different encoded raw slot, distinct strides
on the same slot, and the resulting residency bitmap. It passed on the ARM64
Redmi Note 8 on 2026-10-20. This is a CPU shader-metadata check, not a GPU or
gameplay compatibility result. Characters are visible in the tested S25 Ultra
chocolate level, but a black right arm and intermittent corrupted menu geometry
remain under investigation.

La prueba sintética pasó en ARM64 (Redmi Note 8). Verifica los metadatos del
shader, no el dibujo de personajes ni la compatibilidad de gameplay. La prueba
visual en el S25 mostró personajes visibles, pero también el brazo derecho negro
y corrupción intermitente de geometría del menú.

## ARM64 ordering and vertex residency (0.3.3)

Android restores 71 generated PPC `sync`, `lwsync` and `eieio` barriers in five
build-local source copies. Upstream generated files are preserved. Two regression
tests cover instruction matching, CRLF, source preservation, incremental writes
and removal from the manifest. NDK disassembly confirms ARM64 `dmb` instructions.
The black arm remained in the barrier-only test: ordering alone is not a fix.

Vulkan's vertex residency shortcut now resets on swap, GPU-memory invalidation
and fetch-register invalidation. Previously, the frame boundary invalidated
CPU-uploaded shared-memory pages, but unchanged buffer addresses and sizes could
bypass `RequestRange`, retaining stale dynamic vertices. Shared memory still
decides which pages need upload; unchanged pages within the frame remain cached.

New installations default to 30 FPS; existing FPS selections are preserved.
60 FPS remains selectable. Eye shading, outlines and conservative resolve can
be selected independently for comparisons and are included in reports. These
settings are not a confirmed black-arm workaround. First-use pipeline compilation
can still cause pauses.

Se restauran las barreras de memoria PPC para ARM64 y se corrige la invalidación
de vértices reutilizados. Las nuevas instalaciones usan 30 FPS por defecto;
se conserva la selección existente y la opción de 60 FPS. El cambio de barreras
solo no corrigió el brazo negro; la validación visual continúa por separado.

Latest S25 test: characters are visible, but the black right arm still appears
after both corrections. Recent menu captures are clean, without proving all
flicker resolved. Stable gameplay scheduler windows measured 29.8?29.9 Hz
(~33.5 ms); later samples still contain 79?140 ms spikes. These are host
scheduler measurements, not presented-frame FPS. Work paused at the user?s
request; APK 0.3.3 builds and its debug signature verifies. The phone currently
has the intermediate 0.3.2-labelled test build with the same native corrections.

El brazo negro sigue pendiente tras las dos correcciones. Se observaron ventanas
estables cercanas a 30 Hz, pero contin?an picos ocasionales. El trabajo se detuvo
a petici?n del usuario; no se publica una release como correcci?n definitiva.

## GitHub reports / Informes en GitHub

The launcher action **Reportar problema en GitHub** prepares a bounded ZIP:
device/build/settings summary, the latest three runtime logs (last 1 MiB each),
available app-native crash logs, and app process-exit information/traces on
Android 11+. Game files, save data and folder grants are excluded.

The user chooses where to save the ZIP. The launcher opens a prefilled GitHub
issue; attach the saved ZIP and publish from the browser. No token is embedded,
and the launcher does not silently upload files or publish an issue.

La opción del launcher prepara un ZIP limitado con datos del dispositivo y los
últimos logs. Se guarda mediante el selector del sistema y se abre un issue
preparado en GitHub. Adjunta el ZIP guardado y publica desde el navegador.

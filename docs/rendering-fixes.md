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
gameplay compatibility result. Visual character/flicker verification is pending
on the Galaxy S25 Ultra where the original failure was recorded.

La prueba sintética pasó en ARM64 (Redmi Note 8). Verifica los metadatos del
shader, no el dibujo de personajes ni la compatibilidad de gameplay. La prueba
visual sigue pendiente en el S25 Ultra del fallo original.

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

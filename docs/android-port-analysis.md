# Auditoría para Android — 2026-10-09

## Alcance y fuentes fijadas

Inspección de código, no prueba del runtime. Milestone 1 no enlaza ni ejecuta el juego.

| Fuente | Commit inspeccionado |
|---|---|
| https://github.com/YesterMester/TheSimpsonsGameRecomp | a13469eae45ce3b7326e28706775315324529515 |
| https://github.com/rexglue/rexglue-sdk | c94f5ebdcb3c9d1a460ca48e04f9758448f8d518 |
| https://github.com/R-drg/the-simpsons-game-nx | 3cb2cdf7fa1665a1f2d653aa7502fe4baa03680f |

Los checkouts están en `../upstream`. La base PC contiene su propio SDK en
`tools/rexglue-sdk`; NO sustituirlo por SDK upstream 0.10 sin resolver diferencias
de API. El manifest del juego declara SDK 0.8. No se modifican estos checkouts.

## Código portable y arquitectura

`simpsons/src/main.cpp` registra SimpsonsApp mediante REX_DEFINE_APP e incluye
`generated/default/simpsons_init.h`. `simpsons/generated/default` contiene la
recompilación estática. El tool de recompilación se ejecutará en host, no Android.
`simpsons/src/simpsons_app.h` es el punto de integración del arranque y hooks.
Los helpers PPC, conversiones endian, texturas Xenos y kernel deben conservarse.
ARM64 requiere auditar SIMD, alineación y casts de los TUs generados, no interpretar PPC.

`src/system/xmemory.cpp` reserva un espacio de direcciones con vistas guest
virtual y física; physical_membase está a +0x100000000 de virtual_membase.
`TranslateVirtual` y `TranslatePhysical` traducen direcciones guest de 32 bits.
Esto es una reserva virtual, no una asignación física equivalente de RAM.
Mantener las vistas alias y helpers big-endian; no castear guest addresses a host pointers.
La reconciliación de permisos para páginas host grandes existente necesita tests
Android de 4 y 16 KiB. No cambiar a mappings arbitrarios para lograr un arranque.

## Matriz exacta de futuros archivos a modificar

Rutas SDK relativas al SDK vendorizado; upstream independiente sirve de comparación.

| Archivo | Acción Android necesaria / evidencia |
|---|---|
| `simpsons/CMakeLists.txt` | Añadir biblioteca shared/entrada SDL Android; conservar executable PC. Flags x86 ya están condicionados por processor. |
| `simpsons/generated/rexglue.cmake` | Revisar configuración generada, dependencia SDK y fuentes; preferir wrapper sin editar generación manualmente. |
| `simpsons/src/main.cpp`, `simpsons/src/simpsons_app.h` | Adaptar creación de app al lifecycle SDL y rutas privadas, sin arrancar juego en M1. |
| `simpsons/src/frame_pacing.cpp`, `game_clock.cpp`, `physics_step.cpp`, `tick_count.cpp` | Restaurar objetivo 30 FPS explícito: la base actual aplica mejoras para framerate mayor. |
| SDK `CMakeLists.txt`, `thirdparty/CMakeLists.txt`, `cmake/rexglue_vulkan_stack.cmake` | Rama ANDROID antes de UNIX; no paquetes desktop, tools de host ni RPATH como requisito. FFmpeg cross compile independiente. |
| SDK `include/rex/platform.h` | Ya define REX_PLATFORM_ANDROID y ARM64; no duplicar. |
| SDK `src/core/CMakeLists.txt` | Reusar POSIX, evitar enlace `rt`/`pthread` como bibliotecas separadas en Bionic. |
| SDK `src/core/memory_posix.cpp` | AndroidInitialize llama GetAndroidApiLevel sin implementación encontrada; usa ASharedMemory y ashmem legado. Resolver init/shutdown y NDK actual, verificar MAP_FIXED_NOREPLACE sin destruir mappings ajenos. |
| SDK `src/core/mapped_memory_posix.cpp`, `filesystem_posix.cpp`, `include/rex/filesystem.h` | OpenAndroidContentFileDescriptor sólo tiene declaración encontrada. Añadir puente JNI SAF, fd seekable, lifetime y tree traversal; URI no es path POSIX. |
| SDK `src/core/threading_posix.cpp`, `fiber_posix.cpp`, `exception_handler_posix.cpp`, `seh_posix.cpp` | Resolver Android API helper; revisar ucontext ARM64, señales, TLS, scheduling y libc Bionic. No prometer compatibilidad sólo por POSIX. |
| SDK `src/core/system_posix.cpp`, `logging.cpp`, `dynlib_posix.cpp`, `clock_posix.cpp` | Información Android/JNI y logcat; revisar namespace de librerías, timers y diagnósticos. |
| SDK `src/system/xmemory.cpp`, `include/rex/system/xmemory.h` | Validar alias mapping, host page size, permisos guest y fallos de reserva en dispositivo. |
| SDK `src/ui/CMakeLists.txt`, `surface_gnulinux.cpp`, `window_sdl.cpp`, `windowed_app_context_sdl.cpp` | CMake actualmente selecciona surface_gnulinux en Android por rama else. Añadir surface_android/SDL y lifecycle móvil. |
| SDK `src/ui/vulkan/vulkan_instance.cpp`, `vulkan_device.cpp`, `vulkan_presenter.cpp` | Instancia 1.1; negociación capabilities, recreación surface/swapchain y sincronización portable. |
| SDK `src/graphics/vulkan/*`, `src/graphics/pipeline/shader/spirv_translator*.cpp` | Revisar requisitos graphics completos, SPIR-V target, descriptors, resolves, memoria y caches; clear-screen no demuestra compatibilidad del renderer Xenos. |
| SDK `src/audio/CMakeLists.txt`, `src/input/CMakeLists.txt` | Reusar SDL, validar pausa/audio y mandos reales sin APIs desktop obligatorias. |

## Riesgos gráficos comprobados

`src/ui/vulkan/vulkan_device.cpp` exige, en la base PC, fragmentStoresAndAtomics,
otras features y geometryShader por defecto; geometry shaders no son una garantía
de Vulkan 1.1 móvil. Enumera también extensiones promovidas a 1.2 como spirv_1_4:
debe distinguirse enumerar de exigir antes de cambiar código. Hace falta auditar
cada consumidor, shaders y caminos de fallback. No basta bajar apiVersion.
El runtime conserva subsistemas derivados de Xenia, pero no se utilizará Xenia
como aplicación/runtime externo ni emulación CPU/JIT.

## Switch: referencia, no base Android

`src/game/cmake/switch.cmake`, `tools/switch/toolchain.cmake` y
`patches/rexglue-sdk/0001-switch-port.patch` incluyen Horizon, libnx, servicios,
threads con afinidad, memory mapping y presenter específicos. No aplicar ese patch.
`patches/mesa-switch` y `src/game/src/native/*` requieren una evaluación separada;
no trasladar NVK/mesa ni shaders específicos GM20B.
`src/game/src/movie_decoder.cpp` sustituye hooks PPC de VP6 por avcodec, copia
planos YUV a buffers guest y revierte filas. Es una referencia útil para M7,
pero offsets/direcciones deben coincidir con el executable validado.
`0002-ffmpeg-vp6-decoder.patch` demuestra los componentes VP6/NEON necesarios;
Android debe configurar FFmpeg ARM64 por NDK, sin configuración Switch.
Frame pacing y ring_wait son referencias para evitar spins, sin copiar afinidad.

## Pendientes que no deben fingirse

No se ha establecido un SHA-256 único autorizado del XEX desde los manifests.
No calcular un valor y declararlo compatible sólo porque existe una copia local.
Antes de M3 obtener fingerprint de la versión asociada a la generación y validar
el XEX seleccionado; nunca empaquetarlo. SAF y UI de selección corresponden a
integración del juego, no son necesarios para el clear-screen M1.

## Estrategia

1. App aislada Gradle/NDK ARM64, SDL3, Vulkan 1.1, render pass tradicional y FIFO.
2. Verificar en dispositivo frames, HOME/resume, surface recreation y errores.
3. M2: llevar SDK vendorizado a Android, tests memoria/endian/TLS/señales sin juego.
4. M3–M4: entrada recompilada y kernel/filesystem. Lista de imports faltantes.
5. M5–M7: renderer PC adaptado y VP6/audio/input. Features superiores opcionales
   con fallback 1.1 real antes de considerarlas disponibles.

Esta auditoría identifica puntos de cambio; no certifica que todo el SDK compile
en Android ni que el renderer del juego sea ya compatible con Vulkan 1.1.

## Referencia local NFSMW aportada por el usuario

Inspeccionado `C:/Users/Sistemas/Documents/nfsmw android evolved/nfsmw-android`.
`sdk/src/ui/vulkan/android_gpu_driver.cpp` usa libadrenotools con selección
explícita del driver custom; falla explícitamente y no informa un fallback
silencioso. No aplica KGSL turbo ni esa integración a Mali. Es una referencia
para una opción futura Adreno; no se incluye en M1 y no es requisito Vulkan.
`docs/android-xclipse-diagnostic.md` documenta una corrección de sincronización
en Xclipse530 y pruebas individuales BC1/2/3 frente a BC4/5. Reutilizar la
metodología de capabilities/formatos y barreras, no asumir los resultados para
otro teléfono o para las texturas de Simpsons. El proyecto local no se modifica.

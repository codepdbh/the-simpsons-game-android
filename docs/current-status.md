# Estado — 2026-10-09

| Milestone | Estado | Evidencia / alcance |
|---|---|---|
| 0 Auditoría | PARTIAL | Tres repositorios inspeccionados y matriz de archivos; requiere auditoría adicional por subsistema antes de portar runtime. |
| 1 Hello Android | WORKING | APK ARM64 instalado en SM-S938B, SDL3, Vulkan baseline 1.1, pantalla azul y HOME/resume con recreación de contexto, Khronos validation activa sin errores observados. |
| 2 ReXGlue Android | PARTIAL | Runtime, memoria invitada, XEX, renderer Xenos e input inicializados en SM-S938B. Falta validar otros dispositivos y ciclo de vida del juego. |
| 3 Código recompilado | WORKING | Funciones ARM64 compiladas, enlazadas y ejecutadas hasta título e introducción. |
| 4 Boot | WORKING | Arranque hasta título comprobado; versión local del XEX. |
| 5 Primer frame del juego | WORKING | Captura real de pantalla de título y vídeo introductorio inspeccionadas. |
| 6 EA logo | NOT STARTED | Sin juego. |
| 7 Menú | PARTIAL | Pantalla de título y transición a introducción observadas; falta probar todas las opciones del menú. |
| 8 Gameplay | NOT STARTED | Sin juego. |
| 9 Rendimiento juego 30 FPS | NOT STARTED | M1 usa target 30 FPS; no benchmark gameplay. |

## Verificación

- Debug y Release: Gradle BUILD SUCCESSFUL, NDK 28.2.13676358, ARM64.
- Debug instalado por `adb install -r`, activity iniciada, GPU Adreno 830 API 1.3.284.
- Instancia solicita 1.1; sólo swapchain como extensión device; ninguna feature extra.
- Surface/swapchain 2340x1080, FIFO, 5 imágenes. Captura azul inspeccionada.
- 300 frames en unos 10 segundos: cerca de 30 presentaciones/s, sin afirmar display frame pacing medido.
- HOME: log `Paused; Vulkan surface released`; regreso: `Resumed; Vulkan surface recreated`, nuevos frames.
- Khronos layer 1.4.363.0 activo. Sin mensajes VALIDATION ERROR/WARNING encontrados
  en el log capturado durante estas pruebas. No se certifican todos los drivers.
- APK inspeccionado: sólo ABI arm64-v8a. Layer presente sólo en Debug.
- Release sin firma compila; no instalado ni probado como Release.

Evidencias locales (ignoradas por git): `artifacts/milestone1-screen.png`,
`artifacts/milestone1-logcat.txt`, `build-debug.log`, `build-release.log`.
No se han modificado los proyectos PC/Switch/NFSMW ni los archivos del juego.

## Límites pendientes

Sin pruebas físicas Mali, Xclipse, PowerVR o dispositivo con driver sólo 1.1.
No probado con host pages 16 KiB, pérdida de dispositivo ni cambios de resolución
por ventanas múltiples. Orientation bloqueada landscape. Surface recreation
comprobada con HOME/resume; OUT_OF_DATE/SUBOPTIMAL/SURFACE_LOST implementados,
no forzados artificialmente. La ruta de rechazo Vulkan <1.1 no se probó en hardware.
Selector SAF e importación con SHA implementados en el launcher experimental;
selección, permiso persistente e importación comprobados en SM-S938B. El runtime usa una copia privada,
sin VFS SAF directo. FFmpeg/audio del juego, crash reporting guest,
shader/pipeline cache y drivers adicionales siguen sin verificación completa.
El mando táctil está conectado al input real; overlay y editor comprobados
visualmente. Falta validar combinaciones multitáctiles, mando físico y gameplay.
Español comprobado tras seleccionar idioma. Ver [integración experimental](experimental-runtime.md)
y [controles](touch-controls.md).

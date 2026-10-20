# Estado — 2026-10-20

| Milestone | Estado | Evidencia / alcance |
|---|---|---|
| 0 Auditoría | PARTIAL | Tres repositorios inspeccionados y matriz de archivos; requiere auditoría adicional por subsistema antes de portar runtime. |
| 1 Hello Android | WORKING | APK ARM64 instalado en SM-S938B, SDL3, Vulkan baseline 1.1, pantalla azul y HOME/resume con recreación de contexto, Khronos validation activa sin errores observados. |
| 2 ReXGlue Android | PARTIAL | Runtime, memoria invitada, XEX, renderer Xenos e input inicializados en SM-S938B. Falta validar otros dispositivos y ciclo de vida del juego. |
| 3 Código recompilado | WORKING | Funciones ARM64 compiladas, enlazadas y ejecutadas hasta título e introducción. |
| 4 Boot | WORKING | Arranque hasta título comprobado; versión local del XEX. |
| 5 Primer frame del juego | WORKING | Captura real de pantalla de título y vídeo introductorio inspeccionadas. |
| 6 EA logo | NOT VERIFIED | No se registró una comprobación independiente de esta pantalla. |
| 7 Menú | PARTIAL | Pantalla de título y transición a introducción observadas; falta probar todas las opciones del menú. |
| 8 Gameplay | PARTIAL | El usuario reportó personajes invisibles. Logs de 0.3.1 muestran rechazo de streams opcionales vacíos; 0.3.2 corrige el caso, con prueba visual pendiente. |
| 9 Rendimiento juego 30 FPS | PARTIAL | Cadencia del scheduler de menús medida cerca de 30 Hz en S25; sin benchmark de gameplay. |

## Verificación M1 — 2026-10-09

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
Las adaptaciones de los submódulos se registran en `patches/`; los datos originales
del juego y el proyecto NFSMW de referencia del usuario se conservan.

## Preparación del Redmi — 2026-10-20

- APK 0.3.2 instalado y launcher inspeccionado; Adreno 610 / Vulkan 1.1.
- Perfil Compatibilidad, 30 FPS, un hilo de shaders, compilación asíncrona apagada.
- Regresión sintética ARM64 de stride cero y mini-fetches: PASS.
- Reporte de GitHub: ZIP guardado en Descargas e inspeccionado; navegador abierto
  con el formulario preparado. No se publicó un issue de prueba.
- Prueba visual de personajes/parpadeos pendiente; ver [correcciones](rendering-fixes.md).

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

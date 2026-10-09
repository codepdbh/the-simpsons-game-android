# Integración Android experimental

El selector de carpetas usa SAF y recomienda `TSG` en la memoria interna
compartida. La selección conserva permiso de lectura. La importación explícita
crea una copia privada para el VFS POSIX del runtime; el acceso directo por SAF
del runtime todavía está pendiente. No se incluyen datos del juego en el APK.

El build diagnóstico conserva la prueba azul de Vulkan como botón separado.
El build `-PwithGame` integra el código recompilado y el SDK Android de la
referencia NFSMW, sin incorporar sus hooks de gameplay.

```powershell
.\scripts\setup-game.ps1
.\scripts\build-android.ps1 -WithGame
```

Las referencias están fijadas como submódulos. `patches/android-sdk.patch`
registra las adaptaciones locales: solicitud y negociación de Vulkan 1.1,
ciclo de vida SDL Android, registro FPCR ARM64 y macro de funciones débiles.
Los shaders nativos y dynamic rendering están desactivados al iniciar.
El renderer real requiere features del dispositivo adicionales; la prueba
azul no establece compatibilidad del renderer Xenos.

El hook del anillo GPU mantiene las comprobaciones originales de espera y
timeout. Mientras el juego indica espera, cede el hilo durante 1 ms: este SDK
no ofrece la notificación de progreso del SDK más reciente del proyecto PC.
No se simula progreso ni se fabrican retornos de éxito.

El SHA de `default.xex` se comprueba antes de activar una importación y antes
de iniciar. El hash corresponde al ejecutable local de este experimento;
no demuestra por sí solo compatibilidad con el código recompilado.
Arranque y título comprobados en SM-S938B; introducción y salida de datos de
audio observadas. Menú completo, calidad de audio y gameplay requieren pruebas
adicionales. Overlay táctil y editor implementados; ver [controles](touch-controls.md).

## Evidencia inicial — 2026-10-09

- Gradle `assembleDebug -PwithGame`: BUILD SUCCESSFUL; instalado en SM-S938B.
- APK inspeccionado: ABI arm64-v8a, SDL3, tsg_game, plugin Xenos, libc++ y layer
  de validación de Debug; sin archivos del juego.
- Launcher inspeccionado visualmente; selector SAF y permiso persistente para
  la carpeta `TSG` comprobados. Importación privada finalizada y XEX validado.
- Transferencia a memoria compartida: 15.369 archivos, 5.883.836.623 bytes;
  SHA-256 de todos los archivos comprobado contra la fuente local.
- Instancia y dispositivo Vulkan negociados a API 1.1.0 en el renderer real.
- Guest arena mapeada, tabla PPC instalada y XEX cargado. Pantalla de título
  italiana y posteriormente texto de introducción en español capturados.
- Intro animada y archivo de guardado de 114.800 bytes observados. No constituye
  verificación de gameplay ni medición de rendimiento.

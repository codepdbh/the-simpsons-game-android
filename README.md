# The Simpsons Game Android

Prototipo Android ARM64 SDL3 + Vulkan 1.1 que presenta una pantalla azul.
El build diagnóstico conserva M1; se está integrando un runtime experimental
en el build opcional `-PwithGame`. No contiene XEX, vídeos ni assets EA.
La auditoría e integración pendiente están en [docs/android-port-analysis.md](docs/android-port-analysis.md).

## Requirements

- Git, Android Studio / JDK 17 o 21.
- Android SDK platform 35 y build-tools; Android NDK **28.2.13676358**.
- SDK CMake **3.30.5**; minSdk **26**; teléfono **arm64-v8a** con Vulkan >= 1.1.
- Gradle wrapper 8.13, AGP 8.9.1. No usar el Java 26 del sistema.

## Configure / Build

Windows PowerShell, desde esta carpeta:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
.\scripts\build-android.ps1
.\scripts\build-android.ps1 -Configuration Release
```

Linux/macOS: establecer JAVA_HOME y ANDROID_HOME, ejecutar
`bash scripts/build-android.sh` o `bash scripts/build-android.sh Release`.
Estos scripts descargan SDL3 **release-3.2.28**, configuran CMake por Gradle y
generan APK. Abrir esta carpeta como proyecto Android Studio también funciona.
Los repositorios de referencia en `../upstream` no participan en este build.

## Install / Run / Debug

```powershell
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n org.tsg.android/.LauncherActivity
adb logcat -s TSGAndroid VALIDATION AndroidRuntime
adb shell run-as org.tsg.android cat files/game.log
```

El launcher ofrece selección de carpeta y la prueba azul de Vulkan. HOME pausa y libera Vulkan;
volver a la app recrea instancia, dispositivo, surface y swapchain. Objetivo
30 FPS, sin busy loop. `game.log` vive en almacenamiento privado.
APK Debug firmado automáticamente para pruebas; Release produce
`app/build/outputs/apk/release/app-release-unsigned.apk` y necesita firma propia.

## Validation opcional

Ejecutar `scripts/setup-validation.ps1` para descargar el layer oficial Khronos
**vulkan-sdk-1.4.363.0**. Se empaqueta exclusivamente en Debug; recompilar después.
La app detecta el layer y habilita debug_utils si existe. Release usa sólo loader
del sistema. No se incluye ningún driver custom. La referencia libadrenotools de
NFSMW se evaluará posteriormente como opción Adreno, nunca requisito para Mali/Xclipse.

## Provide game files

Guardar los datos propios en `TSG` dentro de la memoria interna compartida.
Seleccionar esa carpeta con SAF y pulsar importación: el runtime usa una copia
privada y verifica SHA-256 del ejecutable. El acceso directo SAF del runtime
está pendiente. Ver [integración experimental](docs/experimental-runtime.md).
No copiar la carpeta del juego al APK ni a `app/src/main/assets`.

## Estado y evidencia

Ver [current-status](docs/current-status.md), [device compatibility](docs/device-compatibility.md)
y [Vulkan 1.1](docs/vulkan-1.1.md). Compatibilidad funcional sólo se declara para
los dispositivos realmente probados; clear-screen no verifica el renderer del juego.

# The Simpsons Game — Android Evolved

[English](#english) · [Español](#español) · [Download / Descargar](https://github.com/codepdbh/the-simpsons-game-android/releases)

Experimental Android ARM64 port using statically recompiled Xbox 360 code, ReXGlue, SDL3 and Vulkan 1.1.

## English

### Features and status

**0.3.0-experimental** boots to the title screen and animated introduction on a Samsung Galaxy S25 Ultra (SM-S938B, Adreno 830).

- Landscape launcher with game-file import and language selection.
- Multitouch virtual controller with a persistent editor to move and resize buttons. Physical controllers are also supported.
- Balanced and Compatibility GPU profiles, 30/60 FPS targets and shader-worker settings. Menu logic stays at its original 30 FPS cadence.
- Optional Vulkan driver ZIP import and a GPU capability probe in a separate process.
- Optimized native code and frame-cadence logs to investigate stutter.

Full gameplay, audio quality and Mali/Exynos/Xclipse hardware support still need testing. A 60 FPS target does not guarantee 60 FPS performance. The compatibility profile enables existing renderer fallbacks; the GPU probe does not certify every game scene.

### Install and play

1. Download the experimental APK from [Releases](https://github.com/codepdbh/the-simpsons-game-android/releases).
2. Install on **ARM64 Android 8.0+ with Vulkan 1.1 or newer**. Additional renderer features are checked by the launcher GPU probe.
3. Put your own extracted game files in **TSG** at the root of shared internal storage (`/sdcard/TSG`). `default.xex` must be directly inside that folder.
4. Choose **Elegir carpeta TSG**, grant folder access, then **Importar los datos seleccionados**. Allow space for another copy: the runtime currently uses a private import rather than direct SAF access.
5. Select a language matching your game data and press **JUGAR**. Inside the game, use **EDITAR** to move or resize controls, then **Guardar**.

The executable SHA-256 is checked before activating an import and before launch. Only the locally tested executable described in [runtime documentation](docs/experimental-runtime.md) is currently accepted. No game executables, EA assets, videos or external GPU driver binaries are bundled.

The published 0.3 APK is the **signed experimental Debug build** tested on the device. It retains diagnostics and is not a production-signed Release build.

### Build from source

Requirements: Git, Python 3, Android Studio/JDK **17 or 21**, Android SDK **35**, NDK **28.2.13676358** and SDK CMake **3.30.5**. Gradle **8.13**, AGP **8.9.1**. Do not use system Java 26 for this build.

Windows PowerShell, from the repository root:

```powershell
git clone https://github.com/codepdbh/the-simpsons-game-android.git
cd the-simpsons-game-android
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
.\scripts\setup-game.ps1
.\scripts\build-android.ps1 -WithGame
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Setup initializes pinned submodules, fetches SDK dependencies and applies tracked Android patches. Without `-WithGame`, the build contains only the standalone Vulkan diagnostic app. `-Configuration Release -WithGame` produces an unsigned Release APK that requires your own signing configuration. The Linux/macOS shell helper currently builds only the diagnostic variant; game setup is documented for Windows PowerShell.

## Español

### Funciones y estado

**0.3.0-experimental** llega al título y a la introducción animada en un Samsung Galaxy S25 Ultra (SM-S938B, Adreno 830).

- Launcher horizontal con importación de archivos y selector de idioma.
- Mando virtual multitáctil con editor para mover y redimensionar botones; el diseño se guarda entre sesiones. También admite mandos físicos.
- Perfiles Equilibrado y Compatibilidad, objetivos de 30/60 FPS y ajuste de hilos para shaders. La lógica de los menús conserva su cadencia original de 30 FPS.
- Importación opcional de drivers Vulkan en ZIP y prueba de capacidades de GPU en otro proceso.
- Código nativo optimizado y registros de cadencia para investigar tirones.

Faltan pruebas de gameplay completo, calidad de audio y hardware Mali/Exynos/Xclipse. El objetivo de 60 FPS no garantiza ese rendimiento. El perfil de compatibilidad permite los fallbacks existentes del renderer; la prueba de GPU no certifica todas las escenas del juego.

### Instalar y jugar

1. Descarga el APK experimental desde [Releases](https://github.com/codepdbh/the-simpsons-game-android/releases).
2. Instálalo en un dispositivo **ARM64 con Android 8.0+ y Vulkan 1.1 o superior**. La prueba de GPU del launcher comprueba funciones adicionales del renderer.
3. Guarda tus propios datos extraídos del juego en **TSG**, en la raíz de la memoria interna compartida (`/sdcard/TSG`). `default.xex` debe estar directamente dentro de esa carpeta.
4. Pulsa **Elegir carpeta TSG**, concede acceso y selecciona **Importar los datos seleccionados**. Necesitas espacio para otra copia: el runtime utiliza por ahora una importación privada, no acceso directo mediante SAF.
5. Selecciona un idioma disponible en tus datos y pulsa **JUGAR**. Dentro del juego, pulsa **EDITAR** para mover o cambiar el tamaño de los controles y **Guardar** para conservar el diseño.

Se comprueba el SHA-256 del ejecutable antes de activar la importación y de iniciar. Solo se admite por ahora el ejecutable probado localmente, identificado en la [documentación del runtime](docs/experimental-runtime.md). El APK no contiene ejecutables del juego, assets de EA, vídeos ni drivers externos.

El APK 0.3 publicado es el **build Debug experimental firmado** probado en el teléfono. Conserva herramientas de diagnóstico; no es un build Release con firma de producción.

### Compilar desde el código fuente

Requisitos: Git, Python 3, Android Studio/JDK **17 o 21**, Android SDK **35**, NDK **28.2.13676358** y CMake del SDK **3.30.5**. Gradle **8.13**, AGP **8.9.1**. No uses Java 26 del sistema para este build.

Utiliza los comandos PowerShell de [Build from source](#build-from-source). `setup-game.ps1` inicializa los submódulos fijados, descarga dependencias y aplica los parches Android. Sin `-WithGame` se compila únicamente la app de diagnóstico Vulkan. `-Configuration Release -WithGame` genera un APK Release sin firmar que necesita tu propia configuración de firma. El script Linux/macOS compila actualmente solo la variante de diagnóstico; el setup del juego está documentado para Windows.

## Documentation / Documentación

- [Experimental runtime / Runtime experimental](docs/experimental-runtime.md)
- [Graphics and performance / Gráficos y rendimiento](docs/graphics-and-performance.md)
- [Touch controls and editor / Controles y editor](docs/touch-controls.md)
- [Vulkan 1.1 baseline](docs/vulkan-1.1.md)
- [Initial port analysis / Análisis inicial](docs/android-port-analysis.md)

## Credits and license / Créditos y licencia

Based on [TheSimpsonsGameRecomp](https://github.com/YesterMester/TheSimpsonsGameRecomp) and the Android ReXGlue integration from [nfsmw-android](https://github.com/codepdbh/nfsmw-android), with SDL3 and Vulkan. See [LICENSE](LICENSE) and [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md). The Simpsons and its game assets belong to their respective owners.

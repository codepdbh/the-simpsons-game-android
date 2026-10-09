# Launcher, GPU y rendimiento

El launcher adapta el diseño y el importador de drivers del proyecto NFSMW:
identidad y archivos a la izquierda, opciones gráficas a la derecha. Conserva
idioma, importación TSG y editor del mando virtual. No incorpora opciones de
resolución ni shaders específicos de NFSMW: no pertenecen al renderer de Simpsons.

- **Equilibrado:** framebuffers convencionales, Vulkan 1.1 y MAILBOX cuando está
  disponible. Objetivo de gameplay de 60 FPS, configurable a 30.
- **Compatibilidad:** presentación FIFO y preferencia por el fallback sin shaders
  geométricos. Ambos perfiles permiten dispositivos sin geometryShader ni
  fillModeNonSolid; mantienen las funciones realmente obligatorias del SDK.
- **Shaders:** dos hilos por defecto en lugar de la selección automática de hasta
  tres cuartos de los núcleos. Esto reduce competencia con los hilos del juego.
  Compilación asíncrona y almacenamiento de shaders activados. Al aparecer un
  pipeline nuevo pueden omitirse frames; desactivar la opción puede causar pausas.
- **CPU:** SDK nativo compilado con `-O2` también en el APK experimental Debug.
  Menús a 30 FPS para conservar la cadencia original de su lógica.

Los logs iniciales muestran creación de pipelines al cambiar de escena. Eso es
una fuente posible de tirones, pero no identifica por sí solo todos los cuellos
de botella. El hook de inicio de frame ahora registra cada cinco segundos la
cadencia del scheduler del juego, media y máximo en ms y frames lentos. Estos
datos **no son FPS de presentación**; permiten comparar la misma escena entre
builds. Pausas mayores de un segundo reinician la muestra.

En la primera ejecución de 0.3 en Adreno 830, con el juego en menús, se registró
una ventana de 29,6 Hz con un pico de 76 ms mientras se creaban pipelines. Las
tres ventanas siguientes dieron 29,9 Hz, media de 33,44–33,45 ms, máximo de
33,49–33,51 ms y ningún frame por encima de 50 ms. Esto comprueba la cadencia de
menú en esa sesión, no una mejora de FPS en gameplay frente al build anterior.

La prueba de GPU corre en un proceso separado, abre el driver seleccionado y
comprueba Vulkan 1.1, independentBlend, atomics de fragmento/vértice, swapchain,
cola graphics/compute y creación del dispositivo lógico. No verifica todos los
formatos del renderer ni certifica gameplay. Timeout: 20 segundos.

Mali y Exynos/Xclipse usan inicialmente el driver del sistema. Los ZIP externos
son opcionales y deben corresponder al hardware y firmware; no se distribuye
ningún driver. El importador limita tamaño y rutas y exige bibliotecas ELF ARM64.
Las pruebas de host verifican traversal, tamaño de metadata y rechazo de x86.

La validación física disponible es SM-S938B / Adreno 830. Mali, Exynos y Xclipse
todavía requieren pruebas en hardware. El objetivo de 60 FPS no garantiza 60 FPS
reales, y no hay una comparativa de gameplay antes/después medida todavía.

Orientación: launcher, juego y diagnóstico fijados a horizontal. SDL solicitaba
FULL_USER para la ventana redimensionable y reemplazaba el manifiesto; la clase
LandscapeSDLActivity ahora fuerza LANDSCAPE en esa llamada y al reanudar. El
runtime también especifica únicamente las orientaciones horizontales en SDL.

Para reproducir las pruebas del importador (JDK 21):

```powershell
javac -d artifacts/driver-tests app/src/main/java/org/tsg/android/DriverArchive.java tools/tests/DriverArchiveTest.java
java -cp artifacts/driver-tests org.tsg.android.DriverArchiveTest
```

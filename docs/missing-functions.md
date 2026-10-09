# Pendientes del runtime — todavía no integrado

- `rex::GetAndroidApiLevel`: referencias encontradas, resolver plataforma/JNI.
- `filesystem::OpenAndroidContentFileDescriptor`: declaración sin implementación encontrada.
- Inicialización y cierre Android de memory/thread/filesystem.
- CMake y surface Android, señales/fibers ARM64 y tests memoria 4/16 KiB.
- Auditar features Vulkan renderer y target SPIR-V contra baseline 1.1.
- Determinar SHA-256 del executable asociado a la generación recompilada.

No se registran imports del juego como implementados sin ejecutarlos.

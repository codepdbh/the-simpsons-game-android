# Compatibilidad comprobada — 2026-10-20

Boot/Menu/Gameplay se refieren al juego; App M1 se registra por separado.

| Device | SoC | GPU | Android | Vulkan | App M1 | Boot | Menu | Gameplay | FPS | Issues |
|---|---|---|---|---|---|---|---|---|---|---|
| Samsung SM-S938B | SM8750 | Adreno 830 | 16 | 1.3.284, baseline 1.1 | WORKING | WORKING | PARTIAL | Rendering issues reported | Sin benchmark de gameplay | Título e intro observados; el usuario reportó parpadeos y personajes invisibles en 0.3.1. Corrección de streams opcionales en 0.3.2 pendiente de prueba visual. |
| Xiaomi Redmi Note 8 | qcom (reportado) | Adreno 610 | 17 / API 37 (reportado por el dispositivo) | 1.1 | NOT TESTED | NOT TESTED | NOT TESTED | NOT TESTED | Sin medir | APK 0.3.2 instalado; prueba Vulkan básica y regresión ARM64 de metadatos de shaders pasaron. Datos y prueba de escena en preparación. |
| Pendiente | Pendiente | ARM Mali | Pendiente | Pendiente | NOT STARTED | NOT STARTED | NOT STARTED | NOT STARTED | — | Sin dispositivo probado |
| Pendiente | Pendiente | Samsung Xclipse | Pendiente | Pendiente | NOT STARTED | NOT STARTED | NOT STARTED | NOT STARTED | — | NFSMW Xclipse es referencia, no evidencia Simpsons |
| Pendiente | Pendiente | PowerVR | Pendiente | Pendiente | NOT STARTED | NOT STARTED | NOT STARTED | NOT STARTED | — | Validar capacidades reales |

El sistema reportado por el Redmi no implica disponibilidad de una actualización
oficial de Xiaomi. La prueba de capacidades crea un dispositivo Vulkan, pero no
verifica todos los formatos y pases del renderer ni certifica gameplay.

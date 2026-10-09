# Vulkan 1.1 — Milestone 1

## REQUIRED

Loader y physical device API >= 1.1. Instancia solicita exactamente 1.1.
`vkEnumerateInstanceVersion` se resuelve vía SDL y se llama si está disponible;
su ausencia implica loader 1.0 y error `Vulkan 1.1 or newer is required.`
Extensiones de instancia enumeradas por SDL (Android surface y KHR surface).
Device: sólo `VK_KHR_swapchain`. Graphics y presentation pueden usar familias
distintas: en ese caso sharing CONCURRENT. Color attachment usage comprobado.
Formato surface disponible, feature COLOR_ATTACHMENT y extent dentro de límites.
FIFO, image count limitado por capabilities y composite alpha soportado.

API usada: instancia/device enumeration, properties/memory/format queries,
surface capabilities/formats/present support, swapchain acquire/present,
image views/framebuffers, command pools/buffers, render pass tradicional,
binary semaphores y fences. La dependencia external→subpass sincroniza color
attachment y transición; el render pass lleva a PRESENT_SRC_KHR.
Un semaphore de presentación por imagen; fence protege submit y acquire semaphore.
M1 serializa presentación con queue idle; evitar trasladar esto al renderer del
juego sin perfilar. Pausa libera el contexto y resume reconstruye superficie.

## OPTIONAL

Debug detecta VK_LAYER_KHRONOS_validation y VK_EXT_debug_utils. Se activan sólo
si están disponibles. El layer puede empaquetarse sólo en `app/src/debug/jniLibs`.
Release no requiere ni empaqueta el layer. Se registran warnings/errors en game.log.

## UNUSED

Dynamic rendering, synchronization2, timeline semaphores, descriptor indexing,
buffer device address y memory budget. No shaders, descriptors, texturas guest,
depth attachments ni pipeline cache en clear-screen: no se simulan componentes
innecesarios. Depth/stencil formats se consultan para diagnóstico, sin exigirlos.
Las capabilities del renderer real se auditarán antes de integrarlo.

Referencias: [Android validation](https://developer.android.com/ndk/guides/graphics/validation-layer),
[Khronos semaphore reuse](https://docs.vulkan.org/guide/latest/swapchain_semaphore_reuse.html).

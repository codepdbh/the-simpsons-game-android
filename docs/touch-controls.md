# Mando táctil e idiomas

El launcher guarda el idioma elegido y pasa `user_language` al runtime antes
del arranque. `XGetLanguage` y la configuración de consola usan el mismo valor.
Español verificado con los datos locales; otros idiomas requieren que la copia
del juego los incluya (las carpetas de vídeos locales contienen `es` e `it`).

El mando tiene stick izquierdo de movimiento, derecho de cámara, A/B/X/Y,
cruceta, LB/RB, LT/RT, L3/R3, BACK y START. Envía el estado multitáctil a la
API real `rex_sdl_set_touch_gamepad_state`; no simula respuestas del juego.
Al pausar o cancelar los toques, libera botones y ejes.

`EDITAR` entra al modo de ajuste y libera la entrada del mando. Tocar un
control lo selecciona; arrastrarlo mueve su posición. El deslizador ajusta
el tamaño del control seleccionado. `Guardar` sale del editor y conserva la
distribución en preferencias privadas. `Restablecer` recupera la distribución
inicial. Coordenadas y tamaños se guardan como proporciones de la pantalla,
y los límites impiden arrastrar un control fuera del área visible.

## Gráficos generados

Herramienta: imagegen integrada, fondo transparente RGBA. Arte reutilizado en
botones y sticks: `app/src/main/res/drawable-nodpi/control_button.png`.
Las letras y etiquetas se dibujan con texto nativo para conservar nitidez.

Prompt utilizado:

> Create one production-ready mobile virtual gamepad button background asset for a Simpsons-themed Android game port. A perfectly circular tactile button, centered and occupying 90 percent of the square image. Hand-drawn cartoon aesthetic: thick clean charcoal outline, golden yellow doughnut-inspired glazed outer rim with a few subtle pink sprinkles, flat dark charcoal central disc with plenty of empty space for a crisp letter that will be drawn by code. No text, no letters, no logos, no characters, no extra objects. Straight-on orthographic view, radial symmetry, clean antialiased edges, strong readable silhouette at 80 pixels, restrained detail, subtle dimensional shading. Genuine transparent background outside the button. This will be reused as the face-button and joystick artwork in an Android touch-control overlay.

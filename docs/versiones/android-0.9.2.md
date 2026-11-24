# Kiwu para Android 0.9.2 · Corrección de gestos en Drawer, fluidez en Editor y unificación de tarjetas de repaso

Se instala encima de la 0.9.1 y conserva los datos. La base de datos no cambia. `versionCode = 35`.

---

### 🚀 Novedades y Mejoras Principales

- **🖐️ Sistema de Drag & Drop fluido en el Menú Lateral (Drawer):**
  - **Sin colisión de gestos:** Se eliminaron los gestos de pulsación larga (`onLongClick`) en las filas de listas y carpetas, manteniendo únicamente el toque simple para abrir/desplegar. Las acciones de edición/eliminación se realizan mediante el menú de tres puntos `⋮`.
  - **Arrastre preciso con respuesta háptica:** Inicie el arrastre manteniendo presionada una lista (`detectDragGesturesAfterLongPress` con vibración háptica).
  - **Desactivación de gestos del Drawer:** Mientras se arrastra una lista, el gesto lateral de cerrar el menú se deshabilita automáticamente (`gesturesEnabled = false`), evitando cierres accidentales.
  - **Feedback visual enriquecido:** Elevación con sombra (`graphicsLayer`, `zIndex`), resaltado en color `primaryContainer` con borde `1.5.dp` sobre la carpeta destino, y franja dedicada para devolver listas a la raíz (*"Sacar de la carpeta"*).

- **✍️ Refactorización completa del Editor de Notas (Fluidez y Estabilidad):**
  - **Edición más fluida:** El campo local actualiza el modelo del editor tras 350 ms sin cambios. El ViewModel de la página aplica su propio *debounce* de 500 ms para guardar.
  - **Cursor siempre visible sobre el teclado:** Uso de `BringIntoViewRequester` y `onTextLayout` para desplazar automáticamente el lienzo cuando el cursor se acerca al borde inferior (`+ 80.dp`), evitando que `BarraFormato` o el teclado tapen el texto al escribir párrafos largos.
  - **Nueva Barra Horizontal de Comandos `/` y `+ Bloque`:** Los comandos se despliegan en una barra horizontal deslizable (`LazyRow`) anclada directamente sobre el teclado, eliminando el menú vertical flotante que cubría el texto.
  - **Preservación del texto al insertar bloques:** Al usar `+ Bloque` en un párrafo con contenido, la app inserta un nuevo bloque inmediatamente debajo sin sobrescribir ni borrar el texto actual.
  - **Prevención de duplicación de texto y bloques:** IDs estrictamente únicos (`UUID.randomUUID()`) por bloque, sanitización de caracteres `\n` al presionar Enter o borrar, e interceptación de eventos repetidos de composición de Gboard.

- **🎯 Unificación de Tarjetas de Repaso (Single Source of Truth):**
  - **Conexión centralizada a `RepasoGlobal.cola`:** Todos los contadores de repaso de la app se alimentan del mismo flujo en `Repositorio` (`tarjetasParaRepasar`).
  - **Pantalla de Inicio (Home):** El banner *"Tienes X tarjetas para repasar hoy"* refleja exactamente la cantidad de tarjetas vencidas. Si no hay tarjetas pendientes (`0`), el banner se **oculta completamente**.
  - **Menú Lateral & Pantalla de Notas:** El contador numérico del menú lateral y el chip de *"Repaso hoy"* en Notas se sincronizan al 100% con las tarjetas reales.

- **📱 Cierre automático del teclado al abrir el Menu Lateral:**
  - `LaunchedEffect` conectado a `drawer.targetValue` que remueve el foco y oculta el teclado desde el primer fotograma en que se inicia el gesto de apertura del menú.

- **🎨 Ajuste de contraste en Modo Oscuro:**
  - Colores de texto y resaltado (`ColoresNota`) optimizados con tonos pasteles de alto contraste en el tema oscuro para una legibilidad sin fatiga visual.

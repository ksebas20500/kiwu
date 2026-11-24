# Kiwu para Android 0.9.1 · Correcciones de tarjetas "Para hoy", Drawer de navegación y Ajustes

Se instala encima de la 0.9.0 y conserva los datos. La base de datos no cambia.

### Arreglos y Correcciones

- **📅 Corrección en la tarjeta "Para hoy" (Pantalla de inicio):**
  - **Eventos pasados filtrados:** Los eventos de Google Calendar o calendario local de días anteriores ya no aparecen en la tarjeta de "Para hoy". Únicamente se muestran los eventos que corresponden al día de hoy.
  - **Diferenciación visual Evento vs. Tarea:** Los eventos de hoy se representan con un ícono circular de calendario (`Icons.Rounded.Event`) y su hora programada, eliminando el checkbox. Las tareas normales conservan su casilla interactiva y las tareas vencidas incorporan una etiqueta discreta en rojo (`Vencida · fecha`).
  - **Priorización:** Se muestran primero los eventos de hoy, seguidos por las tareas de hoy y luego las tareas vencidas (máximo 3 elementos).

- **🗂️ Restauración del menú contextual `⋮` en el menú lateral (Drawer):**
  - Se restauraron los botones de opciones `⋮` compactos a la derecha de cada fila de **carpetas, listas, cuadernos y horarios** en la sección "TUS ESPACIOS".
  - Los menús de listas y carpetas vuelven a ofrecer acciones para editar y eliminar. Las listas se pueden mover entre carpetas, y desde el menú de una carpeta se puede crear una lista directamente con «Nueva lista aquí».
  - Se incluyó un selector opcional de carpeta al crear una nueva lista.

- **🚫 Solución a pantalla en blanco al tocar eventos de Google:**
  - Se deshabilitó la navegación a la pantalla de detalle de tareas al tocar eventos externos de Google Calendar o Google Tasks, evitando que la app muestre una pantalla vacía.

- **🎨 Refactorización completa del módulo de Ajustes:**
  - Reorganización en 3 tarjetas contenedoras (*Personalización y uso*, *Cuenta y datos*, *Aplicación*) con bordes redondeados y divisores sutiles.
  - Velo (scrim) automático para garantizar legibilidad cuando se usa un fondo de imagen personalizado.
  - Banner destacado e independiente para *"Quitar anuncios"*.
  - Formulario de cuenta/login rediseñado con prioridad para *"Continuar con Google"*, toggle de visibilidad de contraseña y selector compacto.
  - Alerta prioritaria para el permiso de alarmas exactas (Android 12+) y diálogos de confirmación al restaurar/importar copias de seguridad.

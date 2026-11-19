# Kiwu para Android 0.9.0 · Eventos, Resumen Semanal y Mejoras de Usabilidad

Se instala encima de la 0.8.2 y conserva los datos. La base de datos no cambia.

### Novedades

- **📅 Eventos de Google Calendar unificados:** Los eventos externos de Google Calendar ahora aparecen reflejados de forma consistente dentro de la sección de eventos y la vista del calendario.
- **📊 Resumen semanal:** Se incluye la opción del resumen semanal (con preparación de interfaz y arquitectura hacia la futura pantalla principal "Home").
- **💥 Animación estilo cómic al completar tareas (Swipe):** Al deslizar una tarea hacia la derecha para completarla, el fondo actúa como barra de progreso y muestra una animación estilo manga/cómic con nubes e insignia `Complete ✔`.
- **📅 Fecha de examen en notas/tarjetas:** Ahora puedes asignar una fecha de examen directamente en la pantalla de tarjetas de cualquier nota. El sistema de Repaso Espaciado (*Repasar hoy*) elevará automáticamente su prioridad a medida que se acerque la fecha.
- **🎴 Mejoras en el flujo de Repaso:** Clarificación de la opción "Repasar todas" e icono descriptivo de tarjetas de estudio.

### Arreglos y Ajustes

- **🎨 Modo Oscuro y Tema:** Se eliminó la opción del tema Negro (AMOLED) dejando un modo oscuro unificado. Se corrigió la visibilidad del texto de racha en modo oscuro.
- **⏪ Posponer tareas por Swipe:** Corregida la lógica al deslizar a la izquierda en tareas programadas a futuro para posponerlas día a día sin adelantar su fecha.
- **📱 AdMob en pruebas:** Inyección de IDs de prueba en compilaciones de depuración (*debug*) para asegurar que los anuncios de test carguen correctamente.
- **📐 Maquetación del Calendario:** Corregido el solapamiento visual que ocurría en la barra superior del calendario al solicitar permisos de Google Calendar.

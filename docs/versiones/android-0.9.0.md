# Kiwu para Android 0.9.0 · Inicio, portadas de cuadernos y mejoras visuales

Se instala encima de la 0.8.2 y conserva los datos. Room incorpora las migraciones de v9 a v13. Incluyen faltas sincronizables, sesiones de estudio, color de listas y foto y color de portada de cuadernos. `versionCode = 33`.

### Novedades

- **Inicio:** La barra inferior reúne Inicio, Tareas, Notas y Horario. Inicio muestra un saludo según la hora, la fecha y accesos a la racha y al temporizador. La tarjeta «Siguiente clase» muestra la clase en curso o próxima con su horario, aula y faltas. «Para hoy» reúne tareas próximas y el acceso al repaso. El carrusel muestra hasta cuatro cuadernos ordenados por última actualización.
- **Notas:** La estantería presenta los cuadernos como libros con lomo y permite elegir una foto o un color de portada. Dentro del cuaderno, las páginas se muestran en una lista con icono, título, extracto, fecha y acceso a favoritas.
- **Editor:** La barra de formato incorpora «+ Bloque». El menú vertical de comandos `/` admite desplazamiento y tiene una altura máxima de 280 dp. Con una imagen de fondo personalizada, el editor aplica un velo al 90 %.
- **Ajustes y cuenta:** Se renovaron la presentación de Ajustes y los formularios de acceso y registro.

- **📅 Eventos de Google Calendar unificados:** Los eventos externos de Google Calendar ahora aparecen reflejados de forma consistente dentro de la sección de eventos y la vista del calendario.
- **📊 Resumen semanal:** Se incluye la opción del resumen semanal.
- **💥 Animación estilo cómic al completar tareas (Swipe):** Al deslizar una tarea hacia la derecha para completarla, el fondo actúa como barra de progreso y muestra una animación estilo manga/cómic con nubes e insignia `Complete ✔`.
- **📅 Fecha de examen en notas/tarjetas:** Ahora puedes asignar una fecha de examen directamente en la pantalla de tarjetas de cualquier nota. El sistema de Repaso Espaciado (*Repasar hoy*) elevará automáticamente su prioridad a medida que se acerque la fecha.
- **🎴 Mejoras en el flujo de Repaso:** Clarificación de la opción "Repasar todas" e icono descriptivo de tarjetas de estudio.

### Arreglos y Ajustes

- **🎨 Modo Oscuro y Tema:** Se eliminó la opción del tema Negro (AMOLED) dejando un modo oscuro unificado. Se corrigió la visibilidad del texto de racha en modo oscuro.
- **⏪ Posponer tareas por Swipe:** Corregida la lógica al deslizar a la izquierda en tareas programadas a futuro para posponerlas día a día sin adelantar su fecha.
- **📱 AdMob en pruebas:** Inyección de IDs de prueba en compilaciones de depuración (*debug*) para asegurar que los anuncios de test carguen correctamente.
- **📐 Maquetación del Calendario:** Corregido el solapamiento visual que ocurría en la barra superior del calendario al solicitar permisos de Google Calendar.

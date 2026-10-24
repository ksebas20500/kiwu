## Kiwu para Android 0.4.1 · racha, guía de inicio y widgets

Se instala encima de la 0.3.2 conservando los datos. No cambia la base de datos (sigue en la versión 4) ni el formato de la copia `.kiwu.json`.

### Novedades

#### Racha y guía de inicio

- **Racha 🔥:** cuenta los días seguidos en los que completas al menos una tarea. Se ve con la llama y el número arriba en Tareas y en Notas, y arriba del menú de Tareas. Al tocarla se abre su pantalla con la semana, tu mejor racha, los días cumplidos, las tareas hechas, la siguiente meta y un calendario por meses. Se calcula con tu historial, así que puede empezar con días que ya tenías.
- **Celebración:** al completar la primera tarea del día, una animación con la llama, chispas y el número de días subiendo.
- **Aviso de la racha:** a las 20:00, solo si hoy aún no has completado nada y tienes una racha que perder. Se desactiva en Ajustes → Avisos.
- **Guía de inicio:** la primera vez que abres la app, unas pantallas cortas explican lo principal y se crea la lista «Empieza aquí» con tareas para aprender usándola. Puedes repetirla en Ajustes → Acerca de.

#### Widgets

- **Widget de deberes (4×2):** las tareas pendientes ordenadas por fecha, con las vencidas primero y su lista debajo. La cabecera abre Tareas, el título abre la tarea y el círculo la completa sin abrir la app. Las tareas sin fecha van al final y los eventos no aparecen (no se completan). Se adapta al alto: menos filas en un widget pequeño y un aviso cuando no hay pendientes.
- **Widget de horario (4×2):** una tarjeta con la clase de **ahora** o, si no hay ninguna, la **siguiente** (con el día cuando no es hoy: «Mañana», «Mar»…), su hora, aula y color. Debajo, las clases de hoy con un punto rojo si esa materia tiene tareas vencidas, ámbar si vencen pronto y verde si no tiene. Tocar el widget abre la pestaña Horario.
- **Widget de nota rápida (2×2):** botón «+ Nueva» que crea una nota y la abre directamente en el editor, y las notas más recientes con su emoji y un resumen. La cabecera abre Notas y cada nota se abre con un toque.

#### Solo en la versión de Google Play

- **Aviso de versión nueva:** al abrir Kiwu o volver a ella, si Google Play tiene una versión más nueva sale «Hay una versión nueva de Kiwu». **Actualizar** la descarga mientras sigues usando la app y luego pide **Reiniciar**; **Más tarde** no vuelve a preguntar por esa versión en un día.
- **Buscar actualizaciones** en Ajustes → Acerca de: pregunta en el momento y, si Google no responde, abre la ficha de Kiwu en Play.

### Configuración de los widgets

Se añaden como cualquier widget (mantener pulsado el fondo → **Widgets** → **Kiwu**) y al soltarlos se abre la configuración:

- **Deberes:** todas las tareas, solo hoy, próximos 7 días, una lista o una carpeta. La lista especial «Eventos» no se ofrece porque sus elementos no se completan.
- **Horario:** todos los horarios juntos o uno solo (el nombre se ve en la cabecera).
- **Nota rápida:** todos los cuadernos o solo uno (el cuaderno donde se crean las notas nuevas).

Si se vuelve a configurar un widget ya puesto (mantener pulsado → **Settings**), la pantalla marca lo que tenía guardado. La elección se guarda por widget, así que se pueden tener varios con ajustes distintos.

### Refresco y apariencia de los widgets

- Siguen el tema (claro, oscuro o el del sistema) y el color de la app; con el acento «sistema» usan los colores dinámicos de Android 12 o superior. Las clases usan su color (o el automático) y los avisos urgentes, el rojo o el ámbar del tema.
- Se actualizan solos cuando cambian las tareas, las listas, las notas, el horario o los ajustes, con un pequeño retardo para agrupar los cambios.
- Además se refrescan cada 30 minutos y con una alarma al próximo cambio de clase o a medianoche, para que «Ahora / Siguiente» y los puntos de urgencia no se queden viejos. Al reiniciar el teléfono o actualizar la app se vuelve a programar esa alarma.
- Completar desde el widget guarda el cambio con el mismo repositorio que la app: se cancela el recordatorio de esa tarea y se refrescan los tres widgets (también los puntos del horario).

### Cambios

- Ajustes → Menú de Tareas tiene el interruptor «Mostrar la racha».

### Limitaciones conocidas

- La racha es solo tuya y de este teléfono; la racha con amigos llegará cuando haya cuentas.
- Los eventos no cuentan para la racha (no se completan).
- El aviso de versión nueva solo existe desde esta versión y solo con la app instalada desde Google Play: quien tenga una versión anterior tiene que actualizar a esta a mano una vez.
- El tamaño final de los widgets lo decide el lanzador: en uno muy pequeño caben menos tareas y el horario puede recortar las clases de hoy.
- El widget de horario enseña la clase de hoy o la próxima de la semana; no se puede elegir otro día (para eso está la pestaña Horario).
- El *Calendario de deberes* (mes con puntos) queda para más adelante, junto con un widget de racha.
- En Android 8 y 9 el lanzador no ofrece volver a configurar un widget ya puesto (se quita y se añade de nuevo).
- Los widgets leen la base de datos de la app en el mismo proceso; no hay widget en la pantalla de bloqueo ni versión para Wear.

## Kiwu para Android 0.5.4 · racha que no retrocede, eventos desde el widget y fechas completas

Se instala encima de la 0.5.3 y conserva los datos. La base de datos sigue en la **versión 5**: no hay migración.

### Novedades

- **Eventos desde el widget de Calendario:** en la hoja del día (al tocar un día del widget) hay un botón **«Evento»**. Con él activado, lo que escribes se crea como evento y no como tarea. Los eventos van siempre en **azul claro pastel**, en el widget, en la hoja del día y en la lista de tareas.
- **Fechas completas en Deberes:** las tareas con fecha muestran el día de la semana, el número y el mes, por ejemplo «jueves 19 de septiembre» (con el año solo si no es el actual). Hoy, Mañana y Ayer se quedan como estaban. Vale para la lista, el selector de fecha al crear una tarea y las tareas de una clase.
- **Widget de Deberes:** cada tarea muestra su fecha con día y mes: «Vie 2 oct», y las pasadas «Vencida · 28 sep».

### Arreglos

- **La racha ya no baja por tocar tareas viejas:** desmarcar una tarea antigua podía romper todos los días desde su fecha y dejar la racha en 0. Ahora los días que ya contaron como cumplidos se guardan en el teléfono y no se pierden. La racha solo baja si desmarcas una tarea **de hoy**.
- **La racha no «pierde» el día de ayer:** completar después de medianoche una tarea que vencía ayer ya no lo cuenta como día roto (antes pasaba de 2 días a 1).

### Limitaciones

- Una racha que ya se había roto antes de esta versión no se recupera.
- Los días guardados de la racha viven solo en el teléfono: no se sincronizan con otros dispositivos.
- Las tareas repetitivas tipo hábito todavía no existen.

### Para quien compila

Sin cambios respecto a la 0.5.0: `firebase.apiKey`, `firebase.projectId` y `google.webClientId` van en `android/local.properties`.

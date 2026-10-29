## Kiwu para Android 0.5.5 · Google Calendar en el widget

Se instala encima de la 0.5.4 y conserva los datos. La base de datos sigue en la **versión 5**: no hay migración.

### Novedades

- **Google Calendar en el widget de Calendario:** en Ajustes → Copia de seguridad hay un interruptor «Calendario de Google». Al activarlo (pide el permiso de calendario) los eventos de Google Calendar de este teléfono salen en el widget en el mismo azul pastel de los eventos de Kiwu, y en la hoja del día al tocar una casilla.
- Es **solo lectura**: Kiwu no guarda ni modifica nada de tu Google Calendar. Para editar un evento, ábrelo en Google Calendar.

### Para tener en cuenta

- Usa los calendarios que el teléfono ya tiene sincronizados (cualquier cuenta de Google añadida al teléfono). No hay inicio de sesión aparte.
- Un evento de varios días solo aparece en su primer día.
- El widget se actualiza al cambiar de mes, a medianoche o cuando cambian los datos de Kiwu; un evento nuevo de Google puede tardar en verse hasta la próxima actualización.
- Permiso nuevo: `READ_CALENDAR`. Hay que declararlo en la ficha de Play (seguridad de los datos).
- Las «Tareas» de Google Calendar (Google Tasks) **todavía no salen**: son otro servicio de Google y se añadirán más adelante.

### Para quien compila

Sin cambios respecto a la 0.5.0.

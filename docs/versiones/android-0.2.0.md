## Kiwu para Android 0.2.0 · más fluida, con fondo encuadrable y segundo aviso

Segunda versión de prueba de Kiwu para Android. Arregla un fallo importante de la navegación y añade mejoras que pidieron las personas que la probaron.

### Archivos

| Archivo | Para qué sirve |
| --- | --- |
| `kiwu-android-0.2.0.apk` | **Recomendado.** Versión optimizada con R8. Identificador `app.kiwu.android.min` |
| `kiwu-android-0.2.0-debug.apk` | Versión de depuración. Más lenta; solo para diagnosticar fallos. Identificador `app.kiwu.android` |
| `SHA256SUMS.txt` | Sumas de verificación de los dos APK |

Se instala encima de la 0.1.0 (mismo identificador y misma firma), así que conserva tus datos. Aun así, **haz antes una copia desde Ajustes → Exportar**.

### Arreglos

- **Pantalla en blanco al volver.** Al salir de una tarea o nota y tocar enseguida el menú, la app se quedaba solo con el fondo. Ahora cada pantalla solo se cierra una vez.
- **El fondo de imagen ya no se pierde** al cambiar de aplicación o minimizar: la app guarda su propia copia de la foto.
- En el editor, el menú del `/` se desplaza para no quedar tapado por el teclado.
- El código ya no tiene barra deslizante: las líneas largas saltan de línea.
- El recuadro de «Añadir tarea» ya no se ensancha a lo alto al elegir una fecha.

### Novedades

- **Encuadrar el fondo:** al elegir una imagen se mueve con un dedo y se amplía con dos para elegir qué parte se ve. Botón **Encuadrar** para repetirlo.
- **Deslizadores del fondo fluidos:** la visibilidad y el velo se aplican al instante al arrastrar.
- **Segundo aviso** (Ajustes → Recordatorios): otro recordatorio 5, 10, 15, 30 o 60 minutos antes de la entrega, para todas las tareas con fecha.
- **Fecha y hora separadas** en la tarea, con la opción «Todo el día».
- **Ayuda visible** en el editor: «Escribe aquí o usa / para insertar bloques», sin tener que tocar antes.
- Transiciones suaves entre Tareas, Notas y Horario, y entre la vista de día y la de semana.
- Nuevo icono y pantalla de arranque.

### Limitaciones conocidas

- Sin sincronización entre dispositivos todavía (ver `docs/plan-sincronizacion.md`).
- El tablero no se ofrece en teléfonos (solo en pantallas anchas).
- Los textos están escritos en español en el código: aún no hay traducción.
- Los APK van firmados con la clave de depuración.

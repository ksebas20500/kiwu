## Kiwu para Android 0.5.3 · widget de calendario más claro

Se instala encima de la 0.5.2 y conserva los datos. La base de datos sigue en la **versión 5**: no hay migración.

### Arreglos

- **Widget de Calendario:** cada día del mes es ahora una **casilla con fondo propio** y esquinas redondeadas, así que se distinguen bien los días, sobre todo en modo oscuro. Los días de relleno de otros meses quedan sin casilla.
- **Hoja del día (al tocar un día del widget):** el campo «Añadir tarea» **ya no queda tapado por los botones de navegación** del teléfono y sube con el teclado al escribir.

### Para quien compila

Sin cambios respecto a la 0.5.0: `firebase.apiKey`, `firebase.projectId` y `google.webClientId` van en `android/local.properties`.

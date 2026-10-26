## Kiwu para Android 0.5.0 · cuenta, sincronización y copias en Drive

Se instala encima de la 0.4.3 y conserva los datos. La base de datos sigue en la **versión 5** (las tablas de sincronización se crean al abrir). Todo es opcional: sin cuenta, Kiwu funciona igual que antes.

### Novedades

- **Cuenta y sincronización** (Ajustes → Cuenta y sincronización): inicia sesión con correo y contraseña o con **Google** y ve las mismas listas, tareas, notas, horarios y clases en tu teléfono y tu tablet. Lo que cambias llega a tus otros dispositivos en menos de un minuto con la app abierta.
- Solo viaja el **texto**: las imágenes, los archivos adjuntos y las faltas de clase se quedan en cada dispositivo. Si dos dispositivos editan lo mismo a la vez, gana la edición más reciente.
- **Borrar cuenta y datos de la nube** desde la misma pantalla; lo del dispositivo se queda.
- **Copia de seguridad en Google Drive** (Ajustes → Copia de seguridad): «Guardar en Drive» y «Restaurar de Drive». Es el mismo archivo `.kiwu.json`, un único archivo en «Mi unidad»; Kiwu solo ve ese archivo.
- Los widgets ya no se confunden entre sí al tocarlos (antes uno podía abrir la sección de otro).
- La nota de privacidad de Ajustes y la política de privacidad explican la cuenta y Drive.

### Para compilar

`firebase.apiKey`, `firebase.projectId` y `google.webClientId` van en `android/local.properties` (ver `local.properties.example`). Sin ellos la app compila, sin cuenta ni Google.

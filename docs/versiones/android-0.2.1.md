## Kiwu para Android 0.2.1 · arreglos para tablets

Versión de corrección sobre la 0.2.0. Se instala encima de ella conservando los datos.

### Archivos

| Archivo | Para qué sirve |
| --- | --- |
| `kiwu-android-0.2.1.apk` | **Recomendado.** Versión optimizada con R8. Identificador `app.kiwu.android.min` |
| `kiwu-android-0.2.1-debug.apk` | Versión de depuración. Más lenta; solo para diagnosticar fallos. Identificador `app.kiwu.android` |
| `SHA256SUMS.txt` | Sumas de verificación de los dos APK |

### Arreglos

- **Tablets en horizontal:** se reforzó la pantalla para encuadrar el fondo, que podía cerrar la app al usar una foto vertical en una pantalla horizontal grande. El encuadre ya no falla por redondeos, por cambios de tamaño de la ventana ni por falta de memoria, y el fondo se limita a un tamaño que cualquier pantalla soporta.
- Si algo falla al guardar el fondo, la app vuelve atrás en lugar de cerrarse.

### Limitaciones conocidas

- El cierre original se observó en una tablet real y no se pudo reproducir en el emulador, así que si vuelve a ocurrir, el informe de fallos de Google Play o `adb logcat` ayudarán a localizarlo.
- Sin sincronización entre dispositivos todavía; el tablero no se ofrece en teléfonos.

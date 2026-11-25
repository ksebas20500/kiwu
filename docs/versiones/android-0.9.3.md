# Kiwu para Android 0.9.3 · Widgets y agenda de tareas

`versionCode = 36`. Requiere Android 8.0 (API 26) o superior. La actualización desde la 0.9.2 conserva los datos y usa el esquema Room v13 existente.

## Widgets de escritorio

- **Los cuatro widgets** usan superficies translúcidas para dejar ver el fondo del teléfono. La opacidad es del 84 % en modo oscuro y del 90 % en claro. Horario, Deberes y Calendario llevan un borde de cristal de 1 dp. Los widgets se renderizan con Jetpack Glance 1.2.0.
- **Horario** presenta la clase actual o siguiente en dos columnas, con el aula y las faltas en píldoras. La sesión destacada aparece una sola vez. Las filas de hoy muestran el color de la materia o el acento del widget y se atenúan al terminar. El nombre del horario sigue disponible cuando se muestran varios.
- **Deberes** muestra el filtro activo y el total de pendientes en una píldora. El botón de añadir tiene un área táctil de 48 dp. Las fechas quedan a la derecha con énfasis para hoy, mañana y vencidas. Las filas se separan con un divisor y omiten el nombre de la carpeta cuando ya es el filtro de la cabecera.
- **Calendario** mantiene la cuadrícula mensual. En formatos amplios, tocar un día actualiza la agenda lateral, desde la que se pueden abrir, completar o añadir tareas. En formatos estrechos, tocar el día abre la agenda flotante. Los puntos debajo de las fechas ocupan una altura fija. Cada actualización escribe el fondo del círculo de selección para limpiar el resaltado anterior. La agenda lateral usa títulos de 11 sp y destina más ancho al texto en paneles compactos.
- **Nota rápida** muestra el cuaderno de destino cuando el widget presenta todas las notas. Sus filas y el botón «Nueva» tienen 48 dp de alto. «Nueva» abre el editor de Kiwu.
- **Configuración** de Deberes, Horario y Nota rápida se guarda por instancia. Deberes agrupa filtros rápidos, carpetas y listas, y muestra la carpeta en el selector. La pantalla presenta estados de carga, guardado y error.

## Captura rápida desde Deberes y Calendario

- La tarjeta flotante enfoca el título y permite introducir una descripción opcional, prioridad, fecha y destino. La descripción se guarda como nota de la tarea y se escribe en la misma operación que los demás campos.
- El selector «Elegir lista» se abre dentro de la tarjeta, agrupa las listas por carpeta y marca la selección actual. Al volver al formulario se conservan el texto, la fecha y la prioridad. Deberes respeta la lista fija y prioriza las listas de la carpeta configurada.
- Mientras se guarda, se bloquean los envíos repetidos. Si falla el guardado, el formulario mantiene el borrador. La captura directa desde `+` se cierra al guardar o descartar. La captura abierta desde una agenda explícita vuelve a esa agenda. Desde Calendario, el día queda fijado y la hora inicial es las 18:00.

## Carpetas y calendario dentro de Kiwu

- Las tareas de listas parecidas se distinguen por su carpeta en la agenda del widget, Deberes y las vistas generales de la app. La lista y el tablero muestran esa etiqueta en «Todas las tareas» y «Próximos 7 días». El calendario interno la muestra tanto en pendientes como en completadas. Las listas sueltas conservan su nombre.
- El mes y la agenda del calendario interno comparten un desplazamiento. El campo de creación reserva su propio espacio encima del anuncio. Al seleccionar un día, la agenda se desplaza si queda fuera de vista o tiene poco espacio. El flujo se aplica con y sin permiso de Google Calendar.

## Verificación

- El usuario confirmó la corrección de color y que la cuadrícula del widget volvía a aparecer. También confirmó que la identificación por carpeta funcionaba.
- La prueba focalizada de `DatosWidgetsTest` pasó en una revisión previa e incluye la regresión de tareas homónimas. Compilaciones posteriores de `:app:compileDebugKotlin --offline` pasaron para la interfaz de Glance y Compose, incluida la corrección del resaltado del calendario.
- Los últimos ajustes de tipografía y espaciado de la agenda lateral no se recompilaron ni se probaron en el launcher durante esta sesión.

[Documentación de widgets](../plan-widgets-android.md) · [Notas para Google Play](../play-console-release-notes-0.9.3.md)

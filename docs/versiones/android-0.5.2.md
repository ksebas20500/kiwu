## Kiwu para Android 0.5.2 · widget de calendario y «Posponer a mañana»

Se instala encima de la 0.5.1 y conserva los datos. La base de datos sigue en la **versión 5**: no hay migración.

### Novedades

#### Widget de Calendario

- **Nuevo widget «Calendario»:** el mes con las **tareas y eventos pendientes de todas tus listas**, cada uno en su día. Las tareas vencidas salen en rojo y hoy va resaltado. Las flechas ‹ › cambian de mes y tocar el nombre del mes vuelve a hoy.
- **Toca un día** y se abre una hoja sobre el escritorio, sin abrir Kiwu, con lo que hay ese día: puedes **completar** una tarea con su casilla, tocarla para abrirla en la app y **añadir una tarea nueva** con el mismo campo de la app (lista y hora opcionales; sin hora vence a las 18:00).
- Cuántas tareas caben en cada casilla depende del tamaño del widget: si no cabe ninguna, se marca un punto en los días con algo, y si hay más de las que caben sale «+N».
- Se actualiza al momento cuando cambias algo en la app.

#### Avisos

- **«Posponer a mañana»** en el aviso de una tarea, junto a *Completar* y *Posponer 1 h*: el aviso vuelve a sonar **mañana a las 8:00**, o 30 minutos antes de la entrega si esta cae mañana antes de las 8:00. Sirve para el aviso de «un día antes»: lo pospones y te vuelve a salir al despertar, el día de la entrega.
- Un aviso pospuesto a mañana **sobrevive a un reinicio del teléfono**: se guarda y se reprograma al arrancar.

#### Arreglos

- **Menú de los tres puntos de una tarea:** mostraba «Mover a «2131689673»» con números del sistema en lugar de los nombres de las columnas. Ahora el menú solo tiene **Eliminar tarea**; mover una tarea de columna se hace arrastrándola (pulsación larga) en el tablero o desde el selector de columna del detalle.
- **Guía de inicio en modo oscuro:** el título de cada pantalla («Te damos la bienvenida a Kiwu»…) salía negro sobre el fondo oscuro y no se veía.

### Para quien compila

Sin cambios respecto a la 0.5.0: `firebase.apiKey`, `firebase.projectId` y `google.webClientId` van en `android/local.properties`.

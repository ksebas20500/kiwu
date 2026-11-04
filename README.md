# Kiwu para Android

App nativa de Android (Kotlin, Jetpack Compose, Room). Mismas funciones que la de escritorio, con widgets de pantalla de inicio. Para ver quÃ© hace Kiwu, la ruta de desarrollo estÃ¡ en [docs/android-plan.md](docs/android-plan.md).

VersiÃ³n actual: **0.7.5** Â· Android 8.0 (API 26) o superior.

> Novedades de **0.7.5**: el Pomodoro se puede **pausar y reanudar**, con notificaciÃ³n permanente en curso, **descansos de 5 min**, **Â«No molestarÂ»** y color del nÃºmero; el **widget de Deberes** ahora tambiÃ©n muestra los **eventos** al fijarlo a una lista o carpeta; y por las festividades de octubre estrenamos **icono nuevo**: el gato mago ðŸ§™â€â™‚ï¸ðŸ± (tambiÃ©n dentro de la app y como silueta para las notificaciones; el arte para Play Store estÃ¡ en `play_store_512.png`).

## QuÃ© incluye

| MÃ³dulo | QuÃ© ofrece |
| --- | --- |
| **Tareas** | Listas por materia agrupables en **carpetas** (se crean dentro, se arrastran con pulsaciÃ³n larga y llevan emoji opcional), fecha con **hora opcional**, vistas Hoy / PrÃ³ximos 7 dÃ­as / Todas / Completadas, en **lista o tablero** (Por hacer Â· En curso Â· Hecho) |
| **Eventos** | Lista fija Â«EventosÂ» para cosas que pasan y no se completan (cumpleaÃ±os, charlas): salen en el calendario, Hoy y PrÃ³ximos y en el widget de Deberes (con su pastilla azul), sin cÃ­rculo y sin contar como pendientes |
| **Calendario** | Mes completo con las entregas en colores pastel y creaciÃ³n rÃ¡pida en el dÃ­a elegido |
| **Notas** | Cuadernos, pÃ¡ginas con emoji, favoritas, bÃºsqueda y editor por bloques con menÃº `/`, formato en lÃ­nea, cÃ³digo con colores, enlaces con vista previa (web, YouTube, Vimeo), imÃ¡genes y archivos adjuntos |
| **Archivos** | Carpeta del dispositivo vinculada a cada lista, con explorador integrado |
| **Racha** | DÃ­as seguidos Â«al dÃ­aÂ» (sin tareas vencidas): cuentan los dÃ­as con entregas y los dÃ­as con clase en que completas o aÃ±ades una tarea; los dÃ­as libres no suman ni rompen. Arriba del menÃº de Tareas, con la mejor racha y las Ãºltimas 8 semanas; aviso opcional a las 20:00 si hoy quedan entregas |
| **Estudio** | Temporizador de Pomodoro (25/50/5/10 min) que avisa al terminar aunque cierres la app. Mientras corre sale en la barra de notificaciones y en la pantalla de bloqueo con el tiempo que queda y botÃ³n *Parar*; se puede **pausar y reanudar**, arrancar un **descanso de 5 min** que no cuenta como estudio, activar **Â«No molestarÂ»** mientras estudia (silencia el telÃ©fono) y elegir el **color del nÃºmero**. El tiempo estudiado se apunta por lista en la semana. Calificaciones con promedio ponderado y Â«Â¿cuÃ¡nto necesito?Â», y tarjetas de repaso |
| **GuÃ­a de inicio** | Al abrir la app por primera vez: unas pantallas de bienvenida y la lista de ejemplo Â«Empieza aquÃ­Â»; se puede repetir desde Ajustes â†’ Acerca de |
| **Recordatorios** | Notificaciones con las acciones *Completar* y *Posponer 1 h*; se reprograman al reiniciar el telÃ©fono |
| **Ajustes** | Ordenados por secciones (Apariencia, Idioma, MenÃº de Tareas, Avisos, Copia de seguridad, Acerca de): tema claro/oscuro/sistema, color, fondo de imagen con velo, aspecto y orden del menÃº, segundo aviso y copia de seguridad `.kiwu.json` (el mismo formato que Linux) |
| **Widgets** | Cuatro widgets de pantalla de inicio: **Deberes** (pendientes por fecha, con casilla para completar y botÃ³n Â«+Â» para aÃ±adir una tarea sin abrir la app; los **eventos** de la app tambiÃ©n salen, mezclados por fecha con su pastilla azul, sin casilla y sin contar en el nÃºmero del encabezado), **Horario** (Â«Ahora / SiguienteÂ» y clases de hoy con sus tareas urgentes) **Nota rÃ¡pida** (crea una nota y muestra las recientes) y **Calendario** (el mes con lo pendiente de todas las listas; al tocar un dÃ­a ves lo que hay, puedes completar tareas y aÃ±adir una sin abrir la app). Los otros se configuran al aÃ±adirlos: todas, una lista/carpeta, un horario o un cuaderno |

| **Faltas de clase** | MÃ¡ximo de faltas por materia, chip Â«Faltas: NÂ» en la hoja de la clase, contador Â«quedan NÂ» en el horario y en el widget, faltas justificadas que no cuentan, aviso al llegar al mÃ¡ximo y Â«Vaciar faltasÂ» al cambiar de semestre |
| **Idiomas** | EspaÃ±ol, inglÃ©s y portuguÃ©s. En Android 13 o superior sigue el idioma del telÃ©fono y se cambia en el ajuste de idioma de Android para Kiwu; en Android 8 a 12 se elige en Ajustes â†’ Idioma |

No incluye atajos de teclado editables (no aplican en el mÃ³vil).

## Compilar y ejecutar

Necesitas **JDK 17** y el **SDK de Android** (plataforma 35). Con Android Studio basta con abrir la carpeta del repositorio (la raíz es el proyecto Android). Desde la terminal:

```bash
cd kiwu
cp local.properties.example local.properties   # y pon la ruta de tu SDK en sdk.dir
./gradlew testDebugUnitTest      # pruebas (JVM, no necesitan emulador)
./gradlew assembleDebug          # APK de depuraciÃ³n en app/build/outputs/apk/debug/
./gradlew installDebug           # instalarlo en el dispositivo o emulador conectado
./gradlew lintDebug              # revisiÃ³n estÃ¡tica
```

La variante `minificado` es como la de publicaciÃ³n (R8) pero firmada con la clave de depuraciÃ³n, para probar que la minificaciÃ³n no rompe nada: `./gradlew installMinificado` (se instala aparte, con el sufijo `.min`).

## Estructura

```
app/src/main/java/app/kiwu/android/
  domain/    Modelos, formato de las notas, reglas de las tareas, recordatorios, enlaces, resaltado (Kotlin puro)
  data/      Room (borrado lÃ³gico), repositorio, ajustes y copia de seguridad
  notify/    Alarmas, notificaciones y reprogramaciÃ³n
  ui/        Pantallas de Compose: tareas, calendario, notas, editor, archivos y ajustes
  widgets/   Widgets de inicio (Glance): deberes, horario, nota rÃ¡pida y su configuraciÃ³n
app/src/test/   Pruebas: dominio, contrato compartido, editor, Room (Robolectric) y widgets
```

## Horarios

Tercera pestaÃ±a de la barra inferior. Se pueden crear varios horarios (Universidad, Trabajo, Ocioâ€¦) con clases o actividades que se repiten cada semana, en varios dÃ­as a la vez, con aula, notas y aviso opcional. En el telÃ©fono se ve un dÃ­a cada vez (o la semana entera con el botÃ³n de la barra); en pantallas anchas, la semana. Ver [docs/plan-horario.md](docs/plan-horario.md).

Desde la 0.4.3 cada materia puede llevar la cuenta de sus **faltas**: se marcan desde la hoja de la clase (hoy u otro dÃ­a), se pueden justificar y el horario muestra cuÃ¡ntas quedan antes del mÃ¡ximo. Ver [docs/plan-0.4.3.md](docs/plan-0.4.3.md).

## Widgets

Se aÃ±aden desde el lanzador (mantÃ©n pulsado el fondo â†’ Widgets â†’ Kiwu) y se configuran al ponerlos:

- **Deberes:** tareas pendientes ordenadas por fecha (las vencidas primero), con su lista y una casilla que las completa sin abrir la app. Se puede mostrar todo, solo hoy, los prÃ³ximos 7 dÃ­as, una lista o una carpeta.
- **Horario:** la clase de ahora o la siguiente (con dÃ­a, hora, aula y color) y las clases de hoy con un punto de urgencia si esa materia tiene tareas. Todos los horarios o solo uno.
- **Nota rÃ¡pida:** botÃ³n Â«+ NuevaÂ» para crear una nota en el cuaderno elegido y las notas mÃ¡s recientes. Todos los cuadernos o solo uno.

Siguen el tema y el color de la app, se refrescan solos cuando cambian los datos y, ademÃ¡s, cada 30 minutos y con una alarma al prÃ³ximo cambio de clase o a medianoche. Ver [docs/plan-widgets-android.md](docs/plan-widgets-android.md).

## Idiomas

Todos los textos estÃ¡n en `res/values/strings.xml` (espaÃ±ol, el idioma base), `values-en` y `values-pt`. Las fechas, los dÃ­as y los plurales salen del idioma activo. `Idioma.kt` decide quÃ© idioma usar y `lintDebug` falla si a una traducciÃ³n le falta un texto (`MissingTranslation`, `MissingQuantity`). Para aÃ±adir un texto nuevo hay que ponerlo en los tres archivos. En la rama de Google Play, los textos que solo existen allÃ­ (anuncios, compra y actualizaciones) van aparte, en `strings_play.xml`, para que unir `main` no choque.

## Datos y compatibilidad

- El modelo es el mismo que en Linux (esquema v2): UUID como identificador y **borrado lÃ³gico** (`eliminada`), listo para sincronizar.
- El formato de las notas es el JSON comÃºn de macOS y Linux. La prueba `DocumentoTest` lee las fixtures de [`docs/contrato`](docs/contrato/README.md) y exige el mismo resultado.
- La copia `.kiwu.json` se puede mover entre el telÃ©fono y el escritorio. Las carpetas vinculadas y los archivos adjuntos solo valen en el dispositivo donde se eligieron.

## Pendiente

- Firma de publicaciÃ³n, identificador definitivo y ficha de Google Play (ver [plan](docs/plan-desarrollo.md) Â§5).

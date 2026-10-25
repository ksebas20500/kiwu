# Kiwu para Android

App nativa de Android (Kotlin, Jetpack Compose, Room). Mismas funciones que la de escritorio, con widgets de pantalla de inicio. Para ver qué hace Kiwu, consulta el [README principal](../README.md); la ruta de desarrollo está en [docs/android-plan.md](../docs/android-plan.md).

Versión actual: **0.4.3** (en desarrollo) · Android 8.0 (API 26) o superior.

## Qué incluye

| Módulo | Qué ofrece |
| --- | --- |
| **Tareas** | Listas por materia agrupables en **carpetas** (se crean dentro, se arrastran con pulsación larga y llevan emoji opcional), fecha con **hora opcional**, vistas Hoy / Próximos 7 días / Todas / Completadas, en **lista o tablero** (Por hacer · En curso · Hecho) |
| **Eventos** | Lista fija «Eventos» para cosas que pasan y no se completan (cumpleaños, charlas): salen en el calendario, Hoy y Próximos, sin círculo y sin contar como pendientes |
| **Calendario** | Mes completo con las entregas en colores pastel y creación rápida en el día elegido |
| **Notas** | Cuadernos, páginas con emoji, favoritas, búsqueda y editor por bloques con menú `/`, formato en línea, código con colores, enlaces con vista previa (web, YouTube, Vimeo), imágenes y archivos adjuntos |
| **Archivos** | Carpeta del dispositivo vinculada a cada lista, con explorador integrado |
| **Racha** | Días seguidos completando al menos una tarea, arriba del menú de Tareas, con la mejor racha y las últimas 8 semanas; aviso opcional a las 20:00 si hoy aún falta |
| **Guía de inicio** | Al abrir la app por primera vez: unas pantallas de bienvenida y la lista de ejemplo «Empieza aquí»; se puede repetir desde Ajustes → Acerca de |
| **Recordatorios** | Notificaciones con las acciones *Completar* y *Posponer 1 h*; se reprograman al reiniciar el teléfono |
| **Ajustes** | Ordenados por secciones (Apariencia, Idioma, Menú de Tareas, Avisos, Copia de seguridad, Acerca de): tema claro/oscuro/sistema, color, fondo de imagen con velo, aspecto y orden del menú, segundo aviso y copia de seguridad `.kiwu.json` (el mismo formato que Linux) |
| **Widgets** | Tres widgets de pantalla de inicio: **Deberes** (pendientes por fecha, con casilla para completar sin abrir la app), **Horario** («Ahora / Siguiente» y clases de hoy con sus tareas urgentes) y **Nota rápida** (crea una nota y muestra las recientes). Se configuran al añadirlos: todas, una lista/carpeta, un horario o un cuaderno |

| **Faltas de clase** | Máximo de faltas por materia, chip «Faltas: N» en la hoja de la clase, contador «quedan N» en el horario y en el widget, faltas justificadas que no cuentan, aviso al llegar al máximo y «Vaciar faltas» al cambiar de semestre |
| **Idiomas** | Español, inglés y portugués. En Android 13 o superior sigue el idioma del teléfono y se cambia en el ajuste de idioma de Android para Kiwu; en Android 8 a 12 se elige en Ajustes → Idioma |

No incluye atajos de teclado editables (no aplican en el móvil).

## Compilar y ejecutar

Necesitas **JDK 17** y el **SDK de Android** (plataforma 35). Con Android Studio basta con abrir la carpeta `android/`. Desde la terminal:

```bash
cd android
cp local.properties.example local.properties   # y pon la ruta de tu SDK en sdk.dir
./gradlew testDebugUnitTest      # pruebas (JVM, no necesitan emulador)
./gradlew assembleDebug          # APK de depuración en app/build/outputs/apk/debug/
./gradlew installDebug           # instalarlo en el dispositivo o emulador conectado
./gradlew lintDebug              # revisión estática
```

La variante `minificado` es como la de publicación (R8) pero firmada con la clave de depuración, para probar que la minificación no rompe nada: `./gradlew installMinificado` (se instala aparte, con el sufijo `.min`).

## Estructura

```
android/app/src/main/java/app/kiwu/android/
  domain/    Modelos, formato de las notas, reglas de las tareas, recordatorios, enlaces, resaltado (Kotlin puro)
  data/      Room (borrado lógico), repositorio, ajustes y copia de seguridad
  notify/    Alarmas, notificaciones y reprogramación
  ui/        Pantallas de Compose: tareas, calendario, notas, editor, archivos y ajustes
  widgets/   Widgets de inicio (Glance): deberes, horario, nota rápida y su configuración
android/app/src/test/   Pruebas: dominio, contrato compartido, editor, Room (Robolectric) y widgets
```

## Horarios

Tercera pestaña de la barra inferior. Se pueden crear varios horarios (Universidad, Trabajo, Ocio…) con clases o actividades que se repiten cada semana, en varios días a la vez, con aula, notas y aviso opcional. En el teléfono se ve un día cada vez (o la semana entera con el botón de la barra); en pantallas anchas, la semana. Ver [docs/plan-horario.md](../docs/plan-horario.md).

Desde la 0.4.3 cada materia puede llevar la cuenta de sus **faltas**: se marcan desde la hoja de la clase (hoy u otro día), se pueden justificar y el horario muestra cuántas quedan antes del máximo. Ver [docs/plan-0.4.3.md](../docs/plan-0.4.3.md).

## Widgets

Se añaden desde el lanzador (mantén pulsado el fondo → Widgets → Kiwu) y se configuran al ponerlos:

- **Deberes:** tareas pendientes ordenadas por fecha (las vencidas primero), con su lista y una casilla que las completa sin abrir la app. Se puede mostrar todo, solo hoy, los próximos 7 días, una lista o una carpeta.
- **Horario:** la clase de ahora o la siguiente (con día, hora, aula y color) y las clases de hoy con un punto de urgencia si esa materia tiene tareas. Todos los horarios o solo uno.
- **Nota rápida:** botón «+ Nueva» para crear una nota en el cuaderno elegido y las notas más recientes. Todos los cuadernos o solo uno.

Siguen el tema y el color de la app, se refrescan solos cuando cambian los datos y, además, cada 30 minutos y con una alarma al próximo cambio de clase o a medianoche. Ver [docs/plan-widgets-android.md](../docs/plan-widgets-android.md).

## Idiomas

Todos los textos están en `res/values/strings.xml` (español, el idioma base), `values-en` y `values-pt`. Las fechas, los días y los plurales salen del idioma activo. `Idioma.kt` decide qué idioma usar y `lintDebug` falla si a una traducción le falta un texto (`MissingTranslation`, `MissingQuantity`). Para añadir un texto nuevo hay que ponerlo en los tres archivos. En la rama de Google Play, los textos que solo existen allí (anuncios, compra y actualizaciones) van aparte, en `strings_play.xml`, para que unir `main` no choque.

## Datos y compatibilidad

- El modelo es el mismo que en Linux (esquema v2): UUID como identificador y **borrado lógico** (`eliminada`), listo para sincronizar.
- El formato de las notas es el JSON común de macOS y Linux. La prueba `DocumentoTest` lee las fixtures de [`docs/contrato`](../docs/contrato/README.md) y exige el mismo resultado.
- La copia `.kiwu.json` se puede mover entre el teléfono y el escritorio. Las carpetas vinculadas y los archivos adjuntos solo valen en el dispositivo donde se eligieron.

## Pendiente

- Firma de publicación, identificador definitivo y ficha de Google Play (ver [plan](../docs/plan-desarrollo.md) §5).

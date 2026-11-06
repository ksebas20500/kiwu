# Kiwu para Android

App nativa de Android (Kotlin, Jetpack Compose, Room). Mismas funciones que la de escritorio, con widgets de pantalla de inicio. Para ver qué hace Kiwu, la ruta de desarrollo está en [docs/android-plan.md](docs/android-plan.md).

Versión actual: **0.7.8** · Android 8.0 (API 26) o superior.

## Qué incluye

| Módulo | Qué ofrece |
| --- | --- |
| **Tareas** | Listas por materia agrupables en **carpetas** (se crean dentro, se arrastran con pulsación larga y llevan emoji opcional), fecha con **hora opcional**, vistas Hoy / Próximos 7 días / Todas / Completadas, en **lista o tablero** (Por hacer · En curso · Hecho) |
| **Eventos** | Lista fija «Eventos» para cosas que pasan y no se completan (cumpleaños, charlas): salen en el calendario, Hoy y Próximos y en el widget de Deberes (con su pastilla azul), sin círculo y sin contar como pendientes |
| **Calendario** | Mes completo con las entregas en colores pastel y creación rápida en el día elegido |
| **Notas** | Cuadernos, páginas con emoji, favoritas, búsqueda y editor por bloques con menú `/`, formato en línea, código con colores, enlaces con vista previa (web, YouTube, Vimeo), imágenes y archivos adjuntos |
| **Archivos** | Carpeta del dispositivo vinculada a cada lista, con explorador integrado |
| **Racha** | Días seguidos «al día» (sin tareas vencidas): cuentan los días con entregas y los días con clase en que completas o añades una tarea; los días libres no suman ni rompen. Arriba del menú de Tareas, con la mejor racha y las últimas 8 semanas; aviso opcional a las 20:00 si hoy quedan entregas |
| **Estudio** | Temporizador de Pomodoro (25/50/5/10 min) que avisa al terminar aunque cierres la app. Mientras corre sale en la barra de notificaciones y en la pantalla de bloqueo con el tiempo que queda y botón *Parar*; se puede pausar y reanudar, arrancar un descanso de 5 min que no cuenta como estudio, activar «No molestar» mientras estudia (silencia el teléfono) y elegir el color del número. El tiempo estudiado se apunta por lista en la semana. **Calificaciones predictivas** con nota acumulada, promedio de lo calificado, barra de progreso segmentada, configuración de nota mínima y cálculo de nota necesaria para aprobar |
| **Guía de inicio** | Al abrir la app por primera vez: unas pantallas de bienvenida y la lista de ejemplo «Empieza aquí»; se puede repetir desde Ajustes → Acerca de |
| **Recordatorios** | Notificaciones con cuenta atrás en vivo (cronómetro), acciones *Completar* y *Posponer 1 h*; se reprograman al reiniciar el teléfono |
| **Ajustes** | Ordenados por secciones (Apariencia, Idioma, Menú de Tareas, Avisos, Copia de seguridad, Acerca de): tema claro/oscuro/sistema, color, fondo de imagen con velo, aspecto y orden del menú, segundo aviso y copia de seguridad `.kiwu.json` (el mismo formato que Linux) |
| **Widgets** | Cuatro widgets de pantalla de inicio: **Deberes** (pendientes por fecha, con casilla para completar y botón «+» para añadir una tarea sin abrir la app; los eventos de la app también salen, mezclados por fecha con su pastilla azul, sin casilla y sin contar en el número del encabezado), **Horario** («Ahora / Siguiente» y clases de hoy con sus tareas urgentes), **Nota rápida** (crea una nota y muestra las recientes) y **Calendario** (el mes con lo pendiente de todas las listas; al tocar un día ves lo que hay, puedes completar tareas y añadir una sin abrir la app) |
| **Faltas de clase** | Máximo de faltas por materia, chip «Faltas: N» en la hoja de la clase, contador «quedan N» en el horario y en el widget, faltas justificadas que no cuentan, aviso al llegar al máximo y «Vaciar faltas» al cambiar de semestre |
| **Idiomas** | Español, inglés y portugués. En Android 13 o superior sigue el idioma del teléfono y se cambia en el ajuste de idioma de Android para Kiwu; en Android 8 a 12 se elige en Ajustes → Idioma |

## Compilar y ejecutar

Necesitas **JDK 17** y el **SDK de Android** (plataforma 35). Con Android Studio basta con abrir la carpeta del repositorio (la raíz es el proyecto Android). Desde la terminal:

```bash
cd kiwu
cp local.properties.example local.properties   # y pon la ruta de tu SDK en sdk.dir
./gradlew testDebugUnitTest      # pruebas (JVM, no necesitan emulador)
./gradlew assembleDebug          # APK de depuración en app/build/outputs/apk/debug/
./gradlew installDebug           # instalarlo en el dispositivo o emulador conectado
./gradlew lintDebug              # revisión estática
```

## Estructura

```
app/src/main/java/app/kiwu/android/
  domain/    Modelos, formato de las notas, reglas de las tareas, recordatorios, enlaces, resaltado (Kotlin puro)
  data/      Room (borrado lógico), repositorio, ajustes y copia de seguridad
  notify/    Alarmas, notificaciones y reprogramación
  ui/        Pantallas de Compose: tareas, calendario, notas, editor, archivos y ajustes
  widgets/   Widgets de inicio (Glance): deberes, horario, nota rápida y su configuración
app/src/test/   Pruebas: dominio, contrato compartido, editor, Room (Robolectric) y widgets
```

## Datos y compatibilidad

- El modelo es el mismo que en Linux (esquema v2): UUID como identificador y **borrado lógico** (`eliminada`), listo para sincronizar.
- El formato de las notas es el JSON común de macOS y Linux. La prueba `DocumentoTest` lee las fixtures de [`docs/contrato`](docs/contrato/README.md) y exige el mismo resultado.
- La copia `.kiwu.json` se puede mover entre el teléfono y el escritorio. Las carpetas vinculadas y los archivos adjuntos solo valen en el dispositivo donde se eligieron.

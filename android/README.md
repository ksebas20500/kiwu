# Kiwu para Android

App nativa de Android (Kotlin, Jetpack Compose, Room). Mismas funciones que la de escritorio, sin widgets. Para ver qué hace Kiwu, consulta el [README principal](../README.md); la ruta de desarrollo está en [docs/android-plan.md](../docs/android-plan.md).

Versión actual: **0.2.0** (en desarrollo) · Android 8.0 (API 26) o superior.

## Qué incluye

| Módulo | Qué ofrece |
| --- | --- |
| **Tareas** | Listas por materia, fecha y hora, vistas Hoy / Próximos 7 días / Todas / Completadas, en **lista o tablero** (Por hacer · En curso · Hecho) |
| **Calendario** | Mes completo con las entregas en colores pastel y creación rápida en el día elegido |
| **Notas** | Cuadernos, páginas con emoji, favoritas, búsqueda y editor por bloques con menú `/`, formato en línea, código con colores, enlaces con vista previa (web, YouTube, Vimeo), imágenes y archivos adjuntos |
| **Archivos** | Carpeta del dispositivo vinculada a cada lista, con explorador integrado |
| **Recordatorios** | Notificaciones con las acciones *Completar* y *Posponer 1 h*; se reprograman al reiniciar el teléfono |
| **Ajustes** | Tema claro/oscuro/sistema, fondo de imagen con velo, copia de seguridad `.kiwu.json` (el mismo formato que Linux) |

No incluye widgets ni atajos de teclado editables (no aplican en el móvil).

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
android/app/src/test/   Pruebas: dominio, contrato compartido, editor y Room (Robolectric)
```

## Horarios

Tercera pestaña de la barra inferior. Se pueden crear varios horarios (Universidad, Trabajo, Ocio…) con clases o actividades que se repiten cada semana, en varios días a la vez, con aula, notas y aviso opcional. En el teléfono se ve un día cada vez (o la semana entera con el botón de la barra); en pantallas anchas, la semana. Ver [docs/plan-horario.md](../docs/plan-horario.md).

## Datos y compatibilidad

- El modelo es el mismo que en Linux (esquema v2): UUID como identificador y **borrado lógico** (`eliminada`), listo para sincronizar.
- El formato de las notas es el JSON común de macOS y Linux. La prueba `DocumentoTest` lee las fixtures de [`docs/contrato`](../docs/contrato/README.md) y exige el mismo resultado.
- La copia `.kiwu.json` se puede mover entre el teléfono y el escritorio. Las carpetas vinculadas y los archivos adjuntos solo valen en el dispositivo donde se eligieron.

## Pendiente

- Traducción al inglés (las cadenas aún están en el código de las pantallas).
- Firma de publicación, identificador definitivo y ficha de Google Play (ver [plan](../docs/plan-desarrollo.md) §5).

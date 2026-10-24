# Plan de Kiwu para Android 0.4.1 · widgets

Estado: **completado** (24 de octubre de 2026). Se publica en la **0.4.1** junto con la racha y la guía de inicio ([plan-0.4.1.md](plan-0.4.1.md)). Recupera lo que dejó escrito [plan-0.3.0.md](plan-0.3.0.md) §2 (widgets), con un cambio de alcance: el *Calendario de deberes* queda para más adelante y en su lugar entra *Nota rápida* (como en macOS). El widget de Horario sí se incluye, como pidió una persona que probó la app.

Son tres widgets de pantalla de inicio con **Jetpack Glance**:

| Widget | Qué hace |
| --- | --- |
| **Deberes** | Tareas pendientes ordenadas por fecha (vencidas primero), con su lista y fecha. El círculo completa sin abrir la app; tocar el título abre la tarea; la cabecera abre Tareas. Configurable: *Todas · Hoy · Próximos 7 días · una lista · una carpeta*. |
| **Horario** | Tarjeta «Ahora / Siguiente» (clase en curso o la próxima, con horas, aula y color) y las clases de hoy con sus puntos de urgencia. Configurable: todos los horarios o uno. Tocar abre la pestaña Horario. |
| **Nota rápida** | Botón «+ Nueva» (crea la nota en el cuaderno elegido) y las notas más recientes del cuaderno. Tocar una nota la abre. Configurable: todas las notas o un cuaderno. |

## Decisiones

- **Glance 1.2** (`androidx.glance:glance-appwidget`): el resto de la app ya es Compose.
- **Los widgets leen la base de datos de la app** (Room) en el mismo proceso. No hace falta la instantánea JSON que usa macOS con el App Group.
- **Completar desde el widget** usa un `ActionCallback` de Glance: escribe con el `Repositorio` y refresca. Los avisos se cancelan solos porque `ProgramadorAvisos` observa las tareas.
- **Configuración**: una sola `ConfiguracionWidgetsActivity` para los tres; detecta de qué widget viene por el proveedor (`AppWidgetManager.getAppWidgetInfo`) y guarda la elección en el estado de Glance (`PreferencesGlanceStateDefinition`), por id de widget. Se puede volver a abrir con una pulsación larga → **Settings**.
- **Abrir la app**: `PendingIntent` con extras (`tarea`, `nota`, `nueva_nota`, `seccion`), reutilizando la navegación que ya existe.
- **Refresco**: (1) los cambios de datos (listas, tareas, notas, horario y ajustes) refrescan los widgets con un pequeño retardo; (2) `updatePeriodMillis` de 30 minutos; (3) una alarma al próximo cambio de clase o a medianoche; (4) al arrancar el teléfono o actualizar la app.
- **Colores**: fondo y acento salen del tema y del color de la app (`esquemaDe`, o los colores dinámicos con el acento «sistema»); las clases usan su color (o el automático) y las tareas vencidas se ven en rojo.

## Fases

1. **Base**: dependencia, receptores y metadatos, extras en `MainActivity`, refresco desde `KiwuApp`, alarma de refresco.
2. **Deberes**: datos, vista por tamaño, completar, abrir tarea, configuración.
3. **Horario**: datos («Ahora / Siguiente», clases de hoy, puntos), vista, abrir Horario, configuración.
4. **Nota rápida**: crear nota y abrir nota, vista, configuración.
5. **Pulido y entrega**: estados vacíos, tamaños, prueba en el emulador, `DatosWidgetsTest`, `docs/versiones/android-0.4.1.md` y actualización de `android-plan.md` (los widgets ya no están «fuera de alcance»).

## Pruebas

- `DatosWidgetsTest`: tareas (sin eventos, orden, vencidas, filtros por lista/carpeta/hoy/próximos, fechas en palabras), horario («Ahora / Siguiente», clases del día, puntos de urgencia), notas (filtro y resumen), próximo refresco (cambio de clase o medianoche).
- Emulador: añadir los tres widgets, configurarlos, completar una tarea desde el widget, abrir tarea/nota/horario, crear una nota y cambiar entre tema claro y oscuro.

## Resultado

Los tres widgets están en la 0.4.1 y se probaron en el emulador: añadir y configurar, reconfigurar con pulsación larga → **Settings** (queda marcada la opción guardada), completar una tarea (desaparece de la lista y baja el contador), abrir la tarea, la nota y Horario, crear una nota con «+ Nueva» y el tema oscuro. La suite completa (177 pruebas) y `lintDebug` pasan.

Pendiente de probar en un teléfono: la alarma de refresco después de reiniciar y el aspecto en lanzadores que no permiten reconfigurar (Android 8–9).

## Más adelante

- Widget *Calendario de deberes* (mes con puntos), con RemoteViews dedicado si Glance no da para una cuadrícula cómoda.
- Widget de la racha.
- Elegir un día distinto de hoy en el widget de Horario (o un widget de semana).

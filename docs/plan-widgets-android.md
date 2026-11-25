# Plan de Kiwu para Android 0.4.1 · widgets

Estado: **completado** (24 de octubre de 2026). Se publica en la **0.4.1** junto con la racha y la guía de inicio ([plan-0.4.1.md](plan-0.4.1.md)). Recupera lo que dejó escrito [plan-0.3.0.md](plan-0.3.0.md) §2 (widgets), con un cambio de alcance: el *Calendario de deberes* queda para más adelante y en su lugar entra *Nota rápida* (como en macOS). El widget de Horario sí se incluye, como pidió una persona que probó la app.

Son tres widgets de pantalla de inicio con **Jetpack Glance**:

| Widget | Qué hace |
| --- | --- |
| **Deberes** | Tareas pendientes ordenadas por fecha (vencidas primero), con su lista y fecha. El círculo completa sin abrir la app; tocar el título abre la tarea; la cabecera abre Tareas. Configurable: *Todas · Hoy · Próximos 7 días · una lista · una carpeta*. |
| **Horario** | Tarjeta «Ahora / Siguiente» (clase en curso o la próxima, con horas, aula y color) y las clases de hoy con sus puntos de urgencia. Configurable: todos los horarios o uno. Tocar abre la pestaña Horario. |
| **Nota rápida** | Botón «+ Nueva» (crea la nota en el cuaderno elegido) y las notas más recientes del cuaderno. Tocar una nota la abre. Configurable: todas las notas o un cuaderno. |

La agenda del widget Calendario tiene una mejora posterior para [mostrar la carpeta de cada tarea](plan-carpetas-agenda-widget.md).

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

- Widget de la racha.
- Elegir un día distinto de hoy en el widget de Horario (o un widget de semana).

## Rediseño de los cuatro widgets · noviembre de 2026

| Widget | Comportamiento implementado |
| --- | --- |
| Deberes | La cabecera muestra el filtro activo y el total de tareas pendientes de ese alcance. Cada fila muestra la carpeta antes de la lista y la fecha. El botón de añadir, el control de completar y la zona que abre la tarea tienen un objetivo táctil de 48 dp de alto. |
| Horario | La clase en curso o siguiente se destaca arriba. Si esa sesión es de hoy, se omite de «Clases de hoy». Al mostrar todos los horarios, cada clase identifica su horario. El contador indica explícitamente que las cifras se refieren a faltas. |
| Calendario | Mantiene la cuadrícula mensual de siete columnas en todos los tamaños, con flechas de 48 dp para cambiar de mes y un toque en el encabezado para volver al mes actual. Cada día abre su agenda. Según el espacio, las celdas muestran títulos abreviados o conteos. La cuadrícula aprovecha todo el ancho y las celdas pueden medir menos de 48 dp en tamaños compactos. |
| Nota rápida | «Nueva» y las filas tienen 48 dp de alto. En «Todas las notas», la cabecera muestra el cuaderno que recibirá la nota nueva cuando ya existe uno. El título y el extracto de cada nota se muestran en una línea cada uno. |

La agenda del Calendario, Deberes y el selector de Deberes comparten la resolución de la carpeta a partir de los IDs de la tarea y la lista. Por ejemplo, «Revisión del proyecto» se distingue entre **Universidad › Proyecto integrador** y **Cursos › Proyecto de integración**. Las listas sueltas o con una carpeta ausente muestran solo el nombre de la lista. La apertura y la finalización se resuelven por ID de tarea. [Detalle del caso reportado](plan-carpetas-agenda-widget.md)

La configuración agrupa filtros rápidos, carpetas y listas en Deberes, presenta un único control de selección por fila y guarda la elección por instancia. La pantalla informa si falla la lectura o el guardado. La agenda aplica los márgenes del sistema y del teclado, carga los elementos externos en segundo plano y oculta el teclado al abrir el selector de lista o de fecha u hora. Los cambios utilizan el esquema de datos existente.

Tras revisar las capturas del launcher, se recuperó la cuadrícula mensual en tamaños compactos. Los cuatro widgets usan una superficie gris cálida y un acento arena en modo oscuro. El modo claro conserva su paleta.

La prueba focalizada `DatosWidgetsTest` pasó antes de los últimos ajustes. La validación de la versión final en el launcher, incluidos los tamaños, el contraste y TalkBack, queda pendiente.

### Captura rápida y agenda lateral

Deberes y Calendario permiten crear tareas desde una tarjeta flotante con título, descripción opcional, fecha, prioridad y destino. La descripción se guarda en la nota de la tarea. Calendario vuelve a la agenda del día al guardar y Deberes cierra la tarjeta. El borrador se conserva si falla el guardado.

Los cuatro widgets usan fondos translúcidos con una opacidad inicial del 84 % en oscuro y del 90 % en claro. El Calendario reparte el ancho entre la cuadrícula y la agenda, separadas por una línea discreta. Con la escala de texto estándar, el diseño lateral aparece desde 297 dp de ancho y requiere entre 160 y 176 dp de alto para meses de cinco o seis semanas. En tamaños compactos, el mes mantiene el ancho completo y tocar una fecha abre su agenda. En la vista amplia, la selección y el mes visible se guardan por instancia del widget.

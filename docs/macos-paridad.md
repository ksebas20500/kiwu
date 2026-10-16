# macOS: paridad con Android y Linux

Diario del trabajo descrito en [`prompt-macos.md`](prompt-macos.md). Rama: `macos-paridad`. Actualizado el 15 de octubre de 2026.

## Estado por bloque

- [x] **3 · Borrado lógico y contrato.** `eliminada` en las cuatro entidades (migración ligera), todas las lecturas filtran, borrar en cascada, `purgar(antesDe:)` sin usar. `Tools/probar-contrato.sh` pasa (3 fixtures).
- [x] **4 · Copia de seguridad `.kiwu.json`.** `Services/CopiaService.swift` + diálogos en `Views/CopiaUI.swift`. Archivo → Exportar/Importar copia… y Ajustes → Copia. Incluye `horarios` y `clases`. Ejemplo en `docs/contrato/copia/`.
- [x] **5 · Horario semanal.** Modelo, `ReglasHorario`, `HorarioService`, `AvisosHorario` y `Views/Horario/HorarioRootView.swift` (semana, día, Ahora/Siguiente, formulario con choques). Atajo ⌘3 (`irHorario`).
- [x] **6 · Color de la app, calendario y «Añadir tarea».** `Services/ColorAcento.swift` y selector en Apariencia; calendario con puntos y detalle del día desplazable con hora (18:00 por defecto); botón de fecha y hora al añadir una tarea.
- [x] **7 · Notas.** `BusquedaTexto` (sin mayúsculas ni tildes) en la búsqueda y el menú «/»; el editor garantiza un párrafo final tras borrar.
- [x] **8 · Comprobaciones.** Orden de tarea nueva = máximo + 1 (prueba en `BorradoLogicoTests`); `avisarErrorGrave` limita a un aviso por minuto y cierra con `cerrar: true`.
- [x] **9 · Pruebas, CI y documentación.** Destino `KiwuTests` en el `.xcodeproj` (no hizo falta el paquete local); el flujo `macos.yml` ejecuta las pruebas; README actualizado.

## Qué ha cambiado desde macOS 2.0

| Área | macOS |
| --- | --- |
| Borrado lógico | Hecho |
| Contrato de datos | Hecho (se ejecuta y pasa) |
| Copia de seguridad `.kiwu.json` | Hecho |
| Horario semanal | Hecho |
| Color de la app | Hecho |
| Calendario y «Añadir tarea» | Hecho |
| Búsqueda de notas y menú «/» | Hecho |
| Robustez del editor | Revisado y reforzado |
| Orden de tareas y errores graves | Comprobado |
| Tablero | Sin cambios (se conserva) |
| Sincronización | No implementada (plan con Supabase) |
| Marca | Sin cambios: nada en duro que impida renombrar |

## Decisiones tomadas

- **Color por defecto «coral» (`#E8705A`).** El documento lo pide, pero hasta ahora macOS usaba el color de acento del sistema (el `AccentColor` del catálogo estaba vacío), así que **quien no toque nada verá el acento coral en lugar del azul del sistema**. Para volver al anterior basta elegir «Colores del sistema». Para no cambiar nada por defecto, bastaría poner `ColorAcento.predeterminado = "sistema"`.
- **Contraste del acento:** se garantiza ≥ 4,5:1 contra blanco (claro) o `#1E1E1E` (oscuro) oscureciendo o aclarando el color; el coral en modo claro queda por tanto algo más oscuro que `#E8705A`, como en Android. El cálculo usa un margen de 4,6 por el redondeo a 8 bits.
- **Calendario:** se eliminó la tarjeta emergente del doble clic; el detalle del día es ahora un panel fijo bajo la cuadrícula. La hora al añadir una tarea desde el calendario es siempre visible (Core Data no distingue «sin hora»).
- **Añadir tarea:** la fecha se elige en una ventana con calendario y hora; en la vista Hoy, sin fecha elegida, sigue siendo hoy a las 18:00.
- **Avisos del horario:** notificaciones de una sola vez, reprogramadas al abrir la app, al guardar y cada 30 s mientras el módulo Horario está abierto (más al activar la app). Si la app lleva días cerrada, el aviso de la semana siguiente se programa al abrirla.
- **Pruebas:** el destino `KiwuTests` se firma con el mismo equipo que la app (`Base.xcconfig`); sin eso, macOS rechazaba cargar el paquete de pruebas por tener otro Team ID. En CI se usa `CODE_SIGNING_ALLOWED=NO`.
- **Hora de las clases:** en vez de un menú de 288 opciones (se salía de la pantalla), el formulario usa un selector con horas 1–12, minutos 00–59 y a. m./p. m., que se escriben o se mueven con flechas (`DatePicker` en estilo `stepperField`, con formato de 12 h forzado). Se guarda igual que antes (minutos desde las 00:00); en el fin, 12:00 a. m. es 24:00 (1440). Ya se admite cualquier minuto, no solo múltiplos de 5.
- **Estado vacío del Horario:** el botón «Nuevo horario» va centrado bajo el texto (`EmptyStateView` admite un botón opcional).
- **Búsqueda sin tildes:** «ano» encuentra «Año» (la ñ se pliega a n, como hace el plegado de Unicode).

## Lo que no se pudo comprobar

- **Interfaz a mano:** el Horario, el selector de color y el calendario compilan y su lógica tiene pruebas, pero no se han recorrido visualmente en esta sesión (solo `xcodebuild`).
- **Notificaciones del horario:** dependen del permiso del sistema; la lógica de cuándo avisar está probada (`momentoDeAviso`), no la entrega real.
- **Widgets y firma:** no se tocaron ni se comprobaron.
- **Bases reales:** las migraciones están probadas con bases creadas en las pruebas; no se ha abierto la app con una copia de `~/Library/Application Support/Kiwu/`. Hazlo antes de publicar.
- **CI en GitHub:** el nuevo paso de pruebas se ha ejecutado en local con `CODE_SIGNING_ALLOWED=NO`, no aún en el runner `macos-26`.

## Ideas para después

- Widgets del horario: `WidgetDataService` podría publicar las clases de hoy en la instantánea JSON.
- Sincronización con Supabase (ver `plan-sincronizacion.md`); `purgar(antesDe:)` está listo para activarse entonces.
- Imágenes de notas fuera del JSON, cambio de marca y traducción.

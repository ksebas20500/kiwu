# Prompt para Claude Code en el Mac: poner Kiwu para macOS al día

> **Cómo usarlo.** En el Mac: `git clone https://github.com/ksebas20500/kiwu.git && cd kiwu && git pull`, abre una terminal en la carpeta del repositorio, ejecuta `claude` y pega **todo lo que hay debajo de la línea** (desde «Eres un ingeniero…» hasta el final). El texto está escrito para que Claude lo entienda sin más contexto. Se actualizó el 15 de octubre de 2026, con el repositorio en la etiqueta `android-v0.1.0` (commit `c73011d`).

> **Estado:** este trabajo ya se hizo y se publicó en Kiwu 2.1 para macOS. El diario está en [`macos-paridad.md`](macos-paridad.md). Se conserva como referencia.

---

Eres un ingeniero de software senior de macOS (Swift, SwiftUI, Core Data, WidgetKit). Trabajas en la carpeta `macos/` del repositorio **Kiwu** (gestor universitario: tareas, calendario y notas). Desde la última versión de macOS (2.0) se han hecho muchos cambios en el resto del proyecto: una app de **Android** nueva, mejoras en **Linux**, un **contrato de datos compartido** y varios planes. Tu trabajo es **llevar a macOS todo lo que falta**, sin romper lo que ya funciona y comprobando cada paso en este Mac (aquí sí se puede compilar y probar; en los otros equipos no se pudo).

## 0. Reglas de trabajo (léelas primero)

1. **Lee antes de escribir.** Por este orden: `docs/contrato/README.md`, `docs/plan-horario.md`, `docs/plan-sincronizacion.md` (solo la sección 2, «Qué se sincroniza y qué no»), `docs/android-ruta-correccion.md` (lecciones aprendidas), `docs/plan-desarrollo.md` §2–4, y `macos/README.md`. Mira cómo está escrito el código existente en `macos/Kiwu/` y **imita su estilo** (comentarios en español, nombres en español, mismo nivel de comentarios, SwiftUI y Core Data como ya están). No introduzcas dependencias externas.
2. **Referencias que puedes leer** (no las modifiques): la app de Android en `android/` (Kotlin; las reglas puras están en `android/app/src/main/java/app/kiwu/android/domain/` y sus pruebas en `android/app/src/test/`) y Linux en `linux/kiwu/` (Python; `db.py`, `modelos.py`, `servicios.py`). El comportamiento de referencia está en esas pruebas: **los vectores de prueba de este documento salen de ellas**.
3. **No cambies** el identificador de la app (`com.ksebas.gestoruniversitario`), el App Group, los nombres de los archivos de base de datos (`GestorUniversitario.sqlite`, `Notas.sqlite`) ni el nombre «Kiwu»: el cambio de marca está **pendiente de decidir** (ver `docs/marca-nombres.md`) y se hará aparte. Tampoco toques `android/` ni `linux/`.
4. **Los datos de las personas son sagrados.** Toda modificación del modelo de Core Data debe ser una **migración ligera** que conserve lo existente. Antes de migrar, prueba con una copia de las bases reales (`~/Library/Application Support/Kiwu/`). Nunca borres datos sin una confirmación explícita.
5. **Trabaja en una rama** `macos-paridad` y haz **un commit por bloque** (secciones 3 a 8), con un mensaje en español que diga qué cambia y por qué. Después de cada bloque: compila (`xcodebuild -project macos/Kiwu.xcodeproj -scheme Kiwu -destination 'platform=macOS' build`) y ejecuta las pruebas. No hagas `push --force`. No abras pull requests: deja la rama lista.
6. **Pruebas.** El proyecto de macOS **no tiene pruebas automáticas** todavía. Crea un destino de pruebas `KiwuTests` (XCTest) y pon ahí la lógica pura nueva (reglas del horario, copia de seguridad, borrado lógico). Cada bloque debe dejar sus pruebas en verde.
7. **Pregunta solo si algo bloquea.** Si una decisión es de producto y no está en estos documentos, elige la opción más conservadora, anótala en `docs/macos-paridad.md` (ver sección 9) y sigue.
8. Si algo no se puede comprobar (por ejemplo, los widgets necesitan firma con tu equipo), **dilo claramente** en el informe final en lugar de darlo por hecho.

## 1. Qué ha cambiado en el proyecto desde macOS 2.0

La columna **macOS** indica qué debes hacer tú. «Hecho» significa que el código ya está en `macos/` pero hay que comprobarlo.

| Área | Qué se hizo (Android / Linux / contrato) | macOS |
| --- | --- | --- |
| **Borrado lógico** | Linux (esquema v2) y Android marcan `eliminada` en las cuatro entidades en lugar de borrar la fila (y los hijos al borrar un padre). Es el requisito para sincronizar | **Pendiente**: el modelo de Core Data no tiene `eliminada` (bloque 3) |
| **Contrato de datos compartido** | Fixtures de notas en `docs/contrato/notas/` y reglas de formato (`docs/contrato/README.md`); pruebas en Linux (`test_contrato.py`) y Android (`DocumentoTest`) | **Pendiente de ejecutar**: `macos/Tools/probar-contrato.sh` nunca se ha podido ejecutar (bloque 3) |
| **Copia de seguridad `.kiwu.json`** | Linux y Android exportan e importan el mismo formato (versión 1) | **Pendiente**: macOS no tiene exportar/importar (bloque 4) |
| **Horario semanal (módulo nuevo)** | Android: varios horarios (Universidad, Trabajo, Ocio…), clases en varios días, vista por día y por semana, «Ahora / Siguiente», aviso de choques, avisos opcionales. Contrato en `docs/contrato/README.md` | **Pendiente**: módulo completo (bloque 5) |
| **Color de la app** | Android: 8 colores a elección, «colores del sistema» y selector libre; el esquema se deriva del color. Es una preferencia **local**: no se sincroniza | **Pendiente** (bloque 6) |
| **Calendario** | Android: cada día muestra su número y un punto de color por tarea (hasta 4 y «+»); el detalle del día se desplaza y se puede añadir una tarea con hora | **Revisar y alinear** (bloque 6) |
| **Añadir tarea** | Android: botón para elegir fecha y hora de entrega al crearla | **Revisar** (bloque 6) |
| **Notas: búsqueda y menú «/»** | Android ignora mayúsculas y tildes («cod» encuentra «Código»); Linux y macOS no | **Alinear** (bloque 7) |
| **Editor: robustez** | Android tuvo un cierre al mantener pulsado «borrar» sobre un bloque ya eliminado (operaciones sobre bloques que ya no existen) | **Revisar** que `NoteEditorView` no tenga el mismo problema (bloque 7) |
| **Orden de tareas nuevas** | `orden = max(orden) + 1` en vez de contar filas | **Hecho**: `DeberService.swift`; comprobar (bloque 8) |
| **Errores graves de Core Data** | Aviso con diálogo en vez de fallar en silencio | **Hecho**: `PersistenceController.avisarErrorGrave`; comprobar (bloque 8) |
| **Tablero** | Android solo lo ofrece en pantallas anchas (el arrastre es incómodo en teléfono) | **Sin cambios**: en macOS se conserva |
| **Menú lateral, arranque sin parpadeo, rueda de hora, rendimiento del fondo** | Solo Android (`MainActivity`, `Rueda.kt`, `Fondo.kt`, etc.) | **No aplica** |
| **Sincronización** | Plan con **Supabase (plan gratuito)** en `docs/plan-sincronizacion.md`; aún sin código | **No implementar todavía**; solo respetar el requisito de «qué se sincroniza y qué no» (sección 2 del plan) |
| **CI** | `.github/workflows/macos.yml` (compila en macOS; **ya pasó en GitHub** con el runner `macos-26`, el 15 oct 2026) | Añadir las pruebas nuevas al flujo (bloque 9) |
| **Marca** | `docs/marca-nombres.md`: «Kiwu» se confunde con «kiwi»; candidatos con K (recomendado: Kapunte) | **No cambiar el nombre**; solo dejarlo preparado (nada en duro que impida cambiarlo después) |

## 2. Modelo de datos compartido (resumen del contrato)

- **Identificadores:** UUID en **mayúsculas** en los archivos de copia (`UUID().uuidString` ya lo hace).
- **Fechas** en la copia: ISO-8601 UTC **sin fracciones de segundo** (`2026-10-13T15:04:05Z`).
- **Estados de tarea:** `estado` 0 = por hacer, 1 = en curso; «Hecho» se deduce de `completado == true`.
- **Notas:** el contenido de una nota es el JSON de bloques (`NoteDocument` / `NoteBlock` / `TextRun`), ya implementado en `Models/DomainModels.swift`. Los **campos desconocidos** de bloques y documentos se conservan y se reenvían sin tocar (para que una versión antigua no borre lo que escribió una nueva).
- **Lo que NO viaja entre dispositivos:** tema, **color de la app**, fondo de imagen, vista lista/tablero, atajos de teclado, tamaño de ventana, carpeta vinculada a una lista (`bookmarkCarpeta`/`carpetaDisponible`) y el estado de avisos ya mostrados. Ningún campo de datos puede contener un color en hexadecimal, un tamaño en píxeles ni una fuente: los colores de las notas viajan **por nombre** («rojo», «azul»…) y los pasteles de las tareas se calculan localmente a partir del id.

## 3. Bloque 3 · Borrado lógico y contrato

**Objetivo:** que el modelo de Core Data tenga el mismo borrado lógico que Linux y Android, y que el lector de notas pase el contrato.

1. En `Persistence/PersistenceController.swift` (`makeModel()`), añade `eliminada: Date?` (opcional, sin valor por defecto) a **las cuatro entidades**: `Materia`, `Deber`, `Cuaderno`, `Pagina`. Añade el `@NSManaged var eliminada: Date?` correspondiente en `Models/DomainModels.swift`. Comprueba que los almacenes existentes se migran solos (migración ligera: `shouldMigrateStoreAutomatically` y `shouldInferMappingModelAutomatically` en `true`); si no, actívalos.
2. **Todas las lecturas** (fetch requests, `@FetchRequest`, `NSFetchedResultsController`, relaciones `deberes` y `paginas`, conteos, búsquedas, y la instantánea JSON de `WidgetDataService`) filtran `eliminada == nil`. Busca cada uso con `grep -rn "NSFetchRequest\|@FetchRequest\|\.deberes\|\.paginas" macos/`.
3. **Borrar** pasa a marcar `eliminada = .now` y `fechaActualizacion = .now`: en una materia, la propia materia **y todos sus deberes**; en un cuaderno, el cuaderno **y todas sus páginas**. Los métodos de `DeberService`, `CuadernoService` y `NotaService` que hoy hacen `context.delete(...)` se cambian. Mantén (por si se necesita) una función `purgar(antesDe:)` que borre de verdad lo eliminado hace más de 30 días, **sin llamarla** todavía.
4. Las notas y tareas eliminadas **no deben aparecer** en ninguna parte: listas, calendario, widgets, Spotlight (si se usa), contadores de la barra lateral, ni al elegir «mover a otra lista».
5. Ejecuta `macos/Tools/probar-contrato.sh` y corrige lo que no coincida con `docs/contrato/notas/*.esperado.json`. Si hay que decidir quién tiene razón, el contrato manda (`docs/contrato/README.md`).
6. **Pruebas (`KiwuTests`):** borrado lógico en cascada de materia y de cuaderno, que lo eliminado no aparece en las lecturas, y migración desde una base creada con el modelo anterior (puedes generarla en la prueba con el modelo antiguo en memoria o con un archivo SQLite de ejemplo en `KiwuTests/Recursos/`).

**Criterio de cierre:** el app abre las bases actuales sin perder nada; borrar una lista y una nota las oculta en toda la interfaz y los widgets; `probar-contrato.sh` y las pruebas pasan.

## 4. Bloque 4 · Copia de seguridad `.kiwu.json` (exportar e importar)

**Objetivo:** mover datos entre el Mac, Linux y Android con el mismo archivo.

Formato (versión `1`), objeto JSON con estas claves:

```json
{
  "formato": 1,
  "app": "kiwu",
  "exportado": "2026-10-15T18:00:00Z",
  "materias":  [{ "id": "UUID", "nombre": "…", "orden": 0, "carpeta": null, "creada": "ISO", "actualizada": "ISO" }],
  "deberes":   [{ "id": "UUID", "materia": "UUID", "titulo": "…", "completado": false, "estado": 0,
                  "fechaEntrega": "ISO o null", "recordatorioHoras": 24, "fechaCompletado": "ISO o null",
                  "nota": { …NoteDocument… }, "creada": "ISO", "actualizada": "ISO" }],
  "cuadernos": [{ "id": "UUID", "nombre": "…", "icono": "📓", "orden": 0, "creada": "ISO", "actualizada": "ISO" }],
  "paginas":   [{ "id": "UUID", "cuaderno": "UUID", "titulo": "…", "icono": "", "favorita": false,
                  "contenido": { …NoteDocument… }, "creada": "ISO", "actualizada": "ISO" }],
  "horarios":  [ … ver bloque 5 … ],
  "clases":    [ … ver bloque 5 … ]
}
```

Reglas (idénticas a `android/.../data/Copia.kt` y a `linux/kiwu`, léelos):
- `carpeta` se exporta siempre como `null` (la carpeta vinculada solo vale en el dispositivo).
- `nota` y `contenido` pueden llegar como objeto JSON **o** como texto con JSON; se normalizan al formato canónico del contrato.
- **Importar no pisa lo existente ni resucita lo eliminado**: si el `id` ya existe (vivo o con `eliminada`), se ignora esa fila. Se descartan las filas con referencias que no existen (un deber cuya materia no existe, una página sin cuaderno).
- Si `app` no es `"kiwu"` → error «El archivo no es una copia de seguridad de Kiwu». Si `formato` > 1 → «La copia es de una versión más nueva de Kiwu». Las claves ausentes (`horarios`, `clases`) se tratan como listas vacías (las copias antiguas siguen importándose).
- Importar devuelve un resumen: nº de listas, tareas, cuadernos, notas, horarios y clases **realmente añadidos**.

Interfaz: *Archivo → Exportar copia…* (guardar como `kiwu-AAAA-MM-DD.kiwu.json`) e *Archivo → Importar copia…*, y los mismos botones en Ajustes → «Copia de seguridad». Servicio nuevo `Services/CopiaService.swift` (lógica pura, probable sin interfaz).

**Pruebas:** idempotencia (importar dos veces no duplica), exportar → importar en una base vacía conserva todo, no resucita lo eliminado, filas huérfanas descartadas, copia antigua sin `horarios`, copia con `formato: 2` rechazada, y que un archivo exportado por **Android o Linux** se importa (hay ejemplos en `docs/contrato/`; si falta, crea uno a mano con las claves de arriba).

## 5. Bloque 5 · Horario semanal (módulo nuevo)

Lee `docs/plan-horario.md` y reproduce la funcionalidad de Android (`android/.../ui/horario/HorarioScreen.kt`, `domain/Horario.kt`).

**Modelo (Core Data, en el almacén de tareas, como configuración «Tareas»):**

| Entidad | Atributos |
| --- | --- |
| `Horario` | `id` UUID, `nombre` String, `icono` String (emoji, por defecto 🎓), `orden` Int16, `fechaCreacion`, `fechaActualizacion`, `eliminada` Date? |
| `Clase` | `id` UUID, `horario` (relación a `Horario`, obligatoria), `materiaId` UUID? (**sin** relación: es solo la referencia a una lista de tareas), `grupo` UUID, `titulo` String, `diaSemana` Int16 (1 = lunes … 7 = domingo), `inicioMin` Int16, `finMin` Int16, `aula` String, `notas` String, `avisoMin` Int16? (nil = sin aviso), `fechaCreacion`, `fechaActualizacion`, `eliminada` Date? |

- **Las horas son «flotantes»:** minutos desde las 00:00 en hora local, **sin zona horaria** (`0 ≤ inicio < 1440`, `inicio < fin ≤ 1440`; 1440 = medianoche al final del día). Nunca guardes instantes (`Date`) para la hora de una clase.
- Una clase en varios días son **varias filas con el mismo `grupo`** (se editan juntas). Borrar una clase borra todo su grupo (borrado lógico). Borrar un horario marca el horario y todas sus clases.
- El **color** de una clase **no se guarda**: se calcula con la paleta pastel existente (`Shared/PaletaPastel.swift`), a partir de `materiaId` si existe o de `grupo`. Reutiliza el mismo cálculo de color estable por id que usan las tareas.
- Migración ligera: añadir las dos entidades no toca las existentes.

**Reglas puras** (créalas en `Services/ReglasHorario.swift`, sin Core Data, para probarlas). Réplica exacta de `domain/Horario.kt`:
- `valida(inicio, fin)`: `inicio` en `0..<1440`, `fin` en `1...1440` y `fin > inicio`.
- `hora(min)` → `"HH:mm"` (`24:00` para 1440); `rango(clase)` → `"08:00 – 10:30"`; `nombreDia(1…7)` → Lunes…Domingo; abreviaturas Lun, Mar, Mié, Jue, Vie, Sáb, Dom; letras L M X J V S D.
- `deDia(clases, dia)`: ordenadas por `inicio`, luego `fin`, luego `titulo`.
- `seSolapan(a, b)`: mismo día y `a.inicio < b.fin && b.inicio < a.fin` (**terminar a las 10:00 y empezar a las 10:00 no chocan**).
- `choques(existentes, grupo, dias, inicio, fin)`: las existentes con **otro** `grupo`, en alguno de `dias`, que se solapan con `[inicio, fin)`; ordenadas por día e inicio. Se comprueban contra **todos los horarios**, no solo el actual. Es una **advertencia**, no bloquea guardar.
- `enCurso(clases, dia, minuto)`: la clase con `minuto >= inicio && minuto < fin` (el fin no cuenta).
- `siguiente(clases, dia, minuto)`: recorre hoy y los 7 días siguientes; hoy solo cuentan las que empiezan **después** de `minuto`; devuelve la clase y cuántos días faltan. `cuando(días, día)`: «hoy», «mañana» o el nombre del día en minúsculas.
- `proximoInicio(clase, desde)`: la próxima fecha-hora local en que empieza (si hoy es su día y ya pasó la hora, la semana siguiente).
- `momentoDeAviso(clase, desde, zona)`: `proximoInicio − avisoMin`, **solo si `avisoMin` no es nil** y el aviso es ≥ `desde`; si ya pasó, el de la semana siguiente (hasta 3 intentos). Un aviso de una clase a las 00:05 con 10 min cae a las 23:55 **del día anterior**.
- `textoAviso(clase)`: `"Empieza a las 08:00"` o `"Empieza a las 08:00 · Aula 204"`.

**Vectores de prueba** (copia estas pruebas a `KiwuTests`; salen de `HorarioReglasTest.kt`; zona `America/Bogota`):
- `valida`: (480,600) sí; (1380,1440) sí; (600,600) no; (600,540) no; (-1,60) no; (1440,1470) no.
- Choques con `Cálculo` lun 08:00–10:00 y `Física` mié 08:00–10:00: nueva lun 10:00–11:00 → **ninguno**; lun 09:00–11:00 → [Cálculo]; mar 08:00–10:00 → ninguno; lun+mié 09:00–09:30 → 2.
- Con A lun 08–10, B lun 14–16, C mié 09–10: `enCurso(lun, 09:00)` = A; `enCurso(lun, 10:00)` = nil; `siguiente(lun, 09:00)` = (B, 0 días); `siguiente(lun, 17:00)` = (C, 2); `siguiente(mié, 11:00)` = (A, 5).
- `proximoInicio` de una clase de lunes 08:00 desde miércoles 30-sep-2026 12:00 → lunes 5-oct-2026 08:00; desde el lunes 5-oct 07:00 → el mismo día 08:00; desde las 08:01 → lunes 12-oct 08:00.
- `momentoDeAviso` (aviso 10): desde mié 30-sep 12:00 → lun 5-oct 07:50; desde lun 5-oct 07:55 → lun 12-oct 07:50; desde lun 5-oct 07:40 → lun 5-oct 07:50; sin aviso → nil.

**Interfaz (SwiftUI)** — una pestaña/sección nueva «Horario» junto a Tareas y Notas, con el mismo patrón que ellas (barra lateral + contenido):
- Barra lateral: «Todos los horarios» y cada horario (icono + nombre) con menú contextual *Editar* / *Eliminar*; botón «Nuevo horario» (nombre + icono emoji; sugerencias iniciales: Universidad 🎓, Trabajo 💼, Ocio 🌴).
- Contenido: arriba una tarjeta **«Ahora: … · hasta HH:mm / Siguiente: … · hoy|mañana|día HH:mm»**; después, **vista de semana** (siete columnas, eje de horas, bloques de color; las clases que coinciden se reparten el ancho en carriles; rango visible: de min(07:00, primera clase) a max(19:00, última clase), recortado a 0–24) y una **vista por día** alternativa (lista de tarjetas con hora de inicio y fin, título y aula; con varios horarios, el nombre del horario). En un Mac la semana es la vista por defecto.
- Formulario de clase (hoja modal): *Materia o actividad* (texto; con sugerencias de las listas de tareas existentes: al elegir una se vincula `materiaId`), *Horario* (si hay varios), *Días* (siete conmutadores), *Inicio* y *Fin* (**de 5 en 5 minutos**: `DatePicker` o dos selectores; recuerda que `DatePicker` no admite paso de minutos, así que redondea o usa selectores propios; el fin a las 00:00 se guarda como 1440), *Aula*, *Aviso* (Sin aviso, 5, 10, 15, 30, 60 min antes), *Notas*. Muestra el aviso «Coincide con otra clase: título · día hh:mm – hh:mm (horario)» si hay choques, y el error «La hora de fin debe ser posterior a la de inicio» si no valida. Guardar crea/actualiza/quita filas por día dentro del grupo (como `Repositorio.guardarClase` en Android: conserva `fechaCreacion` de las existentes, marca como eliminados los días quitados).
- **Avisos:** con `UNUserNotificationCenter`, una notificación local por clase con aviso para su **próxima** ocurrencia (`momentoDeAviso`), título = título de la clase, texto = `textoAviso` (+ nombre del horario). Reprograma al guardar, al abrir la app y tras cada aviso (la semana siguiente). Reutiliza el permiso y las categorías que ya usan los recordatorios de tareas. Atajos de teclado y `AccionApp` para «Ir a Horario» (por ejemplo ⌘3), editables como los demás.
- **Copia de seguridad** (bloque 4): claves `horarios` y `clases`:
  `horarios[]`: `{ "id", "nombre", "icono", "orden", "creada", "actualizada" }`;
  `clases[]`: `{ "id", "horario", "materia" (UUID o null), "grupo", "titulo", "diaSemana", "inicioMin", "finMin", "aula", "notas", "avisoMin" (entero o null), "creada", "actualizada" }`. Al importar se descartan clases con día fuera de 1–7, horas no válidas o cuyo horario no existe; las filas se insertan **horarios primero, clases después**.
- **Widgets:** no son parte de esta tarea; si WidgetDataService necesita saber de las clases, déjalo anotado en `docs/macos-paridad.md` como idea.

**Criterio de cierre:** se puede crear «Universidad», añadir una clase lunes y miércoles de 10:00 a 12:00 y otra de 12:00 a 14:00 **sin que avise de choque**, se ve en semana y por día, «Ahora/Siguiente» es correcto, el aviso llega, y exportar → importar en otra base conserva todo.

## 6. Bloque 6 · Color de la app, calendario y «Añadir tarea»

**Color de la app (Ajustes → Apariencia):**
- Nueva preferencia `acento` en `Services/AjustesStore.swift` (cadena): `"coral"` (por defecto), nombres predefinidos, `"sistema"` o `"#RRGGBB"`. Es **local**: no entra en la copia de seguridad ni en nada que viaje a otros dispositivos.
- Colores predefinidos (hex): coral `#E8705A`, rojo `#D0382E`, naranja `#E07B10`, verde `#2E8B57`, azul `#2F6FD0`, morado `#8A55C2`, rosa `#D14A8B`, grafito `#5F6B73`. «Sistema» usa el color de acento del sistema; «Otro» abre `ColorPicker`.
- Aplícalo con `.tint(...)` en la raíz (`RaizView`/`MyApp`) y en los controles propios que hoy usen el color de la marca. Comprueba el **contraste** en claro y en oscuro (texto sobre el color de acento ≥ 4,5:1; en Android se obliga con pruebas); si un color elegido no da contraste suficiente, oscurece o aclara el color de relleno automáticamente. Los **pasteles de las tareas no cambian** con el acento.
- Mantén el color de marca original (coral) como valor por defecto: quien no toque nada no debe ver cambios (en Android se retocó solo el coral claro de `#C9503A` a `#C44B35` para llegar a contraste 4,5; haz lo mismo si tu comprobación lo exige).

**Calendario (`Views/CalendarView.swift`):** alinéalo con Android si hoy no lo hace: cada celda de día muestra el número (hoy resaltado) y **un punto de color por tarea** (máximo 4 y un «+» si hay más; color = tono oscuro del pastel de la tarea), en lugar de textos pequeños; el detalle del día seleccionado va debajo, ocupa el espacio restante y **se desplaza**; desde el detalle se puede añadir una tarea para ese día **con hora opcional** (por defecto 18:00). Los colores de las tareas del mismo día no se repiten mientras haya paleta libre (ya existe `PaletaPastel`).

**Añadir tarea:** permite elegir **fecha y hora de entrega al crearla** (botón de calendario junto al campo; se muestra como una etiqueta que se puede quitar). Si ya existía, solo comprueba que funciona igual en la vista Hoy (fecha por defecto: hoy a las 18:00).

## 7. Bloque 7 · Notas: búsqueda, menú «/» y robustez del editor

- **Búsqueda de notas y menú «/»** insensibles a mayúsculas **y tildes**: «cod» encuentra «Código». Usa `folding(options: [.diacriticInsensitive, .caseInsensitive], locale: .current)` en la comparación (y en `textoPlano` de la búsqueda). Pruebas con varios casos (ñ, tildes, mayúsculas).
- **Editor (`NoteEditorView.swift`, `Views/Notas/`):** revisa que ninguna operación (borrar al inicio, unir con el bloque anterior, convertir, cambiar sangría, duplicar, mover, eliminar, escribir) actúe sobre un bloque que **ya no existe** en el documento (en Android un teclado enviaba eventos tardíos a un bloque eliminado al mantener pulsado «borrar» y la app se cerraba). Si alguna lo hace, comprueba primero que el bloque sigue presente. Pruebas: pulsar «borrar» muchas veces sobre párrafos, listas, código, títulos y divisores vacíos sin que falle y dejando siempre un párrafo al final.
- Reglas de Intro/lista/enlace suelto: Android las copió de Linux/macOS; solo verifica que macOS las sigue cumpliendo (Intro en un elemento de lista vacío sale de la lista; una dirección web sola en un párrafo se convierte en tarjeta).

## 8. Bloque 8 · Comprobar lo que ya estaba hecho en macOS

- `Services/DeberService.swift`: el `orden` de una tarea nueva es `max(orden) + 1` (no el número de filas). Prueba: crear A, B, C, borrar B, crear D → D tiene el mayor orden.
- `Persistence/PersistenceController.swift`: `avisarErrorGrave` muestra un `NSAlert` en vez de fallar en silencio; verifica que no se repite más de una vez por minuto y que con `cerrar: true` termina la app tras aceptar.
- Ejecuta la app con una copia de tus bases de datos reales y comprueba que no se pierde nada tras las migraciones de los bloques 3 y 5.

## 9. Bloque 9 · Pruebas, CI y documentación

1. **Destino de pruebas `KiwuTests`** (XCTest) en `Kiwu.xcodeproj`, con las pruebas de los bloques anteriores. Comando: `xcodebuild test -project macos/Kiwu.xcodeproj -scheme Kiwu -destination 'platform=macOS'`. Si añadir un destino de pruebas al `.xcodeproj` a mano es frágil, crea las pruebas de lógica pura como un paquete Swift local (`macos/Paquetes/KiwuLogica/`) que la app y las pruebas importen; elige lo que resulte más estable y anótalo.
2. **CI:** actualiza `.github/workflows/macos.yml` para ejecutar esas pruebas (el runner `macos-26` existe y el flujo actual pasa; si algún día deja de estar disponible, usa el más reciente y anótalo).
3. **Documentación:**
   - `macos/README.md`: nuevas secciones (Horario, Copia de seguridad, Color de la app), estructura de carpetas actualizada, cómo ejecutar las pruebas, y quita de «Pendiente» lo hecho.
   - **`docs/macos-paridad.md` (nuevo):** diario de este trabajo con la tabla de la sección 1, una casilla `[x]` / `[ ]` por bloque, las decisiones que tomaste, lo que **no se pudo comprobar** y las ideas para después (widgets del horario, sincronización).
   - README principal: si cambian las funciones de macOS, actualiza la tabla.
4. **Versión:** no cambies el número de versión ni crees etiquetas; lo decide la persona que mantiene el repositorio.

## 10. Informe final

Cuando termines, responde con:
1. La rama y la lista de commits (hash y título).
2. Para cada bloque: qué se cambió y en qué archivos.
3. La salida resumida de la compilación y de las pruebas (número de pruebas, fallos).
4. Lo que **no pudiste comprobar** (widgets, firma, notificaciones en un Mac sin permisos…) y por qué.
5. Cualquier decisión de producto que tomaste por tu cuenta.
6. Lo que sigue pendiente (sincronización con Supabase, imágenes de notas fuera del JSON, cambio de marca, traducción).

Empieza por leer los documentos de la sección 0 y por hacer un resumen de lo que entiendes en cinco líneas antes de tocar código.

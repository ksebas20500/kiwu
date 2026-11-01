# opencode.md — Contexto permanente de Kiwu Android

> Archivo de contexto para agentes. Resume la arquitectura completa de `android/` en la versión **0.7.1**
> para no tener que releer todo el código en cada sesión.
> Última revisión: 0.7.1 (`versionCode 18`). Si la versión cambia, actualiza las secciones 1, 3 y 10.

---

## 0. Cómo usar este archivo

- Léelo antes de tocar código. Da el mapa: qué existe, dónde vive y qué reglas respeta.
- Si una tarea afecta a **sincronización**, lee además `domain/MotorSync.kt`, `domain/Sincronizacion.kt`,
  `data/SyncLocal.kt`, `data/Firebase.kt`, `data/ServicioSync.kt`.
- Si afecta al **editor de notas**, lee `ui/editor/EstadoEditor.kt` (máquina de estados) antes de tocar la UI.
- Si afecta a **textos**, recuerda: el build falla por lint de traducciones (sección 9).
- No hay DI framework: las dependencias salen de `KiwuApp`.

---

## 1. Resumen ejecutivo

App nativa **Kotlin + Jetpack Compose**, cliente del ecosistema Kiwu (Linux/macOS): mismo modelo de datos,
mismo JSON de notas (`NoteDocument`/`NoteBlock`/`TextRun`) y misma copia `.kiwu.json`.

| Capa | Paquete | Responsabilidad |
|---|---|---|
| Presentación | `ui/` | Compose: pantallas, ViewModels, navegación, editor, tema |
| Widgets | `widgets/` | Glance (4 widgets) + actividades flotantes |
| Notificaciones | `notify/` | Alarmas, canales, receptores, reprogramación |
| Dominio | `domain/` | Modelos, reglas puras, formato de notas, lógica de sync |
| Datos | `data/` | Room, DAO, repositorio, ajustes, Firebase, Drive, copias, Google |
| Raíz | `KiwuApp.kt`, `MainActivity.kt`, `Idioma.kt` | Service locator, arranque, i18n |

**Datos de versión**

- `versionName = "0.7.1"`, `versionCode = 18`
- `compileSdk`/`targetSdk` = 35, `minSdk` = 26 (Android 8.0)
- Kotlin 2.0.21, AGP 8.7.3, JVM 17, KSP 2.0.21-1.0.28
- Compose BOM 2024.12.01, Room 2.6.1, Glance 1.2.0, DataStore 1.1.1, Navigation 2.8.5
- Firebase por **REST** (sin SDK), Credential Manager + Google Identity, ML Kit OCR
- **Room esquema v9**; la 0.7.1 **no migra la base de datos**

**Escala**: 86 archivos Kotlin de producción (~15.130 líneas), 30 archivos de test (~3.379 líneas), 640 strings base.

---

## 2. Arranque y navegación

### `KiwuApp` (service locator)

`app/src/main/java/app/kiwu/android/KiwuApp.kt`

- `db` → `KiwuDb` (Room, singleton)
- `repo` → `Repositorio(dao, banco de racha en SharedPreferences "racha")`
- `ajustes` → `AjustesStore` (DataStore)
- `sync` → `ServicioSync`
- `alcance` → `CoroutineScope` global (SupervisorJob) que sobrevive a las pantallas

`onCreate()`:

1. `Textos.actualizar()` — nombres de datos («Eventos», «Notas rápidas») en el idioma activo.
2. `iniciarAvisos()` — canales + reconciliación de alarmas (tareas, clases, racha) + reinicio diario de hábitos.
3. `iniciarWidgets()` — `merge` de flujos con debounce 350 ms → refresca/reprograma widgets.
4. `iniciarObservadorCalendario()` — `ContentObserver` de `CalendarContract` (eventos e instancias): cuando el
   calendario del teléfono cambia (Google sincroniza, borra o edita un evento) redibuja los widgets tras 1 s de
   debounce. Necesita `READ_CALENDAR`; si se concede con la app viva, `revisarAvisos()` lo registra al volver.
5. `sync.iniciar()`.

También `attachBaseContext(Idioma.contexto(base))` y refresco de widgets al cambiar claro/oscuro. `revisarAvisos()`
(vuelve a primer plano) re-cuadra alarmas, registra el observador si faltaba y refresca los widgets: red de
seguridad si el proceso estuvo muerto mientras Google cambiaba el calendario.

### `MainActivity`

`app/src/main/java/app/kiwu/android/MainActivity.kt`

- Splash screen + `enableEdgeToEdge()`.
- Lee ajustes **bloqueando con timeout de 1,5 s** antes del primer fotograma (evita flash de tema por defecto).
- Decide mostrar guía: solo primera vez **y** app vacía (`Guia.debeMostrarse`).
- Extras de notificación/widget: `tarea`, `nota`, `nueva_nota`, `seccion`.
- `onStart`/`onStop` → sync a primer plano / pausa en segundo plano.
- `onNewIntent` para reutilizar la actividad (`singleTop`).

### Navegación

`ui/Raiz.kt` — `NavHost` con 11 destinos:

```
guia · repaso · principal · tarea/{id} · ajustes · pagina/{id}
buscar · temporizador · calificaciones/{id} · tarjetas/{id} · racha
```

`principal` = 3 pestañas (`Tareas`, `Notas`, `Horario`) con `AnimatedContent`, `ModalNavigationDrawer`
distinto por módulo y barra inferior común. La celebración de racha se dibuja encima de todo.
ViewModels creados a mano con `viewModelFactory { initializer { … } }` (reciben `app.repo`, `app.alcance`, id de ruta).

---

## 3. Modelo de datos

### Entidades Room

`domain/Entidades.kt`, `domain/Horario.kt`, `domain/Racha.kt`

| Tabla | Campos relevantes |
|---|---|
| `materia` | nombre, icono, orden, `carpeta` (solo local, `content://`), `grupo_id`, `emoji`, `calificaciones` JSON, timestamps, `eliminada` |
| `grupo_lista` | carpeta de listas (un solo nivel), emoji, orden, `plegado`, borrado lógico |
| `deber` | `materia_id` FK CASCADE, título, `completado`, `estado` (0/1), `fecha_entrega`, recordatorio, `repite` (máscara de días), `prioridad`, `etiquetas` CSV, `pasos` JSON, `examen`, `nota` JSON, `recordado`, `eliminada` |
| `cuaderno` | nombre, icono, orden, timestamps |
| `pagina` | `cuaderno_id` FK, título, icono, `contenido` JSON, `texto_plano`, `favorita`, `tarjetas` JSON |
| `horario` | nombre, icono, orden |
| `clase` | `horario_id` FK, `materia_id`, `grupo` (clases multi-día), día 1–7, minutos inicio/fin, aula, notas, `aviso_min`, `color`, `max_faltas` |
| `falta` | `grupo`, `horario_id`, `dia` (`epochDay`), `justificada`; **sin** `actualizada` ni `eliminada` |

Reglas estructurales:

- **Borrado lógico** (`eliminada`) en todo lo sincronizable; las consultas filtran `eliminada IS NULL`;
  los borrados en cascada van en `@Transaction` manual.
- **IDs UUID en mayúsculas** (`nuevoId()`), compatibles con escritorio.
- **Lista especial «Eventos»**: id fijo `"eventos"`, creada perezosamente dentro de `deber`.
- **JSON en columnas** (`nota`, `contenido`, `pasos`, `calificaciones`, `tarjetas`) para evitar tablas hijas
  y mantener compatibilidad multiplataforma.
- **Índices**: `deber(materia_id, fecha_entrega)`, `pagina(cuaderno_id)`, `clase(horario_id, grupo)`, `falta(grupo, horario_id)`.
- **Horas flotantes**: minutos desde las 00:00 locales, sin zona horaria.

### Migraciones

`KiwuDb` en `data/KiwuDao.kt`, migraciones 1→9 escritas a mano:

| Versión | Cambio |
|---|---|
| 1→2 | Horarios y clases |
| 2→3 | Color de clase |
| 3→4 | Carpetas de listas + emoji |
| 4→5 | Faltas + máximo por materia |
| 5→6 | Hábitos (`repite`) |
| 6→7 | Prioridad, etiquetas, pasos |
| 7→8 | Examen + calificaciones |
| 8→9 | Tarjetas de repaso en notas |

Esquemas exportados en `app/schemas/1.json … 9.json`; solo en debug se empaquetan como assets para los tests de migración.

### DAO y repositorio

- `data/KiwuDao.kt` (~315 líneas): DAO abstracto, SQL explícito, `Upsert`, inserciones `IGNORE` para importar, transacciones de cascada.
- `data/Repositorio.kt` (~361 líneas): **fachada única** de la UI.
  - expone `Flow` por colección;
  - aplica `Reglas`, `ReglasHorario`, `Rachas`;
  - actualizaciones parciales leer-modificar-escribir (no pisa campos);
  - centraliza hábitos (`reiniciarRepetidas`), papelera, faltas, notas rápidas y clases multi-día.

---

## 4. Sincronización (subsistema más complejo)

| Archivo | Rol |
|---|---|
| `domain/MotorSync.kt` | Contratos `Nube` / `AlmacenSync` + ciclo |
| `domain/Sincronizacion.kt` | `Marca`, `RelojHibrido`, `Campo`, `FusionSync`, limpieza de documentos |
| `data/SyncLocal.kt` | Triggers SQLite, cola `sync_cola`, `AlmacenRoom` |
| `data/Firebase.kt` | Auth REST, sesión, traducción de valores, `NubeFirestore` |
| `data/ServicioSync.kt` | Orquestación, estado, sondeo, cuenta |

**Ciclo**

1. **Captura por triggers SQLite** (`SyncSql.instalar`), no por código en el DAO:
   `AFTER INSERT` y `AFTER UPDATE` cuando cambia `actualizada`; desactivados mientras se aplican
   cambios remotos (`sync_estado.aplicando = '1'`).
2. Cola local `sync_cola(tabla, fila)`.
3. **Subida** por lotes de **200** a Firestore.
4. **Bajada primero**, con cursor `servidor|nombreDocumento`, también por lotes.
5. **Conflictos**: `Marca = (ms, contador, dispositivo)` con orden total. `RelojHibrido` evita retrocesos y
   descarta marcas con más de 24 h de adelanto. En `AlmacenRoom.aplicarFila` gana `actualizada` mayor;
   empate → comparación textual del contenido.
6. **Notas** viajan sin imágenes ni adjuntos (`paraSincronizar`); al recibirlas se reinsertan las locales por
   id de bloque (`conImagenesDe`).

**Qué viaja**: `materia`, `grupo_lista`, `horario`, `cuaderno`, `deber`, `clase`, `pagina`.

**Qué NO viaja**: `falta`; `materia.carpeta`; `deber.recordado`; imágenes/adjuntos; ajustes, fondo,
banco de racha, sesiones del temporizador.

**Cuenta y nube**

- `CuentaFirebase` usa Identity Toolkit REST: registro, login, Google IDP, recuperación, borrado, refresco de token.
- Sesión en `SharedPreferences("cuenta")`.
- `NubeFirestore` escribe en
  `projects/{proyecto}/databases/(default)/documents/usuarios/{uid}/filas/{tabla}_{id}`, con `runQuery` + `commit`.
- Cuota (`429` / `RESOURCE_EXHAUSTED`) → `CuotaAgotada`, se reintenta «mañana» (no es error).
- Al cambiar de cuenta: se reinicia cursor y se reencola todo.

**Ritmo**

- Con sesión: `InvalidationTracker` de Room → canal conflado → espera 3 s → sincroniza.
- Primer plano: sondeo 60 s, duplicándose hasta 300 s si no hay novedades.
- Segundo plano: se detiene. **No hay WorkManager.**

---

## 5. Copias de seguridad e integraciones

### `.kiwu.json` — `data/Copia.kt`

- `formato: 1`, `app: "kiwu"`, fechas ISO.
- Exporta materias, grupos, deberes, cuadernos, páginas, horarios, clases y faltas (sección propia de Android).
- Importación tolerante: campos opcionales, ids desconocidos descartados, `INSERT IGNORE` (no pisa ni resucita borrados).
- Incluye calificaciones, tarjetas y examen (0.7.x).
- **Incluye imágenes en base64** → archivos potencialmente grandes.

### Google Drive — `data/Drive.kt` + `data/Google.kt`

- Scope `drive.file` (solo ve lo que crea Kiwu).
- Un archivo visible, `Kiwu - copia de seguridad.kiwu.json`, sobrescrito con `POST` + `X-HTTP-Method-Override: PATCH`.
- Autorización con `Identity.getAuthorizationClient`; Drive guarda versiones anteriores por su cuenta.

### Otras

- **Google Calendar**: `CalendarioTelefono` lee `CalendarContract.Instances` (solo lectura, `READ_CALENDAR`);
  usado por el widget de calendario y su hoja del día. Eventos multi-día cuentan solo en su día de inicio.
  `Instances` devuelve una fila **por calendario**, y cada cuenta de Google trae su propio calendario de festivos:
  con varias cuentas el mismo festivo llega repetido con ids distintos. Se quitan con
  `EventosExternos.deduplicar` (clave: título, inicio, fin, todo el día, es tarea), como hace Google Calendar.
  Los cambios del proveedor llegan a los widgets por el `ContentObserver` de `KiwuApp` (§2). La hoja del día
  consulta datos frescos en cada apertura, por eso no sufre de eventos fantasma como el widget.
- **Google Tasks**: `TareasGoogle` implementado pero **desactivado** (`DISPONIBLE = false`); copia local con
  vigencia de 5 minutos que solo se relee cuando un widget se redibuja (la hoja del día la fuerza a fresca).
- **OCR**: ML Kit text recognition, modelo vía Play Services (`meta-data` en manifiesto).
- **Login Google**: Credential Manager + Google ID token → Firebase.

---

## 6. Notificaciones y alarmas (`notify/`)

| Componente | Canal | Función |
|---|---|---|
| `Avisos` | `recordatorios` | Tarea con acciones: Completar / Posponer 1 h / Posponer a mañana |
| `AvisosClases` | `horario` | Aviso de clase; una alarma por clase, reprogramada semanalmente |
| `AvisosRacha` | `racha` | Diaria a las 20:00, solo si la racha está en riesgo |
| `Temporizador` | `temporizador` | Fin de Pomodoro + registro de sesión |

- Alarmas exactas si `canScheduleExactAlarms`; si no, `setAndAllowWhileIdle`.
- Receptores: `AlarmaReceiver`, `AccionReceiver`, `ClaseReceiver`, `RachaReceiver`, `TemporizadorReceiver`, `ArranqueReceiver`.
- `ArranqueReceiver` escucha `BOOT_COMPLETED` y `MY_PACKAGE_REPLACED`.
- Posposiciones «a mañana» persistidas en `SharedPreferences("avisos_pospuestos")`.
- Canales recreados siempre para que su nombre siga el idioma.
- `POST_NOTIFICATIONS` se pide solo cuando hay una tarea con fecha, no al abrir.

---

## 7. Widgets (Glance, `widgets/`)

Cuatro widgets, todos `SizeMode.Exact`:

| Widget | Contenido | Configuración |
|---|---|---|
| **Deberes** | Pendientes con lista, fecha y casilla para completar; los eventos propios también salen mezclados por fecha (pastilla azul, sin casilla, y sin contar en el total) | todas / hoy / próximos 7 / lista / carpeta |
| **Horario** | Ahora / Siguiente, clases de hoy, urgencia de tareas y faltas | todos los horarios / uno |
| **Notas** | Botón «+ Nueva» y notas recientes | todos los cuadernos / uno |
| **Calendario** | Mes con tareas y eventos, navegación de meses | sin config (usa ajustes) |

- `DatosWidgets` = **lógica pura** calculable en JVM (base de `DatosWidgetsTest`).
- `Widgets.refrescar/programar`: refresco por cambios de datos + alarma al próximo cambio de clase o medianoche.
- `ConfiguracionWidgetsActivity` guarda la elección en el estado de Glance (`todas`, `lista:`, `grupo:`, `horario:`, `cuaderno:`).
- `CompletarDeber` completa sin abrir la app; `DiaCalendarioActivity` y `NuevaTareaActivity` son actividades flotantes.
- Colores sincronizados con tema, acento y claro/oscuro.

---

## 8. Inventario de UI

### Tareas (`ui/tareas`, `ui/calendario`)

`TareasScreen` (vista lista o tablero, subvista tareas/archivos) · `MenuTareas` + `MenuListasUi`
(racha, carpetas, listas, contadores, orden, aspecto plegable/compacto/color) · `ListaTareas` · `Tablero` ·
`CalendarioScreen` · `DetalleTarea` (chips de fecha, hora, aviso, lista, repetición, prioridad, examen,
etiquetas; pasos; nota con editor completo; plan de repaso) · `Piezas`, `Organizar`, `Arrastre`, `RachaUi`.

### Notas (`ui/notas`, `ui/editor`)

`NotasScreen` (cuadernos, favoritas, búsqueda sin tildes) · `PaginaScreen` (título, emoji, favorita, mover, eliminar) ·
**Editor por bloques**:

- `EstadoEditor.kt` — máquina de estados: selección, formato, foco, menú «/», conversión de bloques, sangría,
  borrado al inicio. Usa un **carácter invisible de ancho cero (`ZW`)** para detectar backspace al inicio.
- `EditorBloques`, `BloquesEspeciales` (divisor, código, tarjeta, imagen, archivo, enlace/embed, reproductor),
  `BarraFormato`, `Formato`, `Adjuntos`.
- Bloques: párrafo, títulos, viñetas, numerada, checklist, cita, destacado, código, imagen, archivo,
  divisor, enlace, **tarjeta de repaso**.
- `ArchivosScreen`: explorador SAF de la carpeta vinculada a una lista.

### Horario (`ui/horario`)

`HorarioScreen` (día/semana, tarjeta «Ahora», colores, urgencia, contador de faltas) · formulario multi-día
con detección de choques · `HojaTareasClase`, `FaltasUi`, `ColoresClase`, `MenuHorario`.

### Estudio (`ui/estudio` + `domain/Estudio.kt`)

Pomodoro con sesiones por materia/semana · calificaciones con promedio ponderado y nota necesaria ·
tarjetas de repaso manuales, desde el apunte y **generadas sin IA** por reglas (títulos, preguntas,
«Término: definición», negritas).

### Ajustes (`ui/ajustes`)

Apariencia · Idioma · Menú de Tareas · Avisos · Cuenta · Copia de seguridad · Papelera · Acerca de.
Tema sistema/claro/oscuro, acento predefinido/sistema/personalizado, fondo con encuadre y velo.

### Otros

`BuscarScreen` (búsqueda global) · `GuiaScreen` (onboarding + lista «Empieza aquí») ·
`Componentes`, `Fechas`, `Fondo`, `Rueda`, `Tema`.

---

## 9. Aspectos transversales

**i18n**

- Español base, inglés, portugués. **640 strings** en `values/strings.xml`.
- Lint **rompe el build** con `MissingTranslation`, `ExtraTranslation`, `MissingQuantity`, `StringFormatMatches`.
- `bundle.language.enableSplit = false` → el APK incluye los tres idiomas.
- `Idioma`: Android 13+ usa el ajuste del sistema (`localeConfig`); 8–12 usa selector propio
  (SharedPreferences, leído antes del primer fotograma).
- `Textos` mantiene nombres que también viven en los datos; widgets y notificaciones envuelven el contexto con `Idioma.contexto`.

**Tema**

`KiwuTheme` con acentos predefinidos y colores dinámicos (Android 12+), edge-to-edge, iconos de barras
según tema, fondo de imagen guardado como copia propia de la app (no depende del permiso del archivo original).

**Concurrencia**

Flows de Room + `stateIn(WhileSubscribed(5_000))`; guardado con `debounce` (notas 500 ms); `app.alcance`
para guardados que deben sobrevivir a la pantalla.

**Pruebas**

30 archivos, todos JVM/Robolectric (**sin instrumentación ni tests de UI Compose**):
dominio puro (reglas, racha, horario, faltas, repetición, organización, tarjetas, enlaces, estudio, fechas de aviso),
datos (migraciones, `MotorSync`, `SyncRoom`, Firestore, Drive, copias, papelera, widgets) y lógica de UI
(editor, tablero/tema, resumen de ajustes, textos de aviso).

---

## 10. Riesgos y deudas conocidas

1. **`FusionSync` no está conectado.** `AlmacenRoom.aplicarFila` resuelve por **fila completa**
   (`actualizada` + desempate textual), no campo a campo. La fusión por campo de los comentarios no se usa.
2. **Marcas de salida con reloj del sistema.** `Repositorio` usa `System.currentTimeMillis()`; `RelojHibrido`
   solo avanza con marcas recibidas. Con un reloj local atrasado, un cambio podría quedar «viejo» y perder.
3. **Sin sync en segundo plano.** No hay WorkManager: los cambios suben solo con la app abierta o al abrirla.
4. **`falta` no se sincroniza** (decisión documentada) → historial de faltas por dispositivo.
5. **Errores por fila silenciados** en `aplicar`: un `SQLException` se ignora y la fila se pierde sin aviso.
6. **Google Tasks apagado** (`DISPONIBLE = false`): hay código y UI condicional, pero no se usa publicado.
7. **Copias con imágenes base64**: el `.kiwu.json` puede crecer mucho (la sync sí las excluye).
8. **Archivos UI grandes**: `HorarioScreen` (666), `MenuListasUi`/`RachaUi` (521), `AjustesScreen` (489),
   `EstadoEditor` (464), `BloquesEspeciales` (450).
9. **Cadenas mágicas de estado** (`"tareas"`, `"lista"`, `"tablero"`, `"sistema"`, `"carpeta"`…) repartidas
   entre UI y ajustes; faltan enums centralizados.
10. **Sin tests de UI/instrumentación**: la lógica está bien cubierta, la interacción no.
11. **`README.md` de Android dice «Versión actual: 0.5.2»** — desactualizado respecto a 0.7.1.

---

## 11. Índice rápido de archivos

```
KiwuApp.kt / MainActivity.kt / Idioma.kt      arranque, DI manual, i18n
ui/Raiz.kt                                    navegación, pestañas, overlays
domain/Entidades.kt                           materia, grupo_lista, deber, cuaderno, pagina
domain/Horario.kt                             horario, clase, falta + ReglasHorario
domain/Documento.kt                           formato de notas compartido con macOS/Linux
domain/Reglas.kt · Racha.kt · Organizar.kt    reglas puras de tareas, racha, pasos
domain/Estudio.kt · Recordatorios.kt          estudio, avisos
domain/MotorSync.kt · Sincronizacion.kt       contrato de sync, Marca, RelojHibrido, FusionSync
data/KiwuDao.kt                               Room, esquema v9, migraciones 1→9
data/Repositorio.kt                           fachada de negocio para la UI
data/SyncLocal.kt                             triggers, sync_cola, AlmacenRoom
data/Firebase.kt                              auth REST, NubeFirestore
data/ServicioSync.kt                          orquestación y estado de sync
data/Copia.kt · Drive.kt · Google.kt          copias .kiwu.json y Drive
data/TareasGoogle.kt · CalendarioTelefono.kt  integraciones (Tasks apagado)
notify/*.kt                                   4 canales + receptores + programadores
widgets/DatosWidgets.kt                       lógica pura de widgets
widgets/Widgets.kt                            refresco/reprogramación Glance
docs/versiones/android-0.7.1.md               notas de la versión
app/build.gradle.kts · gradle/libs.versions.toml   versiones y lint
```

---

## 12. Conclusión

Arquitectura coherente y mantenible para su tamaño: dominio puro y testeable, Room con borrado lógico y
migraciones versionadas, sync propia por REST + triggers (sin dependencias pesadas), UI Compose modular con
un editor de notas notablemente completo, 4 widgets Glance y alarmas bien separadas por canal, e i18n estricta.

El punto más delicado es la **sincronización**: granularidad real de fila (no de campo), marcas basadas en el
reloj del sistema y ausencia de sync en segundo plano. Después, el tamaño de algunas pantallas y el README
desactualizado.

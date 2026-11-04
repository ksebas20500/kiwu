# opencode.md â€” Contexto permanente de Kiwu Android

> Archivo de contexto para agentes. Resume la arquitectura completa de este repo (la versión Android móvil) en la versiÃ³n **0.7.1**
> para no tener que releer todo el cÃ³digo en cada sesiÃ³n.
> Ãšltima revisiÃ³n: 0.7.1 (`versionCode 18`). Si la versiÃ³n cambia, actualiza las secciones 1, 3 y 10.

---

## 0. CÃ³mo usar este archivo

- LÃ©elo antes de tocar cÃ³digo. Da el mapa: quÃ© existe, dÃ³nde vive y quÃ© reglas respeta.
- Si una tarea afecta a **sincronizaciÃ³n**, lee ademÃ¡s `domain/MotorSync.kt`, `domain/Sincronizacion.kt`,
  `data/SyncLocal.kt`, `data/Firebase.kt`, `data/ServicioSync.kt`.
- Si afecta al **editor de notas**, lee `ui/editor/EstadoEditor.kt` (mÃ¡quina de estados) antes de tocar la UI.
- Si afecta a **textos**, recuerda: el build falla por lint de traducciones (secciÃ³n 9).
- No hay DI framework: las dependencias salen de `KiwuApp`.

---

## 1. Resumen ejecutivo

App nativa **Kotlin + Jetpack Compose**, cliente del ecosistema Kiwu (Linux/macOS): mismo modelo de datos,
mismo JSON de notas (`NoteDocument`/`NoteBlock`/`TextRun`) y misma copia `.kiwu.json`.

| Capa | Paquete | Responsabilidad |
|---|---|---|
| PresentaciÃ³n | `ui/` | Compose: pantallas, ViewModels, navegaciÃ³n, editor, tema |
| Widgets | `widgets/` | Glance (4 widgets) + actividades flotantes |
| Notificaciones | `notify/` | Alarmas, canales, receptores, reprogramaciÃ³n |
| Dominio | `domain/` | Modelos, reglas puras, formato de notas, lÃ³gica de sync |
| Datos | `data/` | Room, DAO, repositorio, ajustes, Firebase, Drive, copias, Google |
| RaÃ­z | `KiwuApp.kt`, `MainActivity.kt`, `Idioma.kt` | Service locator, arranque, i18n |

**Datos de versiÃ³n**

- `versionName = "0.7.1"`, `versionCode = 18`
- `compileSdk`/`targetSdk` = 35, `minSdk` = 26 (Android 8.0)
- Kotlin 2.0.21, AGP 8.7.3, JVM 17, KSP 2.0.21-1.0.28
- Compose BOM 2024.12.01, Room 2.6.1, Glance 1.2.0, DataStore 1.1.1, Navigation 2.8.5
- Firebase por **REST** (sin SDK), Credential Manager + Google Identity, ML Kit OCR
- **Room esquema v9**; la 0.7.1 **no migra la base de datos**

**Escala**: 86 archivos Kotlin de producciÃ³n (~15.130 lÃ­neas), 30 archivos de test (~3.379 lÃ­neas), 640 strings base.

---

## 2. Arranque y navegaciÃ³n

### `KiwuApp` (service locator)

`app/src/main/java/app/kiwu/android/KiwuApp.kt`

- `db` â†’ `KiwuDb` (Room, singleton)
- `repo` â†’ `Repositorio(dao, banco de racha en SharedPreferences "racha")`
- `ajustes` â†’ `AjustesStore` (DataStore)
- `sync` â†’ `ServicioSync`
- `alcance` â†’ `CoroutineScope` global (SupervisorJob) que sobrevive a las pantallas

`onCreate()`:

1. `Textos.actualizar()` â€” nombres de datos (Â«EventosÂ», Â«Notas rÃ¡pidasÂ») en el idioma activo.
2. `iniciarAvisos()` â€” canales + reconciliaciÃ³n de alarmas (tareas, clases, racha) + reinicio diario de hÃ¡bitos.
3. `iniciarWidgets()` â€” `merge` de flujos con debounce 350 ms â†’ refresca/reprograma widgets.
4. `iniciarObservadorCalendario()` â€” `ContentObserver` de `CalendarContract` (eventos e instancias): cuando el
   calendario del telÃ©fono cambia (Google sincroniza, borra o edita un evento) redibuja los widgets tras 1 s de
   debounce. Necesita `READ_CALENDAR`; si se concede con la app viva, `revisarAvisos()` lo registra al volver.
5. `sync.iniciar()`.

TambiÃ©n `attachBaseContext(Idioma.contexto(base))` y refresco de widgets al cambiar claro/oscuro. `revisarAvisos()`
(vuelve a primer plano) re-cuadra alarmas, registra el observador si faltaba y refresca los widgets: red de
seguridad si el proceso estuvo muerto mientras Google cambiaba el calendario.

### `MainActivity`

`app/src/main/java/app/kiwu/android/MainActivity.kt`

- Splash screen + `enableEdgeToEdge()`.
- Lee ajustes **bloqueando con timeout de 1,5 s** antes del primer fotograma (evita flash de tema por defecto).
- Decide mostrar guÃ­a: solo primera vez **y** app vacÃ­a (`Guia.debeMostrarse`).
- Extras de notificaciÃ³n/widget: `tarea`, `nota`, `nueva_nota`, `seccion`.
- `onStart`/`onStop` â†’ sync a primer plano / pausa en segundo plano.
- `onNewIntent` para reutilizar la actividad (`singleTop`).

### NavegaciÃ³n

`ui/Raiz.kt` â€” `NavHost` con 11 destinos:

```
guia Â· repaso Â· principal Â· tarea/{id} Â· ajustes Â· pagina/{id}
buscar Â· temporizador Â· calificaciones/{id} Â· tarjetas/{id} Â· racha
```

`principal` = 3 pestaÃ±as (`Tareas`, `Notas`, `Horario`) con `AnimatedContent`, `ModalNavigationDrawer`
distinto por mÃ³dulo y barra inferior comÃºn. La celebraciÃ³n de racha se dibuja encima de todo.
ViewModels creados a mano con `viewModelFactory { initializer { â€¦ } }` (reciben `app.repo`, `app.alcance`, id de ruta).

---

## 3. Modelo de datos

### Entidades Room

`domain/Entidades.kt`, `domain/Horario.kt`, `domain/Racha.kt`

| Tabla | Campos relevantes |
|---|---|
| `materia` | nombre, icono, orden, `carpeta` (solo local, `content://`), `grupo_id`, `emoji`, `calificaciones` JSON, timestamps, `eliminada` |
| `grupo_lista` | carpeta de listas (un solo nivel), emoji, orden, `plegado`, borrado lÃ³gico |
| `deber` | `materia_id` FK CASCADE, tÃ­tulo, `completado`, `estado` (0/1), `fecha_entrega`, recordatorio, `repite` (mÃ¡scara de dÃ­as), `prioridad`, `etiquetas` CSV, `pasos` JSON, `examen`, `nota` JSON, `recordado`, `eliminada` |
| `cuaderno` | nombre, icono, orden, timestamps |
| `pagina` | `cuaderno_id` FK, tÃ­tulo, icono, `contenido` JSON, `texto_plano`, `favorita`, `tarjetas` JSON |
| `horario` | nombre, icono, orden |
| `clase` | `horario_id` FK, `materia_id`, `grupo` (clases multi-dÃ­a), dÃ­a 1â€“7, minutos inicio/fin, aula, notas, `aviso_min`, `color`, `max_faltas` |
| `falta` | `grupo`, `horario_id`, `dia` (`epochDay`), `justificada`; **sin** `actualizada` ni `eliminada` |

Reglas estructurales:

- **Borrado lÃ³gico** (`eliminada`) en todo lo sincronizable; las consultas filtran `eliminada IS NULL`;
  los borrados en cascada van en `@Transaction` manual.
- **IDs UUID en mayÃºsculas** (`nuevoId()`), compatibles con escritorio.
- **Lista especial Â«EventosÂ»**: id fijo `"eventos"`, creada perezosamente dentro de `deber`.
- **JSON en columnas** (`nota`, `contenido`, `pasos`, `calificaciones`, `tarjetas`) para evitar tablas hijas
  y mantener compatibilidad multiplataforma.
- **Ãndices**: `deber(materia_id, fecha_entrega)`, `pagina(cuaderno_id)`, `clase(horario_id, grupo)`, `falta(grupo, horario_id)`.
- **Horas flotantes**: minutos desde las 00:00 locales, sin zona horaria.

### Migraciones

`KiwuDb` en `data/KiwuDao.kt`, migraciones 1â†’9 escritas a mano:

| VersiÃ³n | Cambio |
|---|---|
| 1â†’2 | Horarios y clases |
| 2â†’3 | Color de clase |
| 3â†’4 | Carpetas de listas + emoji |
| 4â†’5 | Faltas + mÃ¡ximo por materia |
| 5â†’6 | HÃ¡bitos (`repite`) |
| 6â†’7 | Prioridad, etiquetas, pasos |
| 7â†’8 | Examen + calificaciones |
| 8â†’9 | Tarjetas de repaso en notas |

Esquemas exportados en `app/schemas/1.json â€¦ 9.json`; solo en debug se empaquetan como assets para los tests de migraciÃ³n.

### DAO y repositorio

- `data/KiwuDao.kt` (~315 lÃ­neas): DAO abstracto, SQL explÃ­cito, `Upsert`, inserciones `IGNORE` para importar, transacciones de cascada.
- `data/Repositorio.kt` (~361 lÃ­neas): **fachada Ãºnica** de la UI.
  - expone `Flow` por colecciÃ³n;
  - aplica `Reglas`, `ReglasHorario`, `Rachas`;
  - actualizaciones parciales leer-modificar-escribir (no pisa campos);
  - centraliza hÃ¡bitos (`reiniciarRepetidas`), papelera, faltas, notas rÃ¡pidas y clases multi-dÃ­a.

---

## 4. SincronizaciÃ³n (subsistema mÃ¡s complejo)

| Archivo | Rol |
|---|---|
| `domain/MotorSync.kt` | Contratos `Nube` / `AlmacenSync` + ciclo |
| `domain/Sincronizacion.kt` | `Marca`, `RelojHibrido`, `Campo`, `FusionSync`, limpieza de documentos |
| `data/SyncLocal.kt` | Triggers SQLite, cola `sync_cola`, `AlmacenRoom` |
| `data/Firebase.kt` | Auth REST, sesiÃ³n, traducciÃ³n de valores, `NubeFirestore` |
| `data/ServicioSync.kt` | OrquestaciÃ³n, estado, sondeo, cuenta |

**Ciclo**

1. **Captura por triggers SQLite** (`SyncSql.instalar`), no por cÃ³digo en el DAO:
   `AFTER INSERT` y `AFTER UPDATE` cuando cambia `actualizada`; desactivados mientras se aplican
   cambios remotos (`sync_estado.aplicando = '1'`).
2. Cola local `sync_cola(tabla, fila)`.
3. **Subida** por lotes de **200** a Firestore.
4. **Bajada primero**, con cursor `servidor|nombreDocumento`, tambiÃ©n por lotes.
5. **Conflictos**: `Marca = (ms, contador, dispositivo)` con orden total. `RelojHibrido` evita retrocesos y
   descarta marcas con mÃ¡s de 24 h de adelanto. En `AlmacenRoom.aplicarFila` gana `actualizada` mayor;
   empate â†’ comparaciÃ³n textual del contenido.
6. **Notas** viajan sin imÃ¡genes ni adjuntos (`paraSincronizar`); al recibirlas se reinsertan las locales por
   id de bloque (`conImagenesDe`).

**QuÃ© viaja**: `materia`, `grupo_lista`, `horario`, `cuaderno`, `deber`, `clase`, `pagina`.

**QuÃ© NO viaja**: `falta`; `materia.carpeta`; `deber.recordado`; imÃ¡genes/adjuntos; ajustes, fondo,
banco de racha, sesiones del temporizador.

**Cuenta y nube**

- `CuentaFirebase` usa Identity Toolkit REST: registro, login, Google IDP, recuperaciÃ³n, borrado, refresco de token.
- SesiÃ³n en `SharedPreferences("cuenta")`.
- `NubeFirestore` escribe en
  `projects/{proyecto}/databases/(default)/documents/usuarios/{uid}/filas/{tabla}_{id}`, con `runQuery` + `commit`.
- Cuota (`429` / `RESOURCE_EXHAUSTED`) â†’ `CuotaAgotada`, se reintenta Â«maÃ±anaÂ» (no es error).
- Al cambiar de cuenta: se reinicia cursor y se reencola todo.

**Ritmo**

- Con sesiÃ³n: `InvalidationTracker` de Room â†’ canal conflado â†’ espera 3 s â†’ sincroniza.
- Primer plano: sondeo 60 s, duplicÃ¡ndose hasta 300 s si no hay novedades.
- Segundo plano: se detiene. **No hay WorkManager.**

---

## 5. Copias de seguridad e integraciones

### `.kiwu.json` â€” `data/Copia.kt`

- `formato: 1`, `app: "kiwu"`, fechas ISO.
- Exporta materias, grupos, deberes, cuadernos, pÃ¡ginas, horarios, clases y faltas (secciÃ³n propia de Android).
- ImportaciÃ³n tolerante: campos opcionales, ids desconocidos descartados, `INSERT IGNORE` (no pisa ni resucita borrados).
- Incluye calificaciones, tarjetas y examen (0.7.x).
- **Incluye imÃ¡genes en base64** â†’ archivos potencialmente grandes.

### Google Drive â€” `data/Drive.kt` + `data/Google.kt`

- Scope `drive.file` (solo ve lo que crea Kiwu).
- Un archivo visible, `Kiwu - copia de seguridad.kiwu.json`, sobrescrito con `POST` + `X-HTTP-Method-Override: PATCH`.
- AutorizaciÃ³n con `Identity.getAuthorizationClient`; Drive guarda versiones anteriores por su cuenta.

### Otras

- **Google Calendar**: `CalendarioTelefono` lee `CalendarContract.Instances` (solo lectura, `READ_CALENDAR`);
  usado por el widget de calendario y su hoja del dÃ­a. Eventos multi-dÃ­a cuentan solo en su dÃ­a de inicio.
  `Instances` devuelve una fila **por calendario**, y cada cuenta de Google trae su propio calendario de festivos:
  con varias cuentas el mismo festivo llega repetido con ids distintos. Se quitan con
  `EventosExternos.deduplicar` (clave: tÃ­tulo, inicio, fin, todo el dÃ­a, es tarea), como hace Google Calendar.
  Los cambios del proveedor llegan a los widgets por el `ContentObserver` de `KiwuApp` (Â§2). La hoja del dÃ­a
  consulta datos frescos en cada apertura, por eso no sufre de eventos fantasma como el widget.
- **Google Tasks**: `TareasGoogle` implementado pero **desactivado** (`DISPONIBLE = false`); copia local con
  vigencia de 5 minutos que solo se relee cuando un widget se redibuja (la hoja del dÃ­a la fuerza a fresca).
- **OCR**: ML Kit text recognition, modelo vÃ­a Play Services (`meta-data` en manifiesto).
- **Login Google**: Credential Manager + Google ID token â†’ Firebase.

---

## 6. Notificaciones y alarmas (`notify/`)

| Componente | Canal | FunciÃ³n |
|---|---|---|
| `Avisos` | `recordatorios` | Tarea con acciones: Completar / Posponer 1 h / Posponer a maÃ±ana |
| `AvisosClases` | `horario` | Aviso de clase; una alarma por clase, reprogramada semanalmente |
| `AvisosRacha` | `racha` | Diaria a las 20:00, solo si la racha estÃ¡ en riesgo |
| `Temporizador` | `temporizador` / `temporizador_curso` | Fin de Pomodoro + registro de sesiÃ³n; mientras corre, notificaciÃ³n permanente con cuenta atrÃ¡s (`setUsesChronometer` + `setChronometerCountDown`) y acciÃ³n Parar |

- Alarmas exactas si `canScheduleExactAlarms`; si no, `setAndAllowWhileIdle`.
- Receptores: `AlarmaReceiver`, `AccionReceiver`, `ClaseReceiver`, `RachaReceiver`, `TemporizadorReceiver`, `ArranqueReceiver`.
- `ArranqueReceiver` escucha `BOOT_COMPLETED` y `MY_PACKAGE_REPLACED`.
- Posposiciones Â«a maÃ±anaÂ» persistidas en `SharedPreferences("avisos_pospuestos")`.
- Canales recreados siempre para que su nombre siga el idioma.
- `POST_NOTIFICATIONS` se pide solo cuando hay una tarea con fecha, no al abrir.
- `ACCESS_NOTIFICATION_POLICY` (sin diÃ¡logo, se concede en Ajustes del sistema): Â«No molestarÂ» opcional del temporizador â€” se activa al iniciar una cuenta atrÃ¡s de estudio (no en los descansos) y se devuelve a `INTERRUPTION_FILTER_ALL` al terminar o parar.

---

## 7. Widgets (Glance, `widgets/`)

Cuatro widgets, todos `SizeMode.Exact`:

| Widget | Contenido | ConfiguraciÃ³n |
|---|---|---|
| **Deberes** | Pendientes con lista, fecha y casilla para completar; los eventos propios tambiÃ©n salen mezclados por fecha (pastilla azul, sin casilla, y sin contar en el total) | todas / hoy / prÃ³ximos 7 / lista / carpeta |
| **Horario** | Ahora / Siguiente, clases de hoy, urgencia de tareas y faltas | todos los horarios / uno |
| **Notas** | BotÃ³n Â«+ NuevaÂ» y notas recientes | todos los cuadernos / uno |
| **Calendario** | Mes con tareas y eventos, navegaciÃ³n de meses | sin config (usa ajustes) |

- `DatosWidgets` = **lÃ³gica pura** calculable en JVM (base de `DatosWidgetsTest`).
- `Widgets.refrescar/programar`: refresco por cambios de datos + alarma al prÃ³ximo cambio de clase o medianoche.
- `ConfiguracionWidgetsActivity` guarda la elecciÃ³n en el estado de Glance (`todas`, `lista:`, `grupo:`, `horario:`, `cuaderno:`).
- `CompletarDeber` completa sin abrir la app; `DiaCalendarioActivity` y `NuevaTareaActivity` son actividades flotantes.
- Colores sincronizados con tema, acento y claro/oscuro.

---

## 8. Inventario de UI

### Tareas (`ui/tareas`, `ui/calendario`)

`TareasScreen` (vista lista o tablero, subvista tareas/archivos) Â· `MenuTareas` + `MenuListasUi`
(racha, carpetas, listas, contadores, orden, aspecto plegable/compacto/color) Â· `ListaTareas` Â· `Tablero` Â·
`CalendarioScreen` Â· `DetalleTarea` (chips de fecha, hora, aviso, lista, repeticiÃ³n, prioridad, examen,
etiquetas; pasos; nota con editor completo; plan de repaso) Â· `Piezas`, `Organizar`, `Arrastre`, `RachaUi`.

### Notas (`ui/notas`, `ui/editor`)

`NotasScreen` (cuadernos, favoritas, bÃºsqueda sin tildes) Â· `PaginaScreen` (tÃ­tulo, emoji, favorita, mover, eliminar) Â·
**Editor por bloques**:

- `EstadoEditor.kt` â€” mÃ¡quina de estados: selecciÃ³n, formato, foco, menÃº Â«/Â», conversiÃ³n de bloques, sangrÃ­a,
  borrado al inicio. Usa un **carÃ¡cter invisible de ancho cero (`ZW`)** para detectar backspace al inicio.
- `EditorBloques`, `BloquesEspeciales` (divisor, cÃ³digo, tarjeta, imagen, archivo, enlace/embed, reproductor),
  `BarraFormato`, `Formato`, `Adjuntos`.
- Bloques: pÃ¡rrafo, tÃ­tulos, viÃ±etas, numerada, checklist, cita, destacado, cÃ³digo, imagen, archivo,
  divisor, enlace, **tarjeta de repaso**.
- `ArchivosScreen`: explorador SAF de la carpeta vinculada a una lista.

### Horario (`ui/horario`)

`HorarioScreen` (dÃ­a/semana, tarjeta Â«AhoraÂ», colores, urgencia, contador de faltas) Â· formulario multi-dÃ­a
con detecciÃ³n de choques Â· `HojaTareasClase`, `FaltasUi`, `ColoresClase`, `MenuHorario`.

### Estudio (`ui/estudio` + `domain/Estudio.kt`)

Pomodoro con sesiones por materia/semana Â· calificaciones con promedio ponderado y nota necesaria Â·
tarjetas de repaso manuales, desde el apunte y **generadas sin IA** por reglas (tÃ­tulos, preguntas,
Â«TÃ©rmino: definiciÃ³nÂ», negritas).

### Ajustes (`ui/ajustes`)

Apariencia Â· Idioma Â· MenÃº de Tareas Â· Avisos Â· Cuenta Â· Copia de seguridad Â· Papelera Â· Acerca de.
Tema sistema/claro/oscuro, acento predefinido/sistema/personalizado, fondo con encuadre y velo.

### Otros

`BuscarScreen` (bÃºsqueda global) Â· `GuiaScreen` (onboarding + lista Â«Empieza aquÃ­Â») Â·
`Componentes`, `Fechas`, `Fondo`, `Rueda`, `Tema`.

---

## 9. Aspectos transversales

**i18n**

- EspaÃ±ol base, inglÃ©s, portuguÃ©s. **640 strings** en `values/strings.xml`.
- Lint **rompe el build** con `MissingTranslation`, `ExtraTranslation`, `MissingQuantity`, `StringFormatMatches`.
- `bundle.language.enableSplit = false` â†’ el APK incluye los tres idiomas.
- `Idioma`: Android 13+ usa el ajuste del sistema (`localeConfig`); 8â€“12 usa selector propio
  (SharedPreferences, leÃ­do antes del primer fotograma).
- `Textos` mantiene nombres que tambiÃ©n viven en los datos; widgets y notificaciones envuelven el contexto con `Idioma.contexto`.

**Tema**

`KiwuTheme` con acentos predefinidos y colores dinÃ¡micos (Android 12+), edge-to-edge, iconos de barras
segÃºn tema, fondo de imagen guardado como copia propia de la app (no depende del permiso del archivo original).

**Concurrencia**

Flows de Room + `stateIn(WhileSubscribed(5_000))`; guardado con `debounce` (notas 500 ms); `app.alcance`
para guardados que deben sobrevivir a la pantalla.

**Pruebas**

30 archivos, todos JVM/Robolectric (**sin instrumentaciÃ³n ni tests de UI Compose**):
dominio puro (reglas, racha, horario, faltas, repeticiÃ³n, organizaciÃ³n, tarjetas, enlaces, estudio, fechas de aviso),
datos (migraciones, `MotorSync`, `SyncRoom`, Firestore, Drive, copias, papelera, widgets) y lÃ³gica de UI
(editor, tablero/tema, resumen de ajustes, textos de aviso).

---

## 10. Riesgos y deudas conocidas

1. **`FusionSync` no estÃ¡ conectado.** `AlmacenRoom.aplicarFila` resuelve por **fila completa**
   (`actualizada` + desempate textual), no campo a campo. La fusiÃ³n por campo de los comentarios no se usa.
2. **Marcas de salida con reloj del sistema.** `Repositorio` usa `System.currentTimeMillis()`; `RelojHibrido`
   solo avanza con marcas recibidas. Con un reloj local atrasado, un cambio podrÃ­a quedar Â«viejoÂ» y perder.
3. **Sin sync en segundo plano.** No hay WorkManager: los cambios suben solo con la app abierta o al abrirla.
4. **`falta` no se sincroniza** (decisiÃ³n documentada) â†’ historial de faltas por dispositivo.
5. **Errores por fila silenciados** en `aplicar`: un `SQLException` se ignora y la fila se pierde sin aviso.
6. **Google Tasks apagado** (`DISPONIBLE = false`): hay cÃ³digo y UI condicional, pero no se usa publicado.
7. **Copias con imÃ¡genes base64**: el `.kiwu.json` puede crecer mucho (la sync sÃ­ las excluye).
8. **Archivos UI grandes**: `HorarioScreen` (666), `MenuListasUi`/`RachaUi` (521), `AjustesScreen` (489),
   `EstadoEditor` (464), `BloquesEspeciales` (450).
9. **Cadenas mÃ¡gicas de estado** (`"tareas"`, `"lista"`, `"tablero"`, `"sistema"`, `"carpeta"`â€¦) repartidas
   entre UI y ajustes; faltan enums centralizados.
10. **Sin tests de UI/instrumentaciÃ³n**: la lÃ³gica estÃ¡ bien cubierta, la interacciÃ³n no.
11. **`README.md` de Android dice Â«VersiÃ³n actual: 0.5.2Â»** â€” desactualizado respecto a 0.7.1.

---

## 11. Ãndice rÃ¡pido de archivos

```
KiwuApp.kt / MainActivity.kt / Idioma.kt      arranque, DI manual, i18n
ui/Raiz.kt                                    navegaciÃ³n, pestaÃ±as, overlays
domain/Entidades.kt                           materia, grupo_lista, deber, cuaderno, pagina
domain/Horario.kt                             horario, clase, falta + ReglasHorario
domain/Documento.kt                           formato de notas compartido con macOS/Linux
domain/Reglas.kt Â· Racha.kt Â· Organizar.kt    reglas puras de tareas, racha, pasos
domain/Estudio.kt Â· Recordatorios.kt          estudio, avisos
domain/MotorSync.kt Â· Sincronizacion.kt       contrato de sync, Marca, RelojHibrido, FusionSync
data/KiwuDao.kt                               Room, esquema v9, migraciones 1â†’9
data/Repositorio.kt                           fachada de negocio para la UI
data/SyncLocal.kt                             triggers, sync_cola, AlmacenRoom
data/Firebase.kt                              auth REST, NubeFirestore
data/ServicioSync.kt                          orquestaciÃ³n y estado de sync
data/Copia.kt Â· Drive.kt Â· Google.kt          copias .kiwu.json y Drive
data/TareasGoogle.kt Â· CalendarioTelefono.kt  integraciones (Tasks apagado)
notify/*.kt                                   4 canales + receptores + programadores
widgets/DatosWidgets.kt                       lÃ³gica pura de widgets
widgets/Widgets.kt                            refresco/reprogramaciÃ³n Glance
docs/versiones/android-0.7.1.md               notas de la versiÃ³n
app/build.gradle.kts Â· gradle/libs.versions.toml   versiones y lint
```

---

## 12. ConclusiÃ³n

Arquitectura coherente y mantenible para su tamaÃ±o: dominio puro y testeable, Room con borrado lÃ³gico y
migraciones versionadas, sync propia por REST + triggers (sin dependencias pesadas), UI Compose modular con
un editor de notas notablemente completo, 4 widgets Glance y alarmas bien separadas por canal, e i18n estricta.

El punto mÃ¡s delicado es la **sincronizaciÃ³n**: granularidad real de fila (no de campo), marcas basadas en el
reloj del sistema y ausencia de sync en segundo plano. DespuÃ©s, el tamaÃ±o de algunas pantallas y el README
desactualizado.

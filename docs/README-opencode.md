# Contexto de Desarrollo OpenCode — Kiwu Android (v0.7.0 a Versión Actual)

> **Documento de Contexto Técnico Permanente para OpenCode / Agentes IA**  
> **Alcance:** Historial de cambios, arquitectura y decisiones técnicas desde la versión **0.7.0** hasta la versión actual.  
> **Propósito:** Proporcionar contexto claro, estructurado y comentado para que cualquier agente de OpenCode entienda los módulos creados, esquemas de BD, componentes Compose, i18n e integraciones del ecosistema.

---

## 📋 Resumen de Versiones y Evolución

| Versión | Versión Code | Cambios Clave | Estado de BD (Room) |
|---|---|---|---|
| **0.7.0** | `17` | Temporizador Pomodoro, Calificaciones & Promedio Ponderado, Cuenta Atrás de Exámenes, Tarjetas de Repaso manuales. | **Room v9** (Nuevas columnas `calificaciones` en `materia`, `examen` en `deber`, `tarjetas` en `pagina`). |
| **0.7.1** | `18` | Bloque de editor `/tarjeta`, comando `/repasar`, generador automático de tarjetas sin IA, cancelación remota de avisos, tiempo relativo mejorado. | **Room v9** (Sin cambios de esquema). |
| [**0.9.0**](versiones/android-0.9.0.md) | `33` | Inicio, estantería de cuadernos, portadas, mejoras de editor y Ajustes. Commit `488e7a7`. | **Room v13** (migraciones acumuladas desde v9). |
| [**0.9.1**](versiones/android-0.9.1.md) | — | Correcciones de «Para hoy» y eventos externos, menús contextuales para editar y eliminar, y rediseño de Ajustes. Atribución según las notas locales. | **Room v13**. |
| [**0.9.2**](versiones/android-0.9.2.md) | `35` | Arrastre de listas, barra horizontal de comandos, mejoras del cursor y cola común de repaso. Atribución según las notas locales y el código actual. | **Room v13**. |

---

### Cambios locales posteriores al commit de la 0.9.0

El diff posterior a `488e7a7` reúne cambios de la 0.9.1 y la 0.9.2. Las notas locales permiten atribuir parte de ellos. Estos detalles quedan con versión exacta por confirmar:

- Menú lateral con cabecera compacta y contador de racha, cuadrícula de Hoy, 7 días, Calendario y Eventos, accesos a Todas y Repasar, secciones plegables para listas, cuadernos y horarios, y pie fijo con Completadas, Papelera, Ajustes y selector claro/oscuro.
- En Notas, retirada del acceso duplicado a repaso en la barra superior y navegación con Atrás dentro de un cuaderno.
- En Cuenta, etiqueta «Crear cuenta» en el selector y «Crear cuenta (mínimo 6 caracteres)» en el botón.

## 🎯 1. Novedades e Implementaciones Detalladas (0.7.0 a Versión Actual)

### 1.1 Temporizador de Estudio (Pomodoro)
- **Ubicación:** [EstudioScreens.kt](file:///Users/ksebas/Documents/kiwu-android/kiwu/app/src/main/java/app/kiwu/android/ui/estudio/EstudioScreens.kt), [Temporizador.kt](file:///Users/ksebas/Documents/kiwu-android/kiwu/app/src/main/java/app/kiwu/android/notify/Temporizador.kt), [Estudio.kt](file:///Users/ksebas/Documents/kiwu-android/kiwu/app/src/main/java/app/kiwu/android/domain/Estudio.kt)
- **Funcionalidad:**
  - Permite configurar sesiones de estudio (25 min, 50 min) y descansos (5 min, 10 min).
  - Emite notificaciones continuas con cronómetro regresivo (`setUsesChronometer` + `setChronometerCountDown`) e incluye botón de acción para detener.
  - Opcional: Activa automáticamente la política de *"No molestar"* (`ACCESS_NOTIFICATION_POLICY`) durante la sesión de estudio.
  - Funciona mediante alarmas precisas con `AlarmManager`, sonando incluso si el usuario cierra la app.

### 1.2 Calificaciones y Calculadora de Promedio Ponderado
- **Ubicación:** [EstudioScreens.kt](file:///Users/ksebas/Documents/kiwu-android/kiwu/app/src/main/java/app/kiwu/android/ui/estudio/EstudioScreens.kt), [Entidades.kt](file:///Users/ksebas/Documents/kiwu-android/kiwu/app/src/main/java/app/kiwu/android/domain/Entidades.kt)
- **Funcionalidad:**
  - Cada materia (`materia`) almacena un JSON en la columna `calificaciones`.
  - Permite registrar notas con su peso porcentual o ponderación igual.
  - Calcula el promedio acumulado y ofrece la función *"¿Cuánto necesito?"* para determinar la calificación requerida en las evaluaciones restantes para aprobar la materia.

### 1.3 Tarjetas de Repaso (Flashcards) e Integración con el Editor
- **Ubicación:** [EditorBloques.kt](file:///Users/ksebas/Documents/kiwu-android/kiwu/app/src/main/java/app/kiwu/android/ui/editor/EditorBloques.kt), [EstadoEditor.kt](file:///Users/ksebas/Documents/kiwu-android/kiwu/app/src/main/java/app/kiwu/android/ui/editor/EstadoEditor.kt), [EstudioScreens.kt](file:///Users/ksebas/Documents/kiwu-android/kiwu/app/src/main/java/app/kiwu/android/ui/estudio/EstudioScreens.kt)
- **Evolución:**
  - **0.7.0:** Creación manual de preguntas/respuestas desde el menú de la nota, almacenadas en la columna `tarjetas` de la tabla `pagina`.
  - **0.7.1:** Nuevo bloque ejecutable `/tarjeta` embebido en el apunte rich-text (`NoteBlock`).
  - **0.7.1:** Modo `/repasar` para estudiar directamente dentro de la nota.
  - **Generación Heurística sin IA:** Parser automático que extrae tarjetas desde el contenido de la nota detectando:
    1. Títulos con párrafo explicativo debajo.
    2. Preguntas finalizadas con signo `?` y su respuesta siguiente.
    3. Estructuras de definición `Término: Definición`.
    4. Palabras marcadas en **negrita** (convirtiéndolas en huecos para rellenar).

### 1.4 Cuenta Atrás de Exámenes
- **Ubicación:** [DetalleTarea.kt](file:///Users/ksebas/Documents/kiwu-android/kiwu/app/src/main/java/app/kiwu/android/ui/tareas/DetalleTarea.kt), [ListaTareas.kt](file:///Users/ksebas/Documents/kiwu-android/kiwu/app/src/main/java/app/kiwu/android/ui/tareas/ListaTareas.kt)
- **Funcionalidad:**
  - Chip identificador `Examen` en el modelo `Deber` (`examen: Boolean`).
  - Representación visual destacada en las listas con cuenta regresiva dinámica: *"Examen hoy"*, *"Examen mañana"*, *"Examen en X días"*.

### 1.5 Widgets de Inicio (Glance)
- **Ubicación:** [Widgets.kt](file:///Users/ksebas/Documents/kiwu-android/kiwu/app/src/main/java/app/kiwu/android/widgets/Widgets.kt), [DatosWidgets.kt](file:///Users/ksebas/Documents/kiwu-android/kiwu/app/src/main/java/app/kiwu/android/widgets/DatosWidgets.kt), [ConfiguracionWidgetsActivity.kt](file:///Users/ksebas/Documents/kiwu-android/kiwu/app/src/main/java/app/kiwu/android/widgets/ConfiguracionWidgetsActivity.kt)
- **Funcionalidad:**
  - Cuatro widgets con Glance (`androidx.glance`): **Deberes** (tareas pendientes con checkbox), **Horario** (clase actual y siguiente), **Nota rápida** (crear nota / últimas notas de un cuaderno) y **Calendario** (mes con tareas y eventos).
  - Acciones interactivas sin abrir la app mediante activities flotantes (`NuevaTareaActivity`, `DiaCalendarioActivity`).
  - Pantalla de configuración por widget (`ConfiguracionWidgetsActivity`) para elegir lista/cuaderno.
  - Datos provistos por `DatosWidgets.kt`, que consulta el `Repositorio` en un alcance propio y actualiza via `GlanceAppWidget.update`.

### 1.6 Racha de Estudio (Streak)
- **Ubicación:** [RachaUi.kt](file:///Users/ksebas/Documents/kiwu-android/kiwu/app/src/main/java/app/kiwu/android/ui/tareas/RachaUi.kt), [Racha.kt](file:///Users/ksebas/Documents/kiwu-android/kiwu/app/src/main/java/app/kiwu/android/domain/Racha.kt), [AvisosRacha.kt](file:///Users/ksebas/Documents/kiwu-android/kiwu/app/src/main/java/app/kiwu/android/notify/AvisosRacha.kt)
- **Funcionalidad:**
  - Llama/chispa que cuenta días consecutivos "cumplidos": un día con entregas cuenta si queda todo entregado; un día con clase cuenta si completas o añades una tarea.
  - Aviso diario a las 20:00 si la racha está en riesgo (`AvisosRacha`), configurable en Ajustes → Avisos.
  - Estado visual: empezar, al día, libre (sin pendientes), en riesgo (llama gris con «!»).

### 1.7 Compras en la App y Anuncios
- **Ubicación:** [BillingManager.kt](file:///Users/ksebas/Documents/kiwu-android/kiwu/app/src/main/java/app/kiwu/android/data/BillingManager.kt), [AdBanner.kt](file:///Users/ksebas/Documents/kiwu-android/kiwu/app/src/main/java/app/kiwu/android/ui/AdBanner.kt), [KiwuApp.kt](file:///Users/ksebas/Documents/kiwu-android/kiwu/app/src/main/java/app/kiwu/android/KiwuApp.kt)
- **Funcionalidad:**
  - Play Billing v8 con compra única «Quitar anuncios» (incluye reconocimiento de compras pendientes y `acknowledgePurchase`).
  - `BillingManager.comprado: StateFlow<Boolean>` expone el estado de la compra.
  - Banner AdMob en `ui/AdBanner.kt`; el SDK de anuncios solo se inicializa si de verdad se va a enseñar (quien compró «Quitar anuncios» no carga nada). Clave AdMob declarada en el `AndroidManifest.xml`.
  - **Reglas de respaldo explícitas** (`res/xml/backup_rules.xml` y `res/xml/data_extraction_rules.xml`, declaradas en el manifest con `allowBackup`): la sesión de la cuenta (tokens Firebase en `cuenta.xml`) no va a los respaldos en la nube ni a las transferencias entre teléfonos; el resto de los datos (base Room incluida) sí se respalda.

### 1.8 Eventos de Google Calendar en la Pantalla de Calendario
- **Ubicación:** [CalendarioScreen.kt](file:///Users/ksebas/Documents/kiwu-android/kiwu/app/src/main/java/app/kiwu/android/ui/calendario/CalendarioScreen.kt), [CalendarioTelefono.kt](file:///Users/ksebas/Documents/kiwu-android/kiwu/app/src/main/java/app/kiwu/android/data/CalendarioTelefono.kt)
- **Funcionalidad:**
  - Lectura asíncrona de `CalendarContract.Instances` a través de `CalendarioTelefono.eventos(...)` con `produceState` en Compose.
  - **Grilla Mensual:** Visualización de eventos externos en la celda diaria mediante indicadores de puntos de color distintivo (`AzulEventoTexto`).
  - **Agenda Diaria:** Lista unificada de eventos de Google Calendar y tareas de Kiwu, utilizando la tarjeta `TarjetaEventoExternoAgenda` con hora y etiqueta *"Google Calendar / Google Agenda"*.
  - **Gestión de Permisos:** Banner/botón integrado en la cabecera del calendario para solicitar el permiso `READ_CALENDAR` mediante `rememberLauncherForActivityResult`.

### 1.9 Repaso Espaciado (SRS) de Tarjetas
- **Ubicación:** [Estudio.kt](file:///Users/ksebas/Documents/kiwu-android/kiwu/app/src/main/java/app/kiwu/android/domain/Estudio.kt) (objeto `Repaso`), [EstudioScreens.kt](file:///Users/ksebas/Documents/kiwu-android/kiwu/app/src/main/java/app/kiwu/android/ui/estudio/EstudioScreens.kt) (`TarjetasScreen`), [EstudioTest.kt](file:///Users/ksebas/Documents/kiwu-android/kiwu/app/src/test/java/app/kiwu/android/EstudioTest.kt)
- **Funcionalidad:**
  - Cajas de Leitner (0–5): acertar sube de caja y la tarjeta vuelve en 1, 2, 4, 8 o 16 días (`Repaso.DIAS`, el índice es la caja); fallar vuelve a empezar y queda pendiente ya mismo.
  - `Tarjeta` guarda `caja` y `vence` en el JSON de la columna `tarjetas` con claves **opcionales** `c`/`v`: las versiones anteriores las leen igual (compatibilidad intacta, sin cambio de esquema).
  - La pantalla de tarjetas tiene el botón **«Repasar hoy (N)»**: arma la cola solo con las vencidas (las nuevas primero, luego las más atrasadas) y **persiste la caja al contestar** («La sé» sube caja, «Repasar otra vez» la reinicia y la manda al final). Si todo está al día se ve «¡Todo al día!».
  - Las tarjetas escritas como bloque `/tarjeta` en el apunte se repasan igual pero no llevan caja (no tienen dónde recordar su progreso).
- **Cómo probar (manual):**
  1. Abre una nota con tarjetas guardadas → Tarjetas de repaso. Debe aparecer «Repasar hoy (N)» con las pendientes.
  2. Repasa: «La sé» → la tarjeta desaparece de hoy y no vuelve al día siguiente; «Repasar otra vez» → reaparece en la misma sesión.
  3. Cierra y reabre la pantalla: las acertadas de hoy ya no cuentan en «Repasar hoy».
  4. Regresión: exporta/ importa `.kiwu.json` con versiones viejas del formato (sin `c`/`v`) y verifica que las tarjetas se leen como nuevas.
  - Tests unitarios: `lasTarjetasConRepasoGuardanSuCajaYLosViejosLasLeenSinElla`, `elRepasoEspaciadoSubeCajasYLasFalladasVuelvenAEmpezar` (en `EstudioTest`).

### 1.10 Gestos (Swipe) en las Filas de Tareas
- **Ubicación:** [Piezas.kt](file:///Users/ksebas/Documents/kiwu-android/kiwu/app/src/main/java/app/kiwu/android/ui/tareas/Piezas.kt) (`FilaTarea` con `SwipeToDismissBox`), [Recordatorios.kt](file:///Users/ksebas/Documents/kiwu-android/kiwu/app/src/main/java/app/kiwu/android/domain/Recordatorios.kt) (`entregaPospuesta`), [Repositorio.kt](file:///Users/ksebas/Documents/kiwu-android/kiwu/app/src/main/java/app/kiwu/android/data/Repositorio.kt) (`posponer`), [TareasViewModel.kt](file:///Users/ksebas/Documents/kiwu-android/kiwu/app/src/main/java/app/kiwu/android/ui/tareas/TareasViewModel.kt), [PosponerManianaTest.kt](file:///Users/ksebas/Documents/kiwu-android/kiwu/app/src/test/java/app/kiwu/android/PosponerManianaTest.kt)
- **Funcionalidad:**
  - **Deslizar a la derecha** = completar la tarea (o **reabrir** una ya completada); la fila siempre vuelve a su sitio (`confirmValueChange` devuelve `false`).
  - **Deslizar a la izquierda** = posponer a **mañana** conservando la hora de entrega (o a las 8:00 si no tenía fecha); el aviso se reprograma. Se ve el fondo terciario con icono de reloj y «Mañana».
  - No aplica a eventos (`esEvento`) ni a tareas repetidas (`repite != 0`, protege el patrón semanal); en completadas el posponer está deshabilitado.
  - Se usa en las 3 ramas de la lista de tareas y en la agenda del calendario (5 call sites).
- **Cómo probar (manual):**
  1. En la lista de tareas, desliza una pendiente a la derecha: queda completada; desliza de nuevo a la derecha: se reabre.
  2. Desliza una pendiente con fecha (p. ej. hoy 18:00) a la izquierda: el chip pasa a mañana 18:00.
  3. Tarea sin fecha: al deslizar a la izquierda queda mañana a las 8:00.
  4. Verifica que en eventos y tareas repetidas el gesto de la izquierda no hace nada (la fila no se desliza hacia allá).
  - Tests unitarios: `laEntregaPospuestaAlDeslizarCaeMananaALaMismaHora` (en `PosponerManianaTest`).

### 1.11 Tema «Negro» (AMOLED)
- **Ubicación:** [Tema.kt](file:///Users/ksebas/Documents/kiwu-android/kiwu/app/src/main/java/app/kiwu/android/ui/Tema.kt) (`KiwuTheme(oscuro, acento, amoled)` + `ColorScheme.enNegro()`), [Raiz.kt](file:///Users/ksebas/Documents/kiwu-android/kiwu/app/src/main/java/app/kiwu/android/ui/Raiz.kt), [AjustesScreen.kt](file:///Users/ksebas/Documents/kiwu-android/kiwu/app/src/main/java/app/kiwu/android/ui/ajustes/AjustesScreen.kt), [ResumenAjustes.kt](file:///Users/ksebas/Documents/kiwu-android/kiwu/app/src/main/java/app/kiwu/android/ui/ajustes/ResumenAjustes.kt), activities de widgets (`NuevaTareaActivity`, `ConfiguracionWidgetsActivity`, `DiaCalendarioActivity`), [Widgets.kt](file:///Users/ksebas/Documents/kiwu-android/kiwu/app/src/main/java/app/kiwu/android/widgets/Widgets.kt)
- **Funcionalidad:**
  - Cuarta opción de tema en Ajustes → Apariencia (Sistema / Claro / Oscuro / **Negro**), con el valor `"negro"` en DataStore.
  - `enNegro()` convierte cualquier esquema oscuro (coral, acento elegido o Material You dinámico) en fondo negro puro con superficies casi negras; el acento no cambia.
  - Las activities flotantes de los widgets siguen el tema (los widgets en sí mantienen su configuración propia).
- **Cómo probar (manual):**
  1. Ajustes → Apariencia → elige **Negro**: toda la app pasa a fondo negro puro (útil en OLED: los píxeles se apagan).
  2. Prueba también con un acento distinto y con «color del sistema» (Material You): el negro debe conservarse y el acento seguir visible.
  3. Con tema claro seleccionado no cambia nada (el negro solo aplica en oscuro).
  4. Revisa la resumen bajo «Apariencia» en la pantalla principal de Ajustes: debe decir «Negro».

### 1.12 Resumen Semanal por Aviso
- **Ubicación:** [Resumen.kt](file:///Users/ksebas/Documents/kiwu-android/kiwu/app/src/main/java/app/kiwu/android/domain/Resumen.kt) (`ResumenSemanal`, `Resumenes`), [AvisosResumen.kt](file:///Users/ksebas/Documents/kiwu-android/kiwu/app/src/main/java/app/kiwu/android/notify/AvisosResumen.kt) (canal `resumen`, `ProgramadorResumen`, `ResumenReceiver`), [AjustesStore.kt](file:///Users/ksebas/Documents/kiwu-android/kiwu/app/src/main/java/app/kiwu/android/data/AjustesStore.kt) (`resumenSemanal`), [KiwuApp.kt](file:///Users/ksebas/Documents/kiwu-android/kiwu/app/src/main/java/app/kiwu/android/KiwuApp.kt) (`iniciarAvisos`), [AjustesScreen.kt](file:///Users/ksebas/Documents/kiwu-android/kiwu/app/src/main/java/app/kiwu/android/ui/ajustes/AjustesScreen.kt), [ResumenTest.kt](file:///Users/ksebas/Documents/kiwu-android/kiwu/app/src/test/java/app/kiwu/android/ResumenTest.kt)
- **Funcionalidad:**
  - Aviso los **domingos a las 19:00** con lo logrado en la semana natural (lunes→domingo): tareas completadas (por `fechaCompletado`), minutos de estudio (sesiones de la base Room, `repo.sesionesAhora()`) y racha, con plurales e «X h Y min».
  - Alarma **inexacta** (`setAndAllowWhileIdle`, unos minutos de retraso no importan) y reprogramación del siguiente domingo dentro del receptor (`reconciliar(true, ahora + 60_000)`).
  - Sin tareas ni estudio **no se avisa** (la racha sola no basta: ya se avisa a diario).
  - Ajuste `resumenSemanal` (default `true`) en Ajustes → Avisos, reaccionando con `distinctUntilChanged` como la racha; desactivado no se reprograma la alarma.
- **Cómo probar (manual):**
  1. Ajustes → Avisos: activa/desactiva «Resumen semanal» y verifica que la fila de resumen de ajustes refleja el estado («sin resumen semanal» al apagarlo).
  2. Para probarlo sin esperar al domingo: completa una tarea y estudia una sesión Pomodoro el día actual; desde `adb shell` lanza el receiver:
     ```bash
     adb shell run-as app.kiwu.android am broadcast -a app.kiwu.android.RESUMEN -n app.kiwu.android/.notify.ResumenReceiver
     ```
     (El receiver es `exported=false`: en builds debug el comando se lanza como la propia app con `run-as`; un `adb shell am broadcast` a secas lo deniega el sistema.)
     Debe llegar el aviso «Tu semana en Kiwu» con las cantidades del día.
  3. Semana vacía (borra la tarea y no estudies): el mismo comando **no** debe notificar nada.
  4. Verifica el canal en Ajustes del sistema → Notificaciones → Kiwu: «Resumen semanal».
  5. También puedes verificar que la alarma quedó programada para el domingo 19:00 con `adb shell dumpsys alarm | grep resumen` (o busca `RESU`).
  - Tests unitarios (en `ResumenTest`): conteo de tareas por semana (excluye eventos/eliminadas), minutos por día natural, aviso siempre un domingo a las 19:00 y `hayQueContar`.

---

### 1.13 Gestos Unificados en la Hoja de la Clase (tanda 1)
- **Ubicación:** [Piezas.kt](file:///Users/ksebas/Documents/kiwu-android/kiwu/app/src/main/java/app/kiwu/android/ui/tareas/Piezas.kt) (composable nuevo `ConGestosTarea` + `FilaTarea` refactorizada para usarlo), [HojaTareasClase.kt](file:///Users/ksebas/Documents/kiwu-android/kiwu/app/src/main/java/app/kiwu/android/ui/horario/HojaTareasClase.kt) (su `FilaTarea` compacta ahora se envuelve en `ConGestosTarea`, parámetro `posponer` nuevo), [HorarioViewModel.kt](file:///Users/ksebas/Documents/kiwu-android/kiwu/app/src/main/java/app/kiwu/android/ui/horario/HorarioViewModel.kt) (`posponerTarea`), [HorarioScreen.kt](file:///Users/ksebas/Documents/kiwu-android/kiwu/app/src/main/java/app/kiwu/android/ui/horario/HorarioScreen.kt)
- **Funcionalidad:**
  - El envoltorio de gestos de las filas de tarea (derecha completa/reabre, izquierda pospone a mañana) se extrajo a `ConGestosTarea(d, alMarcar, alPosponer, contenido)`: un solo código de swipe, dos diseños de fila.
  - La hoja de tareas de la clase (desde el horario) ahora también permite posponer deslizando a la izquierda; el diseño compacto (checkbox + título + fecha) no cambia.
  - Las dos filas comparten las mismas reglas: no se posponen eventos ni tareas repetidas, y las completadas solo aceptan el gesto de la derecha.
- **Cómo probar (manual):**
  1. Horario → pulsa una clase → su hoja de tareas: desliza una pendiente a la izquierda → la entrega pasa a mañana (conserva la hora; sin fecha → mañana 8:00).
  2. Desliza a la derecha: se completa; otra vez a la derecha: se reabre.
  3. Tarea repetida o evento en la hoja: el gesto de la izquierda no se activa.
  4. Regresión: la lista principal de tareas se comporta igual que antes (usa el mismo composable).

### 1.14 Sync de Faltas de Clase (tanda 2)
- **Ubicación:** [Horario.kt](file:///Users/ksebas/Documents/kiwu-android/kiwu/app/src/main/java/app/kiwu/android/domain/Horario.kt) (`Falta` gana `actualizada: Long` y `eliminada: Long?`), [KiwuDao.kt](file:///Users/ksebas/Documents/kiwu-android/kiwu/app/src/main/java/app/kiwu/android/data/KiwuDao.kt) (migración `MIGRACION_9_10`, queries con `WHERE eliminada IS NULL`, quitar/justificar/vaciar como borrado lógico), [SyncLocal.kt](file:///Users/ksebas/Documents/kiwu-android/kiwu/app/src/main/java/app/kiwu/android/data/SyncLocal.kt) (`falta` entra en `tablas`; encolada inicial con marker `faltas_encoladas`), [Repositorio.kt](file:///Users/ksebas/Documents/kiwu-android/kiwu/app/src/main/java/app/kiwu/android/data/Repositorio.kt), [Copia.kt](file:///Users/ksebas/Documents/kiwu-android/kiwu/app/src/main/java/app/kiwu/android/data/Copia.kt) (al importar, `actualizada = creada`)
- **Funcionalidad:**
  - Las faltas viajan en el sync como las demás tablas: marcar, quitar y justificar tocan su `actualizada`; los triggers de SQLite las encolan solas.
  - **Borrado lógico:** quitar una falta (o vaciar un horario) pone `eliminada` en vez de `DELETE`, para que el sync la borre en los demás dispositivos; los queries de lectura filtran `eliminada IS NULL`, así que ya no cuentan.
  - La migración 9→10 añade las columnas y copia `creada` a `actualizada`; la **encolada inicial** de las faltas viejas vive en `SyncSql.instalar` (con marker `faltas_encoladas` en `sync_estado`) porque Room rechaza tablas «inesperadas» creadas dentro de una migración (validación estricta de esquema).
  - El formato `.kiwu.json` no cambia (el campo es interno); al importar un respaldo las faltas estrenan `actualizada = creada`.
- **Cómo probar (manual):**
  1. Dos dispositivos con la misma cuenta: marca una falta en el móvil → en la tablet aparece al sincronizar.
  2. Justifícala en la tablet → en el móvil llega la marca «justificada».
  3. Quitarla en el móvil → desaparece en la tablet (el borrado lógico viaja).
  4. Respaldo: exporta `.kiwu.json`, bórralo todo, importa → las faltas vuelven y `Faltas` de ajustes vuelve a contarlas.

### 1.15 Sesiones de Estudio Portátiles (tanda 3)
- **Ubicación:** [Estudio.kt](file:///Users/ksebas/Documents/kiwu-android/kiwu/app/src/main/java/app/kiwu/android/domain/Estudio.kt) (`SesionEstudio` convertida a entidad Room, tabla `sesion`; `TiempoEstudio.leer` queda solo para la importación), [KiwuDao.kt](file:///Users/ksebas/Documents/kiwu-android/kiwu/app/src/main/java/app/kiwu/android/data/KiwuDao.kt) (migración `MIGRACION_10_11`, queries + `podarSesiones`), [SyncLocal.kt](file:///Users/ksebas/Documents/kiwu-android/kiwu/app/src/main/java/app/kiwu/android/data/SyncLocal.kt) (`sesion` entra en `tablas`), [Repositorio.kt](file:///Users/ksebas/Documents/kiwu-android/kiwu/app/src/main/java/app/kiwu/android/data/Repositorio.kt) (`registrarSesion`, `guardarSesionesImportadas`, flow `sesiones`), [Temporizador.kt](file:///Users/ksebas/Documents/kiwu-android/kiwu/app/src/main/java/app/kiwu/android/notify/Temporizador.kt) (escribe via repo; `sesiones(c)` eliminado), [EstudioScreens.kt](file:///Users/ksebas/Documents/kiwu-android/kiwu/app/src/main/java/app/kiwu/android/ui/estudio/EstudioScreens.kt) (lee el flow), [AvisosResumen.kt](file:///Users/ksebas/Documents/kiwu-android/kiwu/app/src/main/java/app/kiwu/android/notify/AvisosResumen.kt) (`sesionesAhora()`), [Copia.kt](file:///Users/ksebas/Documents/kiwu-android/kiwu/app/src/main/java/app/kiwu/android/data/Copia.kt) (sección «sesiones» del `.kiwu.json`), [KiwuApp.kt](file:///Users/ksebas/Documents/kiwu-android/kiwu/app/src/main/java/app/kiwu/android/KiwuApp.kt) (`importarSesionesViejas()`)
- **Funcionalidad:**
  - Hasta 0.8.2 el tiempo estudiado vivía en preferencias (JSON `{m, d, n}`): no viajaba en el sync, no entraba al respaldo y no podía borrar lógicamente. Ahora es la tabla Room `sesion` (id, materia_id, dia, minutos, creada, actualizada, eliminada) y viaja en el sync.
  - La **poda** (sesiones de más de 8 semanas) pasa de recortar la lista a borrado lógico (`podarSesiones`); los tombstones viajan y los queries filtran `eliminada IS NULL`, así que las viejas nunca cuentan.
  - **Importación una sola vez:** al arrancar, `KiwuApp.importarSesionesViejas()` lee las preferencias «temporizador», las guarda en Room y borra la clave `sesiones` (marker `sesiones_importadas`, así un respaldo restaurado de prefs no las duplica).
  - El temporizador guarda en sus preferencias **solo el estado del reloj** (fin, pausa, materia, color, no molestar); el registro de minutos va al repo, que poda en cada registro.
  - El escritorio (macOS/Linux) no usa sesiones: el `.kiwu.json` lleva la sección nueva «sesiones» pero las versiones viejas la ignoran.
  - El resumen semanal (1.12) ahora lee las sesiones de Room (`sesionesAhora()`), así que cuenta lo estudiado en **cualquier** dispositivo de la cuenta.
- **Cómo probar (manual):**
  1. **Migración de prefs:** instala la versión anterior con al menos una sesión Pomodoro hecha → actualiza a esta build → abre Estudio → la «semana» muestra los minutos viejos (importados una sola vez).
  2. **Sync:** dos dispositivos con la misma cuenta: estudia 25 min en el móvil → la tablet refleja el tiempo en su pantalla de estudio y en el resumen semanal.
  3. **Respaldo:** exporta `.kiwu.json` → verifica la sección `"sesiones"` → importa en otra instalación (o tras borrar datos) → el resumen del import dice cuántas sesiones entraron; importar otra vez no duplica.
  4. **Poda:** registra una sesión con fecha de hace 60 días (o espera) → desaparece de la semana y del dispositivo remoto al sincronizar, sin romper nada.

### 1.16 Color Propio de Cada Lista (materia)
- **Ubicación:** [Entidades.kt](file:///Users/ksebas/Documents/kiwu-android/kiwu/app/src/main/java/app/kiwu/android/domain/Entidades.kt) (`Materia.color: String?`), [KiwuDao.kt](file:///Users/ksebas/Documents/kiwu-android/kiwu/app/src/main/java/app/kiwu/android/data/KiwuDao.kt) (migración `MIGRACION_11_12`, versión 12), [Repositorio.kt](file:///Users/ksebas/Documents/kiwu-android/kiwu/app/src/main/java/app/kiwu/android/data/Repositorio.kt) (`crearMateria`/`editarMateria` con color validado), [TareasViewModel.kt](file:///Users/ksebas/Documents/kiwu-android/kiwu/app/src/main/java/app/kiwu/android/ui/tareas/TareasViewModel.kt), [MenuListasUi.kt](file:///Users/ksebas/Documents/kiwu-android/kiwu/app/src/main/java/app/kiwu/android/ui/tareas/MenuListasUi.kt) (`DialogoNuevoElemento` con `conColor`; punto de color en `IconoLista`), [TareasScreen.kt](file:///Users/ksebas/Documents/kiwu-android/kiwu/app/src/main/java/app/kiwu/android/ui/tareas/TareasScreen.kt), [ColoresClase.kt](file:///Users/ksebas/Documents/kiwu-android/kiwu/app/src/main/java/app/kiwu/android/ui/horario/ColoresClase.kt) (`de(c, colorMateria)`), [HorarioScreen.kt](file:///Users/ksebas/Documents/kiwu-android/kiwu/app/src/main/java/app/kiwu/android/ui/horario/HorarioScreen.kt), [HojaTareasClase.kt](file:///Users/ksebas/Documents/kiwu-android/kiwu/app/src/main/java/app/kiwu/android/ui/horario/HojaTareasClase.kt), [DatosWidgets.kt](file:///Users/ksebas/Documents/kiwu-android/kiwu/app/src/main/java/app/kiwu/android/widgets/DatosWidgets.kt) / [WidgetHorario.kt](file:///Users/ksebas/Documents/kiwu-android/kiwu/app/src/main/java/app/kiwu/android/widgets/WidgetHorario.kt), [Copia.kt](file:///Users/ksebas/Documents/kiwu-android/kiwu/app/src/main/java/app/kiwu/android/data/Copia.kt)
- **Funcionalidad:**
  - Cada lista (materia) puede tener color propio (`#RRGGBB`) desde su diálogo de crear/editar, con el mismo selector de 8 tonos + color a medida que las clases. Sin color → el automático de siempre (hash del id).
  - **Prioridad de color** en el horario y el widget: color de la clase (si lo tiene) → color de su lista → automático. Así cambiar el color de una materia repinta todas sus clases de golpe, y una clase suelta todavía puede llevar otro.
  - El menú de listas muestra el color como punto (`IconoLista`) cuando la lista no tiene emoji; con emoji manda el emoji.
  - El color viaja en el sync (columna nueva, triggers de SQLite) y en el respaldo `.kiwu.json` como campo opcional `"color"` (las copias viejas y el escritorio lo ignoran).
- **Cómo probar (manual):**
  1. Menú de Tareas → «Nueva lista» → elige un color → la fila muestra el punto de ese color.
  2. Edita la lista → «Editar (nombre, emoji y color)» → cambia el color → sus clases del horario (vista día/semana y widget) cambian a la vez.
  3. Una clase con color propio sigue mandando sobre el de su lista.
  4. «Automático» en el selector quita el color y vuelve el tono por hash.
  5. Respaldo: exporta → el JSON lleva `"color"` → importa en otra instalación y se conserva.

### 1.17 Compartir una Nota en PDF Estructurado
- **Ubicación:** [PdfNota.kt](file:///Users/ksebas/Documents/kiwu-android/kiwu/app/src/main/java/app/kiwu/android/ui/notas/PdfNota.kt) (renderizador nuevo), [PaginaScreen.kt](file:///Users/ksebas/Documents/kiwu-android/kiwu/app/src/main/java/app/kiwu/android/ui/notas/PaginaScreen.kt) (acción «Compartir PDF» del menú), [AndroidManifest.xml](file:///Users/ksebas/Documents/kiwu-android/kiwu/app/src/main/AndroidManifest.xml) (`FileProvider` con autoridad `${applicationId}.archivos`) + [archivos.xml](file:///Users/ksebas/Documents/kiwu-android/kiwu/app/src/main/res/xml/archivos.xml) (`cache-path exportaciones/`)
- **Funcionalidad:**
  - El PDF se dibuja con `PdfDocument` + `StaticLayout` (sin dependencias nuevas) y **respeta la estructura** de la nota: títulos y subtítulos con su tamaño, párrafos con negrita/cursiva/subrayado/tachado, colores de texto y resaltado, viñetas y listas numeradas (con su cuenta real y sangría), checklist con ☐/☑ y tachado, citas con barra, destacados y código con fondo, divisor, imágenes (base64 reducida, escaladas a la hoja), adjuntos, enlaces y tarjetas pregunta/respuesta.
  - **A4** (595×842 pt) con márgenes, texto seleccionable y paginación automática línea a línea; al pie de cada hoja: «Kiwu · título · cuaderno» y el número de página.
  - La primera hoja repite el título de la nota como cabecera; el nombre del archivo se deriva del título (`calculo-integral.pdf`), sin acentos ni símbolos.
  - El PDF se guarda en la caché (`exportaciones/`) y se comparte con `ACTION_SEND` vía `FileProvider` (el diálogo del sistema: correo, Drive, mensajería…). Los PDF de más de un día se borran al generar el siguiente.
  - Si el render falla, se muestra el aviso «No se pudo crear el PDF» y la nota queda intacta.
- **Cómo probar (manual):**
  1. Abre una nota con título, viñetas, una lista numerada, una tarea marcada, una cita, un bloque de código, una imagen y una tarjeta.
  2. Menú ⋮ → «Compartir PDF» → elige «Guardar en Drive» o «Archivos» → abre el PDF: se ven los títulos, las listas y la imagen; el texto se puede seleccionar y las hojas llevan el pie con el número.
  3. Una nota larga (varias pantallas) se reparte en hojas sin cortar líneas a la mitad.
  4. Una nota vacía también genera un PDF válido (`nota.pdf`).
  5. En inglés y portugués la acción y el aviso de error salen traducidos.

### 1.18 SRS Unificado: «Repasar Hoy» y Aviso Diario
- **Ubicación:** [RepasoGlobal.kt](file:///Users/ksebas/Documents/kiwu-android/kiwu/app/src/main/java/app/kiwu/android/domain/RepasoGlobal.kt) (`PaginaTarjetas`, `TarjetaDeNota`, `cola`, `prioridad`, `proximoAviso`), [KiwuDao.kt](file:///Users/ksebas/Documents/kiwu-android/kiwu/app/src/main/java/app/kiwu/android/data/KiwuDao.kt) (`paginasConTarjetas`), [Repositorio.kt](file:///Users/ksebas/Documents/kiwu-android/kiwu/app/src/main/java/app/kiwu/android/data/Repositorio.kt), [RepasoHoyViewModel.kt](file:///Users/ksebas/Documents/kiwu-android/kiwu/app/src/main/java/app/kiwu/android/ui/estudio/RepasoHoyViewModel.kt), [RepasoHoyScreen.kt](file:///Users/ksebas/Documents/kiwu-android/kiwu/app/src/main/java/app/kiwu/android/ui/estudio/RepasoHoyScreen.kt), [AvisosRepaso.kt](file:///Users/ksebas/Documents/kiwu-android/kiwu/app/src/main/java/app/kiwu/android/notify/AvisosRepaso.kt), [AjustesStore.kt](file:///Users/ksebas/Documents/kiwu-android/kiwu/app/src/main/java/app/kiwu/android/data/AjustesStore.kt) (`repasoAviso`), [AjustesScreen.kt](file:///Users/ksebas/Documents/kiwu-android/kiwu/app/src/main/java/app/kiwu/android/ui/ajustes/AjustesScreen.kt), [Raiz.kt](file:///Users/ksebas/Documents/kiwu-android/kiwu/app/src/main/java/app/kiwu/android/ui/Raiz.kt) (ruta `repaso-hoy` e icono en Notas), [MainActivity.kt](file:///Users/ksebas/Documents/kiwu-android/kiwu/app/src/main/java/app/kiwu/android/MainActivity.kt) (`EXTRA_REPASO`), [KiwuApp.kt](file:///Users/ksebas/Documents/kiwu-android/kiwu/app/src/main/java/app/kiwu/android/KiwuApp.kt) (programación del aviso)
- **Funcionalidad:**
  - **Una sola cola:** «Repasar hoy» junta las tarjetas vencidas de **todas** las notas (solo las guardadas, que son las que tienen caja) en vez de repasar nota por nota. Se abre desde el icono de repaso de la cabecera de Notas.
  - Al contestar, cada tarjeta guarda su caja en su nota de origen (`Repaso.acertada` / `Repaso.fallada`), igual que el repaso de una nota suelta. «Repasar otra vez» la devuelve al final de la sesión.
  - **Prioridad por examen:** si hay un examen pendiente (`deber.examen`) a 7, 3 o 1 días, las tarjetas de las notas de esa materia van primero (prioridad 1, 2 y 3). La nota se asocia a la materia por el nombre de su cuaderno («Cálculo» ↔ materia «Cálculo») o por el título del examen y la nota; se compara sin acentos ni mayúsculas y se admite que uno contenga al otro. No hay que configurar nada ni cambiar el esquema.
  - **Aviso diario:** una alarma a las 18:00 (`ProgramadorRepaso`) avisa «Tarjetas para hoy» solo si la cola tiene tarjetas vencidas; al tocarlo se abre directamente «Repasar hoy» (`EXTRA_REPASO`). Se puede apagar en Ajustes → Avisos («Aviso de repaso»); la red de seguridad de `revisarAvisos()` vuelve a cuadrar la alarma al volver a la app.
- **Cómo probar (manual):**
  1. Ten dos notas con tarjetas pendientes en cuadernos distintos → Notas → icono de repaso: aparecen las dos con su cuenta y «Repasar hoy (N)».
  2. Empieza la sesión: contesta una tarjeta → vuelve a entrar: la acertada ya no cuenta y la fallada sigue.
  3. Crea un examen de «Cálculo» para dentro de 2 días y una nota en un cuaderno llamado «Cálculo» con tarjetas vencidas → en «Repasar hoy» esa nota sale primero y con la etiqueta «Examen … · 2 d».
  4. Cambia el examen a dentro de 10 días o márcalo completado → la etiqueta y la prioridad desaparecen.
  5. Ajustes → Avisos: apaga «Aviso de repaso» → la línea de resumen dice «sin aviso de repaso»; vuelve a encenderlo. Con tarjetas vencidas, lanza el receiver para verlo ya (build debug):
     ```bash
     adb shell run-as app.kiwu.android am broadcast -a app.kiwu.android.REPASO -n app.kiwu.android/.notify.RepasoReceiver
     ```
     Sin tarjetas vencidas no debe notificar.
  6. Toca la notificación: la app abre «Repasar hoy», no la pantalla principal.

---

## 🗄️ 2. Estado de la Base de Datos (Room Schema v13)

La versión del esquema de Room es **v13** (migraciones acumuladas 1→13 en [KiwuDao.kt](file:///Users/ksebas/Documents/kiwu-android/kiwu/app/src/main/java/app/kiwu/android/data/KiwuDao.kt)).

```sql
-- Estructura de las columnas añadidas en v9 (0.7.0+):
ALTER TABLE materia ADD COLUMN calificaciones TEXT DEFAULT NULL;
ALTER TABLE deber ADD COLUMN examen INTEGER NOT NULL DEFAULT 0;
ALTER TABLE pagina ADD COLUMN tarjetas TEXT DEFAULT NULL;

-- v10 (0.8.3): las faltas de clase viajan en el sync (borrado lógico).
ALTER TABLE falta ADD COLUMN actualizada INTEGER NOT NULL DEFAULT 0;
ALTER TABLE falta ADD COLUMN eliminada INTEGER;

-- v11 (0.8.3): sesiones de estudio portátiles (tabla nueva).
CREATE TABLE IF NOT EXISTS sesion (
    id TEXT NOT NULL PRIMARY KEY,
    materia_id TEXT,
    dia INTEGER NOT NULL,
    minutos INTEGER NOT NULL,
    creada INTEGER NOT NULL,
    actualizada INTEGER NOT NULL,
    eliminada INTEGER
);
CREATE INDEX IF NOT EXISTS index_sesion_dia ON sesion (dia);

-- v12 (0.8.3): color propio de cada lista (null = automático por hash).
ALTER TABLE materia ADD COLUMN color TEXT;

-- v13: portada opcional de cuaderno.
ALTER TABLE cuaderno ADD COLUMN cover_image_path TEXT;
ALTER TABLE cuaderno ADD COLUMN cover_color_hex TEXT;
```

> [!IMPORTANT]
> **Compatibilidad de Copias de Seguridad (.kiwu.json):**  
> El formato de exportación `.kiwu.json` en [Copia.kt](file:///Users/ksebas/Documents/kiwu-android/kiwu/app/src/main/java/app/kiwu/android/data/Copia.kt) incluye los campos de calificaciones, exámenes y tarjetas de repaso, el color de cada lista y las secciones nuevas `faltas` y `sesiones`. Las versiones anteriores (y el escritorio macOS/Linux) ignoran estos campos al importar sin romper la aplicación.

---

## 🏗️ 3. Arquitectura y Módulos Clave

```
app/src/main/java/app/kiwu/android/
├── KiwuApp.kt                    # Service Locator (Db, Repo, Ajustes, ServicioSync, Billing)
├── MainActivity.kt               # Entrypoint, Edge-to-Edge, Handlers de Intents
├── data/
│   ├── KiwuDao.kt                # Room DAO & Migraciones 1->13 (KiwuDb, singleton manual)
│   ├── Repositorio.kt            # Fachada principal de datos para ViewModels
│   ├── CalendarioTelefono.kt     # Lógica de consulta a CalendarContract
│   ├── Firebase.kt / SyncLocal.kt# Motor de sincronización (REST propio, sin SDK; tablas materia, grupo_lista, horario, falta, sesion, cuaderno, deber, clase, pagina)
│   ├── BillingManager.kt         # Compras en la app (Play Billing v8)
│   ├── Copia.kt                  # Export/Import .kiwu.json
│   └── Drive.kt / TareasGoogle.kt# Respaldo en Drive y tareas de Google
├── domain/                       # ~19 archivos: Entidades, Documento, Estudio, Horario, Racha, MotorSync...
├── ui/
│   ├── Raiz.kt                   # NavHost principal
│   ├── Tema.kt                   # Material 3 + colores dinámicos (Material You) + tema noche
│   ├── AdBanner.kt               # Banner AdMob
│   ├── onboarding/OnboardingScreen.kt  # Onboarding (~1.700 líneas, candidato a dividir)
│   ├── calendario/CalendarioScreen.kt  # Grilla mensual + agenda diaria + Google Calendar
│   ├── editor/                   # Editor rich-text por bloques (/tarjeta, barra de formato, adjuntos)
│   ├── estudio/EstudioScreens.kt # Pomodoro, Calificaciones, Tarjetas de Repaso
│   ├── tareas/                   # Lista/tablero Kanban, Racha, menús de listas, ViewModels
│   └── horario/                  # Horario de clases + faltas
├── notify/
│   ├── Avisos.kt                 # Canales y avisos de tareas
│   ├── Temporizador.kt           # Servicio/Notificación de Pomodoro
│   └── AvisosRacha.kt            # Aviso diario de racha en riesgo
└── widgets/
    ├── Widgets.kt                # Receptores Glance de los 4 widgets
    ├── DatosWidgets.kt           # Datos y actualización de widgets
    └── ConfiguracionWidgetsActivity.kt / activities flotantes
```

### CI/CD
- `.github/workflows/android.yml`: en cada push/PR a `main` → `testDebugUnitTest lintDebug assembleDebug` + artefacto APK.
- `.github/workflows/android-release.yml`: al tag `android-v*` → tests + lint + `assembleMinificado` (firmado con clave debug) + release de GitHub con notas de `docs/versiones/android-X.Y.Z.md` y SHA256SUMS.

---

## 🧪 4. Guía de Verificación y Compilación

Para asegurar la integridad del proyecto y evitar regresiones:

```bash
# Compilar el APK de depuración
./gradlew app:assembleDebug

# Ejecutar el conjunto de unit tests (JVM/Robolectric: 39 archivos de test, 322 tests)
./gradlew app:testDebugUnitTest

# Verificar lint de i18n y traducciones
./gradlew app:lintDebug

# Los tres juntos (lo mínimo antes de terminar una tanda)
./gradlew app:assembleDebug app:testDebugUnitTest app:lintDebug
```

> [!WARNING]
> **El build necesita JDK 21.** Con Java 25 como JDK por defecto del sistema, Gradle 8.9 falla con un error críptico (`What went wrong: 25.0.2`). Antes de compilar, exporta un JDK 21 (p. ej. el JBR de Android Studio):
> ```bash
> export JAVA_HOME="/Users/ksebas/Library/Java/JavaVirtualMachines/jbr-21.0.11/Contents/Home"
> ```
> (o fija `org.gradle.java.home` en `gradle.properties` para no depender del entorno).

> [!NOTE]
> **Checklist para Agentes de OpenCode:**  
> 1. Al agregar nuevas cadenas de texto en `values/strings.xml`, es **obligatorio** incluirlas en `values-pt/strings.xml` y `values-en/strings.xml` para evitar fallos de Lint (`MissingTranslation`). Los tres idiomas van emparejados a **757 entradas** (strings, plurals y arrays; español base, inglés y portugués).
> 2. En `values-pt/strings.xml`, las etiquetas `<plurals>` deben incluir siempre los tres casos: `one`, `many`, `other`.
> 3. No utilizar frameworks de Inyección de Dependencias (Hilt/Koin); todas las dependencias se obtienen del singleton `KiwuApp`.
> 4. La terminología en portugués: *racha* = **sequência**, *Ajustes* = **Configurações**, *cuaderno* = **caderno**. Revisa las claves existentes antes de introducir sinónimos nuevos.
> 5. El lint trata `MissingTranslation`, `ExtraTranslation`, `MissingQuantity` y `StringFormatMatches` como error: un desajuste de formato (`%1$s`) entre idiomas frena el build.
> 6. Versiones y dependencias se gestionan en `gradle/libs.versions.toml`; las claves de Firebase/AdMob salen de `local.properties` (no se sube al repo).

### Plan de Prueba Manual de las Novedades (0.8.3 en preparación)

Recorrido de ~10 minutos en un emulador o dispositivo con el APK `debug`. Hazlo también con el idioma en **inglés** y **portugués** (Ajustes → Idioma) para validar las traducciones de las pantallas nuevas.

1. **SRS de tarjetas** — Abre una nota con tarjetas → «Tarjetas de repaso»:
   - «Repasar hoy (N)» aparece solo si hay tarjetas pendientes (nuevas o vencidas); si no, se lee «¡Todo al día!».
   - «La sé» saca la tarjeta de la cola de hoy; reabre la pantalla y ya no cuenta en «Repasar hoy».
   - «Repasar otra vez» la devuelve al final de la misma sesión.
2. **Swipe en tareas** — Lista de tareas y agenda del calendario:
   - Derecha sobre una pendiente → completada; otra vez a la derecha → reabierta (la fila siempre vuelve).
   - Izquierda sobre una pendiente → «Mañana» y el chip de fecha pasa a mañana con la misma hora; sin fecha → mañana 8:00.
   - Eventos y tareas repetidas: el gesto de la izquierda no se activa.
3. **Tema Negro** — Ajustes → Apariencia → Negro: fondo negro puro en toda la app, acento intacto; prueba también con «color del sistema». Vuelve a Sistema u Oscuro.
4. **Resumen semanal** — Ajustes → Avisos:
   - El interruptor «Resumen semanal» está activado por defecto; al apagarlo la línea de resumen dice «sin resumen semanal».
   - Completa una tarea y corre una sesión Pomodoro; dispara el receiver para ver el aviso ya (ver comando en 1.12):
     ```bash
     adb shell run-as app.kiwu.android am broadcast -a app.kiwu.android.RESUMEN -n app.kiwu.android/.notify.ResumenReceiver
     ```
     (El receiver es `exported=false`: en builds debug el comando se lanza como la propia app con `run-as`; un `adb shell am broadcast` a secas lo deniega el sistema.)
   - Con la semana vacía el mismo comando no notifica nada.
5. **Regresión general** — Racha, aviso de la racha (20:00), widgets, nota rápida del widget y respaldo `.kiwu.json` (exportar → importar) funcionan como antes.
6. **Swipe en la hoja de la clase** — Horario → pulsa una clase → su hoja de tareas: izquierda sobre una pendiente → «Mañana» (chip de fecha actualizado); derecha → completada/reabierta. Eventos y repetidas: el gesto de la izquierda no se activa.
7. **Sync de faltas** — Dos dispositivos con la misma cuenta: marca una falta en el móvil → aparece en la tablet al sincronizar; justifícala allá → llega al móvil; quítala → desaparece en ambos (borrado lógico viaja).
8. **Sesiones portátiles** — Actualiza desde la versión anterior con sesiones hechas: los minutos viejos se importan una sola vez y aparecen en la «semana» de Estudio. Estudia en un dispositivo → el otro lo refleja al sincronizar. El respaldo `.kiwu.json` lleva la sección `"sesiones"` y el import la cuenta en su resumen.
9. **Color de lista** — Menú de Tareas → «Nueva lista» → elige color → la fila muestra el punto. Edita la lista → cambia el color → sus clases del horario (día, semana y widget) cambian a la vez. Una clase con color propio sigue mandando. «Automático» vuelve al tono por hash. El respaldo lleva `"color"` y se conserva al importar.
10. **Compartir PDF** — Abre una nota con títulos, listas, una tarea marcada, una cita, código y una imagen → ⋮ → «Compartir PDF» → guárdalo en Archivos/Drive y ábrelo: estructura respetada, texto seleccionable, pie con título y número de página, y las hojas cortan entre líneas, no por la mitad. Una nota larga genera varias hojas; una vacía, un PDF válido (`nota.pdf`).
11. **Repasar hoy (SRS unificado)** — Ten dos notas con tarjetas vencidas en cuadernos distintos → Notas → icono de repaso: salen las dos con su cuenta. Contesta una → vuelve a entrar: la acertada ya no cuenta. Crea un examen de «Cálculo» a 2 días y una nota en un cuaderno llamado «Cálculo» → esa nota sale primero con la etiqueta «Examen … · 2 d». Lanza el aviso a mano y toca la notificación: abre «Repasar hoy» directamente.
    ```bash
    adb shell run-as app.kiwu.android am broadcast -a app.kiwu.android.REPASO -n app.kiwu.android/.notify.RepasoReceiver
    ```

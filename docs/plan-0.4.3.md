# Plan de Kiwu para Android 0.4.3 · faltas de clase e idiomas

Estado: **implementado** (24 de octubre de 2026). Se entrega en la **0.4.3**, después de la 0.4.1 ([plan-0.4.1.md](plan-0.4.1.md) y [plan-widgets-android.md](plan-widgets-android.md)). Notas de la versión: [versiones/android-0.4.3.md](versiones/android-0.4.3.md).

Dos bloques independientes, que se hacen en este orden:

1. **Faltas de clase:** marcar inasistencias desde el horario, con un máximo por materia y un contador en cada clase que dice cuántas quedan.
2. **Idiomas:** **español, inglés y portugués, los tres completos** en esta versión. En Android 13 o superior el idioma es automático (el del teléfono, o el que se elija para Kiwu en los ajustes de Android); en Android 8 a 12 se elige a mano en Ajustes.

La base de datos pasa a la **versión 5** con una migración probada. La copia `.kiwu.json` gana las faltas como sección opcional, sin cambiar `FORMATO` (al importar, `Copia.kt` solo rechaza un `formato` mayor e ignora las secciones que no conoce).

## 1. Faltas de clase

### Concepto y reglas

- El límite va por **materia del horario**, identificada por **`clase.grupo`**: las filas de una materia en varios días (Cálculo lunes y miércoles) comparten `grupo`, y la app ya lo usa como «la materia» para editar, eliminar, vincular la lista y elegir el color (`HorarioScreen.kt`, alrededor de las líneas 198–220). Así, **renombrar la materia no separa el historial** y dos horarios con una materia del mismo nombre no se mezclan.
- Cada materia puede tener un **máximo de faltas** opcional (`null` = sin límite: se cuentan las faltas pero no hay «quedan»).
- Una falta guarda **el día** y si está **justificada**. Las justificadas se ven en la lista pero **no cuentan** para el límite.
- Estados del contador:
  - sin límite: «Faltas: 2», en neutro;
  - tranquilo: menos del 60 % del máximo (verde);
  - aviso: del 60 al 99 % (ámbar);
  - superado: el máximo o más (rojo, «riesgo de perder la materia»).
- Textos: «Faltas: 2/5», «quedan 3», «sin faltas restantes».
- **Vaciar faltas** a mano al empezar un semestre (borra las de ese horario, con confirmación). No hay borrado automático por fechas en esta versión.
- **Sin notificación** en esta versión: solo pantalla y widget. Una notificación al llegar a `máximo − 1` queda para más adelante.

### Datos y migración

- `Clase` (`domain/Horario.kt`): campo nuevo `max_faltas INTEGER NULL`. Se edita en el formulario y se guarda en **todas las filas del grupo**, como ya pasa con el color y el aviso.
- Tabla nueva `falta`:
  - `id` (UUID), `grupo` (el de la clase), `horario_id` (para «Vaciar faltas» por horario), `dia` (`LocalDate` guardado como **epochDay**, un entero: una falta es un día, no un momento, y así no depende de la zona horaria), `justificada` (booleano), `creada`.
  - **Sin clave foránea en cascada:** borrar una fila de la clase no borra el historial. Si se elimina la materia entera, sus faltas quedan huérfanas y no se muestran (se pueden limpiar al eliminar el grupo).
- `KiwuDao`: faltas como `Flow`, insertar, borrar una, borrar por horario, cambiar `justificada` y actualizar `max_faltas` por grupo.
- `Repositorio`: `faltas: Flow<List<Falta>>`, `marcarFalta(grupo, horarioId, dia)`, `quitarFalta(id)`, `justificar(id, si)`, `ponerMaximo(grupo, max)`, `vaciarFaltas(horarioId)`.
- **Migración 4 → 5**: `ALTER TABLE clase ADD COLUMN max_faltas INTEGER` + `CREATE TABLE falta`, con su prueba de Robolectric como las anteriores (`KiwuDao.kt`, `MIGRACION_3_4`).
- **Copia `.kiwu.json`:**
  - exportar `"faltas"` con `dia` como texto `AAAA-MM-DD` y `max_faltas` en cada clase;
  - al importar, opcional (las copias antiguas no lo traen), y se **descartan las faltas de un `grupo` que no exista**, como ya se hace con las listas y carpetas;
  - `FORMATO` sigue en 1;
  - documentarlo en `docs/contrato/` (sin añadirlo a `copia/ejemplo.kiwu.json` hasta comprobar que las pruebas de macOS y Linux lo toleran), y anotar que pasar una copia por macOS o Linux pierde las faltas.

### Lógica pura (`domain/ReglasFaltas.kt`, nuevo)

- `porGrupo(faltas)`: mapa `grupo → faltas`.
- `contar(faltas)`: solo las no justificadas.
- `estado(contador, maximo)`: `SIN_LIMITE / TRANQUILO / AVISO / SUPERADO`.
- `quedan(contador, maximo)`: nunca negativo.
- `tocaClase(dia, clases del grupo)`: si ese día de la semana hay clase de la materia (para avisar al marcar un día suelto, sin impedirlo).
- **Sin textos en la lógica:** devuelve números y estados, y la interfaz pone las palabras (necesario para los idiomas).

### Interfaz (según el boceto)

- **Hoja de la clase** (`ui/horario/HojaTareasClase.kt`): un tercer chip **«Faltas: N»** junto a «Pendientes (N)» y «Hechas (N)» (hoy dos `FilterChip`, alrededor de la línea 122). Con máximo dice «Faltas: 2/5» y toma el color del estado. Al tocarlo, la hoja muestra la sección de faltas en lugar de las tareas:
  - botón «Marcar falta» (hoy por defecto; con selector de fecha para otro día);
  - lista de faltas con el día, el interruptor «Justificada» y «Quitar»;
  - el contador grande con «quedan N».
- **Cada clase del horario** (`TarjetaClase` en la vista de día y la celda en `VistaSemana`): un contador pequeño de faltas con el color del estado, por ejemplo «quedan 3» o «2/5». Si la materia no tiene máximo ni faltas, no se muestra nada.
- **Formulario de clase** (`FormularioClase`): campo «Máximo de faltas (opcional)». En el menú del horario (`MenuHorario`), «Vaciar faltas de este horario» con confirmación.
- **Aviso en Horario:** si alguna materia está superada, una franja arriba (junto a `TarjetaAhora`) con las materias en riesgo.
- **Widget de Horario:** en «Clases de hoy», el mismo contador o un punto rojo si está superado. `DatosWidgets` recibe las faltas y `DatosWidgetsTest` cubre el caso.

### Pruebas

- `ReglasFaltasTest`: agrupar por `grupo`, justificadas fuera, límite nulo, estados y umbrales (59 %, 60 %, 99 %, 100 %), «quedan» sin negativos, día con o sin clase.
- Migración 4 → 5 con Robolectric.
- Copia: exportar e importar faltas, copia antigua sin faltas, falta de un grupo inexistente.
- `DatosWidgetsTest`: faltas en el widget de Horario.
- Emulador: marcar y quitar faltas, justificar, ver el chip y la tarjeta cambiar de color, llegar al máximo, renombrar la materia (el historial se mantiene), vaciar el horario, comprobar el widget.

### Limitaciones conocidas

- Cambiar el horario a mitad de semestre no borra faltas: se vacían a mano.
- Las faltas son solo de Android por ahora; macOS y Linux las ignoran.

## 2. Idiomas

### Situación actual

- Casi todos los textos están **escritos en español dentro del código**: unos **220 `Text("…")` en 28 archivos**, más descripciones de iconos, mensajes, títulos, notificaciones y resúmenes. `strings.xml` solo tiene 8 cadenas (las de los widgets).
- Hay **texto dentro de la lógica pura**, que no tiene acceso a los recursos de Android: `Rachas.texto` («1 día / N días»), `ResumenAjustes`, los días de `ReglasHorario` (`domain/Horario.kt`) y fechas formateadas a mano («sáb 24 oct 2026»).
- `MainActivity` y `ConfiguracionWidgetsActivity` son `ComponentActivity`, no `AppCompatActivity`.
- Contenido que crea la app: la lista fija `Eventos` (id `eventos`), la lista «Empieza aquí» de la guía y el cuaderno «Notas rápidas».

### Cómo se elige el idioma

- **Idiomas:** español (base, `values/`), inglés (`values-en/`) y portugués (`values-pt/`), **los tres completos** en la 0.4.3.
- `res/xml/locales_config.xml` con `es`, `en` y `pt`, y `android:localeConfig` en el manifiesto: así Android 13+ ofrece Kiwu en su ajuste de «Idioma de la aplicación».
- **Android 13 o superior (automático):**
  - por defecto, el idioma del teléfono, gracias a los recursos;
  - en Ajustes, la fila **«Idioma»** muestra el idioma actual y **abre el ajuste de Android** para Kiwu (`Settings.ACTION_APP_LOCALE_SETTINGS`). El cambio lo aplica Android con `LocaleManager`, sin código propio ni `appcompat`.
- **Android 8 a 12 (manual):**
  - en Ajustes, selector **«Sistema / Español / English / Português»**, guardado en `SharedPreferences` (archivo `kiwu`, clave `idioma`), con «Sistema» por defecto. Se usa `SharedPreferences` y no DataStore porque `attachBaseContext` necesita leerlo de forma síncrona antes de crear la actividad;
  - se aplica envolviendo el contexto con la configuración del idioma elegido: `attachBaseContext` en `MainActivity` y `ConfiguracionWidgetsActivity`, y un contexto localizado para widgets, notificaciones y avisos. Al cambiarlo se llama a `recreate()`;
  - **sin `appcompat`:** `AppCompatDelegate.setApplicationLocales` no cambia el idioma en Android 12 o anterior con `ComponentActivity`, y pasar a `AppCompatActivity` obligaría a cambiar el tema base.
- Las dos rutas comparten una función `Idioma.contexto(base)` en un solo sitio, y en Android 13+ el valor guardado se ignora.

### Textos, días y fechas

- **Días y fechas:** retirar los arreglos en español de `ReglasHorario` y usar `DayOfWeek.getDisplayName(TextStyle, locale)` y `DateTimeFormatter.ofLocalizedDate(...)` / `ofPattern` con el locale; `hora()` sigue igual (HH:mm).
- **Plurales:** `<plurals>` para «N tareas», «N días», «N faltas», «quedan N».
- **Lógica pura sin textos:** `Rachas.texto`, `ResumenAjustes` y similares pasan a devolver números o claves, y la interfaz los convierte con `stringResource` / `pluralStringResource`.
- **Contenido del sistema:**
  - lo que crea la persona se queda en el idioma en que lo escribió;
  - `Eventos` (id fijo) toma su nombre de los recursos al mostrarse, sin migrar datos;
  - la lista «Empieza aquí» y sus tareas (`domain/Guia.kt`) y el cuaderno «Notas rápidas» se crean con los recursos del idioma del momento.

### Extracción por fases

Cada fase termina compilando y con las pruebas en verde:

1. **Infraestructura:** `locales_config.xml`, `Idioma.contexto`, preferencia y fila de Ajustes, días y fechas por locale, `strings.xml` base.
2. **Tareas:** menú, Hoy / Próximos / Todas / Completadas, calendario, tablero, formulario, detalle, racha y celebración.
3. **Notas:** lista, cuadernos, búsqueda, editor, menú `/`, barra de formato y diálogos.
4. **Horario:** día, semana, formulario, «Ahora / Siguiente», hoja de tareas y faltas.
5. **Ajustes, guía, avisos y notificaciones** (incluida la del aviso de la racha).
6. **Widgets y su configuración.**
7. **Traducciones:** completar `values-en/` y `values-pt/`, revisar textos largos en botones y chips (el portugués y el inglés ocupan distinto) y activar el lint de traducciones.

### Pruebas

- Lint `MissingTranslation` y `ExtraTranslation` en verde.
- Pruebas JVM de días, fechas y plurales en `es`, `en` y `pt`.
- Las pruebas que hoy comparan textos en español (`RachaTest.textos`, `ResumenAjustesTest`, pruebas de widgets) pasan a comparar números o estados, o a usar un locale fijo.
- Emulador con **Android 13+**: cambiar el idioma del teléfono y el de Kiwu desde la fila de Ajustes, y ver la app, los widgets y las notificaciones.
- Emulador con **Android 12 o anterior**: elegir cada idioma en el selector, volver a «Sistema», girar la pantalla y reiniciar la app; comprobar widgets y notificaciones.

## 3. Entrega

- Las dos funciones van en la **0.4.3**: base de datos en la **versión 5**, `versionCode` **7** en `main` y **12** en la rama de Google Play (el 10 y el 11 ya se usaron).
- Orden: primero las faltas (más pequeñas y con la migración) y después los idiomas. Si los idiomas se alargan, se puede publicar antes una **0.4.2 solo con faltas**.
- Documentos al cerrar: `versiones/android-0.4.3.md` (nuevo), `android-plan.md` (fases K y L, pruebas y registro), `android/README.md`, `docs/contrato/` (faltas en la copia) y, en `kiwu-play`, la ficha de Play en inglés y portugués (`docs/play-store.md`).
- Nada se commitea hasta terminar y probar.

## 4. Decisiones

Tomadas por el usuario:

- **Idiomas:** español, inglés y portugués, los tres listos en la 0.4.3.
- **Android 13+:** automático; la fila «Idioma» de Ajustes abre el ajuste de idioma de Android para Kiwu.
- **Android 8–12:** selector manual en Ajustes.
- **Faltas en el horario:** chip «Faltas: N» en la hoja de la clase junto a Pendientes y Hechas, y un contador de las faltas que quedan en cada clase del horario.

Propuestas por defecto (cambiarlas aquí si se decide otra cosa):

- Las faltas **justificadas no cuentan** para el límite.
- **Sin notificación** al acercarse al límite en esta versión.
- El límite es por **materia del horario** (`grupo`), no por lista de tareas.
- **Semestre:** «Vaciar faltas» a mano, sin fecha de inicio.

## 5. Archivos que se tocarán

- Datos: `domain/Horario.kt` (`max_faltas`, entidad `Falta`), `data/KiwuDao.kt` (tabla, consultas, migración), `data/Repositorio.kt`, `data/Copia.kt`, `data/AjustesStore.kt` (`idioma`).
- Lógica: `domain/ReglasFaltas.kt` (nuevo), `domain/Horario.kt` (días sin textos), `domain/Racha.kt` y `ui/ajustes/ResumenAjustes.kt` (sin textos).
- Interfaz: `ui/horario/HorarioScreen.kt` (`TarjetaClase`, `VistaSemana`, `FormularioClase`, `MenuHorario`), `ui/horario/HojaTareasClase.kt`, `ui/horario/HorarioViewModel.kt`, `ui/ajustes/AjustesScreen.kt` (fila «Idioma»), y todas las pantallas para extraer textos.
- Idioma: `Idioma.kt` (nuevo), `MainActivity.kt`, `widgets/ConfiguracionWidgetsActivity.kt`, `KiwuApp.kt`, `AndroidManifest.xml`, `res/xml/locales_config.xml`, `res/values/strings.xml`, `res/values-en/`, `res/values-pt/`.
- Widgets: `widgets/DatosWidgets.kt`, `widgets/WidgetHorario.kt` y el resto para los textos.
- Pruebas: `ReglasFaltasTest.kt` (nuevo), migración 4 → 5, copia, `DatosWidgetsTest.kt`, días y fechas por locale, y las pruebas que comparan textos.

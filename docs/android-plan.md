# Kiwu para Android: ruta de planeamiento

Estado: **versión 0.1.0 funcional, con las fases A–I cerradas salvo lo marcado como pendiente** · Carpeta: [`android/`](../android) (dentro del mismo repositorio, como `macos/` y `linux/`).

**Por qué en este repositorio y no en otra carpeta:** comparte con macOS y Linux las fixtures de [`docs/contrato/`](contrato/README.md) (la prueba de que los tres leen igual las notas), el CI y la documentación. Cada plataforma sigue siendo independiente: su propio código, pruebas y compilación.

## 1. Alcance: paridad con escritorio, sin widgets

| Módulo de escritorio | En Android |
| --- | --- |
| Tareas: listas por materia, fecha y hora, recordatorio | Igual |
| Vistas Hoy / Próximos 7 días / Todas / Completadas | Igual (menú lateral) |
| Vista de lista y de **tablero** (Por hacer · En curso · Hecho) | Igual; en el tablero la tarea se mueve con su menú (se puede arrastrar entre columnas) |
| Calendario mensual con tareas en pastel | Igual |
| Notas: cuadernos, páginas con emoji, favoritas, búsqueda | Igual |
| Editor por bloques con menú `/`, 13 tipos de bloque | Igual |
| Formato (negrita, cursiva, subrayado, tachado, código, color, resaltado, enlace, sangría) | Igual, con barra de formato sobre el teclado |
| Bloques de código con colores y detección de lenguaje | Resaltador propio (colores Dark+/Light+) |
| Enlaces con vista previa web y vídeo (YouTube/Vimeo) | Igual (tarjeta; el vídeo se abre en un reproductor incrustado) |
| Imágenes y archivos adjuntos (PDF, Word) como referencia | Selector del sistema (SAF); el archivo no se copia |
| Archivos por materia (carpeta vinculada) | Carpeta elegida con el selector del sistema |
| Recordatorios | Notificaciones con acciones «Completar» y «Posponer» |
| Ajustes: tema, fondo de imagen translúcido | Igual |
| Ajustes: **atajos de teclado editables** | No aplica en móvil (se omite) |
| Copia de seguridad `.kiwu.json` (importar / exportar) | Igual, mismo formato que Linux |
| Widgets | **Fuera de alcance** (decisión del proyecto) |

Fuera de alcance de esta ruta: cuenta y sincronización (plan en [plan-desarrollo.md](plan-desarrollo.md) §4), pero el modelo de datos ya nace preparado (UUID, borrado lógico, marcas de tiempo).

## 2. Decisiones técnicas

| Tema | Decisión | Motivo |
| --- | --- | --- |
| Lenguaje / UI | Kotlin + Jetpack Compose + Material 3 | Estándar actual; encaja con una sola persona |
| Datos | Room (SQLite) | Mismo modelo que Linux v2: `materia`, `deber`, `cuaderno`, `pagina` con UUID y `eliminada` |
| Notas | JSON de bloques idéntico al de macOS/Linux (kotlinx.serialization, conservando campos desconocidos) | Es el contrato multiplataforma |
| Lógica de negocio | Kotlin en `domain/` (solo los modelos llevan anotaciones de Room) | Se prueba en JVM sin emulador y se porta casi línea a línea desde `servicios.py` |
| Recordatorios | `AlarmManager` exacto + `BroadcastReceiver`; se reprograman al arrancar el teléfono | Los avisos deben salir a la hora |
| Ajustes | DataStore (Preferences) | |
| Carpetas y adjuntos | Storage Access Framework con permisos persistentes | Sin permisos amplios de almacenamiento |
| Mínimo / destino | minSdk 26 (Android 8), compile/target 35 | Cubre >95 % de los teléfonos |
| Idioma | Español (cadenas en `strings.xml`, listas para traducir a inglés) | Evita repetir el problema de las cadenas dentro del código |
| Identificador | `app.kiwu.android` provisional | **Hay que fijarlo antes de publicar**: no se puede cambiar en Play (ver plan §5) |

Versiones fijadas (conjunto probado y compatible): Gradle 8.9, AGP 8.7.3, Kotlin 2.0.21, KSP 2.0.21-1.0.28, Compose BOM 2024.12.01, Room 2.6.1, JDK 17.

## 3. Estructura

```
android/
  app/src/main/java/app/kiwu/android/
    domain/    Modelos, Documento (JSON de notas), servicios y reglas (Kotlin puro)
    data/      Room: entidades, DAO, base de datos, repositorio, copia .kiwu.json
    notify/    Recordatorios y notificaciones
    ui/        Compose: tareas, calendario, notas, editor, ajustes
  app/src/test/   Pruebas JVM (lógica, contrato, copia de seguridad)
  gradle/         Catálogo de versiones
docs/contrato/    Fixtures compartidas con macOS y Linux
```

## 4. Fases

Cada fase termina con: compila, pruebas verdes y casillas marcadas aquí.

### Fase A: Esqueleto y entorno
- [x] Toolchain en `C:\Users\kevin\android-dev` (JDK 17, Gradle 8.9, SDK 35 y un emulador Android 15), fuera del repositorio
- [x] Proyecto Gradle (`android/`), catálogo de versiones, `.gitignore`
- [x] App mínima que compila (`assembleDebug`) y pasa una prueba JVM

### Fase B: Dominio y contrato
- [x] `Documento` / `Bloque` / `TextRun`: lectura tolerante, escritura, campos desconocidos conservados
- [x] Pasa las fixtures de `docs/contrato/notas` con el mismo resultado esperado que Python y Swift
- [x] Reglas de tareas (`en_alcance`, pendientes, completadas, conteo, columnas, mover, marcar) con las mismas pruebas que `test_logica.py`
- [x] Recordatorios (`pendientes_de_aviso`, `texto_aviso`), detección de lenguaje, análisis de enlaces

### Fase C: Datos
- [x] Room con las 4 entidades, borrado lógico en cascada y consultas que ignoran lo eliminado
- [x] Repositorio con `Flow` para refrescar la interfaz
- [x] Copia `.kiwu.json`: exportar e importar (compatible con Linux, no resucita lo eliminado)
- [x] Pruebas de Room con Robolectric

### Fase D: Tareas
- [x] Navegación: menú lateral (Hoy, Próximos 7 días, Calendario, Todas, Completadas, listas) y cambio Tareas ↔ Notas
- [x] Vista de lista con secciones y colores de vencimiento
- [x] Vista de tablero con tres columnas (la tarea se mueve con su menú; **se puede arrastrar entre columnas**)
- [x] Crear tarea, detalle (título, lista, fecha y hora, recordatorio, nota por bloques), completar, eliminar
- [x] Gestión de listas: crear, renombrar, eliminar

### Fase E: Recordatorios
- [x] Permiso de notificaciones (Android 13+) y de alarmas exactas (Android 12+)
- [x] Programar / cancelar al crear, editar, completar y eliminar
- [x] Reprogramar al reiniciar y tras actualizar la app (implementado; no se probó reiniciando el emulador)
- [x] Acciones de la notificación: Completar (probada) y Posponer (implementada, sin probar)

### Fase F: Calendario
- [x] Mes completo con tareas en pastel, navegación por mes, «Hoy»
- [x] Día seleccionado con su lista y creación rápida de tarea en ese día

### Fase G: Notas
- [x] Cuadernos y páginas (emoji, favoritas), lista y búsqueda por texto
- [x] Editor por bloques: los 13 tipos, menú `/`, Intro/Retroceso con las reglas de escritorio, sangría
- [x] Formato en línea con barra sobre el teclado (negrita, cursiva, subrayado, tachado, código, color, resaltado, enlace)
- [x] Bloque de código con lenguaje y colores
- [x] Enlaces con vista previa y vídeo; imágenes y archivos adjuntos (SAF) (implementados; **sin probar en dispositivo**: dependen de la red y de los selectores del sistema)
- [x] Guardado automático

### Fase H: Archivos, ajustes y pulido
- [x] Carpeta vinculada por lista con explorador integrado (implementado; solo se vio la pantalla inicial, falta probarlo con una carpeta real)
- [x] Ajustes: tema, fondo de imagen y translucidez (el tema se probó; el fondo de imagen está implementado pero no se vio con una imagen real)
- [x] Iconos, tema Material con la paleta de Kiwu (coral `E8705A`), icono adaptativo
- [ ] Accesibilidad (TalkBack, tamaños de toque, contraste): los iconos llevan descripción, pero **no se probó con TalkBack**

### Fase I: Calidad y entrega
- [x] CI de Android (compilar + pruebas + lint) en GitHub Actions (escrito; **aún no se ejecutó en GitHub**)
- [x] README de Android y actualización del README principal
- [x] APK de depuración (firmado con la clave de depuración; la firma de publicación queda para Play)
- [ ] Lista previa a Play: identificador definitivo, política de privacidad, ficha (depende de las decisiones de marca; ver plan §5)

## 5. Cómo se verificó

Hay un emulador (Android 15, con aceleración WHPX) en este equipo, así que se probó la app real además de las pruebas automáticas.

| Verificado | Cómo |
| --- | --- |
| 69 pruebas automáticas (dominio, contrato con macOS y Linux, copia `.kiwu.json`, Room, editor, resaltado) | `./gradlew testDebugUnitTest` |
| Lint, compilación de depuración y de release con R8 | `./gradlew lintDebug assembleRelease` |
| Crear listas y tareas, menú lateral, tablero, calendario, tema oscuro, Ajustes | A mano en el emulador |
| Editor: escribir, Intro que parte el bloque, menú `/`, bloque de código, barra de formato, guardado | A mano en el emulador |
| Notas: cuaderno automático, página, listado | A mano en el emulador |
| Recordatorio: permiso, notificación, acción *Completar* | A mano en el emulador |
| La versión minificada (R8) crea listas y tareas y guarda notas sin fallos | Variante `minificado` en el emulador |

**Sin probar todavía:** imágenes y archivos adjuntos, vista previa de enlaces y vídeo (red), explorador de carpetas con una carpeta real, fondo de imagen, *Posponer*, reprogramación tras reiniciar, TalkBack, y el CI en GitHub.

## 6. Registro de avance

**2026-10-13.** Fases A–I completadas en una sola sesión de desarrollo.

- Fallos encontrados y corregidos al probar en el emulador: margen de la barra de estado duplicado, campo de «añadir tarea» separado del teclado, textos negros sobre fondo oscuro (los `Scaffold` transparentes no definían color de contenido), celdas del calendario que no llenaban su fila, iconos de la barra de estado que no seguían el tema de la app, y un recordatorio que se daba por «avisado» sin haberse mostrado (sin permiso de notificaciones).
- El menú `/` y la búsqueda de notas no distinguen mayúsculas ni tildes (en Linux y macOS «cod» no encuentra «Código»; conviene alinearlo).
- Siguiente: probar lo pendiente en un teléfono real, ejecutar el CI y empezar la fase de cuenta y sincronización (plan §4).

**2026-10-13 (tarde).** Primera prueba en un teléfono real y ronda de correcciones: ver [android-ruta-correccion.md](android-ruta-correccion.md). Se añadieron el arrastre en el tablero, el color de la app a elección, el arranque sin parpadeo y la protección del editor al mantener «borrar». Quedan verificadas en un teléfono la carpeta vinculada y el fondo de imagen.

**2026-10-13 (noche).** Horarios de clases (ver [plan-horario.md](plan-horario.md)): tercera pestaña «Horario» con varios horarios (Universidad, Trabajo, Ocio…), vista por día y por semana, formulario de clases en varios días, aviso de choques, «Ahora / Siguiente» y avisos opcionales antes de cada clase. La base de datos pasa a la versión 2 con migración probada. Comprobado en un emulador; los avisos y la vista de semana en tabletas faltan por probar en dispositivo.

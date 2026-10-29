# Kiwu para Android · novedades de la 0.5.4 a la 0.7.0
Documento para alimentar el prompt del gestor digital de videos de TikTok. Todo lo que aparece aquí está hecho en el código y probado con las pruebas automáticas; **ninguna de estas versiones está publicada todavía** salvo que se indique lo contrario (ver «Estado»). No hay fechas de lanzamiento ni enlaces de descarga confirmados: no inventarlos.

---

## 1. Qué es Kiwu (contexto fijo para cualquier video)

- **App para estudiantes** que junta en un solo sitio: **tareas** (con fechas y avisos), **notas** (cuadernos con títulos, listas, código e imágenes), **horario de clases** y una **racha «al día»** que premia estar al corriente.
- **Widgets en el escritorio de Android:** Deberes, Horario, Notas rápidas y Calendario.
- **Idiomas de la app:** español, inglés y portugués.
- Funciona **sin cuenta**: todo queda en el teléfono. La cuenta (opcional) sincroniza el texto de listas, tareas y notas entre dispositivos; hay copia de seguridad manual (archivo `.kiwu.json`) y en Google Drive.
- **Modelo:** gratis. La versión de Google Play muestra un banner pequeño de anuncios en las listas de Tareas y Notas, y hay una compra única para quitarlos.
- También existen versiones para macOS y Linux, pero **todo lo de este documento es de Android**.
- Tono sugerido de marca: cercano, de estudiante a estudiante, sin presión («estar al día» sin ansiedad). Evitar promesas de «mejores notas» o «aprobar»: Kiwu organiza, no estudia por ti.

## 2. Resumen de un vistazo

| Versión | Tema | Qué trae (en corto) | Cambia la base de datos |
|---|---|---|---|
| 0.5.4 | Racha estable, eventos y fechas | La racha no retrocede, eventos desde el widget (azul pastel), fechas completas | No |
| 0.5.5 | Google Calendar en el widget | Ver tus eventos de Google Calendar en el widget de calendario | No |
| 0.5.6 | Papelera y búsqueda | Papelera con restaurar, búsqueda global, «El lunes» como fecha rápida | No |
| 0.6.0 | Hábitos | Tareas que se repiten (todos los días, L–V, fines de semana) | Sí (v6) |
| 0.6.1 | Organización | Pasos (subtareas), prioridad y etiquetas | Sí (v7) |
| 0.7.0 | Estudio | Temporizador, calificaciones con promedio, cuenta atrás de exámenes, tarjetas de repaso | Sí (v9) |

Orden recomendado para una serie de videos: **0.6.0 (hábitos)** y **0.7.0 (herramientas de estudio)** son las que más se prestan a contenido visual; 0.5.5 y 0.6.1 son buenas para «tips»; 0.5.4 y 0.5.6 para videos cortos de «mejoras».

---

## 3. Detalle por versión

### 3.1 Versión 0.5.4 · «Racha estable, eventos desde el widget y fechas completas»

**Datos:** versionName 0.5.4 · AAB de Google Play generado con código 17 (en la rama de Play) · sin migración de base de datos.
**En una frase:** arreglos que la gente notaba (la racha) y mejoras de claridad (fechas y eventos).

**Qué hace**
- **La racha ya no baja por tocar tareas viejas.** Antes, desmarcar una tarea antigua podía romper todos los días desde su fecha y dejar la racha en 0. Ahora los días que ya contaron como cumplidos **se guardan en el teléfono** y no se pierden. La racha solo baja si desmarcas una tarea **de hoy**.
- **La racha no «pierde» ayer.** Completar después de medianoche una tarea que vencía ayer ya no cuenta ayer como día roto (antes pasaba de 2 días a 1).
- **Eventos desde el widget de Calendario:** al tocar un día se abre una hoja con un botón **«Evento»**; con él activado lo que escribes se crea como evento, no como tarea. Los eventos van siempre en **azul claro pastel (estilo acuarela suave)** en el widget, en la hoja del día y en la lista.
- **Fechas completas en Deberes:** «jueves 19 de septiembre» (con el año solo si no es el actual) en la lista de tareas, el selector de fecha y las tareas de una clase. Hoy / Mañana / Ayer se quedan como siempre.
- **Widget de Deberes:** cada tarea muestra día y mes abreviado («Vie 2 oct») y las pasadas «Vencida · 28 sep».

**Cómo se usa (para demostrarlo en video)**
1. Racha: marca tareas, desmarca una vieja → la racha no se mueve.
2. Widget de calendario → tocar un día → activar «Evento» → escribir → verlo en azul pastel.
3. Crear una tarea con fecha → ver «jueves 19 de septiembre» en la lista.

**Gancho de video:** «Desmarqué una tarea vieja y mi racha NO se rompió» / «Mi widget ahora se ve como acuarela».
**No prometer:** que la racha «nunca» baja (sí baja si desmarcas algo de hoy); que una racha ya rota antes de esta versión se recupera (no se recupera); que los días guardados de la racha se sincronizan entre teléfonos (viven solo en el teléfono).

### 3.2 Versión 0.5.5 · «Google Calendar en el widget»

**Datos:** versionName 0.5.5 · sin migración · permiso nuevo `READ_CALENDAR`.
**En una frase:** tus eventos de Google Calendar aparecen en el widget de calendario de Kiwu, junto a tus tareas.

**Qué hace**
- En **Ajustes → Copia de seguridad** hay un interruptor **«Calendario de Google»**. Al activarlo, Android pide el permiso de calendario.
- Los eventos de los calendarios que ya tienes sincronizados en el teléfono salen en el widget en el mismo **azul pastel** de los eventos de Kiwu, y también en la hoja del día al tocar una casilla.
- **Solo lectura:** Kiwu no crea ni cambia nada en tu Google Calendar; para editar un evento se abre en Google Calendar.
- No hay un inicio de sesión aparte: usa lo que el teléfono ya sincroniza (la cuenta de Google añadida al teléfono con Calendario activado).
- El widget se vuelve a leer al cambiar de mes, al cambiar el interruptor y cuando cambian los datos de Kiwu.

**Cómo se usa:** Ajustes → Copia de seguridad → «Calendario de Google» → aceptar permiso → mirar el widget del escritorio.

**Gancho de video:** «Mi horario, mis tareas y mi Google Calendar en un solo widget».
**No prometer:** edición de eventos de Google desde Kiwu; que un evento nuevo aparezca al instante (puede tardar hasta la siguiente actualización del widget); eventos de varios días (solo salen en su primer día).
**⚠ Interno, no anunciar:** en el código existe además un interruptor «Tareas de Google» (Google Tasks) que se dejó **para más adelante** porque exige activar la API de Google Tasks y pasar la verificación de Google. **No mencionarlo en videos** hasta que se confirme.

### 3.3 Versión 0.5.6 · «Papelera, búsqueda y El lunes»

**Datos:** versionName 0.5.6 · sin migración.
**En una frase:** recuperar lo que borraste y encontrar cualquier cosa.

**Qué hace**
- **Papelera:** Ajustes → **Papelera** lista lo último que borraste (hasta 100 tareas, eventos y notas) con botón **Restaurar**. Solo aparecen los elementos cuya lista o cuaderno sigue existiendo; no se vacía sola y no tiene borrado definitivo.
- **Búsqueda global:** una **lupa** junto a la racha, en Tareas y en Notas, busca a la vez en tareas y eventos (por título y etiquetas) y en notas (por título o texto). Tocar un resultado lo abre.
- **«El lunes»:** nueva opción en el menú de fecha de una tarea, junto a Hoy, Mañana y En una semana.

**Cómo se usa:** borrar una tarea → Ajustes → Papelera → Restaurar. Tocar la lupa → escribir una palabra → ver tareas y notas.

**Gancho de video:** «Borré mi tarea por error… y la recuperé» / «Busca en TODO tu Kiwu con una palabra».
**No prometer:** recuperar listas o cuadernos enteros borrados; búsqueda dentro del cuerpo de las tareas (solo título y etiquetas); traducción automática de nombres que ya estaban guardados («Notas rápidas», listas de ejemplo).

### 3.4 Versión 0.6.0 · «Tareas repetitivas (hábitos)»

**Datos:** versionName 0.6.0 · **migración de base de datos a la versión 6** (columna nueva en tareas; las existentes quedan sin repetición).
**En una frase:** tareas que vuelven solas cada día, para usarlas como hábitos.

**Qué hace**
- En el detalle de una tarea hay un chip **Repetir** con cuatro opciones: **No se repite · Todos los días · De lunes a viernes · Fines de semana**.
- Tachas la tarea cuando la haces; **al día siguiente (en los días que toque) vuelve a aparecer sin tachar**, lista para usar de nuevo.
- Si tiene fecha y hora de entrega, la fecha pasa cada día a hoy con la misma hora, así que el aviso vuelve a sonar.
- En la lista las tareas repetitivas llevan un chip «↻ Todos los días» (o la repetición que tengan).
- Se guardan en la copia de seguridad y se sincronizan con la cuenta.
- **Los hábitos no cuentan para la racha** (ni la suman ni la rompen), para que un hábito que se te pasa no te quite la racha de entregas.
- El reinicio ocurre al abrir la app, al cambiar los datos y a medianoche.

**Cómo se usa:** abrir una tarea → chip «Repetir» → «Todos los días» → tacharla → al día siguiente está sin tachar.

**Gancho de video:** «Convierte tus tareas en hábitos: se destachan solas» / rutinas de estudio (leer 20 min, repasar vocabulario).
**No prometer:** historial o estadísticas de cumplimiento de hábitos (no se guarda cuántos días cumpliste); elegir días sueltos (solo hay 4 opciones fijas); que los hábitos suban la racha.

### 3.5 Versión 0.6.1 · «Subtareas, prioridad y etiquetas»

**Datos:** versionName 0.6.1 · **migración de base de datos a la versión 7** (tres columnas nuevas en tareas).
**En una frase:** organizar tareas grandes y distinguir lo importante.

**Qué hace**
- **Pasos (subtareas):** en el detalle de una tarea añades una lista de pasos, los marcas y los quitas. En la lista sale el avance «☑ 2/5». Marcar todos los pasos **no** completa la tarea sola.
- **Prioridad:** chip con bandera (baja, media, alta); en la lista se ve con color (gris, ámbar, rojo).
- **Etiquetas:** escribes una palabra (examen, lectura…) y queda como «#examen»; tocándola se quita; en la lista salen hasta dos por tarea.
- La **búsqueda** global también encuentra tareas por etiqueta.
- Todo va en la copia de seguridad y en la sincronización.

**Cómo se usa:** abrir una tarea → «Prioridad» → elegir; escribir una etiqueta; añadir pasos → ver «☑ 1/3» en la lista.

**Gancho de video:** «Cómo dividir un trabajo gigante en pasos» / «Pon colores a lo urgente».
**No prometer:** filtrar u ordenar la lista por prioridad o etiqueta (todavía no existe; se ven en cada tarea y en la búsqueda); pasos o prioridad en eventos (no aplican).

### 3.6 Versión 0.7.0 · «Herramientas de estudio»

**Datos:** versionName 0.7.0 · **migración de base de datos a la versión 9** (una columna en tareas, otra en listas y otra en notas).
**En una frase:** Kiwu no solo organiza: ahora te ayuda a estudiar.

**Qué hace**
- **Temporizador de estudio:** icono de reloj junto a la lupa (en Tareas y Notas). Presets de **25, 50, 5 y 10 minutos**. Avisa con una notificación al terminar, **aunque cierres la app**.
- **Calificaciones y promedio:** al ver una lista (materia) aparece un icono de birrete. Añades cada nota con **nombre, valor y peso (opcional)** y Kiwu calcula el **promedio ponderado**. La escala (5, 10, 100…) la eliges tú.
- **Cuenta atrás de exámenes:** en el detalle de una tarea el chip **Examen**. Con fecha, la lista muestra «Examen en 5 días», «Examen mañana» o «Examen hoy».
- **Tarjetas de repaso:** en una nota, menú ⋮ (tres puntos, arriba a la derecha) → **Tarjetas de repaso**. Escribes la **pregunta** y la **respuesta** en dos campos y pulsas «Añadir tarjeta»; luego «Empezar repaso»: ves la pregunta, muestras la respuesta y marcas «La sé» o «Repasar otra vez» (esta vuelve al final de la cola). Las tarjetas quedan guardadas en la nota.
- **Detalle de la tarea limpio:** fecha, hora, aviso, lista, repetición, prioridad, examen y etiquetas en una sola zona de chips; etiquetas con «+ Etiqueta» y pasos con «+ Añadir paso».
- Las notas de las listas y la marca de examen van en la copia de seguridad y en la sincronización.

**Cómo se usa**
1. Reloj → elegir 25 min → cerrar la app → llega la notificación «¡Tiempo!».
2. Abrir una lista → birrete → añadir «Parcial 4.0 peso 3» y «Quiz 2.0 peso 1» → «Promedio: 3.50».
3. Tarea con fecha → chip «Examen» → «Examen en 5 días» en la lista.
4. Abrir una nota → ⋮ → Tarjetas de repaso → escribir «Capital de Perú» / «Lima» → Añadir tarjeta → Empezar repaso.

**Gancho de video:** «Todo lo que necesitas para estudiar en una sola app» / «Crea flashcards de tus apuntes en 10 segundos» / «Calcula tu promedio ponderado sin Excel».
**No prometer:** pausa del temporizador ni ligarlo a una tarea; repetición espaciada o historial de tarjetas (las tarjetas se guardan, pero cada repaso empieza de cero); cuenta atrás de exámenes dentro del widget (solo en la lista); que el promedio «predice» notas.

---

## 4. Lo que viene después (no anunciar como hecho)
- **0.7.1 (pendiente, fuera de este documento):** racha en la cuenta, widget de agenda, deslizar tareas para completarlas/posponerlas, compartir y copia automática a Drive.
- **Tareas de Google (Google Tasks):** en pausa (ver aviso interno en la 0.5.5).

## 5. Estado y datos técnicos útiles
- Todo lo anterior está en el código del repositorio y pasa las pruebas automáticas, incluidas las de migración de la base de datos (de la versión 1 hasta la 9). Se probó en un emulador de Android; la prueba con un teléfono real y con cuentas de Google reales sigue pendiente.
- El AAB de Google Play solo se ha generado para la **0.5.4** (código 17). Las versiones 0.5.5 a 0.7.0 aún no tienen commit, AAB ni publicación.
- Cada versión se instala encima de la anterior y conserva los datos.

## 6. Reglas para el gestor de videos
1. Usar solo las funciones listadas como «Qué hace»; respetar los «No prometer».
2. No dar fechas de lanzamiento ni enlaces de descarga si no se han confirmado.
3. No mostrar ni mencionar «Tareas de Google».
4. Mostrar siempre datos de ejemplo (no datos personales reales) en las demos.
5. Mantener el tono: cercano, sin presión, de estudiante a estudiante.

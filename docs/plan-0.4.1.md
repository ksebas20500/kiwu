# Plan de Kiwu para Android 0.4.1 · Racha y guía de inicio

Estado: **implementado en `main` sin publicar** (24 de octubre de 2026). Sale de lo que pidieron testers y amigos: algo que anime a abrir la app cada día y una ayuda para entenderla al principio. Se publica en la **0.4.1** junto con los widgets ([plan-widgets-android.md](plan-widgets-android.md)) y, en la versión de Google Play, el aviso de versión nueva. Notas de la versión: [versiones/android-0.4.1.md](versiones/android-0.4.1.md).

Decisiones: un día cuenta si se **completa al menos una tarea** · la guía son **pantallas de bienvenida + una lista de ejemplo** · todo **sin cuenta ni servidor**; la racha con amigos va después (ver abajo).

## Racha

- **Regla** (`domain/Racha.kt`, función pura): se calcula desde `fecha_completado`, que ya existía. No hay migración ni campo nuevo en la copia `.kiwu.json`, y la racha sale del historial desde el primer día.
  - Cuentan las tareas borradas (borrar una tarea hecha no rompe la racha); no cuentan los eventos ni las tareas desmarcadas.
  - Si hoy aún no se ha completado nada pero ayer sí, la racha sigue viva «en riesgo» y se pierde al acabar el día.
  - Los días se cuentan en la zona horaria del teléfono; un día «del futuro» (reloj mal puesto) no alarga la racha.
- **Dónde se ve:** fila con la llama y «N días» arriba del menú de Tareas (la llama se apaga mientras hoy falta; «Hoy falta» en rojo si está en riesgo). Al tocarla se abre la **pantalla de la racha** (inspirada en [media/racha referencia.jpeg](media/racha%20referencia.jpeg)): el número grande y la llama (gris con «!» si está en riesgo), la semana con los días cumplidos unidos, un aviso del estado, tres cifras (mejor racha, días cumplidos, tareas hechas), la siguiente meta (3, 7, 14, 30, 50, 100, 200, 365 días) con su barra, y el calendario del mes con los tramos de racha unidos en píldoras y flechas para ver meses anteriores. El «?» explica las reglas.
- **Celebración (al estilo de Duolingo):** al completar la primera tarea del día salta, por encima de cualquier pantalla, una celebración: la llama entra con rebote, resplandor y chispas, el número sube de ayer a hoy (con vibración), el círculo de hoy se rellena en la semana y aparece «Continuar». La primera vez dice «¡Empezaste una racha!» y al llegar a una meta, «¡Alcanzaste la meta de N días!». Sale una vez al día (se guarda el día celebrado en `racha_celebrada`), aunque se desmarque y se vuelva a marcar, y no sale si la racha está oculta.
- El naranja del fuego es fijo (no sigue el color de la app), para que la racha se reconozca siempre.
- **Aviso:** a las 20:00, solo si la racha está en riesgo, en un canal propio («Racha») para poder silenciarlo aparte. Si luego se completa una tarea, el aviso se quita.
- **Ajustes:** «Mostrar la racha» (Menú de Tareas) y «Aviso de la racha» (Avisos), ambos activados por defecto.

## Guía de inicio

- Sale **solo la primera vez y con la app vacía** (sin listas, cuadernos ni horarios, aunque estén borrados). Quien actualiza con datos no la ve y se marca como vista (`guia_vista`).
- **5 pantallas** que se pasan deslizando: bienvenida, tareas en listas y carpetas, fechas/avisos/eventos, notas y horario, y la racha. Botones «Saltar» y «Siguiente»/«Empezar».
- Al terminar se crea la lista **«Empieza aquí» 👋** con 5 tareas que enseñan haciendo, y se abre. Tiene id fijo (`empieza-aqui`): no se duplica y, si se borró, no vuelve.
- **Repetir:** Ajustes → Acerca de → «Ver la guía de nuevo» (solo las pantallas, sin volver a crear la lista).

## Pruebas

- `RachaTest`: días seguidos, hueco, hoy en riesgo, mejor racha, zona horaria y medianoche, días del futuro, hora del aviso, semanas, metas, cuándo celebrar y filas del calendario del mes.
- `GuiaTest`: cuándo sale, detección de app con datos, lista de ejemplo (contenido, sin duplicar ni resucitar) y qué fechas cuentan para la racha.
- `ResumenAjustesTest`: resúmenes con la racha apagada.
- Emulador: actualizar encima con datos → sin guía y la racha sale del historial; instalación limpia → guía, lista de ejemplo abierta.

## Ideas para más adelante

- **Protector de racha / días de descanso** (las lunas de la referencia): un día sin tareas que no rompe la racha, por ejemplo uno por semana o ganado al llegar a una meta. Cambia la regla, así que necesita decidir cuántos y cómo se ganan.

## Después: racha con amigos

Necesita cuenta y servidor. Encaja sobre la cuenta de Firebase (plan Spark) de [plan-sincronizacion.md](plan-sincronizacion.md) (§4.5), pero **no hace falta esperar a la sincronización completa**: basta con subir los días cumplidos, no las tareas.

- Colección de rachas por usuario (días cumplidos) y de amistades, con reglas de seguridad de Firestore (`firestore.rules`): cada persona solo ve la racha de sus amigos.
- Invitación por código o enlace; racha compartida = días en que **todos** cumplieron.
- Antes de lanzarla: actualizar [privacidad.md](privacidad.md) (hoy dice «sin cuentas ni servidor») y la ficha de seguridad de datos de Google Play.

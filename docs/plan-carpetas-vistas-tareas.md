# Carpetas en las vistas generales de Tareas

El tester validó la etiqueta en la agenda del widget. Dentro de la app, «Todas las tareas» aún mezclaba cuatro tareas de Universidad y ocho de Por terminar. Las filas mostraban nombres de listas como Modelado, Juegos y Pelis, sin identificar su carpeta.

## Implementación

La vista de lista y el tablero muestran una etiqueta de carpeta junto a la lista en «Todas las tareas» y «Próximos 7 días». La regla incluye las tareas completadas y usa los IDs de la lista y la carpeta activa. Las etiquetas pueden saltar de línea según el espacio disponible. Dentro de una carpeta, en una lista individual o en «Hoy», las filas se muestran sin etiqueta de carpeta. Las listas sueltas y los eventos tampoco la muestran.

## Validación

- En «Todas las tareas», distinguir las cuatro tareas de Universidad y las ocho de Por terminar sin abrirlas. El total y el orden se conservan.
- En «Próximos 7 días», comprobar la etiqueta en las tareas que cumplan el filtro. Abrir ambas carpetas y verificar que desaparece.
- Repetir en lista y tablero, también con tareas completadas, listas sueltas, nombres largos y texto ampliado. Mover una lista entre carpetas y verificar que su etiqueta se actualiza.

Versión de entrega por confirmar.

## Resultados

- Pasaron `testDebugUnitTest`, `lintDebug` y `assembleDebug`.
- Para completar lint, se añadió la variante plural `many` en portugués al recurso del widget `w_fecha_elementos`.
- Validación visual confirmada por el usuario. Revisó las tareas en cada sección y comprobó que muestran la carpeta correspondiente en las vistas previstas.

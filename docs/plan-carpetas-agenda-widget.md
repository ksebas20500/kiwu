# Carpetas en la agenda del widget Calendario

Una persona que probó Kiwu tiene tareas con el mismo título en listas parecidas de Universidad y Cursos. La agenda del widget mostraba la lista, así que tenía que abrir cada tarea para identificar su carpeta.

## Cambio implementado

La agenda observa las carpetas activas del repositorio y vincula cada tarea con su lista y su carpeta por ID. Si hay carpeta, muestra su nombre en una etiqueta redondeada con los colores del tema, antes del nombre de la lista. Los datos secundarios pueden pasar al siguiente renglón según el espacio disponible.

Las listas sueltas y los eventos conservan su presentación. Abrir o completar una tarea funciona desde la misma fila. El rediseño posterior comparte esta resolución con el widget de Deberes y su selector de listas. Las tres vistas usan el modelo de datos existente. [Rediseño de los widgets](plan-widgets-android.md#rediseño-de-los-cuatro-widgets--noviembre-de-2026)

## Validación

- Crear dos tareas y dos listas con nombres idénticos en Universidad y Cursos. Al abrir el día desde el widget, deben distinguirse por la etiqueta.
- Renombrar la carpeta, mover una lista a otra carpeta y luego dejarla suelta. Al abrir la agenda, la etiqueta debe cambiar o desaparecer según corresponda.
- Verificar eventos, tareas con hora, nombres largos, pantalla estrecha, texto ampliado y temas claro y oscuro. Comprobar que tocar la fila abre la tarea y que la casilla permite completarla.

## Resultado del cambio inicial en la agenda

- Verificación automática completada. Pasaron `testDebugUnitTest`, `lintDebug` y `assembleDebug`.
- Validación visual pendiente. ADB no detectó ningún dispositivo Android ni emulador en ejecución.
- Versión de entrega por confirmar. La mejora se añadirá a sus notas cuando se asigne la versión.

La mejora relacionada dentro de la app está descrita en [Carpetas en las vistas generales de Tareas](plan-carpetas-vistas-tareas.md).

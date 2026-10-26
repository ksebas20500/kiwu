## Kiwu para Android 0.5.1 · racha «al día» y widgets más ágiles

Se instala encima de la 0.5.0 y conserva los datos. La base de datos sigue en la **versión 5**: no hay migración.

### Novedades

#### Racha «al día»

- **Nueva regla:** un día cuenta si lo terminas **sin tareas vencidas**. Ya no basta con completar una tarea cualquiera, así que no se mantiene con tareas triviales.
- **Días exigentes y días libres:** cuentan los días con una **entrega** (si queda todo entregado) y los días con **clase** en que completas o añades una tarea. Los días sin entregas ni actividad no suman ni rompen la racha, así que un fin de semana libre no la pierde.
- **Qué rompe la racha:** una tarea con fecha de entrega que sigue sin resolver al terminar el día. Se resuelve completándola o borrándola; desmarcarla la vuelve a dejar pendiente. Las tareas sin fecha y los eventos no cuentan.
- **Crear un horario no activa la racha:** un día de clase solo cuenta si ese día completas o añades una tarea.
- El aviso de las 20:00 sale solo si te quedan entregas pendientes, y los textos de ayuda, ajustes y guía explican la regla nueva en español, inglés y portugués.
- La racha se **recalcula del historial** al actualizar: la cifra puede subir o bajar respecto a la 0.5.0. Una tarea vencida vieja deja la racha en 0 hasta que la resuelvas o la borres.

#### Widget de Deberes

- **Botón «+»** en la cabecera: abre una barra sobre el escritorio, con el mismo campo que la app, para añadir una tarea sin abrir Kiwu. Si el widget muestra una lista, la tarea se crea en ella; si muestra «Hoy», vence hoy a las 18:00 para que aparezca.
- **Se actualiza al momento:** las tareas nuevas y las completadas se ven en cuanto se guardan. Antes el widget leía los datos una sola vez y podía tardar en reflejarlas.
- **Casilla de completar más grande:** toda la caja de 44 dp es el botón, no solo el círculo. El número de tareas va junto al título.

#### Arreglos

- **Los widgets ya no se mezclan** en la versión de Play: en las compilaciones minificadas los tres widgets se fusionaban en una sola clase y acababan mostrando el mismo contenido (los tres «Deberes» o los tres «Horario»). Se añadió la regla de R8 que lo evita.
- **Menos lecturas en la nube con la cuenta activa:** con la app abierta y sin cambios de otros dispositivos, la comprobación de novedades pasa de cada minuto a espaciarse hasta 5 minutos, y ya no se hace una consulta repetida al abrir. Los cambios de otro dispositivo se siguen viendo en menos de un minuto cuando los hay.

### Para quien compila

Sin cambios respecto a la 0.5.0: `firebase.apiKey`, `firebase.projectId` y `google.webClientId` van en `android/local.properties`.

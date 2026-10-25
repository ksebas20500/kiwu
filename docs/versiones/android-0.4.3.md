## Kiwu para Android 0.4.3 · faltas de clase e idiomas

Se instala encima de la 0.4.1 y conserva los datos. La base de datos pasa a la **versión 5** con una migración probada. La copia `.kiwu.json` sigue en el formato `1` y gana las faltas como sección opcional (ver [contrato](../contrato/README.md#faltas-de-clase-android-043-linux-y-macos-pendientes)).

### Novedades

#### Faltas de clase

- **Máximo por materia:** al crear o editar una clase hay un campo opcional «Máximo de faltas». Vale para la materia entera (todas sus filas en varios días), y vacío significa sin límite.
- **Chip «Faltas: N»** en la hoja de la clase, junto a Pendientes y Hechas (con máximo, «Faltas: 2/5»). Tiene el color del estado: verde, ámbar desde el 60 % del máximo y rojo al llegar. Al tocarlo se abre la sección de faltas, con **Marcar falta hoy**, **Otro día** (si ese día no hay clase, la app avisa pero deja marcarla), la lista de faltas, **Justificada** (se ve, pero no cuenta) y **Quitar falta**.
- **Contador en cada clase** del horario, en la vista por día y por semana: «quedan N» con el color del estado, o las que llevas si no hay máximo.
- **Aviso arriba del horario** cuando una materia llega al máximo. Al tocarlo se abre esa clase.
- **Widget de horario:** la clase de ahora o la siguiente muestra también su contador de faltas.
- **Vaciar faltas (nuevo semestre)** en el menú del horario: borra las faltas de ese horario y conserva los máximos.
- Las faltas son de la materia (el grupo de la clase), así que renombrarla o cambiarle los días no pierde el historial. Un mismo día solo cuenta una vez.

#### Idiomas

- Kiwu está ahora en **español, inglés y portugués**: textos, días, meses, fechas, plurales, avisos, notificaciones y widgets.
- **Android 13 o superior:** se usa el idioma del teléfono. Ajustes → **Idioma** abre el ajuste de Android para elegir otro solo para Kiwu.
- **Android 8 a 12:** Ajustes → **Idioma** abre un selector (Sistema / Español / English / Português) y la app se recarga al momento.
- Las listas que crea la app («Eventos», «Notas rápidas», «Empieza aquí») toman el nombre en el idioma que haya al crearlas.

#### Solo en la versión de Google Play

- Los textos propios de Play también están en inglés y portugués: «Quitar anuncios», la compra, «Buscar actualizaciones», el aviso «Hay una versión nueva de Kiwu» y la nota de privacidad.
- La sección «Quitar anuncios» y «Buscar actualizaciones» siguen en Ajustes, junto a la nueva fila «Idioma».

### Cambios

- La copia de seguridad exporta e importa las faltas y el máximo de cada clase. Al importar se descartan las faltas de materias que no existen, y el resumen indica cuántas faltas entraron.
- Los mensajes de error al importar una copia salen en el idioma de la app.

### Versiones publicadas

| Canal | versionCode | Archivo |
| --- | --- | --- |
| `main` (APK de GitHub) | 7 | sin publicar todavía |
| Google Play (rama `anuncios-prueba`) | 12 | `kiwu-0.4.3-codigo12.aab` |

### Limitaciones conocidas

- Las faltas solo existen en Android: si una copia pasa por macOS o Linux, se pierden las faltas y los máximos.
- No hay notificación al acercarse al máximo; el aviso solo se ve en el horario.
- Cambiar el idioma no renombra las listas que ya existían.
- El selector manual de Android 8 a 12 no se ha probado en un dispositivo con esas versiones, solo en las pruebas automáticas. En el emulador (API 37) se probaron la ruta automática, inglés y portugués.

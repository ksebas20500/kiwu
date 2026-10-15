# Kiwu para Android: fallos, mejoras y plan de solución

Origen: primera prueba en un teléfono real (vivo V2418, Android 16, arm64), 2026-10-13. Complementa [android-plan.md](android-plan.md), que sigue siendo el plan general.

## 1. Resultado de la prueba

| # | Qué se probó | Resultado |
| --- | --- | --- |
| 1 | Mantener pulsado «borrar» en un párrafo vacío de una nota | **Fallo: la app se cierra** |
| 2 | Arrastrar tareas en el tablero | **No existe** (ya estaba anotado como pendiente) |
| 3 | Vincular la carpeta Documents, entrar en subcarpetas y abrir archivos (Word, PDF…) con su app | Funciona |
| 4 | Fondo de imagen | Funciona. **Mejora pedida:** color de la app a elección |
| 5 | Calendario | Funciona |
| 6 | Cerrar y volver a abrir la app | **Fallo visual:** se ve un instante el fondo oscuro y luego cambia al diseño real |

Con esta prueba quedan verificados en un teléfono real la carpeta vinculada (con subcarpetas y archivos abiertos con otras apps) y el cambio del fondo de imagen.

## 2. Fallos y mejoras, ordenados por gravedad

| Id | Tipo | Gravedad | Descripción | Causa (o hipótesis) |
| --- | --- | --- | --- | --- |
| **F1** | Fallo | Alta (pérdida de la sesión de edición) | La app se cierra al mantener pulsado «borrar» en un párrafo vacío. | **Hipótesis, aún sin confirmar.** Con la tecla mantenida llegan muchos eventos seguidos. Si el primero elimina el bloque (`borrarVacio` → `bloques.removeAt`), el campo ya eliminado puede recibir otro evento y `indiceDe(b)` devuelve -1, con lo que `removeAt(-1)` lanza `IndexOutOfBoundsException` ([EstadoEditor.kt:288](../android/app/src/main/java/app/kiwu/android/ui/editor/EstadoEditor.kt)). Hay que confirmarlo con el volcado de Logcat antes de corregir. |
| **F2** | Fallo de paridad | Media | El tablero no permite arrastrar y soltar; solo se mueve la tarea con su menú. En escritorio sí se arrastra. | Nunca se implementó. |
| **F3** | Fallo visual | Media | Al abrir la app se ve primero el fondo/tema por defecto y luego el real. | `Raiz` lee los ajustes con `collectAsStateWithLifecycle(AjustesUi())`: el primer fotograma usa los valores por defecto (tema del sistema, sin imagen) y los guardados llegan después. Además, el tema de la ventana (`Theme.Kiwu`) es siempre claro, así que el arranque puede mostrar otro color distinto del de Compose. |
| **M1** | Mejora | Media | Elegir el color de la app en vez del coral/rosa fijo. | Los esquemas `Claro` y `Oscuro` de [Tema.kt](../android/app/src/main/java/app/kiwu/android/ui/Tema.kt) están escritos a mano con un solo color de acento. |
| **M2** | Mejora | Baja | La carpeta vinculada se usa poco; no hace falta ampliarla. | Decisión de producto: se deja como está. |

Los colores pastel de las tareas y del calendario se mantienen iguales a los de macOS y Linux. M1 solo cambia el acento y las superficies de la app (decisión que se puede revisar).

## 3. Plan de solución

El orden va de lo más grave a lo más leve, y cada fase se cierra con pruebas automáticas y con una comprobación en el teléfono.

### Fase 1. Evitar el cierre al borrar (F1)

1. Reproducir en el teléfono con Logcat abierto (filtro `app.kiwu.android`, nivel Error) y guardar el `Exception` completo. Si la causa no es la prevista, corregir según lo que diga la traza.
2. Si se confirma la hipótesis:
   - `alBorrarAlInicio`, `borrarVacio`, `convertir`, `cambiarSangria`, `mover`, `duplicar` y `eliminar` salen sin hacer nada cuando `indiceDe(b) < 0`.
   - Un único punto de entrada comprueba que el bloque sigue en la lista antes de aplicar cualquier cambio.
3. Pruebas automáticas en `EditorTest`:
   - borrar muchas veces seguidas sobre un párrafo vacío (último bloque, bloque único, con un divisor delante);
   - llamar a las operaciones con un bloque ya eliminado;
   - mismo caso para listas, tareas y código.
4. Verificación en el teléfono: mantener pulsado «borrar» en una nota vacía, en una con varios párrafos vacíos y en una lista.

Criterio de cierre: ninguna combinación de borrado mantenido cierra la app.

### Fase 2. Arranque sin parpadeo (F3)

1. Leer los ajustes antes de dibujar el primer fotograma: cargarlos en `KiwuApp` al iniciar (lectura única y rápida del DataStore) y pasarlos como valor inicial de `Raiz`, de modo que no haya un estado intermedio con valores por defecto.
2. Usar la API de pantalla de arranque (`androidx.core:core-splashscreen`) con `setKeepOnScreenCondition` hasta que los ajustes estén cargados.
3. Tema de la ventana con modo claro y oscuro (`values/` y `values-night/`) y color de fondo coherente con el de Compose, para que el arranque no muestre otro color.
4. Si el fondo de imagen tarda en decodificarse, mostrar el color liso del tema hasta que esté lista, sin saltos de color.
5. Verificación: abrir y cerrar la app con tema claro, oscuro, del sistema y con fondo de imagen. Grabar la pantalla y revisarla fotograma a fotograma.

Criterio de cierre: desde el primer fotograma visible, la pantalla ya tiene el tema y el fondo definitivos.

### Fase 3. Arrastrar y soltar en el tablero (F2)

1. Arrastre tras una pulsación larga sobre la tarjeta (`detectDragGesturesAfterLongPress`), con la tarjeta flotando bajo el dedo y un resalte en la columna de destino.
2. Soltar sobre otra columna cambia el estado de la tarea con la misma regla que ya usa el menú (`Reglas`), de modo que ambos caminos dan el mismo resultado.
3. Desplazamiento automático al acercarse al borde (tanto el vertical de la columna como el horizontal entre columnas).
4. Se conserva el menú como alternativa y para accesibilidad; la vibración corta al empezar el arrastre indica que se agarró.
5. Pruebas: la lógica de «columna destino → estado» sale a una función pura y se prueba en `ReglasTest`. El gesto se prueba a mano.
6. Verificación: mover una tarea entre las tres columnas, soltarla fuera de las columnas (debe volver a su sitio) y arrastrar con listas largas.

Criterio de cierre: se puede mover cualquier tarea entre columnas arrastrando, y soltar en un sitio no válido no cambia nada.

### Fase 4. Color de la app a elección (M1)

1. Ajuste nuevo `colorAcento` en `AjustesUi` (por defecto, el coral actual, para no cambiar el aspecto de quien ya la usa).
2. Generar el esquema claro y oscuro a partir de ese color (tonos del color para primario, contenedores y superficies), en lugar de las dos paletas escritas a mano. Opciones:
   - añadir una librería que genere la paleta de Material 3 a partir de un color; o
   - escribir una función propia que derive los tonos a partir del color (más simple, menos fiel a Material).
3. Pantalla de Ajustes → «Color de la app»:
   - una fila de colores predefinidos (coral, rojo, naranja, verde, azul, morado, rosa, gris);
   - un selector libre para elegir cualquier otro;
   - en Android 12 o superior, la opción «Color del sistema» (colores dinámicos del fondo de pantalla).
4. Comprobar que el contraste del texto sobre el color elegido es suficiente en claro y en oscuro; si no lo es, ajustar el color de contenido automáticamente.
5. El color elegido se incluye en la Fase 2, para que no haya parpadeo al arrancar.
6. Pruebas: función que construye el esquema (contrastes mínimos para cada color predefinido) y verificación visual en las pantallas principales (tareas, calendario, notas, editor y ajustes) con 3 o 4 colores.

Criterio de cierre: cambiar el color se refleja al instante en toda la app y se mantiene tras cerrarla y abrirla.

## 4. Pendientes que no entran en esta ronda

Siguen sin probarse en dispositivo: imágenes y archivos adjuntos en notas, vista previa de enlaces y vídeo, *Posponer*, reprogramación tras reiniciar y TalkBack. Conviene probarlos en el teléfono tras la Fase 1, porque comparten pantalla con el editor.

Siguen abiertos del plan general: traducción es/en, identificador definitivo y ficha de Play, y la cuenta y la sincronización (dependen de las decisiones abiertas del [plan de desarrollo](plan-desarrollo.md)).

## 5. Seguimiento

| Fase | Estado |
| --- | --- |
| 1. Cierre al borrar (F1) | Hecha: el editor ignora los eventos que llegan a un bloque ya eliminado. **Falta confirmar en tu teléfono** (ver nota) |
| 2. Arranque sin parpadeo (F3) | Hecha y comprobada en el emulador |
| 3. Arrastrar en el tablero (F2) | Hecha y comprobada en el emulador |
| 4. Color de la app (M1) | Hecha y comprobada en el emulador |

### Qué se hizo

- **F1.** `EstadoEditor` comprueba que el bloque sigue en la lista antes de actuar (`alEscribir`, `alBorrarAlInicio`, `borrarVacio`, `convertir`, `cambiarSangria`, `duplicar`) y `borrarVacio` ya no puede indexar fuera de la lista. Pruebas nuevas en `BorradoTest` (borrado repetido en párrafos, listas, código, títulos y divisores; todas las operaciones sobre un bloque eliminado). En el emulador se mantuvo «borrar» en una nota vacía y la app siguió abierta.
- **F3.** Los ajustes se leen antes de dibujar el primer fotograma (`MainActivity`), hay pantalla de arranque con el color de fondo del tema claro u oscuro (`Theme.Kiwu.Arranque`), la ventana toma el fondo del tema y la imagen de fondo aparece con un fundido. Medido en el emulador en modo oscuro: del arranque a la app ya no hay un salto intermedio de tema.
- **F2.** Pulsación larga sobre una tarjeta y arrastre: la tarjeta flota bajo el dedo, la columna de destino se resalta, se desplaza sola cerca de los bordes y vibra al agarrar y al soltar. Soltar fuera de las columnas no cambia nada. El menú se conserva. La lógica de destino y desplazamiento está en `Arrastre.kt` con pruebas.
- **M1.** Ajustes → «Color de la app»: 8 colores, «colores del sistema» (Android 12+) y un selector libre de tono e intensidad. El esquema claro y oscuro se deriva del color y se ajusta para mantener el contraste (pruebas con los 8 colores y con colores extremos como amarillo puro, blanco y negro). Se cambió el primario claro del coral de `#C9503A` a `#C44B35` porque su contraste con el blanco era 4,48 y no llegaba a 4,5.

### Notas

- **F1 sin confirmar en tu teléfono.** En el emulador no pude reproducir el cierre con el código anterior, así que la causa sigue siendo una hipótesis razonable. Si en tu vivo todavía se cierra, copia el error de Logcat (filtro `app.kiwu.android`, nivel Error) y lo corrijo con la traza real.
- **Pantalla de arranque.** Es el color de marca (coral) en claro u oscuro según el sistema. Si eliges otro color de app o un tema distinto al del sistema, verás ese fondo un instante y luego el de tu elección; no hay más saltos después.
- Los colores pastel de las tareas no cambian con el color de la app.

## 6. Segunda ronda (prueba en el teléfono, 2026-10-13)

Los fallos de la primera ronda ya no aparecen en el teléfono. Mejoras pedidas y qué se hizo (compilado, con pruebas y lint en verde; **pendiente de revisar en el teléfono**):

| # | Petición | Solución |
| --- | --- | --- |
| 1 | Desde Notas no se podía entrar al menú | Notas tiene ahora menú lateral (☰) con «Todas las notas», «Favoritas», los cuadernos (con editar y eliminar), «Nuevo cuaderno» y «Ajustes». Antes Ajustes solo se alcanzaba desde Tareas |
| 2 | Calendario amontonado; la segunda tarea del día no se veía | Las celdas muestran el número y un punto de color por tarea (hasta 4 y «+»); el detalle va en la lista de abajo, que ocupa todo el espacio restante y se desplaza |
| 3 | El «+» de «Añadir tarea» no hacía nada | Se sustituye por un botón de calendario para elegir la fecha y hora de entrega antes de crear la tarea (aparece como etiqueta que se puede quitar). En el calendario, que ya fija el día, el botón pasó a ser un reloj para elegir solo la hora de entrega (antes quedaba un «+» decorativo) |
| 4 | El tablero no es práctico en el teléfono | El tablero solo existe en pantallas anchas (≥ 600 dp, tabletas). En el teléfono no aparece el botón y siempre se ve la lista |

Nota: durante esta ronda un comando de prueba se ejecutó por error en el teléfono físico en lugar del emulador y dejó dos tareas de prueba («Examen fisica» y «Entrega informe») en el calendario de hoy. Hay que borrarlas a mano.

## 7. Tercera ronda (prueba del horario en el teléfono, 2026-10-15)

| # | Problema | Causa | Solución |
| --- | --- | --- | --- |
| 1 | Una clase que termina a las 12:00 quedaba a las 00:00 (medianoche) | El reloj de esfera de 24 horas tiene el 0 y el 12 casi en el mismo sitio; al pulsar el «12» se elegía el 0 | El reloj se sustituye por **dos ruedas que se desplazan** (horas y minutos), como la alarma del teléfono. Gira sin fin y se detiene en un número. También se usa al elegir la hora de entrega de una tarea |
| 2 | **La app se cerraba** al editar una clase y pulsar «Fin» | Una clase que termina a las 24:00 abría el selector en la hora 24, que no existe, y el componente lanzaba un error | Las ruedas solo conocen las horas 00–23 (24:00 se muestra como 00:00). Comprobado en el emulador: editar una clase que acaba a las 24:00 y pulsar «Fin» ya no cierra la app |
| 3 | El menú lateral se ve tosco y va a trompicones | El menú se dibujaba solo sobre el contenido, dejando la barra inferior fuera del panel y cortando su esquina; además la foto de fondo se dibujaba a tamaño enorme y con transparencia en cada fotograma | El menú se dibuja ahora **sobre toda la pantalla** (barra inferior incluida) con cabecera de marca, esquinas redondeadas y «Ajustes» como una fila más; «atrás» lo cierra. La foto de fondo se prepara una vez a la medida de la pantalla y con el velo ya mezclado, así que dibujarla cuesta lo mismo que un color liso |
| 4 | El calendario abre lento | Mezcla de lo anterior y de que las pruebas se hacen con la versión de depuración, que va bastante más lenta que la real | Fondo más ligero, colores de las tareas calculados una sola vez (antes se recalculaba un resumen MD5 por celda) y perfil de arranque (`baseline-prof.txt`) que compila de antemano el código de la app |
| 5 | A veces la pantalla se queda en blanco al abrir el menú | **Sin causa confirmada.** La hipótesis más probable es la foto de fondo gigante (hasta 3 200 px) en la memoria gráfica | Mismo arreglo del fondo; además la carga de imagen ya no cierra la app si falta memoria. Si vuelve a pasar, hace falta el error de Logcat |

**Cómo se midió.** En el emulador, abrir y cerrar el menú 8 veces dio entre un 34 % y un 44 % de fotogramas lentos en la versión de depuración y un 34 % en la versión `minificado`, con y sin foto de fondo. El emulador dibuja por software y no distingue las versiones, así que **la mejora de fluidez hay que comprobarla en el teléfono real**.

**Para probar la fluidez real:** en Android Studio, abre *Build Variants* y elige **minificado** (se instala como «Kiwu» aparte, con otro identificador, y no comparte datos con la de depuración). La versión de depuración no es representativa del rendimiento.

## 8. Cuarta ronda (análisis de un video de pruebas, 2026-10-15)

Se analizó un video de 135 s grabado en el teléfono (fotogramas cada 0,1 a 2,5 s y diferencias entre fotogramas consecutivos).

| # | Lo observado | Causa | Solución |
| --- | --- | --- | --- |
| 1 | Al pasar de **Horario** a **Notas**, el menú de Notas se abría solo (se reprodujo en el emulador) | Horario no tenía menú lateral: dejaba el panel vacío, con ancho cero. Al cambiar a Notas el panel pasaba de ancho 0 a 360 dp y el menú se quedaba abierto | Horario tiene ahora su menú (☰): «Todos los horarios», cada horario con editar y eliminar, «Nuevo horario» y «Ajustes». El engranaje de la barra superior pasa al menú |
| 2 | Al **elegir Calendario** en el menú, la pantalla se quedaba quieta unos 0,35 s sin ninguna reacción y luego saltaba de golpe (un solo fotograma de cambio, sin animación de cierre) | El cambio de pantalla (lo más pesado) se hacía en el mismo instante que el cierre del menú, y el fotograma pesado se comía la animación | Al elegir una opción, primero se cierra el menú (se ve su animación) y después se cambia de pantalla |
| 3 | La hora de las ruedas quedaba a veces en 11:59, 12:01… en vez de 12:00 | La rueda avisaba del valor solo al detenerse y a veces avisaba antes de asentarse del todo, dejando el valor anterior. Además con minutos de 1 en 1 es fácil quedarse en el contiguo | La rueda avisa de cada cambio (el último es siempre el valor final). Las horas de inicio y fin de las clases van **de 5 en 5 minutos** (00, 05, 10…), para acertar 12:00 con facilidad; las horas de entrega de las tareas siguen de 1 en 1. Probado en el emulador |
| 4 | En la vista de semana, los nombres de las clases se partían letra por letra | Las siete columnas son muy estrechas en el teléfono | Letra más pequeña (9 sp) y menos relleno |

**Sobre «se ve lento».** En el video, abrir el menú fue razonablemente fluido (unos 12 fotogramas de animación, con un solo salto); el problema estaba en el momento de elegir una opción (punto 2). El calendario en sí aparece en menos de 0,5 s. Para medir de verdad la fluidez hay que usar la variante **minificado** (ver sección 7): la de depuración es bastante más lenta.

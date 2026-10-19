# Lecciones de un intento de optimización que salió mal (octubre 2026)

**Léelo antes de volver a tocar el rendimiento de Kiwu para Android.**

## 1. Resumen

Después de publicar `android-v0.1.0` se hizo una ronda grande de «optimización». El resultado fue que la app **se sintió más lenta**, sobre todo al cambiar de pestaña. La persona que la usa probó el APK de GitHub de la 0.1.0, que iba fluida, y se decidió **descartar todo lo hecho** y volver a esa versión (commit `c34d4f3`; la carpeta `android/` es idéntica a la etiqueta `android-v0.1.0`). Esa rama de trabajo se borró a propósito y no se va a recuperar: lo que importa es lo que se aprendió.

**No se llegó a saber cuál de los cambios fue el culpable.** Se hicieron todos a la vez, se midieron sobre todo en un emulador y no se comparó nunca la versión anterior con la nueva *en el teléfono*, con la misma variante y el mismo recorrido. Esa es la lección principal.

## 2. Qué se intentó

| Cambio | Idea |
| --- | --- |
| Diagnóstico de fluidez en Ajustes (JankStats) con etiquetas de pantalla en todo el código | Medir fotogramas por pantalla |
| Calendario: celdas dibujadas a mano, y luego composición **fila a fila** y después «diferida» con fundido | Repartir el trabajo de abrir el calendario |
| Editor de notas en una **lista perezosa** (`LazyColumn`); antes, composición por tandas | Abrir notas de cientos de bloques |
| Menú de cada fila diferido hasta abrirse | Menos componentes por fila |
| Reglas de Tareas, Notas y Horario calculadas en **segundo plano** (`flowOn`) | Liberar el hilo principal |
| Arranque: la pantalla de arranque **espera** a leer los ajustes (sin bloquear) y la base de datos se abre al iniciar | Arranque más rápido |
| Foto de fondo en dos pasos y sliders que se aplican al soltar | Menos trabajo al mover los controles |
| «Modo rendimiento» (quita el fondo) | Válvula de escape |
| **Pestañas conservadas** (compuestas, medidas e invisibles) con fundido, **precomposición en reposo** de las pestañas que faltan, y espera de 160 ms antes de mostrar la primera visita | Que cambiar de pestaña no cueste nada |
| Búsqueda de notas con textos normalizados en caché; interpretación de la nota fuera del hilo principal | Menos trabajo al buscar y abrir |
| Informes de estabilidad de Compose, `shrinkResources`, scripts de medición y de recorrido | Herramientas |

## 3. Lo que se observó

- **En el emulador** (que dibuja por software y no representa a un teléfono) los fotogramas más lentos de Tareas · Lista, del calendario y de la apertura de una nota de 400 bloques bajaron mucho (por ejemplo, abrir esa nota pasó de 1,7 s a 0,1–0,3 s). Notas, Horario, menú y arranque **no mejoraron** de forma apreciable, y la variación entre pasadas era de ±50 %.
- **En el teléfono real** (vivo V2418, 60 Hz), siempre con la variante de **depuración**: la fluidez normal era buena (mediana de 6 ms con un presupuesto de 16,7 ms; 1,4–3 % de fotogramas lentos), pero **los picos al abrir pantallas por primera vez empeoraron** en el segundo informe (Notas 171 → 371 ms, Horario · Día 102 → 319 ms) y la persona usuaria notó más lento el cambio de pestaña y vio el calendario «cargarse por bloques de arriba a abajo». Compilar la app por adelantado (`cmd package compile -m speed`) no lo arregló.
- La versión **0.1.0 publicada** (variante optimizada, sin nada de lo anterior) **iba fluida** en el mismo teléfono.

## 4. Por qué probablemente salió mal

Son hipótesis, no hechos probados:

1. **Se decidió con datos del lugar equivocado.** El emulador no sirve para juzgar fluidez, y en el teléfono se midió la variante de depuración, que no es la que se usa.
2. **Repartir la composición en varios fotogramas se nota.** Componer por tandas o con un fotograma de retraso evita un fotograma enorme, pero la pantalla se ve construirse (el calendario «por bloques») y retrasa lo que se ve. Un solo fotograma de 100 ms molesta menos que un efecto raro de 300 ms.
3. **Mantener pestañas vivas y precomponerlas** añade trabajo continuo (diseño y recomposición de pantallas que no se ven) y puede coincidir con lo que la persona está haciendo.
4. **Instrumentación y etiquetas por todo el código** añadieron sobrecarga y complejidad en cada pantalla.
5. **Tocar hilos, arranque y fondo sin evidencia** en el teléfono: son cambios con riesgo y sin beneficio demostrado.
6. **Demasiadas variables a la vez**: al ir mal no había forma de aislar la causa, así que no quedó otra que descartar todo.

## 6. Reglas para la próxima vez

1. **Medir primero en el teléfono**, con la variante optimizada (`minificado`/publicación) y, si hace falta, compilada por adelantado. El emulador solo sirve para comprobar que algo funciona, no para decidir.
2. **Comparar siempre contra la versión anterior en el mismo teléfono** y con el mismo recorrido, antes de dar nada por bueno.
3. **Un cambio cada vez, un commit por cambio.** Si un cambio no mejora lo medido, se revierte.
4. **No repartir ni diferir la composición de una pantalla.** Si una pantalla es pesada, se aligera; no se construye por partes.
5. **No conservar pestañas ocultas compuestas ni precomponer en segundo plano.**
6. **No tocar el arranque ni los hilos** sin una medida que lo justifique.
7. **No dejar instrumentación permanente** en el código de las pantallas; si hace falta, va en una rama de pruebas.
8. **Las animaciones deben ser cortas y de dibujo** (transparencia y desplazamiento), sin volver a componer nada.
9. **Que la persona usuaria pruebe y compare** las dos versiones instaladas antes de publicar.

## 7. Lo aprovechable

- **Bug real, aún sin arreglar en 0.1.0:** en «Añadir tarea», si se elige una fecha, el campo de texto se queda sin ancho en una pantalla de 360 dp y el recuadro crece a lo alto. Arreglo conocido: poner la etiqueta de la fecha en su **propia línea** encima del campo y dar un ancho mínimo al campo de texto.
- **Referencias del teléfono (variante de depuración):** mediana de 6 ms por fotograma; los picos de 100–350 ms ocurren al abrir por primera vez una pantalla (Notas, Horario, calendario, editor).
- Un teléfono con la variante de depuración recién instalada va más lento hasta que Android compila el código; `adb shell cmd package compile -m speed -f <paquete>` lo fuerza.
- Estabilidad de Compose: no era un problema (461 de 609 composables ya se saltaban sin recomponer).

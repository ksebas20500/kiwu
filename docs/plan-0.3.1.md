# Plan de Kiwu para Android 0.3.1 y 0.3.2

Estado: **implementado en `main` sin publicar** (octubre de 2026). Sale de las recomendaciones de una persona que probó la app.

## 0.3.1 · La hora es opcional al crear una tarea

Antes, al crear con el calendario del campo «Añadir tarea», siempre había que pasar por el cuadro de la hora. Ahora el cuadro del día tiene **Aceptar** (solo el día, sin hora) y **Añadir hora** (abre el cuadro de la hora, que también trae el interruptor **Todo el día**). Internamente «sin hora» sigue siendo 00:00, igual que antes: no cambia la base de datos ni la copia `.kiwu.json`.

## 0.3.2 · Eventos (cosas que pasan y no se completan)

Decisiones: viven en una lista propia fuera de las carpetas · solo hora de inicio (sin hora de fin) · sin repetición (cumpleaños anuales quedan para más adelante).

- **Dato:** no hay migración. Los eventos son tareas de una lista especial con id fijo `eventos` (nombre «Eventos»), que se crea al añadir el primero. Para la base de datos y la copia `.kiwu.json` es una lista más, así que macOS, Linux y las copias antiguas siguen funcionando (otro cliente la verá como una lista llamada «Eventos»).
- **Menú:** entrada fija «Eventos» bajo «Todas las tareas»; no se puede mover, borrar ni meter en una carpeta, y no aparece entre las listas ni en los selectores de lista.
- **Fila:** icono de calendario en lugar del círculo; su menú solo ofrece eliminar. El detalle no tiene casilla, columnas ni selector de lista.
- **Dónde se ven:** en «Eventos» (todos), en «Hoy» (solo los de hoy), en «Próximos 7 días», en «Todas» (los de hoy en adelante) y en el día del Calendario. Un evento pasado deja de verse en Hoy y Todas; nunca está «vencido», no cuenta en los contadores y no va a «Completadas» ni al tablero.
- **Crear:** desde la lista «Eventos», o eligiendo «Eventos» en el selector de lista de «Añadir tarea» y del Calendario. Sin hora = todo el día.
- **Aviso:** 1 hora antes por defecto (cambiable). Un evento de todo el día avisa respecto de las 9:00, no de la medianoche. El texto dice «Empieza en…» en vez de «Vence en…».
- **Pruebas:** `EventosTest` (alcance, contadores, avisos de todo el día, textos).

### Más adelante

Hora de fin (y eventos de varios días) y repetición anual.

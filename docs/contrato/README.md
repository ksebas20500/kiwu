# Contrato de datos multiplataforma

Fuente de verdad del formato de las notas (`NoteDocument` / `NoteBlock` / `TextRun`) que comparten macOS, Linux y, en el futuro, Android.

Cada `notas/<nombre>.json` es una nota escrita como la escribiría macOS, y `notas/<nombre>.esperado.json` dice **qué debe obtener cualquier cliente** al leerla:

- `texto_plano`: el texto legible de la nota (para búsqueda y vistas previas).
- `tipos`: el `kind` de cada bloque, en orden (un tipo desconocido se lee como `paragraph`).
- `sangrias`: el nivel de sangría de cada bloque.

Además, cada cliente debe poder escribir la nota y volver a leerla con el mismo resultado.

Quién lo comprueba:

| Cliente | Prueba | Cómo se ejecuta |
| --- | --- | --- |
| Linux (Python) | `linux/tests/test_contrato.py` | `cd linux && python3 -m unittest tests.test_contrato` |
| macOS (Swift) | `macos/Tools/probar-contrato.swift` | `macos/Tools/probar-contrato.sh` |

**Para cambiar el formato**: añade primero una fixture y su `.esperado.json`, y no fusiones el cambio hasta que pasen las pruebas de todos los clientes. Los campos que un cliente no entiende no pueden provocar un fallo al leer.

Límite conocido: Linux conserva los campos desconocidos al reescribir una nota; Swift los descarta. Por eso las fixtures solo exigen lo que ambos cumplen.


## Horarios (Android; Linux y macOS pendientes)

La copia `.kiwu.json` admite dos claves opcionales nuevas. El formato sigue siendo `1`: las copias antiguas, sin estas claves, se importan igual, y un cliente que no las conoce las ignora.

```json
"horarios": [
  { "id": "UUID", "nombre": "Universidad", "icono": "🎓", "orden": 0, "creada": "ISO-8601", "actualizada": "ISO-8601" }
],
"clases": [
  {
    "id": "UUID", "horario": "UUID-del-horario", "materia": "UUID-de-una-materia o null",
    "grupo": "UUID (une las filas de una misma clase en varios días)",
    "titulo": "Cálculo", "diaSemana": 1, "inicioMin": 480, "finMin": 600,
    "aula": "204", "notas": "", "avisoMin": 10, "color": "#1E88E5 o null",
    "creada": "ISO-8601", "actualizada": "ISO-8601"
  }
]
```

Reglas: `materia` es la lista de tareas vinculada a la clase (Android muestra sus tareas en la hoja de la clase y puntos en el horario); `color` (opcional, desde Android 0.3) es el color elegido para la clase como `#RRGGBB`, y `null` o ausente significa color automático: un valor que no sea `#RRGGBB` se ignora al importar, y las copias sin este campo se importan igual. `diaSemana` va de 1 (lunes) a 7 (domingo); `inicioMin` y `finMin` son **minutos desde las 00:00 en hora local, sin zona horaria** (`0 ≤ inicio < 1440`, `inicio < fin ≤ 1440`); una clase en varios días son varias filas con el mismo `grupo`; `avisoMin` es nulo si no hay aviso. Al importar se descartan las clases con día u horas inválidos o cuyo horario no existe, y no se pisa lo que ya está (ni se resucita lo eliminado).

### Faltas de clase (Android 0.4.3; Linux y macOS pendientes)

Cada clase gana el campo opcional `maxFaltas` y la copia, la clave opcional `faltas`. El formato sigue siendo `1`.

```json
"clases": [ { "...": "...", "maxFaltas": 5 } ],
"faltas": [
  { "id": "UUID", "grupo": "UUID-del-grupo-de-la-clase", "horario": "UUID-del-horario", "dia": "2026-10-20", "justificada": false, "creada": "ISO-8601" }
]
```

Reglas: las faltas son de la **materia**, es decir, del `grupo` de la clase (renombrar o mover una clase de día no separa su historial). `maxFaltas` es un entero mayor que 0, igual en todas las filas del grupo; `null`, ausente o `≤ 0` significa sin límite. `dia` es una fecha sin hora ni zona (`AAAA-MM-DD`). Las faltas `justificada: true` se muestran pero no cuentan para el límite. Al importar se descartan las faltas de un `grupo` que no existe y no se pisa lo que ya está. Linux y macOS todavía no las conocen: una copia que pase por ellos pierde las faltas y los máximos. Por eso aún no están en `copia/ejemplo.kiwu.json`.


## Carpetas de listas (Android; Linux y macOS pendientes)

La copia `.kiwu.json` admite una clave opcional nueva, `grupos`, y dos campos opcionales en cada elemento de `materias` (`grupo` y `emoji`). El formato sigue siendo `1`: las copias antiguas, sin estas claves, se importan igual (las listas quedan sueltas y con su aspecto de siempre), y un cliente que no las conoce las ignora.

```json
"grupos": [
  { "id": "UUID", "nombre": "Semestre 3", "icono": "📁", "orden": 0, "plegado": false,
    "creada": "ISO-8601", "actualizada": "ISO-8601" }
],
"materias": [
  { "id": "UUID", "nombre": "Cálculo", "orden": 0, "carpeta": null,
    "grupo": "UUID-de-una-carpeta o null", "emoji": "📐 o null", "...": "..." }
]
```

Reglas: una lista está como mucho en **una** carpeta y las carpetas **no se anidan**. `materias[].grupo` es el `id` de una carpeta de `grupos`: si no existe, la lista se importa suelta. `icono` (de la carpeta) y `emoji` (de la lista, opcional) son texto libre. **No confundir** con `materias[].carpeta`, que es la carpeta de archivos del dispositivo vinculada a la lista y no viaja entre dispositivos. Al importar no se pisa lo que ya está ni se resucita lo eliminado. Borrar una carpeta no borra sus listas.

**Eventos (Android 0.3.2):** no añaden campos. Un evento es un `deber` cuya `materia` tiene el id fijo `eventos` (lista «Eventos»); no se completa. Un cliente que no los conoce lo ve como una tarea más de una lista llamada «Eventos».


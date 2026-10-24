# Plan de Kiwu para Android 0.3.0 (carpetas) y widgets

> **Cambio (24 oct 2026):** los widgets salen en la **0.4.1**, junto con la racha y la guía de inicio ([plan-0.4.1.md](plan-0.4.1.md)); lo que se hizo al final está en [plan-widgets-android.md](plan-widgets-android.md). Donde abajo dice «0.4.0», léase 0.4.1.

Estado: **decidido, en marcha con la 0.3.0** (octubre de 2026). Parte de la 0.2.1 y de lo ya hecho en `main` (hoja de tareas de la clase, puntos en el horario y color por clase).

Tres bloques, repartidos en dos versiones (la 0.3.0 trae el 1; la 0.4.0 trae el 2):

1. **Carpetas de listas** en el menú de Tareas (como las páginas anidadas de Notion).
2. **Widgets** de pantalla de inicio: *Deberes* (todas las tareas) y *Calendario de deberes*, parecidos a los de macOS.
3. Cierre: pruebas, documentación y publicación.

---

## 1. Carpetas de listas

### Qué ve la persona

```
 Kiwu                      Listas                    ＋
 Tareas y listas           ▾ 📁 Semestre 3        (7)   ⋮
                              ▸ Ing software 2    (3)
 ○ Hoy              (2)       ▸ Química           (2)
 ○ Próximos 7 días  (3)       ▸ Fundamentos       (2)
 ○ Calendario              ▸ 📁 Trabajo           (1)   ⋮
 ○ Todas las tareas (5)    ▸ 📁 Idiomas           (0)   ⋮
                              Proyecto integrador (0)   ⋮     ← una lista suelta, sin carpeta
```

- Una **carpeta** agrupa varias listas y se **pliega y despliega** con la flecha. El contador de la carpeta es la suma de las tareas pendientes de sus listas.
- Tocar el **nombre** de la carpeta muestra **todas sus tareas juntas** (una vista nueva «Carpeta», como «Hoy» o «Todas»).
- Las listas que no están en ninguna carpeta siguen apareciendo sueltas, debajo.
- El estado plegado o desplegado de cada carpeta **se recuerda**.

### Cómo se crean y se organizan

- El «＋» de «Listas» ofrece **Nueva lista** y **Nueva carpeta**.
- En el menú ⋮ de una lista: **Mover a una carpeta…** y **Sacar de la carpeta**.
- En el menú ⋮ de una carpeta: **Renombrar**, **Cambiar emoji y color**, **Ordenar sus listas** y **Eliminar carpeta** (las listas **no** se borran: pasan a estar sueltas).
- Una lista solo puede estar en **una** carpeta, y las carpetas **no se anidan** (un solo nivel). Anidar queda para más adelante.

### Aspecto a gusto del usuario

En **Ajustes → Menú de Tareas**:

| Opción | Valores |
| --- | --- |
| **Aspecto** | Carpetas plegables (por defecto) · Compacto (filas más bajas, sin contadores) · Con color (punto o emoji de color en cada lista) |
| **Orden** | Manual (el que elige la persona) · Alfabético · Por tareas pendientes |
| **Mostrar contadores** | Sí / No |

Cada lista y cada carpeta pueden tener **emoji y color propio**.

### Datos y compatibilidad

- **Atención al nombre:** la lista ya tiene un campo `carpeta` que significa *«carpeta de archivos del teléfono vinculada»*. Para no confundirlo, la agrupación se llamará **grupo** en el código (`GrupoListas`, `grupo_id`); en pantalla siempre se dirá «carpeta».
- **Base de datos v3 → v4**, solo con cambios que **añaden** sin tocar lo existente:
  - tabla nueva `grupo_lista` (`id`, `nombre`, `icono`, `color`, `orden`, `plegado`, `creada`, `actualizada`, `eliminada`);
  - columna opcional `grupo_id` en la tabla de listas (vacía = lista suelta).
- **Copia `.kiwu.json`:** clave opcional nueva `grupos` y campo opcional `grupo` en cada lista. Las copias antiguas se importan igual; un cliente que no los conoce los ignora (macOS y Linux no se rompen). Se documenta en `docs/contrato`.
- **Pruebas:** migración 3→4 y salto 1→4 sin perder datos; importar copias antiguas; exportar e importar con carpetas; borrar una carpeta no borra sus listas; el contador suma bien; el orden.

### Pantallas que cambian

Menú lateral de Tareas, selector de lista de «Añadir tarea» (agrupado por carpeta), selector de lista de las clases del horario, la vista nueva de carpeta y Ajustes.

---

## 2. Widgets de pantalla de inicio

### Tecnología

**Jetpack Glance** (widgets escritos al estilo de Compose). Es lo más parecido a cómo están hechos los de macOS (SwiftUI): una vista por tamaño y una acción por botón. Requiere una librería nueva (`androidx.glance:glance-appwidget`), compatible con Android 8 o superior.

### Widget 1 · «Deberes» (todas las tareas)

```
┌──────────────────────────────┐
│ ✔ Deberes                (5) │
│ ○ Tablas de costos           │
│   Ing software 2 · Mañana    │
│ ○ Project Libre              │
│   Ing software 2 · Mañana    │
│ ○ Quiz de las 50 preguntas   │
│   Fundamentos · 3 oct        │
│ +2 más                       │
└──────────────────────────────┘
```

- Las tareas pendientes **ordenadas por fecha** (vencidas primero, en rojo), con su lista y fecha.
- El **círculo completa la tarea** sin abrir la app.
- Tocar el **título** abre esa tarea; tocar la **cabecera** abre la app en Tareas.
- Contador de pendientes y «+N más» cuando no caben.
- **Configurable** al añadirlo: *Todas · Hoy · Próximos 7 días · una lista · una carpeta* (la carpeta viene del bloque 1).
- Tamaños: 2×2 (3 tareas), 4×2 (4–5), 4×3 y mayores (más filas); se puede redimensionar.

### Widget 2 · «Calendario de deberes»

```
┌──────────────────────────────┐
│ Octubre 2026         ‹   ›   │
│ L  M  X  J  V  S  D          │
│ 28 29 30  1  2  3  4         │
│  5  6  7  8  9 10 11         │
│ 12 13 14 15 16 17 18         │
│ 19 20 21 22 23 24 25         │
│          ●  ●●               │   ← puntos: tareas con entrega ese día
│ 26 27 28 29 30 31  1         │
│ ──────────────────────────── │
│ Próximas entregas            │
│ ○ Taller de modelado  Vie    │
│ ○ Informe final       Lun    │
└──────────────────────────────┘
```

- **Mes** con la semana de la persona (empieza en lunes), el día de hoy resaltado y puntos en los días con entregas (**rojo** si hay vencidas).
- Flechas **‹ ›** para cambiar de mes (se guarda en el propio widget).
- Debajo, **«Próximas entregas»** con círculos para completar.
- Tocar un **día** abre el calendario de la app en ese día.
- Tamaños: **pequeño** (solo hoy y sus tareas), **mediano** (semana + lista) y **grande** (mes completo + lista).

### Cómo se mantienen al día

- Leen **la misma base de datos** de la app; no hay red ni servidor.
- Se **actualizan solos** cuando cambia una tarea (se completa, se crea, se mueve de fecha), se actualiza al **cambiar de día** a medianoche y al cambiar la hora o la zona.
- Cuando se completa una tarea desde el widget, se **cancelan sus avisos** igual que en la app.
- Colores: **siguen el tema de la app** (claro, oscuro y color de acento); opción de usar los colores dinámicos del sistema en Android 12 o superior.

### Widget 3 · «Horario de hoy»

Las clases de hoy con «Ahora / Siguiente» y los puntos de tareas de cada una (los mismos del horario). Tocar una clase abre su hoja de tareas. Entra en la 0.4.0 junto con los otros dos.

### Límites a tener en cuenta

- Los widgets de Android **no se pueden desplazar con fluidez** como una lista de la app: muestran las filas que caben y «+N más».
- Cada fabricante (Xiaomi, Samsung…) los dibuja un poco distinto; hay que probarlos en un teléfono real además del emulador.
- Algunos fabricantes **limitan la actualización en segundo plano**; la medianoche se programa con una alarma para que «Hoy» no se quede viejo.

---

## 3. Orden de trabajo, pruebas y publicación

| Paso | Qué | Resultado |
| --- | --- | --- |
| 1 | Datos de las carpetas (migración v4, copia, pruebas) | Tests en verde |
| 2 | Menú con carpetas y vista «Carpeta» | Probado en emulador |
| 3 | Ajustes del menú (aspecto, orden, contadores) y emoji/color | Probado |
| 4 | Widget «Deberes» (con su configuración) | Probado en emulador y teléfono |
| 5 | Widget «Calendario de deberes» | Probado |
| 6 | Actualización automática y medianoche | Probado |
| 7 | Documentación, notas de versión, `versionName` 0.3.0 | — |
| 8 | Publicación: pre-release en GitHub y paquete para Play (código 8) | — |

**Requisitos antes de publicar en Play como definitiva:** los IDs reales de AdMob (la versión de Play sigue con anuncios de prueba) y la prueba cerrada con 12 testers durante 14 días.

## 4. Decisiones tomadas

1. **Aspecto del menú:** los tres (plegable, compacto y con color).
2. **Carpetas:** de un solo nivel.
3. **Widgets:** siguen el color de la app.
4. **Horario de hoy:** se incluye como tercer widget.
5. **Versiones separadas:**
   - **0.3.0:** carpetas de listas (bloque 1).
   - **0.4.0:** los tres widgets (bloque 2): *Deberes*, *Calendario de deberes* y *Horario de hoy*.

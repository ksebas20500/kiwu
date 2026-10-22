# Plan: Ajustes más claros (agrupados por secciones)

Estado: **implementado en `main` sin publicar**, entra en la 0.3.2 (octubre de 2026).

Decisiones: entra en la 0.3.2 · Compacto ya no apaga los contadores (manda solo el interruptor) · «Fondo de imagen» va dentro de Apariencia · sin buscador de ajustes.

Cómo quedó: la pantalla `ajustes` guarda qué sección está abierta (`rememberSaveable`) y el botón atrás vuelve a la lista de secciones, sin rutas nuevas. Piezas: `FilaAjuste` y `SelectorAspectoMenu` (`AjustesPiezas.kt`), textos de resumen en `ResumenAjustes.kt` (con `ResumenAjustesTest`). Los controles se movieron sin cambiar sus claves.

## El problema

Hoy Ajustes es **una sola pantalla larga** con 8 bloques (Apariencia, Color, Menú de Tareas, Fondo, Recordatorios, Copia de seguridad, Acerca de; en la versión de Play, además, Anuncios). Hay unos **14 controles** y varios párrafos de explicación a la vez. Para llegar a «Copia de seguridad» hay que desplazarse por todo lo demás, y es difícil saber dónde estás o dónde buscar algo.

Además hay cosas repetidas o que se pisan:

- **Compacto** ya implica «sin contadores», y aparte existe el interruptor «Mostrar el número de tareas».
- Cada bloque explica lo que hace con un texto fijo en pantalla.

## La idea: ajustes de dos niveles

Como en los ajustes del teléfono o en los de Claude: una **pantalla principal corta** con las secciones (icono + título + una línea que resume el valor actual) y, al tocar una, **su propia pantalla** con solo esos controles.

```
 Ajustes
 ┌────────────────────────────────────┐
 │ 🎨  Apariencia                     │
 │     Oscuro · Color terracota     › │
 ├────────────────────────────────────┤
 │ ☰   Menú de Tareas                 │
 │     Plegable · orden manual      › │
 ├────────────────────────────────────┤
 │ 🔔  Avisos                         │
 │     Activados · 2.º aviso 10 min › │
 ├────────────────────────────────────┤
 │ 💾  Copia de seguridad             │
 │     Exportar e importar          › │
 ├────────────────────────────────────┤
 │ ⭐  Quitar anuncios   (solo Play)  │
 ├────────────────────────────────────┤
 │ ℹ   Acerca de Kiwu                 │
 │     Versión 0.3.2                › │
 └────────────────────────────────────┘
```

### Qué va en cada sección

| Sección | Contiene | Viene de |
| --- | --- | --- |
| **Apariencia** | Tema (sistema / claro / oscuro), Color de la app, Fondo de imagen (elegir, encuadrar, visibilidad, velo) | «Apariencia», «Color de la app» y «Fondo de imagen» |
| **Menú de Tareas** | Aspecto, Orden, Mostrar contadores | «Menú de Tareas» |
| **Avisos** | Estado de notificaciones, Segundo aviso, Alarmas exactas | «Recordatorios» |
| **Copia de seguridad** | Exportar, Importar | «Copia de seguridad» |
| **Quitar anuncios** | Compra única y estado (solo la versión de Play) | «Anuncios» |
| **Acerca de Kiwu** | Versión, privacidad (enlace a la política), cómo se guardan los datos | «Acerca de Kiwu» |

Pasamos de 8 bloques a **6 filas** en la pantalla principal (5 en GitHub, que no lleva anuncios), y ninguna sección pasa de 5 controles.

### Menos texto

- Los párrafos fijos pasan a **una línea de resumen** bajo cada fila de la pantalla principal (el valor actual), en vez de una explicación.
- Cuando algo necesita aclaración (por ejemplo los aspectos del menú), se muestra con **una vista previa**, no con una frase: tres mini-menús al lado, uno por aspecto, y el elegido resaltado.
- Los textos largos (cómo se guardan los datos, formato `.kiwu.json`) quedan **dentro** de «Acerca de» y «Copia de seguridad», no en la pantalla principal.

### Los 3 modos del Menú de Tareas

- **Plegable (por defecto):** las carpetas se pliegan y despliegan con su flecha y cada fila muestra cuántas tareas pendientes tiene.
- **Compacto:** filas más bajas y sin contadores, para que quepan más listas en pantalla.
- **Con color:** cada lista lleva su emoji o, si no tiene, un punto de color propio, para reconocerlas de un vistazo.

El orden (manual, alfabético, por tareas pendientes) y el interruptor de contadores funcionan con los tres. Propuesta: que **Compacto no apague** los contadores por su cuenta, para que solo mande el interruptor y no haya dos controles para lo mismo.

## Cómo se construye

1. **Navegación:** una ruta `ajustes` (la lista de secciones) y una ruta por sección, con las mismas transiciones de la app. El botón de atrás vuelve a la lista de secciones.
2. **Piezas reutilizables:** `FilaAjuste` (icono, título, resumen, flecha) y `PantallaAjuste` (barra superior y contenido). Los controles actuales se **mueven** a su pantalla sin reescribirlos.
3. **Resúmenes:** funciones puras que convierten los ajustes en el texto de cada fila (se prueban con tests, como `MenuListas`).
4. **Pruebas:** tests de los resúmenes, comprobación en emulador (teléfono y tableta, claro y oscuro) y que los ajustes guardados se conservan igual.
5. **Sin migración de datos:** las claves del `AjustesStore` no cambian; solo cambia dónde se muestran.

## Decisiones

1. Versión: **0.3.2**, junto con los eventos (todavía no se ha publicado nada más).
2. Compacto **no** apaga los contadores: manda solo el interruptor.
3. «Fondo de imagen» va dentro de **Apariencia**.
4. Sin buscador de ajustes.

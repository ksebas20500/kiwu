# Plan del port de Kiwu a Linux (Arch + Hyprland / Wayland)

Estado: **borrador para discutir** · Alcance: solo app de escritorio (sin widgets) · Plataforma objetivo: Arch Linux con Hyprland, Wayland nativo y aceleración gráfica.

> Las estimaciones de tiempo son orientativas (dos personas, unas 10 h por semana cada una). Se revisan al terminar el *spike* de la fase 0.

> **Actualización (decisión de stack).** Se eligió una app **nativa GTK4 + libadwaita en Python (PyGObject)**, no Tauri ni Rust. Motivo: el código se escribió sin poder compilar ni ejecutar GTK en el equipo de desarrollo, y un lenguaje interpretado permite probar la lógica de datos y detectar errores sin compilar; sigue siendo nativa y con Wayland nativo. El código está en [`linux/`](../linux/README.md). Las secciones 3 (comparativa) y 9 (fases) de este plan siguen siendo útiles como referencia; si el rendimiento o la distribución lo piden, el núcleo (`modelos`, `db`, `servicios`) se puede portar a Rust más adelante porque el formato de datos está documentado y probado.

---

## 1. Objetivo y alcance

**Dentro del alcance (versión 1 en Linux)**
- Tareas: listas por materia, fecha y hora, vistas *Hoy* / *Próximos 7 días* / *Todas* / *Completadas*, lista **y tablero** (Por hacer · En curso · Hecho).
- Calendario mensual.
- Notas: cuadernos, páginas, editor por bloques con menú `/`, formato (negrita, cursiva, subrayado, tachado, código, color, resaltado, enlaces, sangría), bloques de código con colores, enlaces con vista previa (web y vídeo), imágenes y archivos adjuntos.
- Archivos por materia (carpeta vinculada).
- Ajustes: atajos editables, tema, fondo de imagen con translucidez.
- Recordatorios con notificaciones del escritorio.
- Importar la copia de seguridad de Kiwu para macOS.

**Fuera del alcance (por ahora)**
- Widgets de escritorio.
- Sincronización entre equipos (iCloud no existe en Linux; se plantea más adelante).
- X11 como objetivo (debe funcionar por XWayland, pero no se prueba ni se optimiza).
- Otras distros como objetivo principal (Flatpak las cubrirá casi sin coste extra).
- Móvil.

## 2. Punto de partida: qué se puede reutilizar

La app de macOS está en SwiftUI, Core Data, WidgetKit y piezas de AppKit/WebKit. **Nada de eso existe en Linux**, así que el port es una **reescritura de la interfaz** que reutiliza el *diseño* y el *formato de datos*, no el código.

| Pieza en macOS | ¿Se reutiliza? | Cómo |
| --- | --- | --- |
| Vistas SwiftUI (tareas, calendario, notas, ajustes) | No | Se reescriben; se copia el diseño y el comportamiento |
| Core Data (`Materia`, `Deber`, `Cuaderno`, `Pagina`) | No el código | Se reutiliza el **modelo**: mismas entidades y campos en SQLite |
| Contenido de las notas (`NoteDocument`, `NoteBlock`, `TextRun` en JSON) | **Sí, tal cual** | Es JSON puro y ya es independiente de la plataforma |
| Reglas de negocio (`DeberService`: estados, mover entre columnas, completar) | Como especificación | Se reimplementan con pruebas compartidas |
| Resaltado de sintaxis (regex propias) | No | Se usa una librería con los temas Dark+/Light+ de VS Code |
| Editor de texto (`NSTextView`) | No | Editor web o `GtkTextView` (ver §3) |
| Widgets, App Groups, `kiwu://` | No | Fuera de alcance |
| Marcadores de seguridad de archivos (security-scoped bookmarks) | No | Se usan rutas normales (ver §6) |

Resumen: se reutiliza el **formato de datos y el diseño**; la interfaz se rehace.

## 3. Decisión de stack

Tres candidatos realistas. Evaluados contra lo que más pesa en Kiwu: **el editor de notas por bloques** (la parte más grande), Wayland/Hyprland y el mantenimiento por dos personas.

| | **A. Tauri 2 + web** (Rust + Svelte o React) | **B. GTK4 + libadwaita** (Rust, `gtk4-rs`) | **C. Qt 6 / QML** |
| --- | --- | --- | --- |
| Editor por bloques | **Muy bueno**: TipTap/ProseMirror ya trae marcas, sangría, menú `/`, enlaces | Costoso: `GtkTextView` con etiquetas; los bloques hay que construirlos | Costoso: `QTextDocument` o editor propio |
| Resaltado de código tipo VS Code | **Shiki** incluye `dark-plus` y `light-plus` | GtkSourceView (esquemas propios hay que ajustar) | KSyntaxHighlighting |
| Vista previa de enlaces y vídeo | Web nativa (iframe) | Requiere WebKitGTK embebido | Requiere QtWebEngine (pesado) |
| Aspecto nativo en Hyprland | Web con CSS (se puede cuidar mucho) | **Nativo** (libadwaita) | Nativo Qt |
| Wayland / aceleración | WebKitGTK sobre Wayland; **riesgo con NVIDIA** (ver §7) | **Mejor**: GTK4 con renderizador GL/Vulkan | Muy bueno |
| Peso en disco y RAM | Bajo (usa WebKitGTK del sistema) | Bajo | Medio/alto |
| Velocidad de desarrollo | **Alta** | Media-baja | Media |
| Paquetes en Arch | `webkit2gtk-4.1`, `gtk3` | `gtk4`, `libadwaita` | `qt6-*` |

**Recomendación: A (Tauri 2 + TipTap + SQLite).** Es la opción que más rápido llega a paridad con el editor de notas, que es donde está el riesgo. El coste es que la aceleración con NVIDIA exige cuidado (§7) y que la interfaz no será GTK nativa.

**Cuándo elegir B:** si lo que más importa es el aspecto nativo y el rendimiento en Wayland, y estáis dispuestos a pagar bastantes semanas más en el editor. Se puede decidir con el *spike* de la fase 0.

**Descartado:** reescribir en Swift multiplataforma (SwiftCrossUI/SwiftGtk). La interfaz sigue sin existir, el ecosistema es pequeño y complicaría el empaquetado en Arch.

## 4. Arquitectura propuesta (opción A)

```
┌───────────── Interfaz (web) ─────────────┐
│ Svelte/React · TipTap · dnd-kit · Shiki   │
└───────────────┬───────────────────────────┘
                │ comandos tipados (invoke)
┌───────────────▼───────────────────────────┐
│ Núcleo en Rust                             │
│  · datos (SQLite, migraciones)             │
│  · recordatorios (notify-rust / D-Bus)     │
│  · vista previa de enlaces (reqwest)       │
│  · archivos por materia (rutas, inotify)   │
│  · IPC de instancia única + CLI            │
└────────────────────────────────────────────┘
```

**Estructura del repositorio.** No se mueve nada de macOS (se conserva el historial); se añade:

```
linux/            app Tauri (src-tauri/ + web/)
spec/             contrato de datos: esquema, JSON de ejemplo, casos de prueba compartidos
docs/             documentación (este plan, formato de datos, guía de Hyprland)
```

**Rutas de datos (estándar XDG)**
- Base de datos: `~/.local/share/kiwu/kiwu.db`
- Configuración: `~/.config/kiwu/settings.json` (atajos, tema, translucidez)
- Fondo e imágenes: `~/.local/share/kiwu/`
- Caché de vistas previas: `~/.cache/kiwu/`

**Identificador de la app (app-id):** `io.github.ksebas20500.Kiwu` (sirve para Flatpak, `.desktop` y reglas de ventana de Hyprland; confirmar en el *spike* qué `app_id` publica realmente la ventana).

## 5. Contrato de datos compartido (lo primero que hay que fijar)

Es lo que evita que macOS y Linux diverjan. Se escribe en `spec/` **antes** de programar la interfaz.

1. **Esquema SQLite** con las mismas entidades que Core Data: `materia`, `deber` (con `estado` 0 por hacer / 1 en curso, y `completado`; la columna «Hecho» se deduce de `completado`), `cuaderno`, `pagina`. Identificadores UUID.
2. **Contenido de notas** en el JSON actual (`blocks`, `runs`, `indent`, `language`, `url`, `preview`). Los lectores deben ser **tolerantes**: ignorar campos desconocidos y aceptar los ausentes (ya lo hace la versión de macOS).
3. **Copia de seguridad (`.kiwu.json`)**: un único archivo con listas, tareas, cuadernos, páginas, imágenes en base64 y ajustes. Es el puente macOS ↔ Linux.
4. **Archivos adjuntos**: en macOS son marcadores del sistema, que no se pueden leer en Linux. El formato guarda además el **nombre** y una **ruta opcional**; en Linux, un adjunto sin ruta válida aparece como «sin vincular» con la acción *Volver a vincular*.
5. **Pruebas compartidas**: un conjunto de JSON de ejemplo que ambas apps deben leer y escribir sin pérdida.

**Tarea previa en macOS** (pequeña): añadir *Exportar copia de seguridad* e *Importar copia* a Kiwu para macOS. Sin esto no hay forma de llevar los datos a Linux.

## 6. Paridad de funciones: macOS → Linux

| Función en macOS | Equivalente en Linux | Notas |
| --- | --- | --- |
| Listas, tareas, vistas Hoy/Próximos/Todas | Igual, consultas SQL | — |
| Lista y tablero con arrastrar y soltar | dnd-kit (web) | Mismo comportamiento de columnas |
| Calendario mensual | Componente propio | El día se expande con doble clic |
| Editor por bloques y menú `/` con flechas | TipTap (`Suggestion`) con nodos propios | Un nodo por cada `NoteBlockKind` |
| Formato, color, resaltado, enlaces, sangría | Marcas de TipTap + extensión de sangría | Atajos: Ctrl+B / I / U / Shift+X / E / K |
| Bloque de código con colores VS Code | Shiki (`dark-plus` / `light-plus`) | Detección automática de lenguaje |
| Enlaces con vista previa (web y vídeo) | Rust (`reqwest` + `scraper`); YouTube/Vimeo con `iframe` | El `iframe` de YouTube puede dar error 153 con orígenes `tauri://`; plan B: miniatura + abrir con `xdg-open` |
| Archivos por materia (carpeta del Finder) | Ruta + `xdg-open`; `inotify` para saber si sigue existiendo | Con Flatpak, usar portales de archivos |
| Recordatorios (UserNotifications) | Notificaciones D-Bus (`notify-rust`) | Necesita un proceso vivo (§7) |
| Ajustes: atajos editables | Igual, guardados en `settings.json` | Dentro de la app (los globales, ver §7) |
| Ajustes: fondo + translucidez | Ventana transparente + CSS; el desenfoque lo hace Hyprland | Es el efecto «tipo Firefox» de verdad |
| Tema claro/oscuro | Sigue `color-scheme` del portal de escritorio | Con opción manual |
| Copia de seguridad / migración | Importar `.kiwu.json` | Ver §5 |

## 7. Integración con Wayland y Hyprland (y aceleración gráfica)

**Gráficos**
- Arrancar siempre en **Wayland nativo** (`GDK_BACKEND=wayland`); XWayland solo como respaldo.
- **AMD e Intel (Mesa):** deberían funcionar sin ajustes.
- **NVIDIA (driver propietario):** WebKitGTK puede dar ventana en blanco, parpadeos o cuelgues. Hay que probar y, si hace falta, activar al inicio `WEBKIT_DISABLE_DMABUF_RENDERER=1` (y, como último recurso, `WEBKIT_DISABLE_COMPOSITING_MODE=1`). Plan: detectar el driver al arrancar y aplicar el ajuste necesario, con una opción en ajustes para cambiarlo.
- Con la opción B (GTK4) se elige renderizador con `GSK_RENDERER=ngl|vulkan`.
- **Escala fraccionaria** y pantallas HiDPI: probar a 1×, 1,25× y 2×.

**Hyprland**
- **Atajos globales:** Wayland no permite que una app capture teclas globales. Se resuelve con un **CLI de la propia app** + instancia única, y el usuario lo enlaza en `hyprland.conf`:
  ```
  bind = SUPER, N, exec, kiwu --nueva-tarea
  bind = SUPER SHIFT, K, exec, kiwu --mostrar
  ```
  Los atajos *dentro* de la ventana sí son editables desde Ajustes.
- **Bandeja del sistema:** Hyprland no trae bandeja; funciona con la de Waybar (StatusNotifierItem). Debe ser opcional.
- **Recordatorios:** requieren que Kiwu esté en marcha. Opción por defecto: arrancar en segundo plano con `exec-once = kiwu --segundo-plano`. Alternativa: temporizadores de `systemd --user`.
- **Reglas de ventana:** documentar en `docs/` ejemplos (diálogo de ajustes flotante, opacidad, tamaño):
  ```
  windowrulev2 = float, class:^(io.github.ksebas20500.Kiwu)$, title:^(Ajustes)$
  ```
  La sintaxis de reglas cambia entre versiones de Hyprland: verificar con la versión estable actual.
- **Translucidez:** la app pinta fondo con transparencia; el desenfoque lo aporta `decoration { blur { … } }` del compositor. Comprobar que la ventana transparente funciona también con NVIDIA.
- **Decoraciones:** en un compositor de mosaico se prefiere sin barra de título; ofrecer barra propia opcional.
- **Portales:** `xdg-desktop-portal-hyprland` y `-gtk` para selector de archivos, tema y capturas.

## 8. Empaquetado y distribución

| Canal | Prioridad | Notas |
| --- | --- | --- |
| **AUR** `kiwu-bin` (binario de los releases) y `kiwu-git` | Alta | Es lo que usará la mayoría en Arch. Nombre libre en AUR |
| **Flatpak** (Flathub) | Media | Cubre otras distros; el runtime GNOME trae WebKitGTK; permisos de archivos por portales |
| **Binario `.tar.zst` / AppImage** en GitHub Releases | Media | AppImage con WebKitGTK suele ser frágil en Wayland: probar antes de prometerlo |
| Repositorio oficial de Arch | No | Fuera de alcance |

Incluir: archivo `.desktop`, iconos `hicolor`, `metainfo.xml` (AppStream) y la licencia MIT.

**CI (GitHub Actions):** contenedor `archlinux:latest` que compile, pase pruebas y genere el paquete; `makepkg` para validar el `PKGBUILD`; subida a Releases con una etiqueta por plataforma (por ejemplo `linux-v0.1.0`, para no mezclar con `v2.0.0` de macOS).

## 9. Fases y entregables

| Fase | Contenido | Entregable | Estimación |
| --- | --- | --- | --- |
| **0. Decisión y *spike*** | Contrato de datos en `spec/`; *hello world* de Tauri en Hyprland con ventana transparente; prueba de TipTap + Shiki; prueba en AMD/Intel/NVIDIA; esqueleto de CI | Informe del spike y decisión A o B | 1 semana |
| **1. Núcleo y tareas** | SQLite + migraciones; listas y tareas; vistas Hoy/Próximos/Todas/Completadas; vista de lista; importar `.kiwu.json` | Gestor de tareas usable con tus datos de macOS | 2–3 semanas |
| **2. Tablero, calendario y ajustes** | Tablero con arrastrar y soltar; calendario; atajos editables; tema; fondo y translucidez | Paridad de tareas y ajustes | 2 semanas |
| **3. Notas** | Cuadernos y páginas; bloques; menú `/`; formato; código con colores; enlaces con vista previa; imágenes y adjuntos | Paridad del editor | 3–4 semanas |
| **4. Sistema** | Recordatorios; archivos por materia; CLI e instancia única; segundo plano y autoarranque | Integración con Hyprland | 1–2 semanas |
| **5. Empaquetado y beta** | AUR, Flatpak, CI de releases, guía de Hyprland, correcciones de la beta | Versión 0.x pública | 1–2 semanas |

**Total orientativo: 10–14 semanas** a tiempo parcial para dos personas. Es una estimación mía, no medida.

## 10. Reparto sugerido entre dos personas

- **Persona A (núcleo y sistema):** contrato de datos, SQLite, importación, recordatorios, CLI/IPC, empaquetado y CI.
- **Persona B (interfaz):** tareas, tablero, calendario, ajustes y editor de notas.
- **Juntos:** el *spike* de la fase 0 y las pruebas de gráficos (cada uno con su GPU: conviene cubrir AMD/Intel y NVIDIA).

Flujo de trabajo: rama `linux` y *pull requests* con revisión del otro; etiquetas `plataforma:linux` / `plataforma:macos`; un archivo `CODEOWNERS` por carpeta.

## 11. Pruebas

- **Núcleo:** pruebas unitarias en Rust (consultas, estados, migraciones) y **pruebas de formato compartidas** con los JSON de `spec/`.
- **Interfaz:** pruebas de componentes y, para flujos clave (crear tarea, arrastrar en tablero, escribir con `/`), pruebas de extremo a extremo con `tauri-driver` (WebKitWebDriver).
- **Gráficos:** lista de comprobación manual por GPU/driver (arranque, redimensionar, transparencia, escala).
- **Migración:** importar una copia real de macOS y comprobar que no se pierde nada (títulos, fechas, estados, formato de notas, imágenes).
- **Paridad:** lista de funciones de la §6 con una casilla por función y por plataforma.

## 12. Riesgos y mitigaciones

| Riesgo | Impacto | Mitigación |
| --- | --- | --- |
| WebKitGTK + NVIDIA (ventana en blanco, parpadeos) | Alto | Probar en el spike; autodetección y variables de entorno; plan B: GTK4 nativo |
| Editor de notas más grande de lo previsto | Alto | Empezar por TipTap; recortar al subconjunto esencial antes de la beta |
| `iframe` de YouTube con error 153 | Medio | Miniatura + abrir con `xdg-open`; probar `youtube-nocookie` |
| Sin atajos globales ni bandeja en Wayland | Medio | CLI + `bind` de Hyprland; bandeja opcional |
| Divergencia entre macOS y Linux | Medio | Contrato en `spec/`, pruebas compartidas y lista de paridad |
| Tiempo de dos personas | Medio | Fases con entregable usable; la beta no espera a la paridad total |
| Cambios de sintaxis de Hyprland | Bajo | Documentar versión probada; ejemplos mínimos |
| Datos de macOS con adjuntos no portables | Bajo | Estado «sin vincular» + *Volver a vincular* |

## 13. Decisiones abiertas

| # | Decisión | Propuesta por defecto |
| --- | --- | --- |
| D1 | Stack (A Tauri, B GTK4, C Qt) | **A**, confirmar con el spike |
| D2 | Framework web (Svelte o React) | Svelte (menos código) salvo que prefiráis React |
| D3 | Idiomas | Español primero; inglés después (i18n desde el principio) |
| D4 | Canal principal | AUR primero, Flatpak después |
| D5 | Versionado | Linux con su propia numeración (`0.x`), etiquetas `linux-v…` |
| D6 | Licencia | MIT, igual que macOS |
| D7 | ¿Sincronización futura? | Posponer; no condiciona la fase 1 |

## 14. Primeros pasos (esta semana)

1. Acordar D1 y D2 con tu amigo.
2. Crear en `spec/` el esquema SQLite y 3–4 JSON de ejemplo (a partir de tus datos reales, anonimizados).
3. Añadir *Exportar/Importar copia* a Kiwu para macOS.
4. Spike en Hyprland: ventana Tauri transparente, `app_id`, `WEBKIT_DISABLE_DMABUF_RENDERER` según GPU, un bloque TipTap con `/` y un bloque de código con Shiki.
5. Dar de alta el esqueleto de CI en un contenedor de Arch y un `PKGBUILD` de prueba.
6. Crear las etiquetas y la rama `linux`.

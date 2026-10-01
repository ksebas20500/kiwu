# Kiwu para Linux

Versión de escritorio para Linux de Kiwu, pensada para **Arch Linux + Hyprland** (Wayland nativo). Es una aplicación **GTK4 + libadwaita** escrita en Python (PyGObject): nativa, sin Electron ni navegador embebido, y con la misma idea que Kiwu para macOS: todo local, sin cuentas y sin nube.

> **Estado: primera versión (0.1.0), escrita sin poder ejecutarla en Linux.** La lógica (datos, importación desde macOS, recordatorios, vistas previas) está probada con pruebas automáticas. La interfaz **todavía no se ha ejecutado sobre GTK real**: espera fallos de interfaz y repórtalos (ver [Probar y reportar](#probar-y-reportar-fallos)). Sin widgets, por decisión de alcance.

## Qué incluye

Las mismas funciones que Kiwu 2.0 para macOS (salvo widgets):

- **Tareas** por lista (materia), con fecha y hora; vistas *Hoy*, *Próximos 7 días*, *Todas* y *Completadas*.
- **Lista y tablero** (Por hacer · En curso · Hecho) con arrastrar y soltar.
- **Calendario** mensual (doble clic en un día para ver todas sus tareas).
- **Notas** con cuadernos y páginas, favoritas y búsqueda; editor **por bloques** con menú `/` navegable con ↑ ↓, formato (negrita, cursiva, subrayado, tachado, código, color, resaltado, enlaces), sangría con Tab, imágenes y archivos adjuntos.
- **Bloques de código con colores** tipo VS Code (Dark+/Light+) para Python, JS/TS, JSON, HTML, CSS, Bash, C/C++, Java, C#, Go, Rust, SQL…, con detección automática del lenguaje.
- **Enlaces con vista previa** (páginas web y vídeos de YouTube/Vimeo; el vídeo se reproduce dentro si tienes WebKitGTK).
- **Archivos por lista**: vincula una carpeta y explórala dentro de Kiwu.
- **Recordatorios** con notificaciones del escritorio.
- **Ajustes**: atajos de teclado editables, tema claro/oscuro, imagen de fondo con visibilidad, opacidad de paneles y desenfoque, y **opacidad real de la ventana** (el desenfoque lo pone Hyprland).
- **Importar tus datos de macOS** (leyendo directamente las bases de datos de Kiwu/TaskFlow) y copias de seguridad `.kiwu.json`.

## Instalar (Arch Linux)

Dependencias:

```bash
sudo pacman -S python python-gobject gtk4 libadwaita gtksourceview5
```

Opcionales:

```bash
sudo pacman -S webkitgtk-6.0 xdg-desktop-portal-gtk mako
```

- `webkitgtk-6.0`: reproducir vídeos de YouTube/Vimeo dentro de las notas (sin él se abren en el navegador).
- `xdg-desktop-portal-gtk`: selector de archivos y tema claro/oscuro del escritorio.
- `mako` (o cualquier demonio de notificaciones): para ver los recordatorios.

### Opción 1: un solo comando (recomendada)

Instala en tu carpeta personal (`~/.local`), sin `sudo`, y crea la entrada en el menú de aplicaciones:

```bash
curl -fsSL https://raw.githubusercontent.com/ksebas20500/kiwu/main/linux/install.sh | bash
```

Descarga el último release de Linux (etiquetas `linux-v…`) y comprueba su suma de verificación; si todavía no hay ninguno, instala la versión de desarrollo. Opciones: `--prefix DIR` (otro destino), `--desinstalar` (no toca tus datos) y `--forzar` (no comprobar dependencias). 

### Opción 2: a mano (descargando el archivo)

Descarga `kiwu-linux.tar.gz` (y, si quieres, su `.sha256`) de [Releases](https://github.com/ksebas20500/kiwu/releases?q=linux-v), descomprímelo y ejecuta su instalador:

```bash
sha256sum -c kiwu-linux.tar.gz.sha256   # opcional: comprobar la descarga
tar -xzf kiwu-linux.tar.gz
cd kiwu-linux-*
./install.sh                            # --prefix DIR, --desinstalar, --forzar
```

El instalador es el mismo en las dos opciones; la diferencia es solo de dónde saca los archivos.

### Otras formas: ejecutar desde el código fuente

```bash
git clone https://github.com/ksebas20500/kiwu.git
cd kiwu/linux
./run.sh
```

### Otras formas: instalar con el PKGBUILD (Arch)

```bash
cd kiwu/linux
makepkg -si
```

> El `PKGBUILD` descarga la etiqueta `linux-v0.1.0` y comprueba su suma de verificación.

## Hyprland: configuración recomendada

Wayland no permite que una aplicación capture atajos globales; en su lugar, Kiwu acepta órdenes de línea de comandos y Hyprland las enlaza. Kiwu es de **instancia única**: si ya está abierto, la orden se envía a esa ventana.

En `~/.config/hypr/hyprland.conf`:

```ini
# Atajos globales
bind = SUPER, N, exec, kiwu --nueva-tarea
bind = SUPER SHIFT, K, exec, kiwu --mostrar

# Recordatorios sin ventana abierta (opcional)
exec-once = kiwu --segundo-plano

# Reglas de ventana (el app_id es io.github.ksebas20500.Kiwu). Ajusta la sintaxis a tu versión de Hyprland.
windowrulev2 = float, class:^(io.github.ksebas20500.Kiwu)$, title:^(Ajustes)$
windowrulev2 = size 720 700, class:^(io.github.ksebas20500.Kiwu)$, title:^(Ajustes)$
windowrulev2 = center, class:^(io.github.ksebas20500.Kiwu)$, title:^(Ajustes)$

# Para que la translucidez de la ventana se vea con desenfoque
decoration {
    blur {
        enabled = true
        size = 8
        passes = 2
    }
}
```

Luego, en *Kiwu → Ajustes → Apariencia → Ventana* baja la **opacidad de la ventana** y, si usas un gestor de mosaico, desactiva **Mostrar la barra de título**.

### Recordatorios

Los avisos solo salen si Kiwu está en marcha. Dos opciones:

- Mantener `exec-once = kiwu --segundo-plano` (arranca sin ventana).
- Copiar `data/kiwu-segundo-plano.desktop` a `~/.config/autostart/`.

Necesitas un demonio de notificaciones (mako, dunst, swaync…). Al pulsar la notificación se abre la tarea.

## Gráficos y rendimiento

- Kiwu usa **GTK4 sobre Wayland** de forma nativa (no pasa por XWayland). Con AMD e Intel (Mesa) no hace falta ningún ajuste.
- **NVIDIA (driver propietario):** si detecta el driver, Kiwu desactiva el renderizador DMABUF de WebKitGTK (`WEBKIT_DISABLE_DMABUF_RENDERER=1`), que solo afecta a la reproducción de vídeo. Si algo falla con la ventana en sí, prueba `GSK_RENDERER=ngl kiwu` o `GSK_RENDERER=gl kiwu`.
- Para anular los ajustes automáticos: `KIWU_SIN_AJUSTES_DE_GPU=1 kiwu`.
- `kiwu --diagnostico` muestra la sesión, la GPU y las versiones de GTK/libadwaita/GtkSourceView/WebKit detectadas; adjúntalo a cualquier informe de errores.

## Importar tus datos de macOS

1. En el Mac, copia la carpeta `~/Library/Application Support/Kiwu` (o `TaskFlow` si vienes de la versión 1) a tu equipo Linux. Contiene `GestorUniversitario.sqlite` y `Notas.sqlite`.
2. En Kiwu: **Ajustes → Datos → Importar desde una carpeta de macOS**.
3. Se importan listas, tareas (con estado y fechas), cuadernos y notas con su formato. No se duplica nada si importas dos veces.

Los archivos adjuntos de macOS no se pueden enlazar automáticamente (allí se guardan como marcadores del sistema): aparecen como «Sin vincular» y basta pulsarlos para elegir el archivo en Linux.

## Dónde se guardan los datos

| Qué | Ruta |
| --- | --- |
| Base de datos | `~/.local/share/kiwu/kiwu.db` |
| Imagen de fondo | `~/.local/share/kiwu/fondo` |
| Ajustes y atajos | `~/.config/kiwu/settings.json` |

Se pueden cambiar con `KIWU_DATOS` y `KIWU_CONFIG`.

## Estructura del código

```
kiwu/
  modelos.py       datos y formato JSON de las notas (idéntico al de macOS)
  db.py            SQLite
  servicios.py     reglas de las tareas (vistas, columnas del tablero…)
  importador.py    importar desde macOS y copias .kiwu.json
  enlaces.py       vistas previas de enlaces
  recordatorios.py qué tareas toca avisar
  ajustes.py       preferencias y atajos
  app.py           aplicación (instancia única, línea de comandos)
  ui/              interfaz GTK4: ventana, tareas, tablero, calendario, notas, ajustes
  ui/editor/       editor por bloques (texto con formato, código, imágenes, enlaces)
  recursos/        temas de colores del código (Dark+/Light+)
tests/             pruebas de la lógica, de los instaladores y prueba de humo de la interfaz
Tools/             crear-paquete.sh (genera dist/kiwu-linux.tar.gz)
install.sh         instalador por terminal
PKGBUILD           paquete para Arch
data/              .desktop, icono y metadatos AppStream
```

## Probar y reportar fallos

```bash
cd linux
python3 -m unittest discover -s tests     # lógica + prueba de humo (no necesita GTK)
./run.sh --diagnostico                    # entorno gráfico detectado
./run.sh                                  # la aplicación
```

Si la app no arranca o algo falla, abre un *issue* con la salida del terminal y de `--diagnostico`. Los fallos más probables de esta primera versión están en el editor de notas (cursor y foco entre bloques, menú `/`, barra de formato) y en detalles de aspecto.

## Licencia

MIT, igual que Kiwu para macOS.

#!/bin/bash
# Instala Kiwu para Linux en tu carpeta personal (no necesita sudo).
#
#   curl -fsSL https://raw.githubusercontent.com/ksebas20500/kiwu/main/linux/install.sh | bash
#
# Opciones:
#   --prefix DIR   Dónde instalar (por defecto ~/.local, o la variable KIWU_PREFIX)
#   --desinstalar  Quita lo instalado (tus datos en ~/.local/share/kiwu NO se tocan)
#   --forzar       No comprobar las dependencias (GTK4, libadwaita…)
#
# Si ejecutas este script desde una copia del repositorio o desde el .tar.gz de un release, instala esa copia.
# Si no, descarga el último release de Linux (etiquetas «linux-v…») y, si todavía no hay ninguno, la rama principal.
set -euo pipefail

REPO="ksebas20500/kiwu"
APP_ID="io.github.ksebas20500.Kiwu"
PREFIJO="${KIWU_PREFIX:-$HOME/.local}"
DESINSTALAR=0
FORZAR=0

while [ $# -gt 0 ]; do
  case "$1" in
    --prefix) PREFIJO="${2:?Falta el directorio}"; shift 2 ;;
    --desinstalar) DESINSTALAR=1; shift ;;
    --forzar) FORZAR=1; shift ;;
    -h|--help) sed -n '2,13p' "$0" | sed 's/^# \{0,1\}//'; exit 0 ;;
    *) echo "Opción desconocida: $1"; exit 1 ;;
  esac
done

[ "$(uname -s)" = "Linux" ] || [ "$FORZAR" = 1 ] || { echo "Este instalador es para Linux. En macOS usa macos/install.sh."; exit 1; }

DESTINO_APP="$PREFIJO/share/kiwu/app"
LANZADOR="$PREFIJO/bin/kiwu"
ARCHIVO_DESKTOP="$PREFIJO/share/applications/$APP_ID.desktop"
ICONO="$PREFIJO/share/icons/hicolor/256x256/apps/$APP_ID.png"
METAINFO="$PREFIJO/share/metainfo/$APP_ID.metainfo.xml"
PLANTILLA_SEGUNDO_PLANO="$PREFIJO/share/kiwu/kiwu-segundo-plano.desktop"

actualizar_cachés() {
  command -v update-desktop-database >/dev/null 2>&1 && update-desktop-database "$PREFIJO/share/applications" >/dev/null 2>&1 || true
  command -v gtk-update-icon-cache >/dev/null 2>&1 && gtk-update-icon-cache -q -t "$PREFIJO/share/icons/hicolor" >/dev/null 2>&1 || true
}

if [ "$DESINSTALAR" = 1 ]; then
  rm -rf "$PREFIJO/share/kiwu"
  rm -f "$LANZADOR" "$ARCHIVO_DESKTOP" "$ICONO" "$METAINFO"
  actualizar_cachés
  echo "✓ Kiwu desinstalado de $PREFIJO"
  echo "  Tus datos siguen en ~/.local/share/kiwu y ~/.config/kiwu (bórralos a mano si quieres)."
  exit 0
fi

# --- 1. Dependencias -----------------------------------------------------------------------------
if [ "$FORZAR" = 0 ]; then
  if ! command -v python3 >/dev/null 2>&1; then
    echo "✗ Falta python3."; exit 1
  fi
  if ! python3 - <<'PY' >/dev/null 2>&1
import gi
gi.require_version("Gtk", "4.0")
gi.require_version("Adw", "1")
PY
  then
    echo "✗ Faltan dependencias (Python con GTK4 y libadwaita). Instálalas y vuelve a ejecutar:"
    echo
    echo "    Arch:    sudo pacman -S python python-gobject gtk4 libadwaita gtksourceview5"
    echo "    Fedora:  sudo dnf install python3-gobject gtk4 libadwaita gtksourceview5"
    echo "    Debian:  sudo apt install python3-gi gir1.2-gtk-4.0 gir1.2-adw-1 gir1.2-gtksource-5"
    echo
    echo "  (Para saltarte esta comprobación: --forzar)"
    exit 1
  fi
  python3 - <<'PY' >/dev/null 2>&1 || echo "• Aviso: sin GtkSourceView 5 el código se verá sin colores (instala gtksourceview5)."
import gi
gi.require_version("GtkSource", "5")
PY
fi

# --- 2. Localizar el código ----------------------------------------------------------------------
AQUI="$(cd "$(dirname "${BASH_SOURCE[0]:-$0}")" 2>/dev/null && pwd || true)"
TRABAJO=""
cleanup() { [ -n "$TRABAJO" ] && rm -rf "$TRABAJO" || true; }
trap cleanup EXIT

if [ -n "$AQUI" ] && [ -f "$AQUI/kiwu/__init__.py" ] && [ -d "$AQUI/data" ]; then
  FUENTE="$AQUI"
  echo "▸ Instalando desde $FUENTE"
else
  command -v curl >/dev/null 2>&1 || { echo "✗ Falta curl."; exit 1; }
  command -v tar >/dev/null 2>&1 || { echo "✗ Falta tar."; exit 1; }
  TRABAJO="$(mktemp -d)"
  ETIQUETA="$(curl -fsSL "https://api.github.com/repos/$REPO/releases?per_page=30" 2>/dev/null \
    | grep -o '"tag_name": *"[^"]*"' | sed 's/.*"\([^"]*\)"$/\1/' | grep -E '^linux-v[0-9]' | head -1 || true)"
  if [ -n "$ETIQUETA" ]; then
    BASE="https://github.com/$REPO/releases/download/$ETIQUETA"
    echo "▸ Descargando Kiwu para Linux ${ETIQUETA}…"
    curl -fL --progress-bar -o "$TRABAJO/kiwu-linux.tar.gz" "$BASE/kiwu-linux.tar.gz"
    curl -fsSL -o "$TRABAJO/kiwu-linux.tar.gz.sha256" "$BASE/kiwu-linux.tar.gz.sha256"
    ESPERADO="$(cut -d' ' -f1 "$TRABAJO/kiwu-linux.tar.gz.sha256")"
    if command -v sha256sum >/dev/null 2>&1; then OBTENIDO="$(sha256sum "$TRABAJO/kiwu-linux.tar.gz" | cut -d' ' -f1)"
    else OBTENIDO="$(shasum -a 256 "$TRABAJO/kiwu-linux.tar.gz" | cut -d' ' -f1)"; fi
    [ "$ESPERADO" = "$OBTENIDO" ] || { echo "✗ La suma de verificación no coincide. No se instala."; exit 1; }
    mkdir -p "$TRABAJO/src" && tar -xzf "$TRABAJO/kiwu-linux.tar.gz" -C "$TRABAJO/src" --strip-components=1
    FUENTE="$TRABAJO/src"
  else
    echo "▸ Todavía no hay un release de Linux: se instala la versión de desarrollo (rama principal)…"
    curl -fL --progress-bar -o "$TRABAJO/main.tar.gz" "https://github.com/$REPO/archive/refs/heads/main.tar.gz"
    mkdir -p "$TRABAJO/src" && tar -xzf "$TRABAJO/main.tar.gz" -C "$TRABAJO/src" --strip-components=1
    FUENTE="$TRABAJO/src/linux"
  fi
fi
[ -f "$FUENTE/kiwu/__init__.py" ] || { echo "✗ No se encontró el código de Kiwu en $FUENTE"; exit 1; }

# --- 3. Instalar ---------------------------------------------------------------------------------
echo "▸ Instalando en ${PREFIJO}…"
rm -rf "$DESTINO_APP"
mkdir -p "$DESTINO_APP" "$(dirname "$LANZADOR")" "$(dirname "$ARCHIVO_DESKTOP")" "$(dirname "$ICONO")" "$(dirname "$METAINFO")"
cp -R "$FUENTE/kiwu" "$DESTINO_APP/"
find "$DESTINO_APP" -name '__pycache__' -type d -prune -exec rm -rf {} + 2>/dev/null || true

cat > "$LANZADOR" <<EOF
#!/bin/sh
# Lanzador de Kiwu (generado por install.sh)
PYTHONPATH="$DESTINO_APP\${PYTHONPATH:+:\$PYTHONPATH}" exec python3 -m kiwu "\$@"
EOF
chmod +x "$LANZADOR"

sed "s|^Exec=kiwu|Exec=$LANZADOR|" "$FUENTE/data/$APP_ID.desktop" > "$ARCHIVO_DESKTOP"
cp "$FUENTE/data/icons/hicolor/256x256/apps/$APP_ID.png" "$ICONO"
cp "$FUENTE/data/$APP_ID.metainfo.xml" "$METAINFO"
sed "s|^Exec=kiwu|Exec=$LANZADOR|" "$FUENTE/data/kiwu-segundo-plano.desktop" > "$PLANTILLA_SEGUNDO_PLANO"
actualizar_cachés

VERSION="$(sed -n 's/^VERSION = "\(.*\)"/\1/p' "$FUENTE/kiwu/__init__.py")"
echo "✓ Kiwu ${VERSION:-} instalado."
echo "  Ejecutar:        kiwu            (o búscalo en el menú de aplicaciones)"
echo "  Desinstalar:     $0 --desinstalar"
case ":$PATH:" in
  *":$PREFIJO/bin:"*) ;;
  *) echo "  ⚠ $PREFIJO/bin no está en tu PATH; añádelo a tu shell o ejecuta $LANZADOR" ;;
esac
echo "  Recordatorios sin ventana: añade «exec-once = kiwu --segundo-plano» a Hyprland"
echo "  (o copia $PLANTILLA_SEGUNDO_PLANO a ~/.config/autostart/)."

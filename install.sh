#!/bin/bash
# Instala la última versión de Kiwu en /Applications.
#
#   curl -fsSL https://raw.githubusercontent.com/ksebas20500/taskflow/main/install.sh | bash
#
# Descarga el .dmg de la última versión publicada, comprueba su suma de verificación, copia la app
# y quita la marca de cuarentena (la app no está notarizada por Apple, ver el README).
set -euo pipefail

REPO="ksebas20500/taskflow"
BASE="https://github.com/$REPO/releases/latest/download"
DESTINO="${TASKFLOW_DESTINO:-/Applications}"

[ "$(uname -s)" = "Darwin" ] || { echo "Kiwu solo funciona en macOS."; exit 1; }
if [ "$(sw_vers -productVersion | cut -d. -f1)" -lt 13 ]; then
  echo "Kiwu necesita macOS 13 (Ventura) o superior."; exit 1
fi

TRABAJO="$(mktemp -d)"
PUNTO="$TRABAJO/volumen"
cleanup() { hdiutil detach "$PUNTO" -quiet 2>/dev/null || true; rm -rf "$TRABAJO"; }
trap cleanup EXIT

echo "▸ Descargando Kiwu…"
curl -fL --progress-bar -o "$TRABAJO/Kiwu.dmg" "$BASE/Kiwu.dmg"
curl -fsSL -o "$TRABAJO/Kiwu.dmg.sha256" "$BASE/Kiwu.dmg.sha256"

echo "▸ Comprobando la descarga…"
ESPERADO="$(cut -d' ' -f1 "$TRABAJO/Kiwu.dmg.sha256")"
OBTENIDO="$(shasum -a 256 "$TRABAJO/Kiwu.dmg" | cut -d' ' -f1)"
[ "$ESPERADO" = "$OBTENIDO" ] || { echo "✗ La suma de verificación no coincide. No se instala."; exit 1; }

echo "▸ Instalando en $DESTINO…"
mkdir -p "$PUNTO"
hdiutil attach "$TRABAJO/Kiwu.dmg" -mountpoint "$PUNTO" -nobrowse -quiet
osascript -e 'tell application "Kiwu" to quit' >/dev/null 2>&1 || true
osascript -e 'tell application "TaskFlow" to quit' >/dev/null 2>&1 || true
# Kiwu es el nuevo nombre de TaskFlow: se sustituye la app antigua (los datos se migran solos al abrir Kiwu).
rm -rf "$DESTINO/TaskFlow.app"
rm -rf "$DESTINO/Kiwu.app"
cp -R "$PUNTO/Kiwu.app" "$DESTINO/"
xattr -dr com.apple.quarantine "$DESTINO/Kiwu.app" 2>/dev/null || true

echo "✓ Kiwu instalado en $DESTINO/Kiwu.app"
[ -n "${KIWU_NO_ABRIR:-}" ] || open "$DESTINO/Kiwu.app"

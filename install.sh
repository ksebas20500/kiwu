#!/bin/bash
# Instalador de Kiwu: detecta tu sistema (macOS o Linux) y ejecuta el instalador que le corresponde.
#
#   curl -fsSL https://raw.githubusercontent.com/ksebas20500/kiwu/main/install.sh | bash
#
# Cada plataforma tiene su propio instalador, que también puedes usar directamente:
#   macOS:  macos/install.sh      Linux:  linux/install.sh
set -euo pipefail

REPO="ksebas20500/kiwu"
RAMA="${KIWU_RAMA:-main}"

case "$(uname -s)" in
  Darwin) RUTA="macos/install.sh" ;;
  Linux)  RUTA="linux/install.sh" ;;
  *) echo "Kiwu solo está disponible para macOS y Linux."; exit 1 ;;
esac

# Desde una copia del repositorio se usa el instalador local; si no, se descarga el de la rama principal.
AQUI="$(cd "$(dirname "${BASH_SOURCE[0]:-$0}")" 2>/dev/null && pwd || true)"
if [ -n "$AQUI" ] && [ -f "$AQUI/$RUTA" ]; then
  exec bash "$AQUI/$RUTA" "$@"
fi
exec bash -c "$(curl -fsSL "https://raw.githubusercontent.com/$REPO/$RAMA/$RUTA")" kiwu-install "$@"

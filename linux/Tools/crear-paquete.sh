#!/bin/bash
# Crea el paquete descargable de Kiwu para Linux: dist/kiwu-linux.tar.gz (+ .sha256).
#
# Contiene el código, los datos de escritorio (.desktop, icono, metadatos), el PKGBUILD y el instalador.
# Se sube al release con etiqueta «linux-vX.Y.Z» (ver docs/publicar-versiones.md).
#
# Uso:  Tools/crear-paquete.sh
set -euo pipefail

RAIZ="$(cd "$(dirname "$0")/.." && pwd)"
cd "$RAIZ"
SALIDA="$RAIZ/dist"
VERSION="$(sed -n 's/^VERSION = "\(.*\)"/\1/p' kiwu/__init__.py)"
[ -n "$VERSION" ] || { echo "✗ No se pudo leer la versión de kiwu/__init__.py"; exit 1; }

TRABAJO="$(mktemp -d)"
trap 'rm -rf "$TRABAJO"' EXIT
CARPETA="$TRABAJO/kiwu-linux-$VERSION"
mkdir -p "$CARPETA" "$SALIDA"

cp -R kiwu data "$CARPETA/"
cp install.sh README.md pyproject.toml PKGBUILD run.sh "$CARPETA/"
cp ../LICENSE "$CARPETA/LICENSE"
find "$CARPETA" \( -name '__pycache__' -o -name '.DS_Store' \) -prune -exec rm -rf {} + 2>/dev/null || true
chmod +x "$CARPETA/install.sh" "$CARPETA/run.sh"

rm -f "$SALIDA/kiwu-linux.tar.gz" "$SALIDA/kiwu-linux.tar.gz.sha256"
# COPYFILE_DISABLE evita que macOS añada archivos «._*» de metadatos al empaquetar
COPYFILE_DISABLE=1 tar -czf "$SALIDA/kiwu-linux.tar.gz" -C "$TRABAJO" "kiwu-linux-$VERSION"
( cd "$SALIDA" && { sha256sum kiwu-linux.tar.gz 2>/dev/null || shasum -a 256 kiwu-linux.tar.gz; } > kiwu-linux.tar.gz.sha256 )

echo "✓ Kiwu para Linux $VERSION"
echo "  $(du -h "$SALIDA/kiwu-linux.tar.gz" | cut -f1)  $SALIDA/kiwu-linux.tar.gz"

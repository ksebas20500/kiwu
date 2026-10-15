#!/bin/bash
# Comprueba que el modelo de notas de macOS cumple el contrato de docs/contrato (sin abrir Xcode).
# Uso: macos/Tools/probar-contrato.sh
set -euo pipefail

raiz="$(cd "$(dirname "$0")/../.." && pwd)"
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT

swiftc -parse-as-library \
    "$raiz/macos/Kiwu/Models/DomainModels.swift" \
    "$raiz/macos/Tools/probar-contrato.swift" \
    -o "$tmp/probar-contrato"

"$tmp/probar-contrato" "$raiz/docs/contrato/notas"

#!/bin/bash
# Ejecuta Kiwu desde el código fuente (sin instalar). Uso: ./run.sh [opciones de kiwu]
cd "$(dirname "$0")" && exec python3 -m kiwu "$@"

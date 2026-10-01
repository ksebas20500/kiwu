"""Rutas de datos, configuración y caché (estándar XDG)."""

import os
from pathlib import Path

_NOMBRE = "kiwu"


def _base(variable: str, relativa: str) -> Path:
    valor = os.environ.get(variable)
    return Path(valor) if valor else Path.home() / relativa


def _crear(ruta: Path) -> Path:
    ruta.mkdir(parents=True, exist_ok=True)
    return ruta


def carpeta_datos() -> Path:
    """`~/.local/share/kiwu` (se puede cambiar con KIWU_DATOS, útil para pruebas)."""
    forzada = os.environ.get("KIWU_DATOS")
    return _crear(Path(forzada) if forzada else _base("XDG_DATA_HOME", ".local/share") / _NOMBRE)


def carpeta_config() -> Path:
    forzada = os.environ.get("KIWU_CONFIG")
    return _crear(Path(forzada) if forzada else _base("XDG_CONFIG_HOME", ".config") / _NOMBRE)


def carpeta_cache() -> Path:
    forzada = os.environ.get("KIWU_CACHE")
    return _crear(Path(forzada) if forzada else _base("XDG_CACHE_HOME", ".cache") / _NOMBRE)


def ruta_db() -> Path:
    return carpeta_datos() / "kiwu.db"


def ruta_ajustes() -> Path:
    return carpeta_config() / "settings.json"


def ruta_fondo() -> Path:
    return carpeta_datos() / "fondo"

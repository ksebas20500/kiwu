"""Preferencias del usuario: atajos, tema, fondo y translucidez. Se guardan en `~/.config/kiwu/settings.json`."""

from __future__ import annotations

import json
from pathlib import Path
from typing import Callable, Optional

from . import rutas

# Acciones con atajo editable. Formato de GTK: «<Control>n», «<Control><Alt>l», …
ACCIONES = {
    "ir-tareas": "Ir a Tareas",
    "ir-notas": "Ir a Notas",
    "nueva-tarea": "Nueva tarea",
    "vista-lista": "Vista de lista",
    "vista-tablero": "Vista de tablero",
    "ir-hoy": "Ir a Hoy",
    "ir-calendario": "Ir al Calendario",
    "abrir-ajustes": "Abrir ajustes",
}

ATAJOS_PREDETERMINADOS = {
    "ir-tareas": "<Control>1",
    "ir-notas": "<Control>2",
    "nueva-tarea": "<Control>n",
    "vista-lista": "<Control><Alt>l",
    "vista-tablero": "<Control><Alt>b",
    "ir-hoy": "<Control><Shift>t",
    "ir-calendario": "<Control><Shift>c",
    "abrir-ajustes": "<Control>comma",
}

PREDETERMINADOS = {
    "tema": "sistema",                 # sistema | claro | oscuro
    "fondo_visibilidad": 0.5,          # 0..1: cuánto se ve la imagen de fondo
    "opacidad_paneles": 0.45,          # 0..1: velo que cubre la imagen para que el contenido se lea
    "desenfoque": 0.0,                 # 0..30 px
    "opacidad_ventana": 1.0,           # 0.3..1: translucidez real de la ventana (el desenfoque lo pone el compositor)
    "barra_de_titulo": True,           # False = ventana sin decoración (útil en Hyprland)
    "vista_tareas": "lista",           # lista | tablero
    "modulo": "tareas",                # tareas | notas
    "atajos": {},
}


class Ajustes:
    def __init__(self, ruta: Optional[Path] = None):
        self.ruta = Path(ruta) if ruta else rutas.ruta_ajustes()
        self.datos: dict = {}
        self._observadores: list[Callable[[str], None]] = []
        self.cargar()

    def cargar(self) -> None:
        guardado: dict = {}
        try:
            guardado = json.loads(self.ruta.read_text(encoding="utf-8"))
            if not isinstance(guardado, dict):
                guardado = {}
        except (OSError, ValueError):
            guardado = {}
        self.datos = {**PREDETERMINADOS, **{k: v for k, v in guardado.items() if k in PREDETERMINADOS}}
        self.datos["atajos"] = {k: v for k, v in (guardado.get("atajos") or {}).items() if k in ACCIONES}

    def guardar(self) -> None:
        try:
            self.ruta.parent.mkdir(parents=True, exist_ok=True)
            self.ruta.write_text(json.dumps(self.datos, ensure_ascii=False, indent=2), encoding="utf-8")
        except OSError:
            pass  # no poder guardar ajustes no debe romper la app

    def suscribir(self, funcion: Callable[[str], None]) -> None:
        self._observadores.append(funcion)

    def __getitem__(self, clave: str):
        return self.datos.get(clave, PREDETERMINADOS.get(clave))

    def __setitem__(self, clave: str, valor) -> None:
        if clave not in PREDETERMINADOS:
            raise KeyError(clave)
        if self.datos.get(clave) == valor:
            return
        self.datos[clave] = valor
        self.guardar()
        for f in list(self._observadores):
            f(clave)

    # --- atajos -------------------------------------------------------------------------------

    def atajo(self, accion: str) -> str:
        return self.datos["atajos"].get(accion) or ATAJOS_PREDETERMINADOS[accion]

    def fijar_atajo(self, accion: str, acelerador: Optional[str]) -> None:
        if accion not in ACCIONES:
            raise KeyError(accion)
        if acelerador is None or acelerador == ATAJOS_PREDETERMINADOS[accion]:
            self.datos["atajos"].pop(accion, None)
        else:
            self.datos["atajos"][accion] = acelerador
        self.guardar()
        for f in list(self._observadores):
            f("atajos")

    def conflicto(self, acelerador: str, excepto: str) -> Optional[str]:
        for accion in ACCIONES:
            if accion != excepto and self.atajo(accion) == acelerador:
                return accion
        return None

    def restaurar_atajos(self) -> None:
        self.datos["atajos"] = {}
        self.guardar()
        for f in list(self._observadores):
            f("atajos")

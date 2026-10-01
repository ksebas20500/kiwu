"""Reglas de negocio de las tareas (equivalente a `DeberService` y `ContentView` de macOS)."""

from __future__ import annotations

import time
from datetime import date, datetime, timedelta
from enum import IntEnum
from typing import Optional

from .db import BaseDeDatos
from .modelos import Deber, Documento


class Columna(IntEnum):
    POR_HACER = 0
    EN_CURSO = 1
    HECHO = 2

    @property
    def titulo(self) -> str:
        return {0: "Por hacer", 1: "En curso", 2: "Hecho"}[int(self)]


# Elementos de la barra lateral: ("hoy",), ("proximos",), ("calendario",), ("todos",), ("completadas",),
# ("materia", id)
HOY = ("hoy",)
PROXIMOS = ("proximos",)
CALENDARIO = ("calendario",)
TODOS = ("todos",)
COMPLETADAS = ("completadas",)


def materia_item(id_: str) -> tuple:
    return ("materia", id_)


def inicio_del_dia(ts: float) -> float:
    d = datetime.fromtimestamp(ts)
    return datetime(d.year, d.month, d.day).timestamp()


def fin_del_dia(ts: float) -> float:
    """Último segundo del día (mismo criterio que `Calendar.finDelDia` en macOS)."""
    d = datetime.fromtimestamp(ts)
    siguiente = datetime(d.year, d.month, d.day) + timedelta(days=1)
    return siguiente.timestamp() - 1


def columna_de(d: Deber) -> Columna:
    if d.completado:
        return Columna.HECHO
    return Columna.EN_CURSO if d.estado == 1 else Columna.POR_HACER


def esta_vencido(d: Deber, ahora: Optional[float] = None) -> bool:
    ahora = time.time() if ahora is None else ahora
    return (not d.completado) and d.fecha_entrega is not None and d.fecha_entrega < ahora


class Servicio:
    def __init__(self, db: BaseDeDatos):
        self.db = db

    # --- consultas ----------------------------------------------------------------------------

    def en_alcance(self, d: Deber, item: tuple, ahora: Optional[float] = None) -> bool:
        ahora = time.time() if ahora is None else ahora
        tipo = item[0]
        if tipo in ("todos", "calendario"):
            return True
        if tipo == "completadas":
            return d.completado
        if tipo == "materia":
            return d.materia_id == item[1]
        if d.fecha_entrega is None:
            return False
        if tipo == "hoy":
            return d.fecha_entrega <= fin_del_dia(ahora)
        if tipo == "proximos":
            limite = fin_del_dia((datetime.fromtimestamp(ahora) + timedelta(days=6)).timestamp())
            return inicio_del_dia(ahora) <= d.fecha_entrega <= limite
        return False

    def pendientes(self, item: tuple, ahora: Optional[float] = None) -> list[Deber]:
        lista = [d for d in self.db.deberes() if not d.completado and self.en_alcance(d, item, ahora)]
        return sorted(lista, key=lambda d: (d.fecha_entrega if d.fecha_entrega is not None else float("inf"), d.creada))

    def completados(self, item: tuple, ahora: Optional[float] = None) -> list[Deber]:
        ahora = time.time() if ahora is None else ahora
        deberes = self.db.deberes()
        tipo = item[0]
        if tipo == "hoy":
            hoy = inicio_del_dia(ahora)
            lista = [d for d in deberes if d.completado and d.fecha_completado is not None
                     and inicio_del_dia(d.fecha_completado) == hoy]
        elif tipo in ("proximos", "calendario"):
            lista = []
        else:
            lista = [d for d in deberes if d.completado and self.en_alcance(d, item, ahora)]
        return sorted(lista, key=lambda d: d.fecha_completado or 0, reverse=True)

    def conteo(self, item: tuple, ahora: Optional[float] = None) -> int:
        if item[0] in ("calendario", "completadas"):
            return 0
        return len(self.pendientes(item, ahora))

    def del_dia(self, dia: date) -> list[Deber]:
        inicio = datetime(dia.year, dia.month, dia.day).timestamp()
        fin = inicio + 86400
        lista = [d for d in self.db.deberes() if d.fecha_entrega is not None and inicio <= d.fecha_entrega < fin]
        return sorted(lista, key=lambda d: d.fecha_entrega)

    # --- acciones -----------------------------------------------------------------------------

    def crear(self, titulo: str, materia_id: str, fecha: Optional[float] = None) -> Deber:
        return self.db.crear_deber(titulo, materia_id, fecha)

    def marcar(self, d: Deber, completado: bool) -> None:
        campos = {"completado": completado, "fecha_completado": time.time() if completado else None}
        if not completado and d.estado != 1:
            campos["estado"] = 0
        self.db.actualizar_deber(d.id, **campos)

    def mover(self, d: Deber, columna: Columna) -> None:
        """Mueve la tarea a una columna del tablero."""
        if columna_de(d) == columna:
            return
        if columna == Columna.HECHO:
            self.db.actualizar_deber(d.id, completado=True, fecha_completado=time.time())
        else:
            self.db.actualizar_deber(d.id, estado=int(columna), completado=False, fecha_completado=None)

    def mover_a_materia(self, d: Deber, materia_id: str) -> None:
        self.db.actualizar_deber(d.id, materia_id=materia_id)

    def poner_fecha(self, d: Deber, fecha: Optional[float]) -> None:
        # Al cambiar la fecha, el recordatorio vuelve a poder dispararse.
        self.db.actualizar_deber(d.id, fecha_entrega=fecha, recordado=None)

    def documento_de(self, d: Deber) -> Documento:
        return Documento.de_json(d.nota)

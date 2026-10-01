"""Recordatorios: qué tareas toca avisar ahora. La parte gráfica (la notificación) vive en la interfaz."""

from __future__ import annotations

import time
from typing import Optional

from .db import BaseDeDatos
from .modelos import Deber

# No se avisa de entregas que vencieron hace más de este tiempo (p. ej. al abrir la app tras días).
GRACIA_SEGUNDOS = 3600


def pendientes_de_aviso(db: BaseDeDatos, ahora: Optional[float] = None) -> list[Deber]:
    """Tareas cuyo aviso (fecha de entrega menos N horas) ya llegó y todavía no se ha notificado."""
    ahora = time.time() if ahora is None else ahora
    salida = []
    for d in db.deberes():
        if d.completado or d.fecha_entrega is None or d.recordatorio_horas <= 0:
            continue
        aviso = d.fecha_entrega - d.recordatorio_horas * 3600
        if aviso <= ahora <= d.fecha_entrega + GRACIA_SEGUNDOS and (d.recordado is None or d.recordado < aviso):
            salida.append(d)
    return sorted(salida, key=lambda d: d.fecha_entrega)


def marcar_avisado(db: BaseDeDatos, d: Deber, ahora: Optional[float] = None) -> None:
    db.actualizar_deber(d.id, silencioso=True, recordado=time.time() if ahora is None else ahora)


def texto_aviso(d: Deber, ahora: Optional[float] = None) -> str:
    """«Vence en 5 h», «Vence mañana», «Venció hace 10 min»…"""
    ahora = time.time() if ahora is None else ahora
    if d.fecha_entrega is None:
        return ""
    resto = d.fecha_entrega - ahora
    if resto < 0:
        minutos = int(-resto // 60)
        return "Acaba de vencer" if minutos < 1 else f"Venció hace {minutos} min"
    horas = resto / 3600
    if horas < 1:
        return f"Vence en {max(1, int(resto // 60))} min"
    if horas < 24:
        return f"Vence en {int(horas)} h"
    dias = int(horas // 24)
    return "Vence mañana" if dias == 1 else f"Vence en {dias} días"

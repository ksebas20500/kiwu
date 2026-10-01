"""Importar y exportar datos.

- `importar_macos`: lee directamente las bases de datos de Kiwu/TaskFlow para macOS
  (`GestorUniversitario.sqlite` y `Notas.sqlite`, que son almacenes de Core Data).
- `exportar_copia` / `importar_copia`: copia de seguridad `.kiwu.json` para mover datos entre equipos.
"""

from __future__ import annotations

import json
import sqlite3
import uuid
from datetime import datetime, timezone
from pathlib import Path
from typing import Optional

from .db import BaseDeDatos
from .modelos import Documento

# Core Data guarda las fechas como segundos desde el 1 de enero de 2001.
DESFASE_COREDATA = 978307200.0
FORMATO_COPIA = 1


class ErrorImportacion(Exception):
    pass


# --- macOS (Core Data) -------------------------------------------------------------------------

def _uuid(valor) -> str:
    if isinstance(valor, (bytes, bytearray)) and len(valor) == 16:
        return str(uuid.UUID(bytes=bytes(valor))).upper()
    if isinstance(valor, str) and valor:
        return valor.upper()
    return str(uuid.uuid4()).upper()


def _fecha(valor) -> Optional[float]:
    return None if valor is None else float(valor) + DESFASE_COREDATA


def _texto_json(valor) -> str:
    if valor is None:
        return '{"blocks":[]}'
    if isinstance(valor, (bytes, bytearray)):
        valor = bytes(valor).decode("utf-8", errors="replace")
    return Documento.de_json(valor).a_json()


def _abrir_lectura(ruta: Path) -> sqlite3.Connection:
    if not ruta.exists():
        raise ErrorImportacion(f"No existe el archivo: {ruta}")
    con = sqlite3.connect(f"file:{ruta}?mode=ro", uri=True)
    con.row_factory = sqlite3.Row
    return con


def _tiene_tabla(con: sqlite3.Connection, nombre: str) -> bool:
    return con.execute("SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = ?", (nombre,)).fetchone() is not None


def importar_macos(db: BaseDeDatos, tareas: Optional[Path | str] = None, notas: Optional[Path | str] = None) -> dict:
    """Copia listas, tareas, cuadernos y páginas. No pisa lo que ya exista (mismo id).

    Devuelve cuántos elementos nuevos se importaron de cada tipo.
    """
    if tareas is None and notas is None:
        raise ErrorImportacion("Indica al menos una base de datos")
    resumen = {"materias": 0, "deberes": 0, "cuadernos": 0, "paginas": 0}

    if tareas is not None:
        con = _abrir_lectura(Path(tareas))
        try:
            pk_materia: dict[int, str] = {}
            filas_materia = []
            if _tiene_tabla(con, "ZMATERIA"):
                for f in con.execute("SELECT * FROM ZMATERIA WHERE ZNOMBRE IS NOT NULL ORDER BY ZORDEN"):
                    id_ = _uuid(f["ZID"])
                    pk_materia[f["Z_PK"]] = id_
                    creada = _fecha(f["ZFECHACREACION"]) or 0.0
                    filas_materia.append({
                        "id": id_, "nombre": f["ZNOMBRE"], "icono": "book", "orden": f["ZORDEN"] or 0,
                        "carpeta": None, "creada": creada, "actualizada": _fecha(f["ZFECHAACTUALIZACION"]) or creada,
                    })
            resumen["materias"] = db.insertar_filas("materia", filas_materia)

            filas_deber = []
            if _tiene_tabla(con, "ZDEBER"):
                for f in con.execute("SELECT * FROM ZDEBER WHERE ZTITULO IS NOT NULL"):
                    materia = pk_materia.get(f["ZMATERIA"])
                    if materia is None:
                        continue
                    creada = _fecha(f["ZFECHACREACION"]) or 0.0
                    filas_deber.append({
                        "id": _uuid(f["ZID"]), "materia_id": materia, "titulo": f["ZTITULO"],
                        "completado": 1 if f["ZCOMPLETADO"] else 0, "estado": f["ZESTADO"] or 0,
                        "fecha_entrega": _fecha(f["ZFECHAENTREGA"]),
                        "recordatorio_horas": f["ZRECORDATORIOANTESENHORAS"] if f["ZRECORDATORIOANTESENHORAS"] is not None else 24,
                        "fecha_completado": _fecha(f["ZFECHACOMPLETADO"]),
                        "nota": _texto_json(f["ZCONTENIDONOTA"]),
                        "creada": creada, "actualizada": _fecha(f["ZFECHAACTUALIZACION"]) or creada, "recordado": None,
                    })
            resumen["deberes"] = db.insertar_filas("deber", filas_deber)
        finally:
            con.close()

    if notas is not None:
        con = _abrir_lectura(Path(notas))
        try:
            pk_cuaderno: dict[int, str] = {}
            filas_cuaderno = []
            if _tiene_tabla(con, "ZCUADERNO"):
                for f in con.execute("SELECT * FROM ZCUADERNO WHERE ZNOMBRE IS NOT NULL ORDER BY ZORDEN"):
                    id_ = _uuid(f["ZID"])
                    pk_cuaderno[f["Z_PK"]] = id_
                    creada = _fecha(f["ZFECHACREACION"]) or 0.0
                    filas_cuaderno.append({
                        "id": id_, "nombre": f["ZNOMBRE"], "icono": f["ZICONO"] or "📓", "orden": f["ZORDEN"] or 0,
                        "creada": creada, "actualizada": _fecha(f["ZFECHAACTUALIZACION"]) or creada,
                    })
            resumen["cuadernos"] = db.insertar_filas("cuaderno", filas_cuaderno)

            filas_pagina = []
            if _tiene_tabla(con, "ZPAGINA"):
                for f in con.execute("SELECT * FROM ZPAGINA"):
                    cuaderno = pk_cuaderno.get(f["ZCUADERNO"])
                    if cuaderno is None:
                        continue
                    creada = _fecha(f["ZFECHACREACION"]) or 0.0
                    filas_pagina.append({
                        "id": _uuid(f["ZID"]), "cuaderno_id": cuaderno, "titulo": f["ZTITULO"] or "",
                        "icono": f["ZICONO"] or "", "contenido": _texto_json(f["ZCONTENIDO"]),
                        "texto_plano": f["ZTEXTOPLANO"] or "", "favorita": 1 if f["ZFAVORITA"] else 0,
                        "creada": creada, "actualizada": _fecha(f["ZFECHAACTUALIZACION"]) or creada,
                    })
            resumen["paginas"] = db.insertar_filas("pagina", filas_pagina)
        finally:
            con.close()
    return resumen


# --- Copia de seguridad .kiwu.json ---------------------------------------------------------------

def _iso(ts: Optional[float]) -> Optional[str]:
    return None if ts is None else datetime.fromtimestamp(ts, tz=timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")


def _desde_iso(texto: Optional[str]) -> Optional[float]:
    if not texto:
        return None
    try:
        return datetime.fromisoformat(texto.replace("Z", "+00:00")).timestamp()
    except ValueError:
        return None


def exportar_copia(db: BaseDeDatos, ruta: Path | str) -> None:
    datos = {
        "formato": FORMATO_COPIA,
        "app": "kiwu",
        "exportado": _iso(datetime.now().timestamp()),
        "materias": [
            {"id": m.id, "nombre": m.nombre, "orden": m.orden, "carpeta": m.carpeta,
             "creada": _iso(m.creada), "actualizada": _iso(m.actualizada)}
            for m in db.materias()
        ],
        "deberes": [
            {"id": d.id, "materia": d.materia_id, "titulo": d.titulo, "completado": d.completado, "estado": d.estado,
             "fechaEntrega": _iso(d.fecha_entrega), "recordatorioHoras": d.recordatorio_horas,
             "fechaCompletado": _iso(d.fecha_completado), "nota": json.loads(Documento.de_json(d.nota).a_json()),
             "creada": _iso(d.creada), "actualizada": _iso(d.actualizada)}
            for d in db.deberes()
        ],
        "cuadernos": [
            {"id": c.id, "nombre": c.nombre, "icono": c.icono, "orden": c.orden,
             "creada": _iso(c.creada), "actualizada": _iso(c.actualizada)}
            for c in db.cuadernos()
        ],
        "paginas": [
            {"id": p.id, "cuaderno": p.cuaderno_id, "titulo": p.titulo, "icono": p.icono, "favorita": p.favorita,
             "contenido": json.loads(Documento.de_json(p.contenido).a_json()),
             "creada": _iso(p.creada), "actualizada": _iso(p.actualizada)}
            for p in db.paginas()
        ],
    }
    Path(ruta).write_text(json.dumps(datos, ensure_ascii=False, indent=2), encoding="utf-8")


def importar_copia(db: BaseDeDatos, ruta: Path | str) -> dict:
    try:
        datos = json.loads(Path(ruta).read_text(encoding="utf-8"))
    except (OSError, ValueError) as e:
        raise ErrorImportacion(f"No se pudo leer la copia: {e}") from e
    if not isinstance(datos, dict) or datos.get("app") != "kiwu":
        raise ErrorImportacion("El archivo no es una copia de seguridad de Kiwu")
    if int(datos.get("formato", 0)) > FORMATO_COPIA:
        raise ErrorImportacion("La copia es de una versión más nueva de Kiwu")

    def contenido(valor) -> str:
        return Documento.de_json(valor).a_json()

    materias = [
        {"id": m["id"], "nombre": m["nombre"], "icono": "book", "orden": m.get("orden", 0), "carpeta": m.get("carpeta"),
         "creada": _desde_iso(m.get("creada")) or 0.0, "actualizada": _desde_iso(m.get("actualizada")) or 0.0}
        for m in datos.get("materias", [])
    ]
    ids_materia = {m["id"] for m in materias} | db.ids("materia")
    deberes = [
        {"id": d["id"], "materia_id": d["materia"], "titulo": d["titulo"], "completado": 1 if d.get("completado") else 0,
         "estado": d.get("estado", 0), "fecha_entrega": _desde_iso(d.get("fechaEntrega")),
         "recordatorio_horas": d.get("recordatorioHoras", 24), "fecha_completado": _desde_iso(d.get("fechaCompletado")),
         "nota": contenido(d.get("nota")), "creada": _desde_iso(d.get("creada")) or 0.0,
         "actualizada": _desde_iso(d.get("actualizada")) or 0.0, "recordado": None}
        for d in datos.get("deberes", []) if d.get("materia") in ids_materia
    ]
    cuadernos = [
        {"id": c["id"], "nombre": c["nombre"], "icono": c.get("icono", "📓"), "orden": c.get("orden", 0),
         "creada": _desde_iso(c.get("creada")) or 0.0, "actualizada": _desde_iso(c.get("actualizada")) or 0.0}
        for c in datos.get("cuadernos", [])
    ]
    ids_cuaderno = {c["id"] for c in cuadernos} | db.ids("cuaderno")
    paginas = []
    for p in datos.get("paginas", []):
        if p.get("cuaderno") not in ids_cuaderno:
            continue
        doc = Documento.de_json(p.get("contenido"))
        paginas.append({
            "id": p["id"], "cuaderno_id": p["cuaderno"], "titulo": p.get("titulo", ""), "icono": p.get("icono", ""),
            "contenido": doc.a_json(), "texto_plano": doc.texto_plano(), "favorita": 1 if p.get("favorita") else 0,
            "creada": _desde_iso(p.get("creada")) or 0.0, "actualizada": _desde_iso(p.get("actualizada")) or 0.0,
        })
    return {
        "materias": db.insertar_filas("materia", materias),
        "deberes": db.insertar_filas("deber", deberes),
        "cuadernos": db.insertar_filas("cuaderno", cuadernos),
        "paginas": db.insertar_filas("pagina", paginas),
    }

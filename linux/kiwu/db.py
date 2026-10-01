"""Base de datos local (SQLite). Un único archivo: `~/.local/share/kiwu/kiwu.db`."""

from __future__ import annotations

import sqlite3
import time
from pathlib import Path
from typing import Callable, Iterable, Optional

from .modelos import Cuaderno, Deber, Materia, Pagina, nuevo_id

VERSION_ESQUEMA = 1

ESQUEMA = """
CREATE TABLE IF NOT EXISTS materia (
    id TEXT PRIMARY KEY,
    nombre TEXT NOT NULL,
    icono TEXT NOT NULL DEFAULT 'book',
    orden INTEGER NOT NULL DEFAULT 0,
    carpeta TEXT,
    creada REAL NOT NULL,
    actualizada REAL NOT NULL
);
CREATE TABLE IF NOT EXISTS deber (
    id TEXT PRIMARY KEY,
    materia_id TEXT NOT NULL REFERENCES materia(id) ON DELETE CASCADE,
    titulo TEXT NOT NULL,
    completado INTEGER NOT NULL DEFAULT 0,
    estado INTEGER NOT NULL DEFAULT 0,
    fecha_entrega REAL,
    recordatorio_horas INTEGER NOT NULL DEFAULT 24,
    fecha_completado REAL,
    nota TEXT NOT NULL DEFAULT '{"blocks":[]}',
    creada REAL NOT NULL,
    actualizada REAL NOT NULL,
    recordado REAL
);
CREATE INDEX IF NOT EXISTS deber_materia ON deber(materia_id);
CREATE INDEX IF NOT EXISTS deber_fecha ON deber(fecha_entrega);
CREATE TABLE IF NOT EXISTS cuaderno (
    id TEXT PRIMARY KEY,
    nombre TEXT NOT NULL,
    icono TEXT NOT NULL DEFAULT '📓',
    orden INTEGER NOT NULL DEFAULT 0,
    creada REAL NOT NULL,
    actualizada REAL NOT NULL
);
CREATE TABLE IF NOT EXISTS pagina (
    id TEXT PRIMARY KEY,
    cuaderno_id TEXT NOT NULL REFERENCES cuaderno(id) ON DELETE CASCADE,
    titulo TEXT NOT NULL DEFAULT '',
    icono TEXT NOT NULL DEFAULT '',
    contenido TEXT NOT NULL DEFAULT '{"blocks":[]}',
    texto_plano TEXT NOT NULL DEFAULT '',
    favorita INTEGER NOT NULL DEFAULT 0,
    creada REAL NOT NULL,
    actualizada REAL NOT NULL
);
CREATE INDEX IF NOT EXISTS pagina_cuaderno ON pagina(cuaderno_id);
"""

CAMPOS_DEBER = {
    "titulo", "completado", "estado", "fecha_entrega", "recordatorio_horas", "fecha_completado",
    "nota", "materia_id", "recordado",
}
CAMPOS_PAGINA = {"titulo", "icono", "contenido", "texto_plano", "favorita", "cuaderno_id"}


def _deber(f: sqlite3.Row) -> Deber:
    return Deber(
        id=f["id"], materia_id=f["materia_id"], titulo=f["titulo"], completado=bool(f["completado"]),
        estado=f["estado"], fecha_entrega=f["fecha_entrega"], recordatorio_horas=f["recordatorio_horas"],
        fecha_completado=f["fecha_completado"], nota=f["nota"], creada=f["creada"],
        actualizada=f["actualizada"], recordado=f["recordado"],
    )


def _pagina(f: sqlite3.Row) -> Pagina:
    return Pagina(
        id=f["id"], cuaderno_id=f["cuaderno_id"], titulo=f["titulo"], icono=f["icono"],
        contenido=f["contenido"], texto_plano=f["texto_plano"], favorita=bool(f["favorita"]),
        creada=f["creada"], actualizada=f["actualizada"],
    )


class BaseDeDatos:
    """Acceso a los datos. Las vistas se suscriben con `suscribir` para refrescarse al haber cambios."""

    def __init__(self, ruta: Optional[Path | str] = ":memory:"):
        self.con = sqlite3.connect(str(ruta), check_same_thread=False)
        self.con.row_factory = sqlite3.Row
        self.con.execute("PRAGMA foreign_keys = ON")
        if str(ruta) != ":memory:":
            self.con.execute("PRAGMA journal_mode = WAL")
        self._observadores: list[Callable[[str], None]] = []
        self._migrar()

    # --- infraestructura ----------------------------------------------------------------------

    def _migrar(self) -> None:
        version = self.con.execute("PRAGMA user_version").fetchone()[0]
        if version < 1:
            self.con.executescript(ESQUEMA)
        self.con.execute(f"PRAGMA user_version = {VERSION_ESQUEMA}")
        self.con.commit()

    def suscribir(self, funcion: Callable[[str], None]) -> None:
        self._observadores.append(funcion)

    def _avisar(self, tipo: str) -> None:
        for funcion in list(self._observadores):
            funcion(tipo)

    def cerrar(self) -> None:
        self.con.close()

    # --- materias -----------------------------------------------------------------------------

    def materias(self) -> list[Materia]:
        filas = self.con.execute("SELECT * FROM materia ORDER BY orden, nombre COLLATE NOCASE").fetchall()
        return [Materia(**dict(f)) for f in filas]

    def materia(self, id_: str) -> Optional[Materia]:
        f = self.con.execute("SELECT * FROM materia WHERE id = ?", (id_,)).fetchone()
        return Materia(**dict(f)) if f else None

    def crear_materia(self, nombre: str, id_: Optional[str] = None) -> Materia:
        ahora = time.time()
        orden = self.con.execute("SELECT COALESCE(MAX(orden), -1) + 1 FROM materia").fetchone()[0]
        m = Materia(id=id_ or nuevo_id(), nombre=nombre.strip(), orden=orden, creada=ahora, actualizada=ahora)
        self.con.execute(
            "INSERT INTO materia (id, nombre, icono, orden, carpeta, creada, actualizada) VALUES (?,?,?,?,?,?,?)",
            (m.id, m.nombre, m.icono, m.orden, m.carpeta, m.creada, m.actualizada))
        self.con.commit()
        self._avisar("materia")
        return m

    def renombrar_materia(self, id_: str, nombre: str) -> None:
        self.con.execute("UPDATE materia SET nombre = ?, actualizada = ? WHERE id = ?", (nombre.strip(), time.time(), id_))
        self.con.commit()
        self._avisar("materia")

    def vincular_carpeta(self, id_: str, carpeta: Optional[str]) -> None:
        self.con.execute("UPDATE materia SET carpeta = ?, actualizada = ? WHERE id = ?", (carpeta, time.time(), id_))
        self.con.commit()
        self._avisar("materia")

    def eliminar_materia(self, id_: str) -> None:
        self.con.execute("DELETE FROM materia WHERE id = ?", (id_,))
        self.con.commit()
        self._avisar("materia")

    # --- deberes ------------------------------------------------------------------------------

    def deberes(self) -> list[Deber]:
        return [_deber(f) for f in self.con.execute("SELECT * FROM deber").fetchall()]

    def deber(self, id_: str) -> Optional[Deber]:
        f = self.con.execute("SELECT * FROM deber WHERE id = ?", (id_,)).fetchone()
        return _deber(f) if f else None

    def crear_deber(self, titulo: str, materia_id: str, fecha: Optional[float] = None,
                    id_: Optional[str] = None) -> Deber:
        ahora = time.time()
        d = Deber(id=id_ or nuevo_id(), materia_id=materia_id, titulo=titulo.strip(), fecha_entrega=fecha,
                  creada=ahora, actualizada=ahora)
        self.con.execute(
            "INSERT INTO deber (id, materia_id, titulo, completado, estado, fecha_entrega, recordatorio_horas,"
            " fecha_completado, nota, creada, actualizada, recordado) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)",
            (d.id, d.materia_id, d.titulo, 0, 0, d.fecha_entrega, d.recordatorio_horas, None, d.nota,
             d.creada, d.actualizada, None))
        self.con.commit()
        self._avisar("deber")
        return d

    def actualizar_deber(self, id_: str, silencioso: bool = False, **campos) -> None:
        desconocidos = set(campos) - CAMPOS_DEBER
        if desconocidos:
            raise ValueError(f"Campos no válidos: {desconocidos}")
        if not campos:
            return
        campos["actualizada"] = time.time()
        asignaciones = ", ".join(f"{k} = ?" for k in campos)
        valores = [int(v) if isinstance(v, bool) else v for v in campos.values()]
        self.con.execute(f"UPDATE deber SET {asignaciones} WHERE id = ?", (*valores, id_))
        self.con.commit()
        if not silencioso:
            self._avisar("deber")

    def eliminar_deber(self, id_: str) -> None:
        self.con.execute("DELETE FROM deber WHERE id = ?", (id_,))
        self.con.commit()
        self._avisar("deber")

    # --- cuadernos y páginas ------------------------------------------------------------------

    def cuadernos(self) -> list[Cuaderno]:
        filas = self.con.execute("SELECT * FROM cuaderno ORDER BY orden, nombre COLLATE NOCASE").fetchall()
        return [Cuaderno(**dict(f)) for f in filas]

    def cuaderno(self, id_: str) -> Optional[Cuaderno]:
        f = self.con.execute("SELECT * FROM cuaderno WHERE id = ?", (id_,)).fetchone()
        return Cuaderno(**dict(f)) if f else None

    def crear_cuaderno(self, nombre: str, icono: str = "📓", id_: Optional[str] = None) -> Cuaderno:
        ahora = time.time()
        orden = self.con.execute("SELECT COALESCE(MAX(orden), -1) + 1 FROM cuaderno").fetchone()[0]
        c = Cuaderno(id=id_ or nuevo_id(), nombre=nombre.strip(), icono=icono, orden=orden, creada=ahora, actualizada=ahora)
        self.con.execute("INSERT INTO cuaderno (id, nombre, icono, orden, creada, actualizada) VALUES (?,?,?,?,?,?)",
                         (c.id, c.nombre, c.icono, c.orden, c.creada, c.actualizada))
        self.con.commit()
        self._avisar("cuaderno")
        return c

    def editar_cuaderno(self, id_: str, nombre: str, icono: str) -> None:
        self.con.execute("UPDATE cuaderno SET nombre = ?, icono = ?, actualizada = ? WHERE id = ?",
                         (nombre.strip(), icono, time.time(), id_))
        self.con.commit()
        self._avisar("cuaderno")

    def eliminar_cuaderno(self, id_: str) -> None:
        self.con.execute("DELETE FROM cuaderno WHERE id = ?", (id_,))
        self.con.commit()
        self._avisar("cuaderno")

    def paginas(self) -> list[Pagina]:
        filas = self.con.execute("SELECT * FROM pagina ORDER BY actualizada DESC").fetchall()
        return [_pagina(f) for f in filas]

    def pagina(self, id_: str) -> Optional[Pagina]:
        f = self.con.execute("SELECT * FROM pagina WHERE id = ?", (id_,)).fetchone()
        return _pagina(f) if f else None

    def crear_pagina(self, cuaderno_id: str, id_: Optional[str] = None) -> Pagina:
        ahora = time.time()
        p = Pagina(id=id_ or nuevo_id(), cuaderno_id=cuaderno_id, creada=ahora, actualizada=ahora)
        self.con.execute(
            "INSERT INTO pagina (id, cuaderno_id, titulo, icono, contenido, texto_plano, favorita, creada, actualizada)"
            " VALUES (?,?,?,?,?,?,?,?,?)",
            (p.id, p.cuaderno_id, p.titulo, p.icono, p.contenido, p.texto_plano, 0, p.creada, p.actualizada))
        self.con.commit()
        self._avisar("pagina")
        return p

    def actualizar_pagina(self, id_: str, silencioso: bool = False, **campos) -> None:
        desconocidos = set(campos) - CAMPOS_PAGINA
        if desconocidos:
            raise ValueError(f"Campos no válidos: {desconocidos}")
        if not campos:
            return
        campos["actualizada"] = time.time()
        asignaciones = ", ".join(f"{k} = ?" for k in campos)
        valores = [int(v) if isinstance(v, bool) else v for v in campos.values()]
        self.con.execute(f"UPDATE pagina SET {asignaciones} WHERE id = ?", (*valores, id_))
        self.con.commit()
        if not silencioso:
            self._avisar("pagina")

    def eliminar_pagina(self, id_: str) -> None:
        self.con.execute("DELETE FROM pagina WHERE id = ?", (id_,))
        self.con.commit()
        self._avisar("pagina")

    # --- utilidades ---------------------------------------------------------------------------

    def esta_vacia(self) -> bool:
        return all(
            self.con.execute(f"SELECT COUNT(*) FROM {t}").fetchone()[0] == 0
            for t in ("materia", "deber", "cuaderno", "pagina")
        )

    def ids(self, tabla: str) -> set[str]:
        if tabla not in {"materia", "deber", "cuaderno", "pagina"}:
            raise ValueError(tabla)
        return {f[0] for f in self.con.execute(f"SELECT id FROM {tabla}").fetchall()}

    def insertar_filas(self, tabla: str, filas: Iterable[dict]) -> int:
        """Inserción masiva para importaciones (ignora los id que ya existen)."""
        if tabla not in {"materia", "deber", "cuaderno", "pagina"}:
            raise ValueError(tabla)
        total = 0
        for fila in filas:
            columnas = ", ".join(fila)
            marcas = ", ".join("?" for _ in fila)
            cursor = self.con.execute(f"INSERT OR IGNORE INTO {tabla} ({columnas}) VALUES ({marcas})", tuple(fila.values()))
            total += cursor.rowcount
        self.con.commit()
        self._avisar("importacion")
        return total

"""Texto con formato sobre `Gtk.TextBuffer`.

Todos los bloques comparten una misma tabla de etiquetas (negrita, cursiva, color…). El formato se guarda
como lista de `TextRun`, igual que en Kiwu para macOS.
"""

from __future__ import annotations

from typing import Optional

import gi

gi.require_version("Gtk", "4.0")
gi.require_version("Adw", "1")
from gi.repository import Adw, Gdk, Gtk, Pango  # noqa: E402

from ...modelos import TextRun, fusionar_runs, runs_o_nada  # noqa: E402

# nombre -> (RGB en tema claro, RGB en tema oscuro). Mismos colores que en macOS.
COLORES = {
    "gris": ((0.45, 0.45, 0.45), (0.62, 0.62, 0.62)),
    "marron": ((0.60, 0.40, 0.27), (0.76, 0.56, 0.42)),
    "naranja": ((0.85, 0.45, 0.05), (0.96, 0.62, 0.27)),
    "amarillo": ((0.74, 0.58, 0.00), (0.96, 0.80, 0.27)),
    "verde": ((0.18, 0.55, 0.34), (0.42, 0.76, 0.52)),
    "azul": ((0.20, 0.45, 0.80), (0.42, 0.66, 0.96)),
    "morado": ((0.55, 0.35, 0.75), (0.72, 0.57, 0.92)),
    "rosa": ((0.80, 0.30, 0.55), (0.96, 0.52, 0.72)),
    "rojo": ((0.80, 0.25, 0.20), (0.96, 0.46, 0.42)),
}
TITULOS_COLOR = {"gris": "Gris", "marron": "Marrón", "naranja": "Naranja", "amarillo": "Amarillo", "verde": "Verde",
                 "azul": "Azul", "morado": "Morado", "rosa": "Rosa", "rojo": "Rojo"}

ETIQUETAS_SIMPLES = ("b", "i", "u", "s", "code")
ETIQUETA_HECHO = "hecho"          # tachado de las tareas marcadas; no se guarda como formato


def rgba(rgb, alfa: float = 1.0) -> Gdk.RGBA:
    c = Gdk.RGBA()
    c.red, c.green, c.blue, c.alpha = rgb[0], rgb[1], rgb[2], alfa
    return c


def _oscuro() -> bool:
    return Adw.StyleManager.get_default().get_dark()


_tabla: Optional[Gtk.TextTagTable] = None


def tabla() -> Gtk.TextTagTable:
    """Tabla de etiquetas compartida por todos los bloques."""
    global _tabla
    if _tabla is not None:
        return _tabla
    t = Gtk.TextTagTable()
    t.add(Gtk.TextTag(name="b", weight=Pango.Weight.BOLD))
    t.add(Gtk.TextTag(name="i", style=Pango.Style.ITALIC))
    t.add(Gtk.TextTag(name="u", underline=Pango.Underline.SINGLE))
    t.add(Gtk.TextTag(name="s", strikethrough=True))
    t.add(Gtk.TextTag(name="code", family="monospace", scale=0.92))
    for nombre in COLORES:
        t.add(Gtk.TextTag(name=f"c:{nombre}"))
        t.add(Gtk.TextTag(name=f"h:{nombre}"))
    t.add(Gtk.TextTag(name=ETIQUETA_HECHO, strikethrough=True))
    _tabla = t
    _pintar_colores(_oscuro())
    Adw.StyleManager.get_default().connect("notify::dark", lambda gestor, _p: _pintar_colores(gestor.get_dark()))
    return t


def _pintar_colores(oscuro: bool) -> None:
    t = _tabla
    if t is None:
        return
    for nombre, (claro, os) in COLORES.items():
        rgb = os if oscuro else claro
        t.lookup(f"c:{nombre}").set_property("foreground-rgba", rgba(rgb))
        t.lookup(f"h:{nombre}").set_property("background-rgba", rgba(rgb, 0.32 if oscuro else 0.24))
    code = t.lookup("code")
    code.set_property("background-rgba", rgba((0.5, 0.5, 0.5), 0.18))
    code.set_property("foreground-rgba", rgba(COLORES["rojo"][1 if oscuro else 0]))
    t.lookup(ETIQUETA_HECHO).set_property("foreground-rgba", rgba((0.5, 0.5, 0.5)))
    for e in _enlaces:
        e.set_property("foreground-rgba", rgba(COLORES["azul"][1 if oscuro else 0]))


_enlaces: list[Gtk.TextTag] = []


def etiqueta_enlace(url: str) -> Gtk.TextTag:
    """Una etiqueta por dirección (`link:<url>`)."""
    t = tabla()
    nombre = f"link:{url}"
    existente = t.lookup(nombre)
    if existente is not None:
        return existente
    tag = Gtk.TextTag(name=nombre, underline=Pango.Underline.SINGLE)
    tag.set_property("foreground-rgba", rgba(COLORES["azul"][1 if _oscuro() else 0]))
    t.add(tag)
    _enlaces.append(tag)
    return tag


def nombres_de_run(run: TextRun) -> list[str]:
    nombres = []
    if run.bold:
        nombres.append("b")
    if run.italic:
        nombres.append("i")
    if run.underline:
        nombres.append("u")
    if run.strike:
        nombres.append("s")
    if run.code:
        nombres.append("code")
    if run.color in COLORES:
        nombres.append(f"c:{run.color}")
    if run.highlight in COLORES:
        nombres.append(f"h:{run.highlight}")
    if run.link:
        etiqueta_enlace(run.link)
        nombres.append(f"link:{run.link}")
    return nombres


def run_desde_nombres(texto: str, nombres) -> TextRun:
    r = TextRun(texto)
    for n in nombres:
        if n == "b":
            r.bold = True
        elif n == "i":
            r.italic = True
        elif n == "u":
            r.underline = True
        elif n == "s":
            r.strike = True
        elif n == "code":
            r.code = True
        elif n.startswith("c:"):
            r.color = n[2:]
        elif n.startswith("h:"):
            r.highlight = n[2:]
        elif n.startswith("link:"):
            r.link = n[5:]
    return r


def runs_de_rango(buf: Gtk.TextBuffer, ini: Gtk.TextIter, fin: Gtk.TextIter) -> list[TextRun]:
    runs: list[TextRun] = []
    it = ini.copy()
    while it.compare(fin) < 0:
        sig = it.copy()
        if not sig.forward_to_tag_toggle(None) or sig.compare(fin) > 0:
            sig = fin.copy()
        texto = buf.get_text(it, sig, False)
        nombres = [t.get_property("name") for t in it.get_tags()]
        runs.append(run_desde_nombres(texto, nombres))
        it = sig
    return fusionar_runs(runs)


def buffer_a_runs(buf: Gtk.TextBuffer):
    """Formato del texto entero; None si no tiene ninguno."""
    ini, fin = buf.get_bounds()
    return runs_o_nada(runs_de_rango(buf, ini, fin))


def cargar(buf: Gtk.TextBuffer, texto: str, runs) -> None:
    """Pone el texto y su formato en el buffer (sin crear pasos de deshacer)."""
    buf.begin_irreversible_action()
    try:
        buf.set_text(texto, -1)
        if runs and "".join(r.text for r in runs) == texto:
            pos = 0
            for run in runs:
                n = len(run.text)
                if n and not run.es_plano():
                    ini = buf.get_iter_at_offset(pos)
                    fin = buf.get_iter_at_offset(pos + n)
                    for nombre in nombres_de_run(run):
                        buf.apply_tag_by_name(nombre, ini, fin)
                pos += n
    finally:
        buf.end_irreversible_action()


def _es_formato(nombre: Optional[str]) -> bool:
    return bool(nombre) and (nombre in ETIQUETAS_SIMPLES or nombre.startswith(("c:", "h:")))


class Formato:
    """Formato que se aplicará a lo que el usuario escriba (como los atributos de escritura de un procesador de texto)."""

    def __init__(self, buf: Gtk.TextBuffer):
        self.buf = buf
        self.activos: set[str] = set()
        self._instantanea: set[str] = set()
        self._insertando = False
        self.hecho = False                     # tarea marcada: todo el texto va tachado
        buf.connect("insert-text", self._antes)
        buf.connect_after("insert-text", self._despues)
        buf.connect("mark-set", self._marca)

    # --- eventos ------------------------------------------------------------------------------

    def _antes(self, _buf, _ubicacion, _texto, _longitud):
        self._insertando = True
        self._instantanea = set(self.activos)

    def _despues(self, buf, ubicacion, texto, _longitud):
        try:
            fin = ubicacion.copy()
            ini = ubicacion.copy()
            ini.backward_chars(len(texto))
            t = tabla()
            for nombre in list(ETIQUETAS_SIMPLES) + [f"{p}:{c}" for c in COLORES for p in ("c", "h")]:
                buf.remove_tag(t.lookup(nombre), ini, fin)
            for nombre in self._instantanea:
                etiqueta = t.lookup(nombre)
                if etiqueta is not None:
                    buf.apply_tag(etiqueta, ini, fin)
            if self.hecho:
                buf.apply_tag_by_name(ETIQUETA_HECHO, ini, fin)
        finally:
            self._insertando = False

    def _marca(self, buf, _it, marca):
        if self._insertando or marca is not buf.get_insert():
            return
        self.activos = self._formato_antes_del_cursor()

    def _formato_antes_del_cursor(self) -> set[str]:
        it = self.buf.get_iter_at_mark(self.buf.get_insert())
        if not it.is_start():
            it.backward_char()
        return {t.get_property("name") for t in it.get_tags() if _es_formato(t.get_property("name"))}

    # --- operaciones --------------------------------------------------------------------------

    def _seleccion(self):
        s = self.buf.get_selection_bounds()
        return s if s else None

    @staticmethod
    def _cubierto(etiqueta: Gtk.TextTag, ini: Gtk.TextIter, fin: Gtk.TextIter) -> bool:
        it = ini.copy()
        if not it.has_tag(etiqueta):
            return False
        return not (it.forward_to_tag_toggle(etiqueta) and it.compare(fin) < 0)

    def alternar(self, nombre: str) -> None:
        s = self._seleccion()
        if not s:
            self.activos.symmetric_difference_update({nombre})
            return
        ini, fin = s
        etiqueta = tabla().lookup(nombre)
        if self._cubierto(etiqueta, ini, fin):
            self.buf.remove_tag(etiqueta, ini, fin)
        else:
            self.buf.apply_tag(etiqueta, ini, fin)

    def colorear(self, color: Optional[str], fondo: bool) -> None:
        prefijo = "h" if fondo else "c"
        s = self._seleccion()
        if not s:
            self.activos = {n for n in self.activos if not n.startswith(prefijo + ":")}
            if color:
                self.activos.add(f"{prefijo}:{color}")
            return
        ini, fin = s
        t = tabla()
        for nombre in COLORES:
            self.buf.remove_tag(t.lookup(f"{prefijo}:{nombre}"), ini, fin)
        if color:
            self.buf.apply_tag(t.lookup(f"{prefijo}:{color}"), ini, fin)

    def enlazar(self, url: Optional[str]) -> None:
        s = self._seleccion()
        if not s:
            return
        ini, fin = s
        self.quitar_enlaces(ini, fin)
        if url:
            self.buf.apply_tag(etiqueta_enlace(url), ini, fin)

    def quitar_enlaces(self, ini, fin) -> None:
        for etiqueta in list(_enlaces):
            self.buf.remove_tag(etiqueta, ini, fin)

    def limpiar(self) -> None:
        s = self._seleccion()
        if not s:
            self.activos.clear()
            return
        ini, fin = s
        t = tabla()
        for nombre in list(ETIQUETAS_SIMPLES) + [f"{p}:{c}" for c in COLORES for p in ("c", "h")]:
            self.buf.remove_tag(t.lookup(nombre), ini, fin)
        self.quitar_enlaces(ini, fin)

    def enlace_en_seleccion(self) -> Optional[str]:
        s = self._seleccion()
        if not s:
            return None
        for t in s[0].get_tags():
            nombre = t.get_property("name")
            if nombre and nombre.startswith("link:"):
                return nombre[5:]
        return None

    def poner_hecho(self, hecho: bool) -> None:
        self.hecho = hecho
        ini, fin = self.buf.get_bounds()
        if hecho:
            self.buf.apply_tag_by_name(ETIQUETA_HECHO, ini, fin)
        else:
            self.buf.remove_tag_by_name(ETIQUETA_HECHO, ini, fin)

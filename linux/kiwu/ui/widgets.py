"""Componentes reutilizables de las vistas de tareas."""

from __future__ import annotations

from typing import Callable, Optional

import gi

gi.require_version("Gtk", "4.0")
from gi.repository import Gtk  # noqa: E402

from ..modelos import Materia  # noqa: E402
from .utiles import etiqueta  # noqa: E402


class SelectorVista(Gtk.Box):
    """Conmutador Lista / Tablero."""

    def __init__(self, vista: str, al_cambiar: Callable[[str], None]):
        super().__init__(spacing=2)
        self.add_css_class("kiwu-selector-vista")
        self._al_cambiar = al_cambiar
        self.b_lista = Gtk.ToggleButton(icon_name="view-list-symbolic")
        self.b_tablero = Gtk.ToggleButton(icon_name="view-grid-symbolic")
        self.b_tablero.set_group(self.b_lista)
        self.b_lista.set_tooltip_text("Vista de lista")
        self.b_tablero.set_tooltip_text("Vista de tablero")
        for b in (self.b_lista, self.b_tablero):
            b.add_css_class("flat")
            self.append(b)
        self.fijar(vista)
        self.b_lista.connect("toggled", lambda b: b.get_active() and self._al_cambiar("lista"))
        self.b_tablero.connect("toggled", lambda b: b.get_active() and self._al_cambiar("tablero"))

    def fijar(self, vista: str) -> None:
        (self.b_tablero if vista == "tablero" else self.b_lista).set_active(True)


class SelectorSub(Gtk.Box):
    """Pestañas «Tareas | Archivos» de una lista."""

    def __init__(self, actual: str, alerta_carpeta: bool, al_cambiar: Callable[[str], None]):
        super().__init__()
        self.add_css_class("linked")
        self.b_tareas = Gtk.ToggleButton(label="Tareas")
        self.b_archivos = Gtk.ToggleButton(label="Archivos")
        self.b_archivos.set_group(self.b_tareas)
        if alerta_carpeta:
            self.b_archivos.set_label("Archivos ⚠")
            self.b_archivos.set_tooltip_text("La carpeta vinculada no está disponible")
        (self.b_archivos if actual == "archivos" else self.b_tareas).set_active(True)
        self.append(self.b_tareas)
        self.append(self.b_archivos)
        self.b_tareas.connect("toggled", lambda b: b.get_active() and al_cambiar("tareas"))
        self.b_archivos.connect("toggled", lambda b: b.get_active() and al_cambiar("archivos"))
        self.set_halign(Gtk.Align.START)


class CampoNuevaTarea(Gtk.Box):
    """Campo «Añadir tarea» con selector de lista (si no hay una lista fija)."""

    def __init__(self, al_crear: Callable[[str, str], None], marcador: str = "Añadir tarea"):
        super().__init__(spacing=8)
        self.add_css_class("kiwu-campo-nueva")
        self.al_crear = al_crear
        self._marcador = marcador
        self._materias: list[Materia] = []
        self._fija: Optional[Materia] = None
        self.append(Gtk.Image.new_from_icon_name("list-add-symbolic"))
        self.entrada = Gtk.Entry(hexpand=True, placeholder_text=marcador)
        self.entrada.connect("activate", self._activado)
        self.append(self.entrada)
        self.selector = Gtk.DropDown.new_from_strings(["—"])
        self.selector.add_css_class("flat")
        self.append(self.selector)

    def actualizar(self, materias: list[Materia], fija: Optional[Materia], permite: bool = True) -> None:
        self.set_visible(permite)
        self._materias, self._fija = materias, fija
        anterior = self.destino_id()
        nombres = [m.nombre for m in materias] or ["—"]
        self.selector.set_model(Gtk.StringList.new(nombres))
        if anterior:
            for i, m in enumerate(materias):
                if m.id == anterior:
                    self.selector.set_selected(i)
        self.selector.set_visible(fija is None and bool(materias))
        self.entrada.set_sensitive(bool(materias))
        self.entrada.set_placeholder_text(self._marcador if materias else "Crea una lista primero")

    def destino_id(self) -> Optional[str]:
        if self._fija is not None:
            return self._fija.id
        if not self._materias:
            return None
        i = self.selector.get_selected()
        return self._materias[i].id if 0 <= i < len(self._materias) else self._materias[0].id

    def enfocar(self) -> None:
        self.entrada.grab_focus()

    def _activado(self, _e) -> None:
        titulo = self.entrada.get_text().strip()
        destino = self.destino_id()
        if titulo and destino:
            self.entrada.set_text("")
            self.al_crear(titulo, destino)


def encabezado(titulo: str, selector: Optional[Gtk.Widget] = None) -> Gtk.Box:
    """Título grande de la vista con el selector Lista/Tablero a la derecha."""
    caja = Gtk.Box(spacing=12)
    caja.set_margin_top(20)
    caja.set_margin_start(20)
    caja.set_margin_end(20)
    caja.set_margin_bottom(10)
    t = etiqueta(titulo, ["kiwu-titulo"], elipsis=True)
    t.set_hexpand(True)
    caja.append(t)
    if selector is not None:
        selector.set_valign(Gtk.Align.CENTER)
        caja.append(selector)
    return caja

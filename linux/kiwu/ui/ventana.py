"""Ventana principal: barra de módulos, tareas, notas y ajustes."""

from __future__ import annotations

import gi

gi.require_version("Gtk", "4.0")
gi.require_version("Adw", "1")
from gi.repository import Adw, Gio, GLib, Gtk  # noqa: E402

from ..ajustes import ACCIONES, Ajustes  # noqa: E402
from ..db import BaseDeDatos  # noqa: E402
from ..servicios import CALENDARIO, HOY, Servicio  # noqa: E402
from .ajustes_dialogo import DialogoAjustes  # noqa: E402
from .estilos import Fondo  # noqa: E402
from .notas import VistaNotas  # noqa: E402
from .vista_tareas import VistaTareas  # noqa: E402


class Ventana(Adw.ApplicationWindow):
    def __init__(self, app, db: BaseDeDatos, servicio: Servicio, ajustes: Ajustes):
        super().__init__(application=app, title="Kiwu", default_width=1240, default_height=760)
        self.app, self.db, self.servicio, self.ajustes = app, db, servicio, ajustes
        self.set_size_request(640, 420)
        self.add_css_class("kiwu-ventana")
        self._dialogo: DialogoAjustes | None = None

        self.cabecera = Adw.HeaderBar()
        self.cabecera.set_title_widget(Adw.WindowTitle(title="Kiwu", subtitle=""))
        self.fondo = Fondo(ajustes)

        # Barra de módulos (izquierda)
        riel = Gtk.Box(orientation=Gtk.Orientation.VERTICAL, spacing=10)
        riel.add_css_class("kiwu-rail")
        riel.set_size_request(76, -1)
        riel.set_margin_top(0)
        self.b_tareas = self._boton_modulo("object-select-symbolic", "Tareas")
        self.b_notas = self._boton_modulo("document-edit-symbolic", "Notas")
        self.b_notas.set_group(self.b_tareas)
        riel.append(self._centrado(self.b_tareas, 14))
        riel.append(self._centrado(self.b_notas, 0))
        espacio = Gtk.Box()
        espacio.set_vexpand(True)
        riel.append(espacio)
        ajustes_btn = Gtk.Button.new_from_icon_name("emblem-system-symbolic")
        ajustes_btn.add_css_class("flat")
        ajustes_btn.add_css_class("kiwu-boton-modulo")
        ajustes_btn.set_tooltip_text("Ajustes")
        ajustes_btn.connect("clicked", lambda _b: self.abrir_ajustes())
        riel.append(self._centrado(ajustes_btn, 0, abajo=12))

        self.tareas = VistaTareas(self, db, servicio, ajustes)
        self.notas = VistaNotas(self, db)
        self.pila = Gtk.Stack()
        self.pila.add_named(self.tareas, "tareas")
        self.pila.add_named(self.notas, "notas")
        self.pila.set_hexpand(True)

        cuerpo = Gtk.Box()
        cuerpo.add_css_class("kiwu-panel")
        cuerpo.append(riel)
        cuerpo.append(Gtk.Separator(orientation=Gtk.Orientation.VERTICAL))
        cuerpo.append(self.pila)

        superposicion = Gtk.Overlay()
        superposicion.set_child(self.fondo)
        superposicion.add_overlay(cuerpo)
        superposicion.set_measure_overlay(cuerpo, True)

        marco = Adw.ToolbarView()
        marco.add_top_bar(self.cabecera)
        marco.set_content(superposicion)
        self.set_content(marco)

        self.b_tareas.connect("toggled", lambda b: b.get_active() and self.ir_a_modulo("tareas"))
        self.b_notas.connect("toggled", lambda b: b.get_active() and self.ir_a_modulo("notas"))
        self._crear_acciones()
        self.aplicar_atajos()
        self.aplicar_ajustes("barra_de_titulo")
        ajustes.suscribir(self.aplicar_ajustes)
        self.ir_a_modulo(ajustes["modulo"] if ajustes["modulo"] in ("tareas", "notas") else "tareas")
        self.connect("close-request", self._al_cerrar)

    # --- construcción ---------------------------------------------------------------------------

    @staticmethod
    def _boton_modulo(icono: str, ayuda: str) -> Gtk.ToggleButton:
        b = Gtk.ToggleButton()
        imagen = Gtk.Image.new_from_icon_name(icono)
        imagen.set_pixel_size(22)
        b.set_child(imagen)
        b.add_css_class("flat")
        b.add_css_class("kiwu-boton-modulo")
        b.set_tooltip_text(ayuda)
        return b

    @staticmethod
    def _centrado(widget: Gtk.Widget, arriba: int, abajo: int = 0) -> Gtk.Widget:
        widget.set_halign(Gtk.Align.CENTER)
        widget.set_margin_top(arriba)
        widget.set_margin_bottom(abajo)
        return widget

    # --- acciones y atajos ----------------------------------------------------------------------

    def _crear_acciones(self) -> None:
        funciones = {
            "ir-tareas": lambda: self.ir_a_modulo("tareas"),
            "ir-notas": lambda: self.ir_a_modulo("notas"),
            "nueva-tarea": self.nueva_tarea,
            "vista-lista": lambda: self._vista("lista"),
            "vista-tablero": lambda: self._vista("tablero"),
            "ir-hoy": lambda: (self.ir_a_modulo("tareas"), self.tareas.ir_a(HOY)),
            "ir-calendario": lambda: (self.ir_a_modulo("tareas"), self.tareas.ir_a(CALENDARIO)),
            "abrir-ajustes": self.abrir_ajustes,
        }
        for nombre in ACCIONES:
            accion = Gio.SimpleAction.new(nombre, None)
            accion.connect("activate", lambda _a, _p, f=funciones[nombre]: f())
            self.add_action(accion)

    def aplicar_atajos(self) -> None:
        for nombre in ACCIONES:
            self.app.set_accels_for_action(f"win.{nombre}", [self.ajustes.atajo(nombre)])

    def aplicar_ajustes(self, clave: str) -> None:
        if clave == "atajos":
            self.aplicar_atajos()
        elif clave == "barra_de_titulo":
            self.cabecera.set_visible(bool(self.ajustes["barra_de_titulo"]))

    def ir_a_modulo(self, modulo: str) -> None:
        self.pila.set_visible_child_name(modulo)
        (self.b_notas if modulo == "notas" else self.b_tareas).set_active(True)
        self.ajustes["modulo"] = modulo

    def nueva_tarea(self) -> None:
        self.ir_a_modulo("tareas")
        GLib.idle_add(lambda: (self.tareas.nueva_tarea_enfocada(), False)[1])

    def _vista(self, vista: str) -> None:
        self.ir_a_modulo("tareas")
        self.tareas.cambiar_vista(vista)

    def abrir_tarea(self, id_: str) -> None:
        self.ir_a_modulo("tareas")
        self.tareas.abrir_tarea(id_)
        self.present()

    def abrir_ajustes(self) -> None:
        if self._dialogo is not None:
            self._dialogo.present()
            return
        self._dialogo = DialogoAjustes(self, self.ajustes, self.db)
        self._dialogo.connect("close-request", self._ajustes_cerrados)
        self._dialogo.present()

    def _ajustes_cerrados(self, _d) -> bool:
        self._dialogo = None
        self.tareas.refrescar()
        return False

    def _al_cerrar(self, _v) -> bool:
        self.tareas.guardar_todo()
        self.notas.guardar_todo()
        return False

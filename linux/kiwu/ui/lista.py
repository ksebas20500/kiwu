"""Vista de lista de tareas."""

from __future__ import annotations

from typing import Optional

import gi

gi.require_version("Gtk", "4.0")
from gi.repository import Gdk, Gtk, Pango  # noqa: E402

from ..modelos import Deber, Materia  # noqa: E402
from ..servicios import Columna, columna_de, esta_vencido  # noqa: E402
from .utiles import etiqueta, fecha_corta, limpiar, menu_contextual  # noqa: E402
from .widgets import CampoNuevaTarea, SelectorSub, SelectorVista, encabezado  # noqa: E402


def tachado(texto: str, activo: bool) -> Gtk.Label:
    e = etiqueta(texto, elipsis=True)
    if activo:
        atributos = Pango.AttrList()
        atributos.insert(Pango.attr_strikethrough_new(True))
        e.set_attributes(atributos)
    return e


def opciones_de_menu(ctx, d: Deber) -> list:
    """Entradas del menú contextual de una tarea (lista y tablero)."""
    items = []
    for c in Columna:
        if columna_de(d) != c:
            items.append((f"Mover a «{c.titulo}»", lambda c=c: ctx.servicio.mover(d, c)))
    items.append(None)
    items.append(("Eliminar tarea", lambda: ctx.eliminar_tarea(d)))
    return items


class ListaTareas(Gtk.Box):
    def __init__(self, ctx):
        super().__init__(orientation=Gtk.Orientation.VERTICAL)
        self.ctx = ctx
        self.completadas_visibles = True
        self.cabecera = Gtk.Box(orientation=Gtk.Orientation.VERTICAL)
        self.append(self.cabecera)
        self.campo = CampoNuevaTarea(ctx.crear_tarea)
        self.campo.set_margin_start(20)
        self.campo.set_margin_end(20)
        self.campo.set_margin_bottom(10)
        self.append(self.campo)
        self.contenido = Gtk.Box(orientation=Gtk.Orientation.VERTICAL, spacing=2)
        self.contenido.set_margin_start(10)
        self.contenido.set_margin_end(10)
        self.contenido.set_margin_bottom(20)
        self.scroll = Gtk.ScrolledWindow()
        self.scroll.set_child(self.contenido)
        self.scroll.set_vexpand(True)
        self.scroll.set_policy(Gtk.PolicyType.NEVER, Gtk.PolicyType.AUTOMATIC)
        self.append(self.scroll)
        self._ultimos: Optional[tuple] = None

    def enfocar_nueva(self) -> None:
        self.campo.enfocar()

    def refrescar(self, titulo: str, pendientes: list[Deber], completados: list[Deber], materias: list[Materia],
                  fija: Optional[Materia], permite_crear: bool) -> None:
        self._ultimos = (titulo, pendientes, completados, materias, fija, permite_crear)
        nombres = {m.id: m.nombre for m in materias}
        mostrar_materia = fija is None

        limpiar(self.cabecera)
        self.cabecera.append(encabezado(titulo, SelectorVista(self.ctx.vista_tareas, self.ctx.cambiar_vista)))
        if fija is not None:
            sub = SelectorSub(self.ctx.sub, bool(fija.carpeta) and not self.ctx.carpeta_disponible(fija),
                              self.ctx.cambiar_sub)
            sub.set_margin_start(20)
            sub.set_margin_bottom(10)
            self.cabecera.append(sub)

        self.campo.actualizar(materias, fija, permite_crear)

        limpiar(self.contenido)
        for d in pendientes:
            self.contenido.append(self._fila(d, nombres.get(d.materia_id) if mostrar_materia else None))
        if completados:
            boton = Gtk.Button()
            boton.add_css_class("flat")
            caja = Gtk.Box(spacing=6)
            caja.append(Gtk.Image.new_from_icon_name("pan-down-symbolic" if self.completadas_visibles
                                                     else "pan-end-symbolic"))
            caja.append(etiqueta("Completada", ["heading"]))
            caja.append(etiqueta(str(len(completados)), ["kiwu-suave"]))
            boton.set_child(caja)
            boton.set_halign(Gtk.Align.START)
            boton.set_margin_top(14)
            boton.connect("clicked", self._alternar_completadas)
            self.contenido.append(boton)
            if self.completadas_visibles:
                for d in completados:
                    self.contenido.append(self._fila(d, nombres.get(d.materia_id) if mostrar_materia else None))
        if not pendientes and not completados:
            vacio = Gtk.Box(orientation=Gtk.Orientation.VERTICAL, spacing=8)
            vacio.set_valign(Gtk.Align.CENTER)
            vacio.set_vexpand(True)
            vacio.set_margin_top(80)
            icono = Gtk.Image.new_from_icon_name("object-select-symbolic")
            icono.set_pixel_size(40)
            icono.add_css_class("kiwu-suave")
            vacio.append(icono)
            vacio.append(etiqueta("Sin tareas", ["title-3", "kiwu-suave"], xalign=0.5))
            self.contenido.append(vacio)

    def _alternar_completadas(self, _b) -> None:
        self.completadas_visibles = not self.completadas_visibles
        if self._ultimos:
            self.refrescar(*self._ultimos)

    def _fila(self, d: Deber, materia: Optional[str]) -> Gtk.Widget:
        fila = Gtk.Box(spacing=10)
        fila.add_css_class("kiwu-fila")
        if self.ctx.seleccion == d.id:
            fila.add_css_class("seleccionada")
        if d.completado:
            fila.add_css_class("kiwu-hecha")

        casilla = Gtk.CheckButton()
        casilla.set_active(d.completado)
        casilla.connect("toggled", lambda c, d=d: self.ctx.servicio.marcar(d, c.get_active()))
        fila.append(casilla)

        titulo = tachado(d.titulo, d.completado)
        titulo.set_hexpand(True)
        fila.append(titulo)
        if columna_de(d) == Columna.EN_CURSO:
            fila.append(etiqueta("En curso", ["kiwu-chip"]))
        if materia:
            fila.append(etiqueta(materia, ["kiwu-minimo"]))
        if d.fecha_entrega is not None:
            fila.append(etiqueta(fecha_corta(d.fecha_entrega),
                                 ["kiwu-minimo"] + (["kiwu-vencida"] if esta_vencido(d) else [])))

        clic = Gtk.GestureClick(button=Gdk.BUTTON_PRIMARY)
        clic.connect("released", lambda *_: self.ctx.seleccionar(d.id))
        fila.add_controller(clic)
        menu_contextual(fila, lambda d=d: opciones_de_menu(self.ctx, d))
        return fila

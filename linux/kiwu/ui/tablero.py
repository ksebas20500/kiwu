"""Vista de tablero: Por hacer · En curso · Hecho, con arrastrar y soltar."""

from __future__ import annotations

from typing import Optional

import gi

gi.require_version("Gtk", "4.0")
from gi.repository import Gdk, GObject, Gtk, Pango  # noqa: E402

from ..modelos import Deber, Materia  # noqa: E402
from ..servicios import Columna, columna_de, esta_vencido  # noqa: E402
from .lista import opciones_de_menu, tachado  # noqa: E402
from .utiles import etiqueta, fecha_corta, limpiar, menu_contextual  # noqa: E402
from .widgets import CampoNuevaTarea, SelectorSub, SelectorVista, encabezado  # noqa: E402


class ColumnaTablero(Gtk.Box):
    def __init__(self, ctx, columna: Columna):
        super().__init__(orientation=Gtk.Orientation.VERTICAL, spacing=8)
        self.ctx = ctx
        self.columna = columna
        self.add_css_class("kiwu-columna")
        self.set_hexpand(True)
        self.set_size_request(210, -1)
        self.titulo = Gtk.Box(spacing=6)
        self.append(self.titulo)
        self.tarjetas = Gtk.Box(orientation=Gtk.Orientation.VERTICAL, spacing=8)
        scroll = Gtk.ScrolledWindow()
        scroll.set_child(self.tarjetas)
        scroll.set_vexpand(True)
        scroll.set_policy(Gtk.PolicyType.NEVER, Gtk.PolicyType.AUTOMATIC)
        self.append(scroll)

        destino = Gtk.DropTarget.new(GObject.TYPE_STRING, Gdk.DragAction.MOVE)
        destino.connect("drop", self._soltado)
        destino.connect("enter", self._entra)
        destino.connect("leave", self._sale)
        self.add_controller(destino)

    def _entra(self, *_a):
        self.add_css_class("resaltada")
        return Gdk.DragAction.MOVE

    def _sale(self, *_a) -> None:
        self.remove_css_class("resaltada")

    def _soltado(self, _destino, id_tarea, _x, _y) -> bool:
        self.remove_css_class("resaltada")
        d = self.ctx.db.deber(str(id_tarea))
        if d is None:
            return False
        self.ctx.servicio.mover(d, self.columna)
        return True

    def rellenar(self, tarjetas: list[Deber], nombres: dict[str, str], mostrar_materia: bool) -> None:
        limpiar(self.titulo)
        self.titulo.append(etiqueta(self.columna.titulo, ["heading", "kiwu-suave"]))
        self.titulo.append(etiqueta(str(len(tarjetas)), ["kiwu-minimo"]))
        limpiar(self.tarjetas)
        for d in tarjetas:
            self.tarjetas.append(self._tarjeta(d, nombres.get(d.materia_id) if mostrar_materia else None))
        if not tarjetas:
            self.tarjetas.append(etiqueta("Arrastra tareas aquí", ["kiwu-minimo"], xalign=0.5))

    def _tarjeta(self, d: Deber, materia: Optional[str]) -> Gtk.Widget:
        caja = Gtk.Box(spacing=10)
        caja.add_css_class("kiwu-tarjeta")
        if self.ctx.seleccion == d.id:
            caja.add_css_class("seleccionada")
        if d.completado:
            caja.add_css_class("kiwu-hecha")
        casilla = Gtk.CheckButton()
        casilla.set_active(d.completado)
        casilla.set_valign(Gtk.Align.START)
        casilla.connect("toggled", lambda c, d=d: self.ctx.servicio.marcar(d, c.get_active()))
        caja.append(casilla)

        textos = Gtk.Box(orientation=Gtk.Orientation.VERTICAL, spacing=3)
        textos.set_hexpand(True)
        titulo = tachado(d.titulo, d.completado)
        titulo.set_ellipsize(Pango.EllipsizeMode.NONE)
        titulo.set_wrap(True)
        titulo.set_xalign(0)
        textos.append(titulo)
        detalle = Gtk.Box(spacing=8)
        if d.fecha_entrega is not None:
            clases = ["kiwu-minimo"] + (["kiwu-vencida"] if esta_vencido(d) else [])
            detalle.append(etiqueta("📅 " + fecha_corta(d.fecha_entrega), clases))
        if materia:
            detalle.append(etiqueta(materia, ["kiwu-minimo"]))
        if detalle.get_first_child() is not None:
            textos.append(detalle)
        caja.append(textos)

        clic = Gtk.GestureClick(button=Gdk.BUTTON_PRIMARY)
        clic.connect("released", lambda *_: self.ctx.seleccionar(d.id))
        caja.add_controller(clic)
        menu_contextual(caja, lambda d=d: opciones_de_menu(self.ctx, d))

        origen = Gtk.DragSource()
        origen.set_actions(Gdk.DragAction.MOVE)
        origen.connect("prepare", lambda _o, _x, _y, i=d.id: Gdk.ContentProvider.new_for_value(
            GObject.Value(GObject.TYPE_STRING, i)))
        origen.connect("drag-begin", lambda o, _drag, c=caja: o.set_icon(Gtk.WidgetPaintable.new(c), 20, 20))
        caja.add_controller(origen)
        return caja


class Tablero(Gtk.Box):
    def __init__(self, ctx):
        super().__init__(orientation=Gtk.Orientation.VERTICAL)
        self.ctx = ctx
        self.cabecera = Gtk.Box(orientation=Gtk.Orientation.VERTICAL)
        self.append(self.cabecera)
        self.campo = CampoNuevaTarea(ctx.crear_tarea, "Añadir tarea a «Por hacer»")
        self.campo.set_margin_start(20)
        self.campo.set_margin_end(20)
        self.campo.set_margin_bottom(12)
        self.campo.set_halign(Gtk.Align.START)
        self.campo.set_size_request(480, -1)
        self.append(self.campo)
        self.columnas = {c: ColumnaTablero(ctx, c) for c in Columna}
        fila = Gtk.Box(spacing=14)
        fila.set_margin_start(20)
        fila.set_margin_end(20)
        fila.set_margin_bottom(20)
        for c in Columna:
            fila.append(self.columnas[c])
        self.scroll = Gtk.ScrolledWindow()
        self.scroll.set_child(fila)
        self.scroll.set_vexpand(True)
        self.scroll.set_policy(Gtk.PolicyType.AUTOMATIC, Gtk.PolicyType.NEVER)
        # Un clic en el fondo cierra el panel de la tarea
        fondo = Gtk.GestureClick(button=Gdk.BUTTON_PRIMARY)
        fondo.connect("released", lambda g, n, x, y: self.ctx.seleccionar(None)
                      if fila.pick(x, y, Gtk.PickFlags.DEFAULT) in (fila, None) else None)
        fila.add_controller(fondo)
        self.append(self.scroll)

    def enfocar_nueva(self) -> None:
        self.campo.enfocar()

    def refrescar(self, titulo: str, pendientes: list[Deber], completados: list[Deber], materias: list[Materia],
                  fija: Optional[Materia], permite_crear: bool) -> None:
        nombres = {m.id: m.nombre for m in materias}
        limpiar(self.cabecera)
        self.cabecera.append(encabezado(titulo, SelectorVista(self.ctx.vista_tareas, self.ctx.cambiar_vista)))
        if fija is not None:
            sub = SelectorSub(self.ctx.sub, bool(fija.carpeta) and not self.ctx.carpeta_disponible(fija),
                              self.ctx.cambiar_sub)
            sub.set_margin_start(20)
            sub.set_margin_bottom(10)
            self.cabecera.append(sub)
        self.campo.actualizar(materias, fija, permite_crear)
        todas = pendientes + completados
        for c in Columna:
            self.columnas[c].rellenar([d for d in todas if columna_de(d) == c], nombres, fija is None)

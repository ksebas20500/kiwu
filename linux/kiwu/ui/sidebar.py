"""Barra lateral de tareas: vistas y listas."""

from __future__ import annotations

from typing import Callable

import gi

gi.require_version("Gtk", "4.0")
from gi.repository import Gtk  # noqa: E402

from ..modelos import Materia  # noqa: E402
from ..servicios import CALENDARIO, COMPLETADAS, HOY, PROXIMOS, TODOS, Servicio, materia_item  # noqa: E402
from .utiles import boton_icono, etiqueta, limpiar, menu_contextual  # noqa: E402


class FilaItem(Gtk.ListBoxRow):
    def __init__(self, item: tuple):
        super().__init__()
        self.item = item


class SidebarTareas(Gtk.Box):
    def __init__(self, servicio: Servicio, al_elegir: Callable[[tuple], None], al_nueva: Callable[[], None],
                 al_renombrar: Callable[[Materia], None], al_eliminar: Callable[[Materia], None]):
        super().__init__(orientation=Gtk.Orientation.VERTICAL)
        self.servicio = servicio
        self.al_elegir = al_elegir
        self.al_nueva = al_nueva
        self.al_renombrar = al_renombrar
        self.al_eliminar = al_eliminar
        self.add_css_class("kiwu-sidebar")
        self.set_size_request(230, -1)
        self.lista = Gtk.ListBox()
        self.lista.set_selection_mode(Gtk.SelectionMode.SINGLE)
        self.lista.add_css_class("navigation-sidebar")
        self.lista.connect("row-activated", lambda _l, fila: self.al_elegir(fila.item))
        scroll = Gtk.ScrolledWindow()
        scroll.set_child(self.lista)
        scroll.set_vexpand(True)
        scroll.set_policy(Gtk.PolicyType.NEVER, Gtk.PolicyType.AUTOMATIC)
        self.append(scroll)
        self._filas: dict[tuple, FilaItem] = {}

    def refrescar(self, actual: tuple, materias: list[Materia]) -> None:
        limpiar(self.lista)
        self._filas.clear()
        for item, titulo, icono in (
            (HOY, "Hoy", "weather-clear-symbolic"),
            (PROXIMOS, "Próximos 7 días", "document-open-recent-symbolic"),
            (CALENDARIO, "Calendario", "x-office-calendar-symbolic"),
            (TODOS, "Todas las tareas", "view-list-symbolic"),
        ):
            self._anadir(item, titulo, icono)

        cabecera = Gtk.ListBoxRow()
        cabecera.set_selectable(False)
        cabecera.set_activatable(False)
        caja = Gtk.Box(spacing=6)
        caja.set_margin_top(14)
        caja.set_margin_start(6)
        titulo_listas = etiqueta("Listas", ["kiwu-seccion"])
        titulo_listas.set_hexpand(True)
        caja.append(titulo_listas)
        nueva = boton_icono("list-add-symbolic", "Nueva lista")
        nueva.connect("clicked", lambda _b: self.al_nueva())
        caja.append(nueva)
        cabecera.set_child(caja)
        self.lista.append(cabecera)

        for m in materias:
            fila = self._anadir(materia_item(m.id), m.nombre, "folder-symbolic",
                                alerta=bool(m.carpeta) and not _carpeta_existe(m.carpeta))
            menu_contextual(fila, lambda m=m: [("Renombrar lista", lambda: self.al_renombrar(m)),
                                               ("Eliminar lista", lambda: self.al_eliminar(m))])

        separador = Gtk.ListBoxRow()
        separador.set_selectable(False)
        separador.set_activatable(False)
        separador.set_child(Gtk.Separator())
        self.lista.append(separador)
        self._anadir(COMPLETADAS, "Completadas", "emblem-ok-symbolic")
        self.marcar(actual)

    def _anadir(self, item: tuple, titulo: str, icono: str, alerta: bool = False) -> FilaItem:
        fila = FilaItem(item)
        caja = Gtk.Box(spacing=10)
        caja.set_margin_top(2)
        caja.set_margin_bottom(2)
        caja.append(Gtk.Image.new_from_icon_name(icono))
        t = etiqueta(titulo, elipsis=True)
        t.set_hexpand(True)
        caja.append(t)
        if alerta:
            aviso = Gtk.Image.new_from_icon_name("dialog-warning-symbolic")
            aviso.set_tooltip_text("La carpeta vinculada no está disponible")
            caja.append(aviso)
        cuenta = self.servicio.conteo(item)
        if cuenta > 0:
            caja.append(etiqueta(str(cuenta), ["kiwu-minimo"]))
        fila.set_child(caja)
        self.lista.append(fila)
        self._filas[item] = fila
        return fila

    def marcar(self, item: tuple) -> None:
        fila = self._filas.get(item)
        if fila is not None:
            self.lista.select_row(fila)


def _carpeta_existe(ruta: str) -> bool:
    import os
    return os.path.isdir(ruta)

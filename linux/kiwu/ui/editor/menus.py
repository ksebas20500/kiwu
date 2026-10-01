"""Ventanas flotantes del editor: menú «/», barra de formato y cuadro de enlace."""

from __future__ import annotations

from typing import Callable, Optional

import gi

gi.require_version("Gtk", "4.0")
from gi.repository import Gdk, GLib, Gtk  # noqa: E402

from ...modelos import ICONOS_BLOQUE, TITULOS_BLOQUE  # noqa: E402
from ...enlaces import url_desde_texto  # noqa: E402
from ..utiles import etiqueta  # noqa: E402
from .texto_rico import COLORES, TITULOS_COLOR, rgba  # noqa: E402


def _rect(x: float, y: float, ancho: int = 1, alto: int = 1) -> Gdk.Rectangle:
    r = Gdk.Rectangle()
    r.x, r.y, r.width, r.height = int(x), int(y), ancho, alto
    return r


def rectangulo_del_cursor(vista: Gtk.TextView, iterador: Gtk.TextIter) -> Gdk.Rectangle:
    """Posición (en coordenadas del widget) justo debajo del carácter indicado."""
    r = vista.get_iter_location(iterador)
    x, y = vista.buffer_to_window_coords(Gtk.TextWindowType.WIDGET, r.x, r.y + r.height)
    return _rect(x, y)


class _Flotante:
    """Popover que se cuelga del bloque activo y no roba el foco del texto."""

    def __init__(self, clase: str):
        self.pop = Gtk.Popover()
        self.pop.set_autohide(False)
        self.pop.set_has_arrow(False)
        self.pop.set_position(Gtk.PositionType.BOTTOM)
        self.pop.add_css_class(clase)
        self.pop.set_can_focus(False)
        self.pop.set_focusable(False)

    def _anclar(self, widget: Gtk.Widget) -> None:
        if self.pop.get_parent() is not widget:
            if self.pop.get_parent() is not None:
                self.pop.unparent()
            self.pop.set_parent(widget)

    def ocultar(self) -> None:
        if self.pop.get_parent() is not None:
            self.pop.popdown()
            self.pop.unparent()

    @property
    def visible(self) -> bool:
        return self.pop.get_parent() is not None and self.pop.get_visible()


class MenuSlash(_Flotante):
    """Lista de bloques que aparece al escribir «/». Se maneja con las flechas, Intro y Esc."""

    def __init__(self, al_elegir: Callable[[str], None]):
        super().__init__("kiwu-slash")
        self.al_elegir = al_elegir
        self.opciones: list[str] = []
        self.indice = 0
        self.bloque = None
        self.lista = Gtk.ListBox()
        self.lista.set_selection_mode(Gtk.SelectionMode.SINGLE)
        self.lista.set_focusable(False)
        self.lista.connect("row-activated", self._activada)
        self.scroll = Gtk.ScrolledWindow()
        self.scroll.set_child(self.lista)
        self.scroll.set_min_content_width(290)
        self.scroll.set_max_content_height(300)
        self.scroll.set_propagate_natural_height(True)
        self.scroll.set_policy(Gtk.PolicyType.NEVER, Gtk.PolicyType.AUTOMATIC)
        self.pop.set_child(self.scroll)

    def mostrar(self, bloque, opciones: list[str]) -> None:
        if not opciones:
            self.ocultar()
            return
        self.bloque = bloque
        self._anclar(bloque.vista)
        if opciones != self.opciones:
            self.opciones = list(opciones)
            self.indice = 0
            self._reconstruir()
        self.indice = min(self.indice, len(self.opciones) - 1)
        self._seleccionar()
        buf = bloque.vista.get_buffer()
        self.pop.set_pointing_to(rectangulo_del_cursor(bloque.vista, buf.get_iter_at_mark(buf.get_insert())))
        if not self.pop.get_visible():
            self.pop.popup()

    def ocultar(self) -> None:
        self.opciones = []
        self.bloque = None
        super().ocultar()

    def mover(self, delta: int) -> None:
        if not self.opciones:
            return
        self.indice = (self.indice + delta) % len(self.opciones)
        self._seleccionar()

    def elegir_actual(self) -> None:
        if 0 <= self.indice < len(self.opciones):
            self.al_elegir(self.opciones[self.indice])

    def _reconstruir(self) -> None:
        hijo = self.lista.get_first_child()
        while hijo is not None:
            siguiente = hijo.get_next_sibling()
            self.lista.remove(hijo)
            hijo = siguiente
        for tipo in self.opciones:
            fila = Gtk.ListBoxRow()
            fila.set_focusable(False)
            caja = Gtk.Box(spacing=10)
            icono = Gtk.Image.new_from_icon_name(ICONOS_BLOQUE.get(tipo, "text-x-generic-symbolic"))
            icono.add_css_class("kiwu-slash-icono")
            caja.append(icono)
            caja.append(etiqueta(TITULOS_BLOQUE[tipo], xalign=0))
            fila.set_child(caja)
            self.lista.append(fila)

    def _seleccionar(self) -> None:
        fila = self.lista.get_row_at_index(self.indice)
        if fila is None:
            return
        self.lista.select_row(fila)
        adj = self.scroll.get_vadjustment()
        a = fila.get_allocation()
        if a.y < adj.get_value():
            adj.set_value(a.y)
        elif a.y + a.height > adj.get_value() + adj.get_page_size():
            adj.set_value(a.y + a.height - adj.get_page_size())

    def _activada(self, _lista, fila) -> None:
        self.indice = fila.get_index()
        self.elegir_actual()


class BarraFormato(_Flotante):
    """Barra con negrita, cursiva, colores, enlace… que aparece al seleccionar texto."""

    def __init__(self, pedir_enlace: Callable[[], None]):
        super().__init__("kiwu-barra-formato")
        self.bloque = None
        self.pedir_enlace = pedir_enlace
        caja = Gtk.Box(spacing=2)
        caja.add_css_class("kiwu-barra-formato")
        self._boton(caja, "format-text-bold-symbolic", "Negrita  Ctrl+B", lambda: self._alternar("b"))
        self._boton(caja, "format-text-italic-symbolic", "Cursiva  Ctrl+I", lambda: self._alternar("i"))
        self._boton(caja, "format-text-underline-symbolic", "Subrayado  Ctrl+U", lambda: self._alternar("u"))
        self._boton(caja, "format-text-strikethrough-symbolic", "Tachado  Ctrl+Mayús+X", lambda: self._alternar("s"))
        self._boton(caja, "utilities-terminal-symbolic", "Código en línea  Ctrl+E", lambda: self._alternar("code"))
        caja.append(Gtk.Separator(orientation=Gtk.Orientation.VERTICAL))
        caja.append(self._boton_colores())
        self._boton(caja, "insert-link-symbolic", "Enlace  Ctrl+K", lambda: self.pedir_enlace())
        self._boton(caja, "edit-clear-all-symbolic", "Quitar formato", self._limpiar)
        self.pop.set_child(caja)

    def _boton(self, caja: Gtk.Box, icono: str, ayuda: str, accion: Callable[[], None]) -> Gtk.Button:
        b = Gtk.Button.new_from_icon_name(icono)
        b.add_css_class("flat")
        b.set_tooltip_text(ayuda)
        b.set_focus_on_click(False)
        b.connect("clicked", lambda _b: accion())
        caja.append(b)
        return b

    def _boton_colores(self) -> Gtk.MenuButton:
        pop = Gtk.Popover()
        caja = Gtk.Box(orientation=Gtk.Orientation.VERTICAL, spacing=8)
        for lado in ("top", "bottom", "start", "end"):
            getattr(caja, f"set_margin_{lado}")(12)
        for titulo, fondo in (("Color del texto", False), ("Resaltado", True)):
            caja.append(etiqueta(titulo, ["kiwu-minimo"]))
            rejilla = Gtk.Grid(row_spacing=6, column_spacing=6)
            sin = Gtk.Button(label="A" if not fondo else "⊘")
            sin.set_tooltip_text("Predeterminado" if not fondo else "Sin resaltado")
            sin.connect("clicked", lambda _b, f=fondo: (pop.popdown(), self._color(None, f)))
            rejilla.attach(sin, 0, 0, 1, 1)
            for i, (nombre, (claro, _oscuro)) in enumerate(COLORES.items(), start=1):
                b = Gtk.Button(label="A" if not fondo else " ")
                b.add_css_class("kiwu-muestra-color")
                b.set_tooltip_text(TITULOS_COLOR[nombre])
                b.connect("clicked", lambda _b, n=nombre, f=fondo: (pop.popdown(), self._color(n, f)))
                self._pintar_muestra(b, claro, fondo)
                rejilla.attach(b, i % 5, i // 5, 1, 1)
            caja.append(rejilla)
        pop.set_child(caja)
        mb = Gtk.MenuButton(icon_name="color-select-symbolic")
        mb.add_css_class("flat")
        mb.set_tooltip_text("Color y resaltado")
        mb.set_popover(pop)
        mb.set_focus_on_click(False)
        return mb

    @staticmethod
    def _pintar_muestra(boton: Gtk.Button, rgb, fondo: bool) -> None:
        proveedor = Gtk.CssProvider()
        hexa = "#%02x%02x%02x" % tuple(round(c * 255) for c in rgb)
        css = (f"button {{ background: {hexa}; color: #fff; }}" if fondo else f"button {{ color: {hexa}; font-weight: 700; }}")
        if hasattr(proveedor, "load_from_string"):
            proveedor.load_from_string(css)
        else:
            proveedor.load_from_data(css.encode())
        boton.get_style_context().add_provider(proveedor, Gtk.STYLE_PROVIDER_PRIORITY_APPLICATION)

    # --- acciones sobre el bloque activo ------------------------------------------------------

    def _alternar(self, nombre: str) -> None:
        if self.bloque:
            self.bloque.formato.alternar(nombre)
            self.bloque.vista.grab_focus()

    def _color(self, nombre: Optional[str], fondo: bool) -> None:
        if self.bloque:
            self.bloque.formato.colorear(nombre, fondo)
            self.bloque.vista.grab_focus()

    def _limpiar(self) -> None:
        if self.bloque:
            self.bloque.formato.limpiar()
            self.bloque.vista.grab_focus()

    def mostrar(self, bloque) -> None:
        self.bloque = bloque
        self._anclar(bloque.vista)
        buf = bloque.vista.get_buffer()
        seleccion = buf.get_selection_bounds()
        if not seleccion:
            self.ocultar()
            return
        r = bloque.vista.get_iter_location(seleccion[0])
        x, y = bloque.vista.buffer_to_window_coords(Gtk.TextWindowType.WIDGET, r.x, r.y)
        self.pop.set_position(Gtk.PositionType.TOP)
        self.pop.set_pointing_to(_rect(x, y))
        if not self.pop.get_visible():
            self.pop.popup()

    def ocultar(self) -> None:
        self.bloque = None
        super().ocultar()


class CuadroEnlace:
    """Pequeña ventana flotante para escribir la dirección de un enlace."""

    def __init__(self):
        self.pop = Gtk.Popover()
        self.bloque = None
        caja = Gtk.Box(orientation=Gtk.Orientation.VERTICAL, spacing=10)
        for lado in ("top", "bottom", "start", "end"):
            getattr(caja, f"set_margin_{lado}")(12)
        caja.append(etiqueta("Enlace", ["heading"]))
        self.campo = Gtk.Entry(placeholder_text="https://…")
        self.campo.set_width_chars(30)
        self.campo.connect("activate", lambda _e: self._aplicar())
        caja.append(self.campo)
        botones = Gtk.Box(spacing=8)
        quitar = Gtk.Button(label="Quitar enlace")
        quitar.connect("clicked", lambda _b: self._quitar())
        aplicar = Gtk.Button(label="Aplicar")
        aplicar.add_css_class("suggested-action")
        aplicar.connect("clicked", lambda _b: self._aplicar())
        espacio = Gtk.Box()
        espacio.set_hexpand(True)
        botones.append(quitar)
        botones.append(espacio)
        botones.append(aplicar)
        caja.append(botones)
        self.pop.set_child(caja)
        self.pop.connect("closed", lambda _p: GLib.idle_add(self._soltar))

    def mostrar(self, bloque) -> None:
        buf = bloque.vista.get_buffer()
        seleccion = buf.get_selection_bounds()
        if not seleccion:
            return
        self.bloque = bloque
        self.campo.set_text(bloque.formato.enlace_en_seleccion() or "")
        if self.pop.get_parent() is not None:
            self.pop.unparent()
        self.pop.set_parent(bloque.vista)
        self.pop.set_pointing_to(rectangulo_del_cursor(bloque.vista, seleccion[0]))
        self.pop.popup()
        self.campo.grab_focus()

    def _soltar(self) -> bool:
        if self.pop.get_parent() is not None:
            self.pop.unparent()
        return False

    def _aplicar(self) -> None:
        url = url_desde_texto(self.campo.get_text())
        if url and self.bloque:
            self.bloque.formato.enlazar(url)
            self.pop.popdown()
            self.bloque.vista.grab_focus()

    def _quitar(self) -> None:
        if self.bloque:
            self.bloque.formato.enlazar(None)
            self.pop.popdown()
            self.bloque.vista.grab_focus()

"""Explorador de la carpeta vinculada a una lista."""

from __future__ import annotations

import os
from typing import Optional

import gi

gi.require_version("Gtk", "4.0")
from gi.repository import Gdk, GLib, Gtk  # noqa: E402

from ..modelos import Materia  # noqa: E402
from .utiles import abrir_archivo, boton_icono, confirmar, etiqueta, limpiar  # noqa: E402
from .widgets import SelectorSub, encabezado  # noqa: E402

LIMITE_RESULTADOS = 300


class ArchivosMateria(Gtk.Box):
    def __init__(self, ctx):
        super().__init__(orientation=Gtk.Orientation.VERTICAL)
        self.ctx = ctx
        self.materia: Optional[Materia] = None
        self.ruta: Optional[str] = None          # carpeta que se está viendo
        self.cabecera = Gtk.Box(orientation=Gtk.Orientation.VERTICAL)
        self.append(self.cabecera)
        self.barra = Gtk.Box(spacing=6)
        self.barra.set_margin_start(20)
        self.barra.set_margin_end(20)
        self.barra.set_margin_bottom(8)
        self.append(self.barra)
        self.contenido = Gtk.Box(orientation=Gtk.Orientation.VERTICAL, spacing=2)
        self.contenido.set_margin_start(10)
        self.contenido.set_margin_end(10)
        self.contenido.set_margin_bottom(20)
        scroll = Gtk.ScrolledWindow()
        scroll.set_child(self.contenido)
        scroll.set_vexpand(True)
        scroll.set_policy(Gtk.PolicyType.NEVER, Gtk.PolicyType.AUTOMATIC)
        self.append(scroll)
        self.busqueda = Gtk.SearchEntry(placeholder_text="Buscar por nombre", hexpand=True)
        self.busqueda.connect("search-changed", lambda _e: self._dibujar_lista())

    def refrescar(self, materia: Materia) -> None:
        if self.materia is None or self.materia.id != materia.id or self.materia.carpeta != materia.carpeta:
            self.ruta = materia.carpeta
        self.materia = materia
        limpiar(self.cabecera)
        self.cabecera.append(encabezado(materia.nombre))
        sub = SelectorSub("archivos", bool(materia.carpeta) and not os.path.isdir(materia.carpeta or ""),
                          self.ctx.cambiar_sub)
        sub.set_margin_start(20)
        sub.set_margin_bottom(10)
        self.cabecera.append(sub)
        self._dibujar()

    def _dibujar(self) -> None:
        m = self.materia
        limpiar(self.barra)
        limpiar(self.contenido)
        if m is None:
            return
        if not m.carpeta:
            self._estado_vacio("Sin carpeta vinculada",
                               "Vincula una carpeta de tu equipo para ver aquí los archivos de esta lista.\n"
                               "Los archivos no se copian: se leen desde su carpeta original.")
            return
        if not os.path.isdir(m.carpeta):
            self._estado_vacio("No se encuentra la carpeta", f"{m.carpeta}\nPuedes elegirla de nuevo.")
            return

        if self.ruta is None or not os.path.isdir(self.ruta) or not self.ruta.startswith(m.carpeta):
            self.ruta = m.carpeta
        subir = boton_icono("go-up-symbolic", "Carpeta superior")
        subir.set_sensitive(self.ruta != m.carpeta)
        subir.connect("clicked", lambda _b: self._ir(os.path.dirname(self.ruta)))
        self.barra.append(subir)
        relativa = os.path.relpath(self.ruta, m.carpeta)
        ruta_visible = etiqueta(os.path.basename(m.carpeta) + ("" if relativa == "." else "/" + relativa),
                                ["kiwu-suave"], elipsis=True)
        ruta_visible.set_hexpand(True)
        self.barra.append(ruta_visible)
        if self.busqueda.get_parent() is not None:
            self.busqueda.unparent()
        self.barra.append(self.busqueda)
        cambiar = boton_icono("folder-open-symbolic", "Cambiar de carpeta")
        cambiar.connect("clicked", lambda _b: self._elegir_carpeta())
        self.barra.append(cambiar)
        quitar = boton_icono("edit-delete-symbolic", "Desvincular la carpeta (no borra nada)")
        quitar.connect("clicked", lambda _b: self.ctx.db.vincular_carpeta(m.id, None))
        self.barra.append(quitar)
        self._dibujar_lista()

    def _dibujar_lista(self) -> None:
        m = self.materia
        limpiar(self.contenido)
        if m is None or not m.carpeta or not os.path.isdir(m.carpeta) or self.ruta is None:
            return
        texto = self.busqueda.get_text().strip().lower()
        entradas = self._buscar(m.carpeta, texto) if texto else self._listar(self.ruta)
        if not entradas:
            vacio = etiqueta("Sin resultados" if texto else "Carpeta vacía", ["title-3", "kiwu-suave"], xalign=0.5)
            vacio.set_margin_top(60)
            self.contenido.append(vacio)
            return
        for ruta, es_carpeta in entradas:
            self.contenido.append(self._fila(ruta, es_carpeta, m.carpeta, buscando=bool(texto)))

    @staticmethod
    def _listar(carpeta: str) -> list[tuple[str, bool]]:
        try:
            with os.scandir(carpeta) as it:
                datos = [(e.path, e.is_dir()) for e in it if not e.name.startswith(".")]
        except OSError:
            return []
        return sorted(datos, key=lambda x: (not x[1], os.path.basename(x[0]).lower()))

    @staticmethod
    def _buscar(raiz: str, texto: str) -> list[tuple[str, bool]]:
        salida: list[tuple[str, bool]] = []
        for base, carpetas, archivos in os.walk(raiz):
            carpetas[:] = [c for c in carpetas if not c.startswith(".")]
            for nombre in carpetas:
                if texto in nombre.lower():
                    salida.append((os.path.join(base, nombre), True))
            for nombre in archivos:
                if not nombre.startswith(".") and texto in nombre.lower():
                    salida.append((os.path.join(base, nombre), False))
            if len(salida) >= LIMITE_RESULTADOS:
                break
        return sorted(salida[:LIMITE_RESULTADOS], key=lambda x: (not x[1], os.path.basename(x[0]).lower()))

    def _fila(self, ruta: str, es_carpeta: bool, raiz: str, buscando: bool) -> Gtk.Widget:
        fila = Gtk.Box(spacing=10)
        fila.add_css_class("kiwu-fila")
        fila.append(Gtk.Image.new_from_icon_name("folder-symbolic" if es_carpeta else "text-x-generic-symbolic"))
        textos = Gtk.Box(orientation=Gtk.Orientation.VERTICAL)
        textos.set_hexpand(True)
        textos.append(etiqueta(os.path.basename(ruta), elipsis=True))
        if buscando:
            textos.append(etiqueta(os.path.relpath(os.path.dirname(ruta), raiz), ["kiwu-minimo"], elipsis=True))
        fila.append(textos)
        if es_carpeta:
            fila.append(Gtk.Image.new_from_icon_name("go-next-symbolic"))
        clic = Gtk.GestureClick(button=Gdk.BUTTON_PRIMARY)
        clic.connect("released", lambda *_: self._ir(ruta) if es_carpeta else abrir_archivo(ruta))
        fila.add_controller(clic)
        return fila

    def _ir(self, ruta: str) -> None:
        self.ruta = ruta
        self.busqueda.set_text("")
        GLib.idle_add(lambda: (self._dibujar(), False)[1])

    def _estado_vacio(self, titulo: str, texto: str) -> None:
        caja = Gtk.Box(orientation=Gtk.Orientation.VERTICAL, spacing=10)
        caja.set_valign(Gtk.Align.CENTER)
        caja.set_margin_top(80)
        icono = Gtk.Image.new_from_icon_name("folder-symbolic")
        icono.set_pixel_size(48)
        icono.add_css_class("kiwu-suave")
        caja.append(icono)
        caja.append(etiqueta(titulo, ["title-3"], xalign=0.5))
        caja.append(etiqueta(texto, ["kiwu-suave"], xalign=0.5, ajustar=True))
        boton = Gtk.Button(label="Vincular carpeta…")
        boton.add_css_class("suggested-action")
        boton.set_halign(Gtk.Align.CENTER)
        boton.connect("clicked", lambda _b: self._elegir_carpeta())
        caja.append(boton)
        self.contenido.append(caja)

    def _elegir_carpeta(self) -> None:
        dialogo = Gtk.FileDialog()
        dialogo.set_title("Elige la carpeta de esta lista")

        def listo(dlg, res):
            try:
                carpeta = dlg.select_folder_finish(res)
            except GLib.Error:
                return
            if carpeta is not None and carpeta.get_path() and self.materia:
                self.ctx.db.vincular_carpeta(self.materia.id, carpeta.get_path())

        dialogo.select_folder(self.ctx.ventana, None, listo)

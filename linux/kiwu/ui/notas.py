"""Módulo de notas: cuadernos, páginas y editor por bloques."""

from __future__ import annotations

from datetime import datetime
from typing import Optional

import gi

gi.require_version("Gtk", "4.0")
from gi.repository import Gdk, GLib, Gtk  # noqa: E402

from ..modelos import Cuaderno, Documento, Pagina  # noqa: E402
from .editor.editor import EditorBloques  # noqa: E402
from .utiles import (boton_icono, confirmar, en_segundo_plano, etiqueta, fecha_corta, limpiar,  # noqa: E402,F401
                     menu_contextual, pedir_texto, selector_emoji)

TODAS = ("todas",)
FAVORITAS = ("favoritas",)


def hace(ts: float) -> str:
    segundos = datetime.now().timestamp() - ts
    if segundos < 60:
        return "ahora"
    if segundos < 3600:
        return f"{int(segundos // 60)} min"
    if segundos < 86400:
        return f"{int(segundos // 3600)} h"
    return fecha_corta(ts)


def esta_en_blanco(p: Pagina) -> bool:
    return not p.titulo.strip() and not p.texto_plano.strip()


class DetallePagina(Gtk.Box):
    def __init__(self, ctx, pagina: Pagina):
        super().__init__(orientation=Gtk.Orientation.VERTICAL)
        self.ctx = ctx
        self.pagina_id = pagina.id
        self._bloqueado = True
        self._cuadernos = ctx.db.cuadernos()

        barra = Gtk.Box(spacing=10)
        barra.set_margin_start(20)
        barra.set_margin_end(20)
        barra.set_margin_top(10)
        barra.set_margin_bottom(10)
        self.cuaderno = Gtk.DropDown.new_from_strings([f"{c.icono} {c.nombre}" for c in self._cuadernos] or ["—"])
        self.cuaderno.add_css_class("flat")
        self.cuaderno.connect("notify::selected", self._cuaderno_elegido)
        barra.append(self.cuaderno)
        espacio = Gtk.Box()
        espacio.set_hexpand(True)
        barra.append(espacio)
        self.editada = etiqueta("", ["kiwu-minimo"])
        barra.append(self.editada)
        self.estrella = Gtk.ToggleButton(icon_name="non-starred-symbolic")
        self.estrella.add_css_class("flat")
        self.estrella.set_tooltip_text("Favorita")
        self.estrella.connect("toggled", self._favorita)
        barra.append(self.estrella)
        adjuntar = boton_icono("mail-attachment-symbolic", "Adjuntar PDF o Word")
        adjuntar.connect("clicked", lambda _b: self.editor.especial(None, "file"))
        barra.append(adjuntar)
        menu = Gtk.MenuButton(icon_name="view-more-symbolic")
        menu.add_css_class("flat")
        pop = Gtk.Popover()
        eliminar = Gtk.Button(label="Eliminar nota")
        eliminar.add_css_class("flat")
        eliminar.connect("clicked", lambda _b: (pop.popdown(), ctx.eliminar_pagina(self.pagina())))
        pop.set_child(eliminar)
        menu.set_popover(pop)
        barra.append(menu)
        self.append(barra)
        self.append(Gtk.Separator())

        cuerpo = Gtk.Box(orientation=Gtk.Orientation.VERTICAL, spacing=10)
        cuerpo.set_margin_start(40)
        cuerpo.set_margin_end(40)
        cuerpo.set_margin_top(20)
        fila_titulo = Gtk.Box(spacing=8)
        self.icono = Gtk.MenuButton()
        self.icono.add_css_class("flat")
        self.icono.set_popover(selector_emoji(self._icono_elegido, self._icono_quitado))
        fila_titulo.append(self.icono)
        self.titulo = Gtk.Entry(hexpand=True)
        self.titulo.set_has_frame(False)
        self.titulo.add_css_class("kiwu-titulo-nota")
        self.titulo.set_placeholder_text("Sin título")
        self.titulo.connect("activate", lambda _e: self._guardar_titulo())
        foco = Gtk.EventControllerFocus()
        foco.connect("leave", lambda _c: self._guardar_titulo())
        self.titulo.add_controller(foco)
        fila_titulo.append(self.titulo)
        cuerpo.append(fila_titulo)
        self.editor = EditorBloques(Documento.de_json(pagina.contenido), self._contenido_cambiado)
        cuerpo.append(self.editor)
        scroll = Gtk.ScrolledWindow()
        scroll.set_child(cuerpo)
        scroll.set_vexpand(True)
        scroll.set_policy(Gtk.PolicyType.NEVER, Gtk.PolicyType.AUTOMATIC)
        self.append(scroll)

        self.sincronizar(pagina, titulo=True)
        self._bloqueado = False

    def pagina(self) -> Pagina:
        return self.ctx.db.pagina(self.pagina_id)

    def sincronizar(self, p: Pagina, titulo: bool = False) -> None:
        self._bloqueado = True
        try:
            self.estrella.set_active(p.favorita)
            self.estrella.set_icon_name("starred-symbolic" if p.favorita else "non-starred-symbolic")
            self.icono.set_label(p.icono or "☺")
            self.editada.set_label("Editada " + hace(p.actualizada))
            if titulo or not self.titulo.has_focus():
                if self.titulo.get_text() != p.titulo:
                    self.titulo.set_text(p.titulo)
            for i, c in enumerate(self._cuadernos):
                if c.id == p.cuaderno_id and self.cuaderno.get_selected() != i:
                    self.cuaderno.set_selected(i)
        finally:
            self._bloqueado = False

    def cerrar(self) -> None:
        self._guardar_titulo()
        self.editor.guardar_ahora()

    def _guardar_titulo(self) -> None:
        if self._bloqueado:
            return
        p = self.ctx.db.pagina(self.pagina_id)
        texto = self.titulo.get_text().strip()
        if p is not None and texto != p.titulo:
            self.ctx.db.actualizar_pagina(p.id, titulo=texto)

    def _favorita(self, boton: Gtk.ToggleButton) -> None:
        if not self._bloqueado:
            self.ctx.db.actualizar_pagina(self.pagina_id, favorita=boton.get_active())

    def _icono_elegido(self, emoji: str) -> None:
        self.ctx.db.actualizar_pagina(self.pagina_id, icono=emoji)

    def _icono_quitado(self) -> None:
        self.ctx.db.actualizar_pagina(self.pagina_id, icono="")

    def _cuaderno_elegido(self, selector: Gtk.DropDown, _p) -> None:
        i = selector.get_selected()
        if not self._bloqueado and 0 <= i < len(self._cuadernos):
            self.ctx.db.actualizar_pagina(self.pagina_id, cuaderno_id=self._cuadernos[i].id)

    def _contenido_cambiado(self, doc: Documento) -> None:
        if self.ctx.db.pagina(self.pagina_id) is not None:
            self.ctx.db.actualizar_pagina(self.pagina_id, silencioso=True, contenido=doc.a_json(),
                                          texto_plano=doc.texto_plano())


class VistaNotas(Gtk.Box):
    def __init__(self, ventana, db):
        super().__init__()
        self.ventana, self.db = ventana, db
        self.item: tuple = TODAS
        self.seleccion: Optional[str] = None
        self._detalle: Optional[DetallePagina] = None
        self._refresco_pendiente = False

        # barra lateral
        self.lateral = Gtk.Box(orientation=Gtk.Orientation.VERTICAL)
        self.lateral.add_css_class("kiwu-sidebar")
        self.lateral.set_size_request(230, -1)
        self.lista_lateral = Gtk.ListBox()
        self.lista_lateral.add_css_class("navigation-sidebar")
        self.lista_lateral.connect("row-activated", lambda _l, fila: self.ir_a(fila.item))
        scroll = Gtk.ScrolledWindow()
        scroll.set_child(self.lista_lateral)
        scroll.set_vexpand(True)
        scroll.set_policy(Gtk.PolicyType.NEVER, Gtk.PolicyType.AUTOMATIC)
        self.lateral.append(scroll)
        self.append(self.lateral)
        self.append(Gtk.Separator(orientation=Gtk.Orientation.VERTICAL))

        # lista de páginas
        centro = Gtk.Box(orientation=Gtk.Orientation.VERTICAL)
        centro.set_size_request(340, -1)
        cab = Gtk.Box(spacing=8)
        cab.set_margin_top(20)
        cab.set_margin_start(20)
        cab.set_margin_end(20)
        cab.set_margin_bottom(10)
        self.titulo = etiqueta("Todas las notas", ["kiwu-titulo"], elipsis=True)
        self.titulo.set_hexpand(True)
        cab.append(self.titulo)
        nueva = boton_icono("document-new-symbolic", "Nueva nota")
        nueva.connect("clicked", lambda _b: self.nueva_pagina())
        cab.append(nueva)
        centro.append(cab)
        self.busqueda = Gtk.SearchEntry(placeholder_text="Buscar en las notas")
        self.busqueda.set_margin_start(20)
        self.busqueda.set_margin_end(20)
        self.busqueda.set_margin_bottom(10)
        self.busqueda.connect("search-changed", lambda _e: self.programar_refresco())
        centro.append(self.busqueda)
        self.paginas_caja = Gtk.Box(orientation=Gtk.Orientation.VERTICAL, spacing=2)
        self.paginas_caja.set_margin_start(10)
        self.paginas_caja.set_margin_end(10)
        sc = Gtk.ScrolledWindow()
        sc.set_child(self.paginas_caja)
        sc.set_vexpand(True)
        sc.set_policy(Gtk.PolicyType.NEVER, Gtk.PolicyType.AUTOMATIC)
        centro.append(sc)
        self.append(centro)
        self.append(Gtk.Separator(orientation=Gtk.Orientation.VERTICAL))

        # detalle
        self.zona = Gtk.Box(orientation=Gtk.Orientation.VERTICAL)
        self.zona.set_hexpand(True)
        self.vacio = Gtk.Box(orientation=Gtk.Orientation.VERTICAL, spacing=8)
        self.vacio.set_valign(Gtk.Align.CENTER)
        self.vacio.set_vexpand(True)
        icono = Gtk.Image.new_from_icon_name("document-edit-symbolic")
        icono.set_pixel_size(40)
        icono.add_css_class("kiwu-suave")
        self.vacio.append(icono)
        self.vacio.append(etiqueta("Selecciona o crea una nota", ["title-3", "kiwu-suave"], xalign=0.5))
        self.append(self.zona)

        self.asegurar_cuaderno()
        db.suscribir(self._base_cambio)
        self.refrescar()

    # --- datos ----------------------------------------------------------------------------------

    def asegurar_cuaderno(self) -> Cuaderno:
        cuadernos = self.db.cuadernos()
        return cuadernos[0] if cuadernos else self.db.crear_cuaderno("Notas rápidas", "📝")

    def _cuaderno_actual(self) -> Optional[Cuaderno]:
        return self.db.cuaderno(self.item[1]) if self.item[0] == "cuaderno" else None

    def _en_alcance(self, p: Pagina) -> bool:
        if self.item == TODAS:
            return True
        if self.item == FAVORITAS:
            return p.favorita
        return p.cuaderno_id == self.item[1]

    def _conteo(self, item: tuple, paginas: list[Pagina]) -> int:
        if item == TODAS:
            return len(paginas)
        if item == FAVORITAS:
            return sum(1 for p in paginas if p.favorita)
        return sum(1 for p in paginas if p.cuaderno_id == item[1])

    # --- acciones -------------------------------------------------------------------------------

    def programar_refresco(self) -> None:
        if not self._refresco_pendiente:
            self._refresco_pendiente = True
            GLib.idle_add(self._diferido)

    def ir_a(self, item: tuple) -> None:
        self.item, self.seleccion = item, None
        self.busqueda.set_text("")
        self.programar_refresco()

    def seleccionar(self, id_: Optional[str]) -> None:
        self.seleccion = id_
        self.programar_refresco()

    def nueva_pagina(self) -> None:
        destino = self._cuaderno_actual() or self.asegurar_cuaderno()
        # Si ya hay una nota en blanco en el cuaderno se reutiliza: pulsar varias veces no llena la lista de vacías.
        en_blanco = next((p for p in self.db.paginas() if p.cuaderno_id == destino.id and esta_en_blanco(p)), None)
        p = en_blanco or self.db.crear_pagina(destino.id)
        if self.item == FAVORITAS:
            self.item = ("cuaderno", destino.id)
        self.seleccion = p.id
        self.programar_refresco()

    def abrir_pagina(self, id_: str) -> None:
        self.item, self.seleccion = TODAS, id_
        self.programar_refresco()

    def eliminar_pagina(self, p: Pagina) -> None:
        def borrar() -> None:
            if self.seleccion == p.id:
                self.seleccion = None
            self.db.eliminar_pagina(p.id)
        confirmar(self.ventana, "¿Eliminar esta nota?", "No se puede deshacer.", "Eliminar", borrar)

    def nuevo_cuaderno(self) -> None:
        def crear(nombre: str) -> None:
            c = self.db.crear_cuaderno(nombre)
            self.ir_a(("cuaderno", c.id))
        pedir_texto(self.ventana, "Nuevo cuaderno", "Nombre del cuaderno", crear)

    def editar_cuaderno(self, c: Cuaderno) -> None:
        pedir_texto(self.ventana, "Renombrar cuaderno", "Nombre", lambda n: self.db.editar_cuaderno(c.id, n, c.icono),
                    texto_inicial=c.nombre, etiqueta_accion="Guardar")

    def eliminar_cuaderno(self, c: Cuaderno) -> None:
        def borrar() -> None:
            self.item, self.seleccion = TODAS, None
            self.db.eliminar_cuaderno(c.id)
        confirmar(self.ventana, f"¿Eliminar {c.nombre}?",
                  "Se eliminarán también todas las notas del cuaderno. Los archivos adjuntos no se tocan.",
                  "Eliminar cuaderno y sus notas", borrar)

    # --- refresco -------------------------------------------------------------------------------

    def _base_cambio(self, tipo: str) -> None:
        if tipo in ("pagina", "cuaderno", "importacion"):
            self.programar_refresco()

    def _diferido(self) -> bool:
        self._refresco_pendiente = False
        self.refrescar()
        return False

    def refrescar(self) -> None:
        cuadernos = self.db.cuadernos()
        paginas = self.db.paginas()
        if self.item[0] == "cuaderno" and self._cuaderno_actual() is None:
            self.item = TODAS
        self._lateral(cuadernos, paginas)

        self.titulo.set_label({"todas": "Todas las notas", "favoritas": "Favoritas"}.get(
            self.item[0], (self._cuaderno_actual().nombre if self._cuaderno_actual() else "Cuaderno")))
        texto = self.busqueda.get_text().strip().lower()
        visibles = [p for p in paginas if self._en_alcance(p)
                    and (not texto or texto in p.titulo.lower() or texto in p.texto_plano.lower())]
        nombres = {c.id: c.nombre for c in cuadernos}
        limpiar(self.paginas_caja)
        for p in visibles:
            self.paginas_caja.append(self._fila(p, nombres.get(p.cuaderno_id, "") if self._cuaderno_actual() is None else ""))
        if not visibles:
            sin_notas = etiqueta("Sin notas", ["title-3", "kiwu-suave"], xalign=0.5)
            sin_notas.set_margin_top(60)
            self.paginas_caja.append(sin_notas)

        if self.seleccion and self.db.pagina(self.seleccion) is None:
            self.seleccion = None
        self._actualizar_detalle()

    def _lateral(self, cuadernos: list[Cuaderno], paginas: list[Pagina]) -> None:
        from .sidebar import FilaItem
        limpiar(self.lista_lateral)
        seleccionable = None

        def anadir(item: tuple, titulo: str, icono: str = "", emoji: str = "") -> FilaItem:
            nonlocal seleccionable
            fila = FilaItem(item)
            caja = Gtk.Box(spacing=10)
            caja.set_margin_top(2)
            caja.set_margin_bottom(2)
            caja.append(etiqueta(emoji, xalign=0.5) if emoji else Gtk.Image.new_from_icon_name(icono))
            t = etiqueta(titulo, elipsis=True)
            t.set_hexpand(True)
            caja.append(t)
            n = self._conteo(item, paginas)
            if n:
                caja.append(etiqueta(str(n), ["kiwu-minimo"]))
            fila.set_child(caja)
            self.lista_lateral.append(fila)
            if item == self.item:
                seleccionable = fila
            return fila

        anadir(TODAS, "Todas las notas", "document-edit-symbolic")
        anadir(FAVORITAS, "Favoritas", "starred-symbolic")
        cab = Gtk.ListBoxRow()
        cab.set_selectable(False)
        cab.set_activatable(False)
        caja = Gtk.Box(spacing=6)
        caja.set_margin_top(14)
        caja.set_margin_start(6)
        t = etiqueta("Cuadernos", ["kiwu-seccion"])
        t.set_hexpand(True)
        caja.append(t)
        mas = boton_icono("list-add-symbolic", "Nuevo cuaderno")
        mas.connect("clicked", lambda _b: self.nuevo_cuaderno())
        caja.append(mas)
        cab.set_child(caja)
        self.lista_lateral.append(cab)
        for c in cuadernos:
            fila = anadir(("cuaderno", c.id), c.nombre, emoji=c.icono)
            menu_contextual(fila, lambda c=c: [("Renombrar cuaderno", lambda: self.editar_cuaderno(c)),
                                               ("Eliminar cuaderno", lambda: self.eliminar_cuaderno(c))])
        if seleccionable is not None:
            self.lista_lateral.select_row(seleccionable)

    def _fila(self, p: Pagina, cuaderno: str) -> Gtk.Widget:
        fila = Gtk.Box(spacing=10)
        fila.add_css_class("kiwu-fila")
        if self.seleccion == p.id:
            fila.add_css_class("seleccionada")
        fila.append(etiqueta(p.icono or "📄", xalign=0.5))
        textos = Gtk.Box(orientation=Gtk.Orientation.VERTICAL, spacing=1)
        textos.set_hexpand(True)
        textos.append(etiqueta(p.titulo or "Sin título", ["heading"], elipsis=True))
        vista = " ".join(p.texto_plano.split())[:90]
        if vista:
            textos.append(etiqueta(vista, ["kiwu-suave", "caption"], elipsis=True))
        textos.append(etiqueta(" · ".join(x for x in (hace(p.actualizada), cuaderno) if x), ["kiwu-minimo"]))
        fila.append(textos)
        if p.favorita:
            fila.append(Gtk.Image.new_from_icon_name("starred-symbolic"))
        clic = Gtk.GestureClick(button=Gdk.BUTTON_PRIMARY)
        clic.connect("released", lambda *_: self.seleccionar(p.id))
        fila.add_controller(clic)
        menu_contextual(fila, lambda p=p: [
            ("Quitar de favoritas" if p.favorita else "Añadir a favoritas",
             lambda: self.db.actualizar_pagina(p.id, favorita=not p.favorita)),
            None,
            ("Eliminar nota", lambda: self.eliminar_pagina(p)),
        ])
        return fila

    def _actualizar_detalle(self) -> None:
        if not self.seleccion:
            if self._detalle is not None:
                self._detalle.cerrar()
                self._detalle = None
            self._poner(self.vacio)
            return
        p = self.db.pagina(self.seleccion)
        if p is None:
            return
        if self._detalle is not None and self._detalle.pagina_id == p.id:
            self._detalle.sincronizar(p)
            return
        if self._detalle is not None:
            self._detalle.cerrar()
        self._detalle = DetallePagina(self, p)
        self._poner(self._detalle)

    def _poner(self, widget: Gtk.Widget) -> None:
        hijo = self.zona.get_first_child()
        if hijo is widget:
            return
        while hijo is not None:
            siguiente = hijo.get_next_sibling()
            self.zona.remove(hijo)
            hijo = siguiente
        widget.set_vexpand(True)
        self.zona.append(widget)

    def guardar_todo(self) -> None:
        if self._detalle is not None:
            self._detalle.cerrar()

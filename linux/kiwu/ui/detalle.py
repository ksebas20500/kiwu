"""Panel de detalle de una tarea: fecha, título y nota por bloques."""

from __future__ import annotations

from datetime import datetime, timedelta
from typing import Optional

import gi

gi.require_version("Gtk", "4.0")
from gi.repository import GLib, Gtk  # noqa: E402

from ..modelos import Deber, Documento  # noqa: E402
from ..servicios import esta_vencido  # noqa: E402
from .editor.editor import EditorBloques  # noqa: E402
from .utiles import boton_icono, etiqueta, fecha_completa  # noqa: E402


class SelectorFecha(Gtk.Popover):
    """Calendario con atajos (Hoy, Mañana, +7 días) y hora."""

    def __init__(self, al_fijar, al_quitar):
        super().__init__()
        self.al_fijar = al_fijar
        self.al_quitar = al_quitar
        self._bloqueado = False
        caja = Gtk.Box(orientation=Gtk.Orientation.VERTICAL, spacing=10)
        for lado in ("top", "bottom", "start", "end"):
            getattr(caja, f"set_margin_{lado}")(12)
        atajos = Gtk.Box(spacing=6, homogeneous=True)
        for texto, dias in (("Hoy", 0), ("Mañana", 1), ("+7 días", 7)):
            b = Gtk.Button(label=texto)
            b.connect("clicked", lambda _b, d=dias: self._atajo(d))
            atajos.append(b)
        caja.append(atajos)
        self.calendario = Gtk.Calendar()
        self.calendario.connect("day-selected", self._dia)
        caja.append(self.calendario)
        hora = Gtk.Box(spacing=6)
        hora.append(etiqueta("Hora"))
        self.h = Gtk.SpinButton.new_with_range(0, 23, 1)
        self.m = Gtk.SpinButton.new_with_range(0, 59, 5)
        for s in (self.h, self.m):
            s.set_wrap(True)
            s.set_numeric(True)
            s.connect("value-changed", self._hora)
        self.h.connect("output", lambda s: self._dos_cifras(s))
        self.m.connect("output", lambda s: self._dos_cifras(s))
        hora.append(self.h)
        hora.append(etiqueta(":"))
        hora.append(self.m)
        caja.append(hora)
        quitar = Gtk.Button(label="Quitar fecha")
        quitar.add_css_class("destructive-action")
        quitar.connect("clicked", lambda _b: (self.popdown(), self.al_quitar()))
        caja.append(quitar)
        self.set_child(caja)
        self._fecha: Optional[float] = None

    @staticmethod
    def _dos_cifras(spin: Gtk.SpinButton) -> bool:
        spin.set_text("%02d" % int(spin.get_value()))
        return True

    def mostrar_fecha(self, ts: Optional[float]) -> None:
        self._fecha = ts
        self._bloqueado = True
        try:
            d = datetime.fromtimestamp(ts) if ts is not None else datetime.now().replace(hour=18, minute=0)
            self.calendario.select_day(GLib.DateTime.new_local(d.year, d.month, d.day, 0, 0, 0))
            self.h.set_value(d.hour)
            self.m.set_value(d.minute)
        finally:
            self._bloqueado = False

    def _base(self) -> datetime:
        if self._fecha is not None:
            return datetime.fromtimestamp(self._fecha)
        return datetime.now().replace(hour=18, minute=0, second=0, microsecond=0)

    def _atajo(self, dias: int) -> None:
        base = self._base()
        d = (datetime.now() + timedelta(days=dias)).replace(hour=base.hour, minute=base.minute, second=0, microsecond=0)
        self.al_fijar(d.timestamp())
        self.popdown()

    def _dia(self, calendario: Gtk.Calendar) -> None:
        if self._bloqueado:
            return
        f = calendario.get_date()
        base = self._base()
        d = datetime(f.get_year(), f.get_month(), f.get_day_of_month(), int(self.h.get_value()),
                     int(self.m.get_value())) if self._fecha is not None else \
            datetime(f.get_year(), f.get_month(), f.get_day_of_month(), base.hour, base.minute)
        self.al_fijar(d.timestamp())

    def _hora(self, _spin) -> None:
        if self._bloqueado:
            return
        f = self.calendario.get_date()
        d = datetime(f.get_year(), f.get_month(), f.get_day_of_month(), int(self.h.get_value()), int(self.m.get_value()))
        self.al_fijar(d.timestamp())


class DetalleTarea(Gtk.Box):
    def __init__(self, ctx, deber: Deber, cerrable: bool):
        super().__init__(orientation=Gtk.Orientation.VERTICAL)
        self.ctx = ctx
        self.deber_id = deber.id
        self._materias = ctx.db.materias()
        self._bloqueado = True

        # --- barra superior ---
        barra = Gtk.Box(spacing=12)
        barra.set_margin_start(20)
        barra.set_margin_end(20)
        barra.set_margin_top(12)
        barra.set_margin_bottom(12)
        self.casilla = Gtk.CheckButton()
        self.casilla.connect("toggled", self._marcado)
        barra.append(self.casilla)
        barra.append(Gtk.Separator(orientation=Gtk.Orientation.VERTICAL))
        self.selector_fecha = SelectorFecha(self._fijar_fecha, self._quitar_fecha)
        self.boton_fecha = Gtk.MenuButton()
        self.boton_fecha.add_css_class("flat")
        self.boton_fecha.set_popover(self.selector_fecha)
        barra.append(self.boton_fecha)
        espacio = Gtk.Box()
        espacio.set_hexpand(True)
        barra.append(espacio)
        adjuntar = boton_icono("mail-attachment-symbolic", "Adjuntar PDF o Word")
        adjuntar.connect("clicked", lambda _b: self.editor.especial(None, "file"))
        barra.append(adjuntar)
        menu = Gtk.MenuButton(icon_name="view-more-symbolic")
        menu.add_css_class("flat")
        pop = Gtk.Popover()
        eliminar = Gtk.Button(label="Eliminar tarea")
        eliminar.add_css_class("flat")
        eliminar.connect("clicked", lambda _b: (pop.popdown(), ctx.eliminar_tarea(self.deber())))
        pop.set_child(eliminar)
        menu.set_popover(pop)
        barra.append(menu)
        if cerrable:
            cerrar = boton_icono("window-close-symbolic", "Cerrar el panel")
            cerrar.connect("clicked", lambda _b: ctx.seleccionar(None))
            barra.append(cerrar)
        self.append(barra)
        self.append(Gtk.Separator())

        # --- cuerpo: título + editor ---
        cuerpo = Gtk.Box(orientation=Gtk.Orientation.VERTICAL, spacing=14)
        cuerpo.set_margin_start(28)
        cuerpo.set_margin_end(28)
        cuerpo.set_margin_top(22)
        self.titulo = Gtk.Entry()
        self.titulo.set_has_frame(False)
        self.titulo.add_css_class("kiwu-titulo-tarea")
        self.titulo.set_placeholder_text("Título")
        self.titulo.connect("activate", lambda _e: self._guardar_titulo())
        foco = Gtk.EventControllerFocus()
        foco.connect("leave", lambda _c: self._guardar_titulo())
        self.titulo.add_controller(foco)
        cuerpo.append(self.titulo)
        self.editor = EditorBloques(Documento.de_json(deber.nota), self._nota_cambiada)
        cuerpo.append(self.editor)
        scroll = Gtk.ScrolledWindow()
        scroll.set_child(cuerpo)
        scroll.set_vexpand(True)
        scroll.set_policy(Gtk.PolicyType.NEVER, Gtk.PolicyType.AUTOMATIC)
        self.append(scroll)
        self.append(Gtk.Separator())

        # --- pie ---
        pie = Gtk.Box(spacing=8)
        pie.set_margin_start(20)
        pie.set_margin_end(20)
        pie.set_margin_top(8)
        pie.set_margin_bottom(8)
        self.materia = Gtk.DropDown.new_from_strings([m.nombre for m in self._materias] or ["—"])
        self.materia.add_css_class("flat")
        self.materia.connect("notify::selected", self._materia_elegida)
        pie.append(self.materia)
        sp = Gtk.Box()
        sp.set_hexpand(True)
        pie.append(sp)
        self.editado = etiqueta("", ["kiwu-minimo"])
        pie.append(self.editado)
        self.append(pie)

        self.sincronizar(deber, titulo=True)
        self._bloqueado = False

    # --- datos ----------------------------------------------------------------------------------

    def deber(self) -> Deber:
        return self.ctx.db.deber(self.deber_id)

    def sincronizar(self, d: Deber, titulo: bool = False) -> None:
        self._bloqueado = True
        try:
            self.casilla.set_active(d.completado)
            self.boton_fecha.set_child(self._contenido_fecha(d))
            self.selector_fecha.mostrar_fecha(d.fecha_entrega)
            if titulo or not self.titulo.has_focus():
                if self.titulo.get_text() != d.titulo:
                    self.titulo.set_text(d.titulo)
            for i, m in enumerate(self._materias):
                if m.id == d.materia_id and self.materia.get_selected() != i:
                    self.materia.set_selected(i)
            self.editado.set_label("Editado " + fecha_completa(d.actualizada))
        finally:
            self._bloqueado = False

    @staticmethod
    def _contenido_fecha(d: Deber) -> Gtk.Widget:
        caja = Gtk.Box(spacing=6)
        caja.append(Gtk.Image.new_from_icon_name("x-office-calendar-symbolic"))
        if d.fecha_entrega is None:
            caja.append(etiqueta("Fecha de vencimiento", ["kiwu-suave"]))
        else:
            caja.append(etiqueta(fecha_completa(d.fecha_entrega), ["kiwu-vencida"] if esta_vencido(d) else ["accent"]))
        return caja

    def cerrar(self) -> None:
        """Se llama antes de quitar el panel: guarda lo pendiente."""
        self._guardar_titulo()
        self.editor.guardar_ahora()

    # --- eventos --------------------------------------------------------------------------------

    def _marcado(self, casilla: Gtk.CheckButton) -> None:
        if not self._bloqueado:
            self.ctx.servicio.marcar(self.deber(), casilla.get_active())

    def _fijar_fecha(self, ts: float) -> None:
        if not self._bloqueado:
            self.ctx.servicio.poner_fecha(self.deber(), ts)

    def _quitar_fecha(self) -> None:
        self.ctx.servicio.poner_fecha(self.deber(), None)

    def _guardar_titulo(self) -> None:
        if self._bloqueado:
            return
        d = self.ctx.db.deber(self.deber_id)
        texto = self.titulo.get_text().strip()
        if d is not None and texto and texto != d.titulo:
            self.ctx.db.actualizar_deber(d.id, titulo=texto)

    def _materia_elegida(self, selector: Gtk.DropDown, _p) -> None:
        i = selector.get_selected()
        if self._bloqueado or not 0 <= i < len(self._materias):
            return
        self.ctx.servicio.mover_a_materia(self.deber(), self._materias[i].id)

    def _nota_cambiada(self, documento: Documento) -> None:
        # Guardado silencioso: no se redibujan las listas mientras se escribe.
        if self.ctx.db.deber(self.deber_id) is not None:
            self.ctx.db.actualizar_deber(self.deber_id, silencioso=True, nota=documento.a_json())

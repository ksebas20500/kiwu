"""Vista de calendario mensual."""

from __future__ import annotations

import calendar as cal
from datetime import date, datetime, timedelta
from typing import Optional

import gi

gi.require_version("Gtk", "4.0")
from gi.repository import Gdk, GLib, Gtk  # noqa: E402

from ..modelos import Deber, Materia  # noqa: E402
from .estilos import asignar_pasteles  # noqa: E402
from .utiles import DIAS_SEMANA, MESES_LARGOS, boton_icono, etiqueta, limpiar  # noqa: E402
from .widgets import CampoNuevaTarea  # noqa: E402

MAX_EVENTOS = 3


class Calendario(Gtk.Box):
    def __init__(self, ctx):
        super().__init__(orientation=Gtk.Orientation.VERTICAL)
        self.ctx = ctx
        hoy = date.today()
        self.mes = date(hoy.year, hoy.month, 1)
        self.dia_sel = hoy
        self._deberes: list[Deber] = []
        self._materias: list[Materia] = []

        self.cabecera = Gtk.Box(spacing=8)
        self.cabecera.set_margin_top(20)
        self.cabecera.set_margin_start(20)
        self.cabecera.set_margin_end(20)
        self.cabecera.set_margin_bottom(10)
        self.titulo = etiqueta("", ["kiwu-titulo"])
        self.titulo.set_hexpand(True)
        self.cabecera.append(self.titulo)
        anterior = boton_icono("go-previous-symbolic", "Mes anterior", plano=False)
        anterior.connect("clicked", lambda _b: self.cambiar_mes(-1))
        ir_hoy = Gtk.Button(label="Hoy")
        ir_hoy.connect("clicked", lambda _b: self.ir_a_hoy())
        siguiente = boton_icono("go-next-symbolic", "Mes siguiente", plano=False)
        siguiente.connect("clicked", lambda _b: self.cambiar_mes(1))
        for b in (anterior, ir_hoy, siguiente):
            self.cabecera.append(b)
        self.append(self.cabecera)

        self.rejilla = Gtk.Grid(column_homogeneous=True, row_homogeneous=False)
        self.rejilla.set_vexpand(True)
        self.rejilla.set_hexpand(True)
        self.append(self.rejilla)

        self.campo = CampoNuevaTarea(self._crear_en_dia)
        self.campo.set_margin_start(16)
        self.campo.set_margin_end(16)
        self.campo.set_margin_top(10)
        self.campo.set_margin_bottom(10)
        self.append(self.campo)

    # --- navegación -----------------------------------------------------------------------------

    def cambiar_mes(self, delta: int) -> None:
        m = self.mes.month - 1 + delta
        self.mes = date(self.mes.year + m // 12, m % 12 + 1, 1)
        self.refrescar(self._deberes, self._materias)

    def ir_a_hoy(self) -> None:
        hoy = date.today()
        self.mes, self.dia_sel = date(hoy.year, hoy.month, 1), hoy
        self.refrescar(self._deberes, self._materias)

    def _crear_en_dia(self, titulo: str, materia_id: str) -> None:
        fecha = datetime(self.dia_sel.year, self.dia_sel.month, self.dia_sel.day, 18, 0).timestamp()
        d = self.ctx.servicio.crear(titulo, materia_id, fecha)
        self.ctx.seleccionar(d.id)

    # --- dibujo ---------------------------------------------------------------------------------

    def refrescar(self, deberes: list[Deber], materias: list[Materia]) -> None:
        self._deberes, self._materias = deberes, materias
        self.titulo.set_label(f"{MESES_LARGOS[self.mes.month - 1].capitalize()} {self.mes.year}")
        limpiar(self.rejilla)
        for i, nombre in enumerate(DIAS_SEMANA):
            e = etiqueta(nombre, ["kiwu-minimo"], xalign=0.5)
            e.set_margin_bottom(4)
            self.rejilla.attach(e, i, 0, 1, 1)

        por_dia: dict[date, list[Deber]] = {}
        for d in deberes:
            if d.fecha_entrega is not None:
                por_dia.setdefault(datetime.fromtimestamp(d.fecha_entrega).date(), []).append(d)

        primero = self.mes
        inicio = primero - timedelta(days=primero.weekday())            # semana que empieza en lunes
        semanas = cal.monthrange(primero.year, primero.month)
        filas = 6 if (primero.weekday() + semanas[1]) > 35 else 5
        hoy = date.today()
        for k in range(filas * 7):
            dia = inicio + timedelta(days=k)
            self.rejilla.attach(self._celda(dia, por_dia.get(dia, []), hoy), k % 7, 1 + k // 7, 1, 1)

        self.campo.actualizar(materias, None, True)
        self.campo.entrada.set_placeholder_text(
            f"Añadir tarea el {self.dia_sel.day} de {MESES_LARGOS[self.dia_sel.month - 1]}" if materias
            else "Crea una lista primero")

    def _celda(self, dia: date, tareas: list[Deber], hoy: date) -> Gtk.Widget:
        caja = Gtk.Box(orientation=Gtk.Orientation.VERTICAL, spacing=2)
        caja.add_css_class("kiwu-dia")
        caja.set_vexpand(True)
        caja.set_size_request(80, 64)
        if dia.month != self.mes.month:
            caja.add_css_class("fuera-de-mes")
        if dia == self.dia_sel:
            caja.add_css_class("seleccionado")
        numero = etiqueta(str(dia.day), ["kiwu-numero-dia"] + (["hoy"] if dia == hoy else []))
        numero.set_halign(Gtk.Align.START)
        caja.append(numero)

        colores = asignar_pasteles([d.id for d in tareas])
        for d in tareas[:MAX_EVENTOS]:
            chip = etiqueta(d.titulo, ["kiwu-evento", colores[d.id]], elipsis=True)
            if d.completado:
                chip.set_opacity(0.5)
            clic = Gtk.GestureClick(button=Gdk.BUTTON_PRIMARY)
            clic.connect("released", lambda g, n, x, y, i=d.id: (self.ctx.seleccionar(i), g.set_state(
                Gtk.EventSequenceState.CLAIMED)))
            chip.add_controller(clic)
            caja.append(chip)
        if len(tareas) > MAX_EVENTOS:
            caja.append(etiqueta(f"+{len(tareas) - MAX_EVENTOS} más", ["kiwu-minimo"]))

        gesto = Gtk.GestureClick(button=Gdk.BUTTON_PRIMARY)
        gesto.connect("released", lambda g, n, x, y, dia=dia, c=caja, t=tareas: self._clic_dia(dia, n, c, t))
        caja.add_controller(gesto)
        return caja

    def _clic_dia(self, dia: date, n_pulsaciones: int, celda: Gtk.Widget, tareas: list[Deber]) -> None:
        if dia != self.dia_sel:
            self.dia_sel = dia
            if dia.month != self.mes.month:
                self.mes = date(dia.year, dia.month, 1)
            # se redibuja fuera del gesto, porque se destruyen las celdas que lo están ejecutando
            GLib.idle_add(lambda: (self.refrescar(self._deberes, self._materias), False)[1])
            return
        if n_pulsaciones >= 2 and tareas:
            self._popover_del_dia(celda, dia, tareas)

    def _popover_del_dia(self, celda: Gtk.Widget, dia: date, tareas: list[Deber]) -> None:
        """Doble clic en un día: todas sus tareas."""
        pop = Gtk.Popover()
        pop.set_parent(celda)
        caja = Gtk.Box(orientation=Gtk.Orientation.VERTICAL, spacing=4)
        for lado in ("top", "bottom", "start", "end"):
            getattr(caja, f"set_margin_{lado}")(10)
        caja.append(etiqueta(f"{dia.day} de {MESES_LARGOS[dia.month - 1]}", ["heading"]))
        colores = asignar_pasteles([d.id for d in tareas])
        for d in tareas:
            b = Gtk.Button()
            b.add_css_class("flat")
            e = etiqueta(d.titulo, ["kiwu-evento", colores[d.id]], elipsis=True)
            e.set_width_chars(28)
            b.set_child(e)
            b.connect("clicked", lambda _b, i=d.id, p=pop: (p.popdown(), self.ctx.seleccionar(i)))
            caja.append(b)
        pop.set_child(caja)
        pop.connect("closed", lambda p: p.unparent())
        pop.popup()

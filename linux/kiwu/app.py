"""Aplicación: instancia única, línea de comandos y recordatorios."""

from __future__ import annotations

import sys

from . import APP_ID, VERSION, entorno

entorno.preparar()          # debe ir antes de importar GTK

import gi  # noqa: E402

gi.require_version("Gtk", "4.0")
gi.require_version("Adw", "1")
from gi.repository import Adw, Gio, GLib  # noqa: E402

from . import recordatorios, rutas  # noqa: E402
from .ajustes import Ajustes  # noqa: E402
from .db import BaseDeDatos  # noqa: E402
from .servicios import Servicio  # noqa: E402
from .ui.estilos import Estilos, registrar_esquemas_de_codigo  # noqa: E402
from .ui.ventana import Ventana  # noqa: E402


class Aplicacion(Adw.Application):
    def __init__(self):
        super().__init__(application_id=APP_ID, flags=Gio.ApplicationFlags.HANDLES_COMMAND_LINE)
        self.ventana: Ventana | None = None
        self.db: BaseDeDatos | None = None
        self.retenida = False
        for nombre, descripcion in (
            ("nueva-tarea", "Abre Kiwu con el cursor en «Añadir tarea» (para enlazarlo a un atajo del escritorio)"),
            ("mostrar", "Muestra la ventana de Kiwu"),
            ("segundo-plano", "Arranca sin ventana y mantiene los recordatorios activos"),
            ("diagnostico", "Muestra información del entorno gráfico y sale"),
            ("version", "Muestra la versión y sale"),
        ):
            self.add_main_option(nombre, 0, GLib.OptionFlags.NONE, GLib.OptionArg.NONE, descripcion, None)

    # --- ciclo de vida --------------------------------------------------------------------------

    def do_startup(self):
        Adw.Application.do_startup(self)
        self.ajustes = Ajustes()
        self.db = BaseDeDatos(rutas.ruta_db())
        self.servicio = Servicio(self.db)
        registrar_esquemas_de_codigo()
        self.estilos = Estilos(self.ajustes)

        abrir = Gio.SimpleAction.new("abrir-tarea", GLib.VariantType.new("s"))
        abrir.connect("activate", self._abrir_tarea)
        self.add_action(abrir)
        mostrar = Gio.SimpleAction.new("mostrar", None)
        mostrar.connect("activate", lambda *_: self.do_activate())
        self.add_action(mostrar)
        salir = Gio.SimpleAction.new("salir", None)
        salir.connect("activate", lambda *_: self.quit())
        self.add_action(salir)
        self.set_accels_for_action("app.salir", ["<Control>q"])

        GLib.timeout_add_seconds(60, self._revisar_recordatorios)
        GLib.timeout_add_seconds(4, lambda: (self._revisar_recordatorios(), False)[1])

    def do_activate(self):
        if self.ventana is None:
            self.ventana = Ventana(self, self.db, self.servicio, self.ajustes)
            self.ventana.connect("destroy", lambda _v: setattr(self, "ventana", None))
        self.ventana.present()

    def do_shutdown(self):
        if self.ventana is not None:
            self.ventana.tareas.guardar_todo()
            self.ventana.notas.guardar_todo()
        if self.db is not None:
            self.db.cerrar()
        Adw.Application.do_shutdown(self)

    def do_command_line(self, linea):
        opciones = linea.get_options_dict().end().unpack()
        if "version" in opciones:
            linea.print_literal(f"Kiwu {VERSION}\n")
            return 0
        if "diagnostico" in opciones:
            linea.print_literal(entorno.diagnostico() + "\n")
            return 0
        if "segundo-plano" in opciones and self.ventana is None:
            if not self.retenida:
                self.hold()                       # sigue vivo sin ventana, para los recordatorios
                self.retenida = True
            return 0
        self.do_activate()
        if "nueva-tarea" in opciones:
            self.ventana.nueva_tarea()
        return 0

    # --- acciones -------------------------------------------------------------------------------

    def _abrir_tarea(self, _accion, parametro) -> None:
        self.do_activate()
        self.ventana.abrir_tarea(parametro.get_string())

    def _revisar_recordatorios(self) -> bool:
        if self.db is None:
            return True
        for d in recordatorios.pendientes_de_aviso(self.db):
            notificacion = Gio.Notification.new(d.titulo)
            notificacion.set_body(recordatorios.texto_aviso(d))
            notificacion.set_icon(Gio.ThemedIcon.new(APP_ID))
            notificacion.set_default_action_and_target_value("app.abrir-tarea", GLib.Variant.new_string(d.id))
            self.send_notification(f"deber-{d.id}", notificacion)
            recordatorios.marcar_avisado(self.db, d)
        return True


def main() -> int:
    return Aplicacion().run(sys.argv)

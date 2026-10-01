"""Ventana de ajustes: apariencia, atajos de teclado y datos."""

from __future__ import annotations

from pathlib import Path
from typing import Callable, Optional

import gi

gi.require_version("Gtk", "4.0")
gi.require_version("Adw", "1")
from gi.repository import Adw, Gdk, GLib, Gtk  # noqa: E402

from .. import importador, rutas  # noqa: E402
from ..ajustes import ACCIONES, ATAJOS_PREDETERMINADOS, Ajustes  # noqa: E402
from .estilos import Fondo  # noqa: E402

TEMAS = ["sistema", "claro", "oscuro"]


def texto_atajo(acelerador: str) -> str:
    ok, tecla, mods = Gtk.accelerator_parse(acelerador)
    return Gtk.accelerator_get_label(tecla, mods) if ok else acelerador


def _escala(minimo: float, maximo: float, paso: float, valor: float, formato: Callable[[float], str],
            al_cambiar: Callable[[float], None]) -> Gtk.Scale:
    escala = Gtk.Scale.new_with_range(Gtk.Orientation.HORIZONTAL, minimo, maximo, paso)
    escala.set_value(valor)
    escala.set_draw_value(True)
    escala.set_value_pos(Gtk.PositionType.RIGHT)
    escala.set_format_value_func(lambda _s, v: formato(v))
    escala.set_size_request(260, -1)
    escala.set_valign(Gtk.Align.CENTER)
    escala.connect("value-changed", lambda s: al_cambiar(s.get_value()))
    return escala


class DialogoAjustes(Adw.PreferencesWindow):
    def __init__(self, ventana, ajustes: Ajustes, db):
        super().__init__(transient_for=ventana, modal=True, title="Ajustes")
        self.ventana, self.ajustes, self.db = ventana, ajustes, db
        self.set_default_size(700, 680)
        self._grabando: Optional[str] = None
        self._filas_atajo: dict[str, tuple[Gtk.Label, Gtk.Button]] = {}

        self.add(self._pagina_apariencia())
        self.add(self._pagina_atajos())
        self.add(self._pagina_datos())

        teclas = Gtk.EventControllerKey()
        teclas.set_propagation_phase(Gtk.PropagationPhase.CAPTURE)
        teclas.connect("key-pressed", self._tecla)
        self.add_controller(teclas)

    # --- apariencia -----------------------------------------------------------------------------

    def _pagina_apariencia(self) -> Adw.PreferencesPage:
        a = self.ajustes
        pagina = Adw.PreferencesPage(title="Apariencia", icon_name="applications-graphics-symbolic")

        grupo = Adw.PreferencesGroup(title="Tema")
        tema = Adw.ComboRow(title="Tema de la aplicación")
        tema.set_model(Gtk.StringList.new(["Sistema", "Claro", "Oscuro"]))
        tema.set_selected(TEMAS.index(a["tema"]) if a["tema"] in TEMAS else 0)
        tema.connect("notify::selected", lambda r, _p: a.__setitem__("tema", TEMAS[r.get_selected()]))
        grupo.add(tema)
        pagina.add(grupo)

        grupo = Adw.PreferencesGroup(title="Fondo de la app",
                                     description="La imagen se copia a la carpeta de datos de Kiwu; puedes mover o borrar el original.")
        fila = Adw.ActionRow(title="Imagen de fondo")
        elegir = Gtk.Button(label="Elegir imagen…")
        elegir.set_valign(Gtk.Align.CENTER)
        elegir.connect("clicked", lambda _b: self._elegir_fondo())
        quitar = Gtk.Button(label="Quitar")
        quitar.set_valign(Gtk.Align.CENTER)
        quitar.connect("clicked", lambda _b: self._quitar_fondo())
        fila.add_suffix(elegir)
        fila.add_suffix(quitar)
        grupo.add(fila)
        for titulo, clave, maximo, formato, ayuda in (
            ("Visibilidad del fondo", "fondo_visibilidad", 1.0, lambda v: f"{round(v * 100)} %", None),
            ("Opacidad de los paneles", "opacidad_paneles", 1.0, lambda v: f"{round(v * 100)} %",
             "Menos opacidad = se ve más el fondo a través de la app."),
            ("Desenfoque", "desenfoque", 30.0, lambda v: f"{round(v)} px", None),
        ):
            f = Adw.ActionRow(title=titulo, subtitle=ayuda or "")
            f.add_suffix(_escala(0.0, maximo, maximo / 100, float(a[clave]), formato,
                                 lambda v, c=clave: a.__setitem__(c, round(v, 3))))
            grupo.add(f)
        pagina.add(grupo)

        grupo = Adw.PreferencesGroup(
            title="Ventana",
            description="La translucidez real de la ventana deja ver lo que hay detrás; el desenfoque lo aplica tu "
                        "compositor (en Hyprland: decoration { blur { enabled = true } }).")
        f = Adw.ActionRow(title="Opacidad de la ventana")
        f.add_suffix(_escala(0.3, 1.0, 0.01, float(a["opacidad_ventana"]), lambda v: f"{round(v * 100)} %",
                             lambda v: a.__setitem__("opacidad_ventana", round(v, 3))))
        grupo.add(f)
        barra = Adw.SwitchRow(title="Mostrar la barra de título",
                              subtitle="Desactívala en un gestor de ventanas de mosaico como Hyprland.")
        barra.set_active(bool(a["barra_de_titulo"]))
        barra.connect("notify::active", lambda r, _p: a.__setitem__("barra_de_titulo", r.get_active()))
        grupo.add(barra)
        pagina.add(grupo)
        return pagina

    def _elegir_fondo(self) -> None:
        dialogo = Gtk.FileDialog()
        dialogo.set_title("Elige la imagen de fondo")
        filtro = Gtk.FileFilter()
        filtro.add_mime_type("image/*")
        filtro.set_name("Imágenes")
        dialogo.set_default_filter(filtro)

        def listo(dlg, res):
            try:
                archivo = dlg.open_finish(res)
            except GLib.Error:
                return
            if archivo is not None and archivo.get_path() and Fondo.instalar(Path(archivo.get_path())):
                self.ventana.fondo.recargar()

        dialogo.open(self, None, listo)

    def _quitar_fondo(self) -> None:
        Fondo.quitar()
        self.ventana.fondo.recargar()

    # --- atajos ---------------------------------------------------------------------------------

    def _pagina_atajos(self) -> Adw.PreferencesPage:
        pagina = Adw.PreferencesPage(title="Atajos", icon_name="input-keyboard-symbolic")
        grupo = Adw.PreferencesGroup(
            title="Atajos de teclado",
            description="Pulsa «Cambiar» y luego la combinación que quieras (debe incluir Ctrl, Alt o Súper). "
                        "Esc cancela. Estos atajos funcionan con Kiwu enfocado; para atajos globales usa "
                        "`kiwu --nueva-tarea` en tu configuración de Hyprland.")
        for accion, titulo in ACCIONES.items():
            fila = Adw.ActionRow(title=titulo)
            etiqueta_atajo = Gtk.Label(label=texto_atajo(self.ajustes.atajo(accion)))
            etiqueta_atajo.add_css_class("monospace")
            cambiar = Gtk.Button(label="Cambiar")
            cambiar.set_valign(Gtk.Align.CENTER)
            cambiar.connect("clicked", lambda _b, a=accion: self._empezar(a))
            restaurar = Gtk.Button.new_from_icon_name("edit-undo-symbolic")
            restaurar.add_css_class("flat")
            restaurar.set_valign(Gtk.Align.CENTER)
            restaurar.set_tooltip_text("Restaurar el atajo predeterminado")
            restaurar.connect("clicked", lambda _b, a=accion: self._restaurar(a))
            fila.add_suffix(etiqueta_atajo)
            fila.add_suffix(cambiar)
            fila.add_suffix(restaurar)
            grupo.add(fila)
            self._filas_atajo[accion] = (etiqueta_atajo, cambiar)
        pagina.add(grupo)

        todos = Adw.PreferencesGroup()
        boton = Gtk.Button(label="Restaurar todos")
        boton.set_halign(Gtk.Align.END)
        boton.connect("clicked", lambda _b: (self.ajustes.restaurar_atajos(), self._refrescar_atajos()))
        todos.add(boton)
        pagina.add(todos)
        return pagina

    def _refrescar_atajos(self) -> None:
        for accion, (etiqueta_atajo, cambiar) in self._filas_atajo.items():
            etiqueta_atajo.set_label(texto_atajo(self.ajustes.atajo(accion)))
            cambiar.set_label("Cambiar")
        self._grabando = None

    def _empezar(self, accion: str) -> None:
        self._refrescar_atajos()
        self._grabando = accion
        etiqueta_atajo, cambiar = self._filas_atajo[accion]
        etiqueta_atajo.set_label("Pulsa la combinación…")
        cambiar.set_label("Cancelar")

    def _restaurar(self, accion: str) -> None:
        self.ajustes.fijar_atajo(accion, None)
        self._refrescar_atajos()

    def _tecla(self, _ctrl, keyval, _keycode, estado) -> bool:
        accion = self._grabando
        if accion is None:
            return False
        if keyval == Gdk.KEY_Escape:
            self._refrescar_atajos()
            return True
        if keyval in (Gdk.KEY_Control_L, Gdk.KEY_Control_R, Gdk.KEY_Shift_L, Gdk.KEY_Shift_R, Gdk.KEY_Alt_L,
                      Gdk.KEY_Alt_R, Gdk.KEY_Super_L, Gdk.KEY_Super_R, Gdk.KEY_Meta_L, Gdk.KEY_Meta_R,
                      Gdk.KEY_ISO_Level3_Shift):
            return True                                  # esperar a la tecla de verdad
        mods = estado & Gtk.accelerator_get_default_mod_mask()
        necesarios = Gdk.ModifierType.CONTROL_MASK | Gdk.ModifierType.ALT_MASK | Gdk.ModifierType.SUPER_MASK
        if not (mods & necesarios):
            self.add_toast(Adw.Toast.new("Incluye Ctrl, Alt o Súper para no chocar con la escritura."))
            return True
        acelerador = Gtk.accelerator_name(Gdk.keyval_to_lower(keyval), mods)
        otro = self.ajustes.conflicto(acelerador, excepto=accion)
        if otro:
            self.add_toast(Adw.Toast.new(f"{texto_atajo(acelerador)} ya se usa en «{ACCIONES[otro]}»."))
            return True
        self.ajustes.fijar_atajo(accion, acelerador)
        self._refrescar_atajos()
        return True

    # --- datos ----------------------------------------------------------------------------------

    def _pagina_datos(self) -> Adw.PreferencesPage:
        pagina = Adw.PreferencesPage(title="Datos", icon_name="folder-symbolic")
        grupo = Adw.PreferencesGroup(
            title="Importar desde Kiwu para macOS",
            description="Copia tus listas, tareas, cuadernos y notas. Elige la carpeta que contiene "
                        "GestorUniversitario.sqlite y Notas.sqlite (en macOS: ~/Library/Application Support/Kiwu). "
                        "No se borra ni se duplica nada.")
        fila = Adw.ActionRow(title="Importar desde una carpeta de macOS")
        b = Gtk.Button(label="Elegir carpeta…")
        b.set_valign(Gtk.Align.CENTER)
        b.connect("clicked", lambda _b: self._importar_macos())
        fila.add_suffix(b)
        grupo.add(fila)
        pagina.add(grupo)

        grupo = Adw.PreferencesGroup(title="Copia de seguridad (.kiwu.json)",
                                     description="Un único archivo con todos tus datos, para mover Kiwu a otro equipo.")
        for titulo, texto, funcion in (("Exportar una copia", "Exportar…", self._exportar),
                                       ("Importar una copia", "Importar…", self._importar_copia)):
            fila = Adw.ActionRow(title=titulo)
            b = Gtk.Button(label=texto)
            b.set_valign(Gtk.Align.CENTER)
            b.connect("clicked", lambda _b, f=funcion: f())
            fila.add_suffix(b)
            grupo.add(fila)
        pagina.add(grupo)

        grupo = Adw.PreferencesGroup(title="Ubicación de los datos")
        fila = Adw.ActionRow(title="Base de datos", subtitle=str(rutas.ruta_db()))
        fila.set_subtitle_selectable(True)
        grupo.add(fila)
        pagina.add(grupo)
        return pagina

    def _avisar(self, texto: str) -> None:
        self.add_toast(Adw.Toast.new(texto))

    @staticmethod
    def _resumen(r: dict) -> str:
        return (f"Importado: {r['materias']} listas, {r['deberes']} tareas, {r['cuadernos']} cuadernos, "
                f"{r['paginas']} notas.")

    def _importar_macos(self) -> None:
        dialogo = Gtk.FileDialog()
        dialogo.set_title("Carpeta de Kiwu / TaskFlow de macOS")

        def listo(dlg, res):
            try:
                carpeta = dlg.select_folder_finish(res)
            except GLib.Error:
                return
            base = Path(carpeta.get_path())
            tareas, notas = base / "GestorUniversitario.sqlite", base / "Notas.sqlite"
            if not tareas.exists() and not notas.exists():
                self._avisar("No se encontraron GestorUniversitario.sqlite ni Notas.sqlite en esa carpeta.")
                return
            try:
                r = importador.importar_macos(self.db, tareas if tareas.exists() else None,
                                              notas if notas.exists() else None)
            except Exception as e:
                self._avisar(f"No se pudo importar: {e}")
                return
            self._avisar(self._resumen(r))

        dialogo.select_folder(self, None, listo)

    def _exportar(self) -> None:
        dialogo = Gtk.FileDialog()
        dialogo.set_title("Guardar copia de seguridad")
        dialogo.set_initial_name("kiwu.kiwu.json")

        def listo(dlg, res):
            try:
                archivo = dlg.save_finish(res)
            except GLib.Error:
                return
            try:
                importador.exportar_copia(self.db, archivo.get_path())
            except OSError as e:
                self._avisar(f"No se pudo guardar: {e}")
                return
            self._avisar("Copia guardada.")

        dialogo.save(self, None, listo)

    def _importar_copia(self) -> None:
        dialogo = Gtk.FileDialog()
        dialogo.set_title("Elige una copia de seguridad de Kiwu")

        def listo(dlg, res):
            try:
                archivo = dlg.open_finish(res)
            except GLib.Error:
                return
            try:
                r = importador.importar_copia(self.db, archivo.get_path())
            except importador.ErrorImportacion as e:
                self._avisar(str(e))
                return
            self._avisar(self._resumen(r))

        dialogo.open(self, None, listo)

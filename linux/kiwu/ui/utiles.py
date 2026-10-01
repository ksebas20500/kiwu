"""Pequeñas ayudas de interfaz compartidas por todas las vistas."""

from __future__ import annotations

import threading
from datetime import datetime
from typing import Callable, Optional, Sequence

import gi

gi.require_version("Gtk", "4.0")
gi.require_version("Adw", "1")
from gi.repository import Adw, Gdk, Gio, GLib, Gtk, Pango  # noqa: E402

MESES = ["ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic"]
MESES_LARGOS = ["enero", "febrero", "marzo", "abril", "mayo", "junio", "julio", "agosto", "septiembre",
                "octubre", "noviembre", "diciembre"]
DIAS_SEMANA = ["lun", "mar", "mié", "jue", "vie", "sáb", "dom"]


def fecha_corta(ts: float) -> str:
    d = datetime.fromtimestamp(ts).date()
    hoy = datetime.now().date()
    delta = (d - hoy).days
    if delta == 0:
        return "Hoy"
    if delta == 1:
        return "Mañana"
    if delta == -1:
        return "Ayer"
    return f"{d.day} {MESES[d.month - 1]}"


def fecha_completa(ts: float) -> str:
    return f"{fecha_corta(ts)}, {datetime.fromtimestamp(ts).strftime('%H:%M')}"


def limpiar(contenedor: Gtk.Widget) -> None:
    """Quita todos los hijos de una caja, lista o similar."""
    hijo = contenedor.get_first_child()
    while hijo is not None:
        siguiente = hijo.get_next_sibling()
        contenedor.remove(hijo)
        hijo = siguiente


def etiqueta(texto: str = "", clases: Sequence[str] = (), xalign: float = 0.0, elipsis: bool = False,
             ajustar: bool = False) -> Gtk.Label:
    e = Gtk.Label(label=texto, xalign=xalign)
    for c in clases:
        e.add_css_class(c)
    if elipsis:
        e.set_ellipsize(Pango.EllipsizeMode.END)
    if ajustar:
        e.set_wrap(True)
        e.set_wrap_mode(Pango.WrapMode.WORD_CHAR)
    return e


def boton_icono(icono: str, ayuda: Optional[str] = None, plano: bool = True) -> Gtk.Button:
    b = Gtk.Button.new_from_icon_name(icono)
    if plano:
        b.add_css_class("flat")
    if ayuda:
        b.set_tooltip_text(ayuda)
    return b


def abrir_uri(uri: str) -> None:
    try:
        Gio.AppInfo.launch_default_for_uri(uri, None)
    except GLib.Error:
        pass


def abrir_archivo(ruta: str) -> None:
    abrir_uri(GLib.filename_to_uri(ruta, None))


def en_segundo_plano(trabajo: Callable[[], object], al_terminar: Callable[[object], None]) -> None:
    """Ejecuta `trabajo` en un hilo y llama a `al_terminar(resultado)` en el hilo de la interfaz."""
    def correr():
        try:
            resultado = trabajo()
        except Exception:
            resultado = None
        GLib.idle_add(lambda: (al_terminar(resultado), False)[1])

    threading.Thread(target=correr, daemon=True).start()


def menu_contextual(widget: Gtk.Widget, items: Callable[[], list]) -> None:
    """Clic derecho sobre `widget`: muestra un menú. `items()` devuelve [(texto, función)] o None (separador)."""
    gesto = Gtk.GestureClick(button=Gdk.BUTTON_SECONDARY)

    def al_pulsar(_g, _n, x, y):
        pop = Gtk.Popover()
        pop.set_has_arrow(False)
        pop.set_parent(widget)
        caja = Gtk.Box(orientation=Gtk.Orientation.VERTICAL)
        for item in items():
            if item is None:
                caja.append(Gtk.Separator())
                continue
            texto, funcion = item
            b = Gtk.Button(label=texto)
            b.add_css_class("flat")
            b.get_child().set_xalign(0)
            b.connect("clicked", lambda _b, f=funcion, p=pop: (p.popdown(), f()))
            caja.append(b)
        pop.set_child(caja)
        r = Gdk.Rectangle()
        r.x, r.y, r.width, r.height = int(x), int(y), 1, 1
        pop.set_pointing_to(r)
        pop.connect("closed", lambda p: GLib.idle_add(p.unparent))
        pop.popup()

    gesto.connect("pressed", al_pulsar)
    widget.add_controller(gesto)


def confirmar(ventana: Gtk.Window, titulo: str, texto: str, etiqueta_accion: str, al_aceptar: Callable[[], None],
              destructivo: bool = True) -> None:
    """Diálogo de confirmación (usa AlertDialog si la versión de libadwaita lo trae)."""
    if hasattr(Adw, "AlertDialog"):
        d = Adw.AlertDialog.new(titulo, texto)
        d.add_response("cancelar", "Cancelar")
        d.add_response("aceptar", etiqueta_accion)
        d.set_response_appearance("aceptar", Adw.ResponseAppearance.DESTRUCTIVE if destructivo
                                  else Adw.ResponseAppearance.SUGGESTED)
        d.set_default_response("cancelar")
        d.set_close_response("cancelar")
        d.choose(ventana, None, lambda dlg, res: al_aceptar() if dlg.choose_finish(res) == "aceptar" else None)
    else:
        d = Adw.MessageDialog(transient_for=ventana, heading=titulo, body=texto)
        d.add_response("cancelar", "Cancelar")
        d.add_response("aceptar", etiqueta_accion)
        d.set_response_appearance("aceptar", Adw.ResponseAppearance.DESTRUCTIVE if destructivo
                                  else Adw.ResponseAppearance.SUGGESTED)
        d.connect("response", lambda _d, r: al_aceptar() if r == "aceptar" else None)
        d.present()


def pedir_texto(ventana: Gtk.Window, titulo: str, marcador: str, al_aceptar: Callable[[str], None],
                texto_inicial: str = "", etiqueta_accion: str = "Crear") -> None:
    """Pequeña ventana con un campo de texto."""
    dialogo = Adw.Window(transient_for=ventana, modal=True, title=titulo, default_width=380)
    caja = Gtk.Box(orientation=Gtk.Orientation.VERTICAL, spacing=14)
    for lado in ("top", "bottom", "start", "end"):
        getattr(caja, f"set_margin_{lado}")(20)
    caja.append(etiqueta(titulo, ["title-2"]))
    campo = Gtk.Entry(placeholder_text=marcador, text=texto_inicial)
    caja.append(campo)
    botones = Gtk.Box(spacing=8, halign=Gtk.Align.END)
    cancelar = Gtk.Button(label="Cancelar")
    aceptar = Gtk.Button(label=etiqueta_accion)
    aceptar.add_css_class("suggested-action")
    botones.append(cancelar)
    botones.append(aceptar)
    caja.append(botones)

    def terminar(*_):
        texto = campo.get_text().strip()
        if texto:
            dialogo.close()
            al_aceptar(texto)

    cancelar.connect("clicked", lambda *_: dialogo.close())
    aceptar.connect("clicked", terminar)
    campo.connect("activate", terminar)
    marco = Adw.ToolbarView()
    marco.add_top_bar(Adw.HeaderBar())
    marco.set_content(caja)
    dialogo.set_content(marco)
    dialogo.present()
    campo.grab_focus()


EMOJIS = ["📝", "📖", "📚", "💡", "🧠", "🔬", "🧪", "📐", "💻", "🧮", "📊", "🗂️", "📌", "⭐️", "🎯", "🔥", "✅", "❓",
          "📅", "🗒️", "🎓", "🏛️", "⚖️", "🩺", "🌍", "🎨", "🎵", "⚙️", "🔍", "📎", "🧾", "🖋️", "🚀", "🧬", "📈", "🗣️"]


def selector_emoji(al_elegir: Callable[[str], None], al_quitar: Optional[Callable[[], None]] = None) -> Gtk.Popover:
    pop = Gtk.Popover()
    caja = Gtk.Box(orientation=Gtk.Orientation.VERTICAL, spacing=8)
    for lado in ("top", "bottom", "start", "end"):
        getattr(caja, f"set_margin_{lado}")(10)
    rejilla = Gtk.Grid(row_spacing=4, column_spacing=4)
    for i, emoji in enumerate(EMOJIS):
        b = Gtk.Button(label=emoji)
        b.add_css_class("flat")
        b.connect("clicked", lambda _b, e=emoji: (pop.popdown(), al_elegir(e)))
        rejilla.attach(b, i % 6, i // 6, 1, 1)
    caja.append(rejilla)
    if al_quitar:
        quitar = Gtk.Button(label="Quitar icono")
        quitar.connect("clicked", lambda _b: (pop.popdown(), al_quitar()))
        caja.append(quitar)
    pop.set_child(caja)
    return pop


def diccionario_margenes(widget: Gtk.Widget, arriba=0, abajo=0, inicio=0, fin=0) -> None:
    widget.set_margin_top(arriba)
    widget.set_margin_bottom(abajo)
    widget.set_margin_start(inicio)
    widget.set_margin_end(fin)

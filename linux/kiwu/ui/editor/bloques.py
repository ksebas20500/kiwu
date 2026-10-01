"""Los distintos tipos de bloque del editor de notas."""

from __future__ import annotations

import base64
import os
from typing import Optional

import gi

gi.require_version("Gtk", "4.0")
gi.require_version("Adw", "1")
from gi.repository import Adw, Gdk, Gio, GLib, Gtk  # noqa: E402

try:
    gi.require_version("GtkSource", "5")
    from gi.repository import GtkSource  # noqa: E402
except (ValueError, ImportError):          # sin GtkSourceView el código se ve sin colores
    GtkSource = None

try:
    gi.require_version("WebKit", "6.0")
    from gi.repository import WebKit  # noqa: E402
except (ValueError, ImportError):          # sin WebKitGTK los vídeos se abren en el navegador
    WebKit = None

from ... import enlaces, lenguajes  # noqa: E402
from ...modelos import (BLOQUES_DE_TEXTO, ICONOS_BLOQUE, TIPOS_BLOQUE, TITULOS_BLOQUE, Bloque,  # noqa: E402
                        runs_o_nada)
from ..utiles import abrir_archivo, abrir_uri, en_segundo_plano, etiqueta  # noqa: E402
from . import texto_rico  # noqa: E402

TIPOS_CONVERTIBLES = [t for t in TIPOS_BLOQUE if t not in ("image", "file", "divider")]

MARCADORES = ["•", "◦", "▪", "•", "◦"]

PLACEHOLDERS = {
    "paragraph": "Escribe algo, o pulsa / para ver los bloques",
    "heading": "Título",
    "subheading": "Título 2",
    "bulletedList": "Elemento de lista",
    "numberedList": "Elemento de lista",
    "checklist": "Tarea",
    "quote": "Cita",
    "callout": "Escribe una nota destacada",
}

PREFIJOS_MARKDOWN = [
    ("# ", "heading"), ("## ", "subheading"), ("- ", "bulletedList"), ("* ", "bulletedList"),
    ("1. ", "numberedList"), ("[] ", "checklist"), ("> ", "quote"), ("```", "code"),
]


# --- utilidades de línea -------------------------------------------------------------------------

def en_primera_linea(vista: Gtk.TextView) -> bool:
    buf = vista.get_buffer()
    cursor = vista.get_iter_location(buf.get_iter_at_mark(buf.get_insert()))
    primera = vista.get_iter_location(buf.get_start_iter())
    return cursor.y < primera.y + primera.height


def en_ultima_linea(vista: Gtk.TextView) -> bool:
    buf = vista.get_buffer()
    cursor = vista.get_iter_location(buf.get_iter_at_mark(buf.get_insert()))
    ultima = vista.get_iter_location(buf.get_end_iter())
    return cursor.y >= ultima.y


def cursor_al_inicio(buf: Gtk.TextBuffer) -> bool:
    return buf.get_iter_at_mark(buf.get_insert()).is_start() and not buf.get_has_selection()


# --- base ----------------------------------------------------------------------------------------

class BloqueBase(Gtk.Box):
    es_texto = False

    def __init__(self, editor, bloque: Bloque):
        super().__init__(orientation=Gtk.Orientation.VERTICAL)
        self.editor = editor
        self.bloque = bloque
        self.add_css_class("kiwu-bloque")
        self.aplicar_sangria()
        self._grupo = self._crear_acciones()
        self.insert_action_group("bloque", self._grupo)

    @property
    def kind(self) -> str:
        return self.bloque.kind

    def aplicar_sangria(self) -> None:
        self.set_margin_start(self.bloque.indent * 26)

    def a_bloque(self) -> Bloque:
        return self.bloque

    def enfocar(self, al_final: bool = True) -> None:
        pass

    def al_quitar(self) -> None:
        """El editor llama a esto antes de sacar el bloque."""

    # --- menú contextual (clic derecho) ---------------------------------------------------------

    def _crear_acciones(self) -> Gio.SimpleActionGroup:
        grupo = Gio.SimpleActionGroup()

        def simple(nombre: str, funcion, tipo_parametro: Optional[str] = None):
            accion = Gio.SimpleAction.new(nombre, GLib.VariantType.new(tipo_parametro) if tipo_parametro else None)
            accion.connect("activate", funcion)
            grupo.add_action(accion)

        simple("convertir", lambda _a, p: self.editor.convertir(self, p.get_string()), "s")
        simple("sangria-mas", lambda *_: self.editor.cambiar_sangria(self, 1))
        simple("sangria-menos", lambda *_: self.editor.cambiar_sangria(self, -1))
        simple("subir", lambda *_: self.editor.mover(self, -1))
        simple("bajar", lambda *_: self.editor.mover(self, 1))
        simple("duplicar", lambda *_: self.editor.duplicar(self))
        simple("eliminar", lambda *_: self.editor.eliminar(self))
        return grupo

    def modelo_menu(self, con_conversion: bool = True) -> Gio.Menu:
        menu = Gio.Menu()
        if con_conversion:
            conv = Gio.Menu()
            for tipo in TIPOS_CONVERTIBLES:
                conv.append(TITULOS_BLOQUE[tipo], f"bloque.convertir::{tipo}")
            menu.append_submenu("Convertir en", conv)
        sangria = Gio.Menu()
        sangria.append("Aumentar sangría", "bloque.sangria-mas")
        sangria.append("Reducir sangría", "bloque.sangria-menos")
        menu.append_section(None, sangria)
        orden = Gio.Menu()
        orden.append("Mover arriba", "bloque.subir")
        orden.append("Mover abajo", "bloque.bajar")
        orden.append("Duplicar", "bloque.duplicar")
        menu.append_section(None, orden)
        borrar = Gio.Menu()
        borrar.append("Eliminar bloque", "bloque.eliminar")
        menu.append_section(None, borrar)
        return menu

    def instalar_menu_clic_derecho(self, widget: Gtk.Widget, con_conversion: bool = False) -> None:
        gesto = Gtk.GestureClick(button=Gdk.BUTTON_SECONDARY)

        def pulsado(_g, _n, x, y):
            pop = Gtk.PopoverMenu.new_from_model(self.modelo_menu(con_conversion))
            pop.set_parent(widget)
            r = Gdk.Rectangle()
            r.x, r.y, r.width, r.height = int(x), int(y), 1, 1
            pop.set_pointing_to(r)
            pop.set_has_arrow(False)
            pop.connect("closed", lambda p: GLib.idle_add(p.unparent))
            pop.popup()

        gesto.connect("pressed", pulsado)
        widget.add_controller(gesto)


# --- texto ---------------------------------------------------------------------------------------

class BloqueTexto(BloqueBase):
    es_texto = True

    def __init__(self, editor, bloque: Bloque):
        super().__init__(editor, bloque)
        self._cargando = True
        self.slash_descartado = False
        kind = bloque.kind

        self.vista = Gtk.TextView()
        self.vista.set_wrap_mode(Gtk.WrapMode.WORD_CHAR)
        self.vista.set_hexpand(True)
        self.vista.set_top_margin(3)
        self.vista.set_bottom_margin(3)
        self.vista.set_left_margin(0)
        self.vista.set_right_margin(0)
        self.vista.set_accepts_tab(False)
        self.buf = Gtk.TextBuffer(tag_table=texto_rico.tabla())
        self.vista.set_buffer(self.buf)
        self.formato = texto_rico.Formato(self.buf)
        if kind == "heading":
            self.vista.add_css_class("kiwu-h1")
        elif kind == "subheading":
            self.vista.add_css_class("kiwu-h2")
        elif kind == "quote":
            self.vista.add_css_class("kiwu-cita")

        # Marcador a la izquierda del texto (viñeta, número, casilla o icono)
        self.marcador: Optional[Gtk.Widget] = None
        if kind == "bulletedList":
            self.marcador = etiqueta(MARCADORES[min(bloque.indent, 4)], xalign=0.5)
            self.marcador.set_size_request(14, -1)
        elif kind == "numberedList":
            self.marcador = etiqueta("1.", ["kiwu-suave"], xalign=1.0)
            self.marcador.set_size_request(22, -1)
        elif kind == "checklist":
            self.marcador = Gtk.CheckButton()
            self.marcador.set_active(bloque.is_checked)
            self.marcador.set_valign(Gtk.Align.START)
            self.marcador.connect("toggled", self._casilla)
        elif kind == "callout":
            self.marcador = etiqueta("💡", xalign=0.5)
        if self.marcador is not None:
            self.marcador.set_valign(Gtk.Align.START)
            self.marcador.set_margin_top(4)

        # Texto de ayuda cuando el bloque está vacío
        marco = Gtk.Overlay()
        marco.set_child(self.vista)
        self.ayuda = etiqueta(PLACEHOLDERS.get(kind, ""), ["kiwu-suave"])
        self.ayuda.set_halign(Gtk.Align.START)
        self.ayuda.set_valign(Gtk.Align.START)
        self.ayuda.set_margin_top(3)
        self.ayuda.set_can_target(False)
        marco.add_overlay(self.ayuda)
        marco.set_hexpand(True)

        fila = Gtk.Box(spacing=8)
        if self.marcador is not None:
            fila.append(self.marcador)
        fila.append(marco)
        if kind == "callout":
            fila.add_css_class("kiwu-callout")
        elif kind == "quote":
            fila.add_css_class("kiwu-cita-caja")
        self.append(fila)

        texto_rico.cargar(self.buf, bloque.text, bloque.runs)
        if kind == "checklist":
            self.formato.poner_hecho(bloque.is_checked)
        self.ayuda.set_visible(not bloque.text)

        self.buf.connect("changed", self._cambio)
        self.buf.connect("notify::has-selection", self._seleccion)
        self.buf.connect("mark-set", self._marca)
        teclas = Gtk.EventControllerKey()
        teclas.set_propagation_phase(Gtk.PropagationPhase.CAPTURE)
        teclas.connect("key-pressed", self._tecla)
        self.vista.add_controller(teclas)
        clic = Gtk.GestureClick(button=Gdk.BUTTON_PRIMARY)
        clic.set_propagation_phase(Gtk.PropagationPhase.CAPTURE)   # no interfiere con la selección del texto
        clic.connect("released", self._soltado)
        self.vista.add_controller(clic)
        self.vista.set_extra_menu(self.modelo_menu(con_conversion=True))
        self._cargando = False

    # --- datos ----------------------------------------------------------------------------------

    def a_bloque(self) -> Bloque:
        self._sincronizar()
        return self.bloque

    def _sincronizar(self) -> None:
        ini, fin = self.buf.get_bounds()
        self.bloque.text = self.buf.get_text(ini, fin, False)
        self.bloque.runs = runs_o_nada(texto_rico.runs_de_rango(self.buf, ini, fin))

    def texto(self) -> str:
        ini, fin = self.buf.get_bounds()
        return self.buf.get_text(ini, fin, False)

    def aplicar_sangria(self) -> None:
        super().aplicar_sangria()
        marcador = getattr(self, "marcador", None)
        if self.bloque.kind == "bulletedList" and isinstance(marcador, Gtk.Label):
            marcador.set_label(MARCADORES[min(self.bloque.indent, 4)])

    def fijar_numero(self, n: int) -> None:
        if self.kind == "numberedList" and isinstance(self.marcador, Gtk.Label):
            self.marcador.set_label(f"{n}.")

    def enfocar(self, al_final: bool = True) -> None:
        self.vista.grab_focus()
        it = self.buf.get_end_iter() if al_final else self.buf.get_start_iter()
        self.buf.place_cursor(it)

    # --- eventos del buffer ---------------------------------------------------------------------

    def _casilla(self, boton: Gtk.CheckButton) -> None:
        self.bloque.is_checked = boton.get_active()
        self.formato.poner_hecho(self.bloque.is_checked)
        self.editor.cambio()

    def _cambio(self, _buf) -> None:
        if self._cargando:
            return
        self._sincronizar()
        self.ayuda.set_visible(not self.bloque.text)
        self.slash_descartado = False
        self.editor.cambio()
        if self.kind == "paragraph":
            for prefijo, tipo in PREFIJOS_MARKDOWN:
                if self.bloque.text.startswith(prefijo):
                    self.editor.convertir(self, tipo, self.bloque.text[len(prefijo):])
                    return
        self.editor.actualizar_slash(self)

    def _seleccion(self, *_args) -> None:
        self.editor.seleccion_cambio(self, self.buf.get_has_selection())

    def _marca(self, _buf, _it, marca) -> None:
        if marca is self.buf.get_insert() and self.editor.slash.visible and self.editor.slash.bloque is self:
            self.editor.actualizar_slash(self)

    def opciones_slash(self) -> list[str]:
        t = self.bloque.text
        if self.kind != "paragraph" or not t.startswith("/") or "\n" in t or self.slash_descartado:
            return []
        filtro = t[1:].lower()
        return [k for k in TIPOS_BLOQUE if k != "paragraph" and (not filtro or filtro in TITULOS_BLOQUE[k].lower())]

    def _soltado(self, _gesto, _n, x, y) -> None:
        """Clic sobre un enlace: se abre en el navegador."""
        if self.buf.get_has_selection():
            return
        bx, by = self.vista.window_to_buffer_coords(Gtk.TextWindowType.WIDGET, int(x), int(y))
        ok, it = self.vista.get_iter_at_location(bx, by)
        if not ok:
            return
        for tag in it.get_tags():
            nombre = tag.get_property("name") or ""
            if nombre.startswith("link:"):
                abrir_uri(nombre[5:])
                return

    # --- teclado --------------------------------------------------------------------------------

    def _tecla(self, _ctrl, keyval, _keycode, estado) -> bool:
        ctrl = bool(estado & Gdk.ModifierType.CONTROL_MASK)
        mayus = bool(estado & Gdk.ModifierType.SHIFT_MASK)
        alt = bool(estado & Gdk.ModifierType.ALT_MASK)
        slash_activo = self.editor.slash.visible and self.editor.slash.bloque is self

        if slash_activo:
            if keyval == Gdk.KEY_Up:
                self.editor.slash.mover(-1)
                return True
            if keyval == Gdk.KEY_Down:
                self.editor.slash.mover(1)
                return True
            if keyval in (Gdk.KEY_Return, Gdk.KEY_KP_Enter, Gdk.KEY_Tab):
                self.editor.slash.elegir_actual()
                return True
            if keyval == Gdk.KEY_Escape:
                self.slash_descartado = True
                self.editor.slash.ocultar()
                return True

        if ctrl and not alt:
            tecla = Gdk.keyval_to_lower(keyval)
            if tecla == Gdk.KEY_b and not mayus:
                self.formato.alternar("b")
                return True
            if tecla == Gdk.KEY_i and not mayus:
                self.formato.alternar("i")
                return True
            if tecla == Gdk.KEY_u and not mayus:
                self.formato.alternar("u")
                return True
            if tecla == Gdk.KEY_e and not mayus:
                self.formato.alternar("code")
                return True
            if tecla == Gdk.KEY_x and mayus:
                self.formato.alternar("s")
                return True
            if tecla == Gdk.KEY_k and not mayus:
                self.editor.pedir_enlace(self)
                return True
            return False

        if keyval in (Gdk.KEY_Return, Gdk.KEY_KP_Enter) and not mayus:
            self._enter()
            return True
        if keyval == Gdk.KEY_BackSpace and not self.buf.get_has_selection():
            if cursor_al_inicio(self.buf):
                if not self.texto():
                    self.editor.borrar_vacio(self)
                    return True
                if self.bloque.indent > 0:
                    self.editor.cambiar_sangria(self, -1)
                    return True
                if self.kind != "paragraph":
                    self.editor.convertir(self, "paragraph", self.texto())
                    return True
            return False
        if keyval == Gdk.KEY_Up and not mayus and en_primera_linea(self.vista):
            return self.editor.navegar(self, -1)
        if keyval == Gdk.KEY_Down and not mayus and en_ultima_linea(self.vista):
            return self.editor.navegar(self, 1)
        if keyval == Gdk.KEY_Tab and not mayus:
            self.editor.cambiar_sangria(self, 1)
            return True
        if keyval == Gdk.KEY_ISO_Left_Tab or (keyval == Gdk.KEY_Tab and mayus):
            self.editor.cambiar_sangria(self, -1)
            return True
        return False

    def _enter(self) -> None:
        buf = self.buf
        if buf.get_has_selection():
            buf.delete_selection(True, True)
        cursor = buf.get_iter_at_mark(buf.get_insert())
        fin = buf.get_end_iter()
        resto_runs = texto_rico.runs_de_rango(buf, cursor, fin)
        resto = buf.get_text(cursor, fin, False)
        if resto:
            buf.delete(cursor, fin)
        self.editor.enter(self, resto, runs_o_nada(resto_runs))


# --- código --------------------------------------------------------------------------------------

class BloqueCodigo(BloqueBase):
    es_texto = True

    def __init__(self, editor, bloque: Bloque):
        super().__init__(editor, bloque)
        self._cargando = True
        self.formato = None
        if GtkSource is not None:
            self.buf = GtkSource.Buffer()
            self.vista = GtkSource.View.new_with_buffer(self.buf)
            self.vista.set_auto_indent(True)
            self.vista.set_insert_spaces_instead_of_tabs(True)
            self.vista.set_tab_width(4)
            self.vista.set_indent_width(4)
            self.vista.set_show_line_numbers(False)
            self.vista.set_highlight_current_line(False)
            self.buf.set_highlight_syntax(True)
            self.buf.set_highlight_matching_brackets(False)
        else:
            self.vista = Gtk.TextView()
            self.buf = self.vista.get_buffer()
        self.vista.set_monospace(True)
        self.vista.set_wrap_mode(Gtk.WrapMode.WORD_CHAR)
        self.vista.set_accepts_tab(False)
        for lado in ("left", "right"):
            getattr(self.vista, f"set_{lado}_margin")(12)
        self.vista.set_top_margin(8)
        self.vista.set_bottom_margin(10)
        self.vista.set_hexpand(True)
        self.buf.set_text(bloque.text, -1)

        marco = Gtk.Overlay()
        marco.set_child(self.vista)
        self.ayuda = etiqueta("Escribe o pega tu código", ["kiwu-suave"])
        self.ayuda.set_halign(Gtk.Align.START)
        self.ayuda.set_valign(Gtk.Align.START)
        self.ayuda.set_margin_start(12)
        self.ayuda.set_margin_top(8)
        self.ayuda.set_can_target(False)
        self.ayuda.set_visible(not bloque.text)
        marco.add_overlay(self.ayuda)

        # Cabecera: lenguaje y botón de copiar
        nombres = ["Automático"] + [n for _i, n, _g in lenguajes.LENGUAJES]
        self.selector = Gtk.DropDown.new_from_strings(nombres)
        self.selector.add_css_class("flat")
        ids = [None] + [i for i, _n, _g in lenguajes.LENGUAJES]
        self._ids = ids
        self.selector.set_selected(ids.index(bloque.language) if bloque.language in ids else 0)
        self.selector.connect("notify::selected", self._lenguaje_elegido)
        self.etiqueta_auto = etiqueta("", ["kiwu-minimo"])
        copiar = Gtk.Button.new_from_icon_name("edit-copy-symbolic")
        copiar.add_css_class("flat")
        copiar.set_tooltip_text("Copiar el código")
        copiar.connect("clicked", self._copiar)
        cabecera = Gtk.Box(spacing=6)
        cabecera.set_margin_start(6)
        cabecera.set_margin_end(6)
        cabecera.set_margin_top(4)
        cabecera.append(self.selector)
        cabecera.append(self.etiqueta_auto)
        espacio = Gtk.Box()
        espacio.set_hexpand(True)
        cabecera.append(espacio)
        cabecera.append(copiar)

        caja = Gtk.Box(orientation=Gtk.Orientation.VERTICAL)
        caja.add_css_class("kiwu-codigo")
        caja.append(cabecera)
        caja.append(marco)
        self.caja = caja
        self.append(caja)

        self._pendiente_deteccion = 0
        self._aplicar_tema()
        Adw.StyleManager.get_default().connect("notify::dark", lambda *_: self._aplicar_tema())
        self._aplicar_lenguaje()

        self.buf.connect("changed", self._cambio)
        teclas = Gtk.EventControllerKey()
        teclas.set_propagation_phase(Gtk.PropagationPhase.CAPTURE)
        teclas.connect("key-pressed", self._tecla)
        self.vista.add_controller(teclas)
        self.vista.set_extra_menu(self.modelo_menu(con_conversion=True))
        self._cargando = False

    def al_quitar(self) -> None:
        if self._pendiente_deteccion:
            GLib.source_remove(self._pendiente_deteccion)
            self._pendiente_deteccion = 0

    def a_bloque(self) -> Bloque:
        self.bloque.text = self.texto()
        self.bloque.runs = None
        return self.bloque

    def texto(self) -> str:
        ini, fin = self.buf.get_bounds()
        return self.buf.get_text(ini, fin, False)

    def enfocar(self, al_final: bool = True) -> None:
        self.vista.grab_focus()
        self.buf.place_cursor(self.buf.get_end_iter() if al_final else self.buf.get_start_iter())

    # --- aspecto --------------------------------------------------------------------------------

    def _aplicar_tema(self) -> None:
        oscuro = Adw.StyleManager.get_default().get_dark()
        self.caja.remove_css_class("kiwu-codigo-claro" if oscuro else "kiwu-codigo-oscuro")
        self.caja.add_css_class("kiwu-codigo-oscuro" if oscuro else "kiwu-codigo-claro")
        if GtkSource is not None:
            gestor = GtkSource.StyleSchemeManager.get_default()
            esquema = gestor.get_scheme("kiwu-dark" if oscuro else "kiwu-light")
            if esquema is not None:
                self.buf.set_style_scheme(esquema)

    def _lenguaje_resuelto(self) -> str:
        return self.bloque.language or lenguajes.detectar(self.texto())

    def _aplicar_lenguaje(self) -> None:
        id_ = self._lenguaje_resuelto()
        self.etiqueta_auto.set_label(f"detectado: {lenguajes.nombre(id_)}" if self.bloque.language is None else "")
        if GtkSource is None:
            return
        gid = lenguajes.id_gtksource(id_)
        lenguaje = GtkSource.LanguageManager.get_default().get_language(gid) if gid else None
        self.buf.set_language(lenguaje)

    def _lenguaje_elegido(self, selector, _p) -> None:
        self.bloque.language = self._ids[selector.get_selected()]
        self._aplicar_lenguaje()
        self.editor.cambio()

    def _copiar(self, _b) -> None:
        Gdk.Display.get_default().get_clipboard().set_text(self.texto())

    # --- eventos --------------------------------------------------------------------------------

    def _cambio(self, _buf) -> None:
        if self._cargando:
            return
        self.bloque.text = self.texto()
        self.ayuda.set_visible(not self.bloque.text)
        self.editor.cambio()
        if self.bloque.language is None:
            if self._pendiente_deteccion:
                GLib.source_remove(self._pendiente_deteccion)
            self._pendiente_deteccion = GLib.timeout_add(400, self._detectar)

    def _detectar(self) -> bool:
        self._pendiente_deteccion = 0
        self._aplicar_lenguaje()
        return False

    def _tecla(self, _ctrl, keyval, _keycode, estado) -> bool:
        mayus = bool(estado & Gdk.ModifierType.SHIFT_MASK)
        if keyval == Gdk.KEY_BackSpace and not self.buf.get_has_selection() and cursor_al_inicio(self.buf) \
                and not self.texto():
            self.editor.borrar_vacio(self)
            return True
        if keyval == Gdk.KEY_Up and not mayus and en_primera_linea(self.vista):
            return self.editor.navegar(self, -1)
        if keyval == Gdk.KEY_Down and not mayus and en_ultima_linea(self.vista):
            return self.editor.navegar(self, 1)
        return False


# --- divisor -------------------------------------------------------------------------------------

class BloqueDivisor(BloqueBase):
    def __init__(self, editor, bloque: Bloque):
        super().__init__(editor, bloque)
        separador = Gtk.Separator()
        separador.set_margin_top(10)
        separador.set_margin_bottom(10)
        self.append(separador)
        self.instalar_menu_clic_derecho(self)


# --- imagen --------------------------------------------------------------------------------------

class BloqueImagen(BloqueBase):
    ALTO_MAXIMO = 380
    ANCHO_MAXIMO = 760

    def __init__(self, editor, bloque: Bloque):
        super().__init__(editor, bloque)
        contenido: Gtk.Widget
        try:
            datos = base64.b64decode(bloque.image_data or "")
            textura = Gdk.Texture.new_from_bytes(GLib.Bytes.new(datos))
            imagen = Gtk.Picture.new_for_paintable(textura)
            imagen.set_content_fit(Gtk.ContentFit.CONTAIN)
            imagen.set_can_shrink(True)
            imagen.set_halign(Gtk.Align.START)
            ancho, alto = textura.get_width(), textura.get_height()
            escala = min(1.0, self.ALTO_MAXIMO / max(alto, 1), self.ANCHO_MAXIMO / max(ancho, 1))
            imagen.set_size_request(int(ancho * escala), int(alto * escala))
            imagen.add_css_class("card")
            contenido = imagen
        except Exception:
            contenido = etiqueta("Imagen no disponible", ["kiwu-suave"])
        contenido.set_margin_top(4)
        contenido.set_margin_bottom(4)
        self.append(contenido)
        self.instalar_menu_clic_derecho(self)


# --- archivo adjunto -----------------------------------------------------------------------------

class BloqueArchivo(BloqueBase):
    ICONOS = {".pdf": "x-office-document-symbolic", ".doc": "x-office-document-symbolic",
              ".docx": "x-office-document-symbolic", ".odt": "x-office-document-symbolic"}

    def __init__(self, editor, bloque: Bloque):
        super().__init__(editor, bloque)
        ruta = bloque.path
        disponible = bool(ruta) and os.path.exists(ruta)
        nombre = bloque.file_name or (os.path.basename(ruta) if ruta else "Archivo")
        extension = os.path.splitext(nombre)[1].lower()

        fila = Gtk.Box(spacing=12)
        fila.add_css_class("kiwu-tarjeta-adjunto")
        icono = Gtk.Image.new_from_icon_name(self.ICONOS.get(extension, "text-x-generic-symbolic"))
        icono.set_pixel_size(28)
        fila.append(icono)
        textos = Gtk.Box(orientation=Gtk.Orientation.VERTICAL, spacing=2)
        textos.set_hexpand(True)
        textos.append(etiqueta(nombre, elipsis=True))
        if disponible:
            tam = os.path.getsize(ruta)
            sub = f"{tam / 1_048_576:.1f} MB" if tam >= 1_048_576 else f"{max(tam // 1024, 1)} KB"
            textos.append(etiqueta(sub, ["kiwu-minimo"]))
        elif ruta:
            textos.append(etiqueta("No se encuentra el archivo. Vuelve a dejarlo en su sitio o quita el adjunto.",
                                   ["kiwu-minimo", "error"], ajustar=True))
        else:
            textos.append(etiqueta("Sin vincular: pulsa para elegir el archivo en este equipo.", ["kiwu-minimo"],
                                   ajustar=True))
        fila.append(textos)

        carpeta = Gtk.Button.new_from_icon_name("folder-open-symbolic")
        carpeta.add_css_class("flat")
        carpeta.set_tooltip_text("Mostrar en la carpeta")
        carpeta.set_sensitive(disponible)
        carpeta.connect("clicked", lambda _b: abrir_archivo(os.path.dirname(ruta)))
        quitar = Gtk.Button.new_from_icon_name("window-close-symbolic")
        quitar.add_css_class("flat")
        quitar.set_tooltip_text("Quitar adjunto (no borra el archivo)")
        quitar.connect("clicked", lambda _b: self.editor.eliminar(self))
        fila.append(carpeta)
        fila.append(quitar)

        clic = Gtk.GestureClick(button=Gdk.BUTTON_PRIMARY)
        clic.connect("released", lambda *_: self._abrir(disponible))
        fila.add_controller(clic)
        fila.set_margin_top(2)
        fila.set_margin_bottom(2)
        self.append(fila)
        self.instalar_menu_clic_derecho(self)

    def _abrir(self, disponible: bool) -> None:
        if disponible:
            abrir_archivo(self.bloque.path)
        else:
            self.editor.vincular_archivo(self)


# --- enlace con vista previa ---------------------------------------------------------------------

class BloqueEnlace(BloqueBase):
    def __init__(self, editor, bloque: Bloque):
        super().__init__(editor, bloque)
        self._reproduciendo = False
        self._cargando_vp = False
        self._fallo = False
        self.contenedor = Gtk.Box(orientation=Gtk.Orientation.VERTICAL)
        self.append(self.contenedor)
        self.instalar_menu_clic_derecho(self)
        self._dibujar()
        if bloque.url and not bloque.preview:
            self.actualizar_vista_previa()

    def enfocar(self, al_final: bool = True) -> None:
        if getattr(self, "campo", None) is not None:
            self.campo.grab_focus()

    def _vaciar(self) -> None:
        hijo = self.contenedor.get_first_child()
        while hijo is not None:
            siguiente = hijo.get_next_sibling()
            self.contenedor.remove(hijo)
            hijo = siguiente
        self.campo = None

    def _dibujar(self) -> None:
        self._vaciar()
        if not self.bloque.url:
            self._dibujar_entrada()
        else:
            self._dibujar_tarjeta()

    def _dibujar_entrada(self) -> None:
        fila = Gtk.Box(spacing=8)
        fila.add_css_class("kiwu-campo-nueva")
        fila.append(Gtk.Image.new_from_icon_name("insert-link-symbolic"))
        self.campo = Gtk.Entry(placeholder_text="Pega un enlace de una página o un vídeo y pulsa Intro", hexpand=True)
        self.campo.connect("activate", self._confirmar)
        fila.append(self.campo)
        quitar = Gtk.Button.new_from_icon_name("window-close-symbolic")
        quitar.add_css_class("flat")
        quitar.connect("clicked", lambda _b: self.editor.eliminar(self))
        fila.append(quitar)
        self.contenedor.append(fila)

    def _confirmar(self, campo: Gtk.Entry) -> None:
        url = enlaces.url_desde_texto(campo.get_text())
        if not url:
            campo.add_css_class("error")
            return
        self.bloque.url = url
        self.bloque.preview = None
        self._fallo = False
        self._dibujar()
        self.editor.cambio()
        self.actualizar_vista_previa()

    def actualizar_vista_previa(self) -> None:
        url = self.bloque.url
        if not url:
            return
        self._cargando_vp = True
        self._dibujar()

        def terminado(resultado) -> None:
            self._cargando_vp = False
            if self.get_root() is None:      # el bloque ya no está en pantalla
                return
            if resultado:
                self.bloque.preview = resultado
                self._fallo = False
                self.editor.cambio()
            else:
                self._fallo = True
            self._dibujar()

        en_segundo_plano(lambda: enlaces.obtener(url, reducir=_reducir_imagen), terminado)

    # --- tarjeta --------------------------------------------------------------------------------

    def _dibujar_tarjeta(self) -> None:
        vp = self.bloque.preview or {}
        url = self.bloque.url
        es_video = bool(vp.get("proveedor"))
        tarjeta = Gtk.Box(orientation=Gtk.Orientation.VERTICAL)
        tarjeta.add_css_class("kiwu-tarjeta-enlace")
        tarjeta.set_overflow(Gtk.Overflow.HIDDEN)

        if self._reproduciendo and WebKit is not None and es_video:
            tarjeta.append(self._reproductor(vp))
            pie = Gtk.Box(spacing=8)
            pie.set_margin_top(6)
            pie.set_margin_bottom(6)
            pie.set_margin_start(12)
            pie.set_margin_end(12)
            pie.append(etiqueta("¿No se reproduce? Algunos vídeos no permiten verse fuera de su web.",
                                ["kiwu-minimo"], elipsis=True))
            abrir = Gtk.Button(label="Abrir en el navegador")
            abrir.add_css_class("flat")
            abrir.set_hexpand(True)
            abrir.set_halign(Gtk.Align.END)
            abrir.connect("clicked", lambda _b: abrir_uri(url))
            pie.append(abrir)
            tarjeta.append(pie)
        else:
            horizontal = not es_video
            cuerpo = Gtk.Box(orientation=Gtk.Orientation.HORIZONTAL if horizontal else Gtk.Orientation.VERTICAL)
            imagen = self._imagen(vp)
            textos = self._textos(vp, url, es_video)
            if es_video:
                portada = Gtk.Overlay()
                portada.set_child(imagen if imagen is not None else _caja_negra())
                reproducir = Gtk.Button.new_from_icon_name("media-playback-start-symbolic")
                reproducir.add_css_class("circular")
                reproducir.add_css_class("osd")
                reproducir.set_halign(Gtk.Align.CENTER)
                reproducir.set_valign(Gtk.Align.CENTER)
                reproducir.set_size_request(56, 56)
                reproducir.connect("clicked", self._reproducir)
                portada.add_overlay(reproducir)
                cuerpo.append(portada)
                cuerpo.append(textos)
            else:
                textos.set_hexpand(True)
                cuerpo.append(textos)
                if imagen is not None:
                    cuerpo.append(imagen)
                clic = Gtk.GestureClick(button=Gdk.BUTTON_PRIMARY)
                clic.connect("released", lambda *_: abrir_uri(url))
                cuerpo.add_controller(clic)
            tarjeta.append(cuerpo)

        acciones = Gtk.Box(spacing=2)
        acciones.set_halign(Gtk.Align.END)
        acciones.set_valign(Gtk.Align.START)
        acciones.set_margin_top(6)
        acciones.set_margin_end(6)
        acciones.add_css_class("osd")
        if self._cargando_vp:
            acciones.append(Gtk.Spinner(spinning=True))
        for icono, ayuda, funcion in (
            ("external-link-symbolic", "Abrir en el navegador", lambda: abrir_uri(url)),
            ("view-refresh-symbolic", "Actualizar vista previa", self.actualizar_vista_previa),
            ("format-justify-left-symbolic", "Convertir en texto", lambda: self.editor.enlace_a_texto(self)),
            ("window-close-symbolic", "Quitar", lambda: self.editor.eliminar(self)),
        ):
            b = Gtk.Button.new_from_icon_name(icono)
            b.add_css_class("flat")
            b.set_tooltip_text(ayuda)
            b.connect("clicked", lambda _b, f=funcion: f())
            acciones.append(b)
        marco = Gtk.Overlay()
        marco.set_child(tarjeta)
        marco.add_overlay(acciones)
        self.contenedor.append(marco)

    def _imagen(self, vp: dict) -> Optional[Gtk.Widget]:
        b64 = vp.get("imagen")
        if not b64:
            return None
        try:
            textura = Gdk.Texture.new_from_bytes(GLib.Bytes.new(base64.b64decode(b64)))
        except Exception:
            return None
        pic = Gtk.Picture.new_for_paintable(textura)
        pic.set_content_fit(Gtk.ContentFit.COVER)
        pic.set_can_shrink(True)
        if vp.get("proveedor"):
            pic.set_size_request(-1, 220)
        else:
            pic.set_size_request(150, 100)
        return pic

    def _textos(self, vp: dict, url: str, es_video: bool) -> Gtk.Widget:
        caja = Gtk.Box(orientation=Gtk.Orientation.VERTICAL, spacing=4)
        for lado in ("top", "bottom", "start", "end"):
            getattr(caja, f"set_margin_{lado}")(12)
        titulo = vp.get("titulo") or ("Cargando vista previa…" if self._cargando_vp else url)
        caja.append(etiqueta(titulo, ["heading"], ajustar=True))
        if vp.get("descripcion"):
            caja.append(etiqueta(vp["descripcion"], ["caption", "kiwu-suave"], ajustar=True))
        sitio = vp.get("sitio") or ""
        if self._fallo:
            sitio = (sitio + " · " if sitio else "") + "No se pudo cargar la vista previa"
        caja.append(etiqueta(sitio, ["kiwu-minimo"], elipsis=True))
        return caja

    def _reproducir(self, _b) -> None:
        if WebKit is None:
            abrir_uri(self.bloque.url)
            return
        self._reproduciendo = True
        self._dibujar()

    def _reproductor(self, vp: dict) -> Gtk.Widget:
        web = WebKit.WebView()
        web.set_size_request(-1, 300)
        peticion = WebKit.URIRequest.new(enlaces.url_incrustada(vp["proveedor"], vp["videoID"]))
        # YouTube exige que la app se identifique con un Referer propio (si no, error 152/153)
        peticion.get_http_headers().append("Referer", "https://io.github.ksebas20500.kiwu/")
        web.load_request(peticion)
        return web


def _caja_negra() -> Gtk.Widget:
    caja = Gtk.Box()
    caja.set_size_request(-1, 220)
    proveedor = Gtk.CssProvider()
    css = "box { background: #101010; }"
    if hasattr(proveedor, "load_from_string"):
        proveedor.load_from_string(css)
    else:
        proveedor.load_from_data(css.encode())
    caja.get_style_context().add_provider(proveedor, Gtk.STYLE_PROVIDER_PRIORITY_APPLICATION)
    return caja


def _reducir_imagen(datos: bytes) -> Optional[bytes]:
    """Reduce la imagen a 640 px de ancho y la guarda como JPEG para no inflar la nota."""
    try:
        from gi.repository import GdkPixbuf
        cargador = GdkPixbuf.PixbufLoader()
        cargador.write(datos)
        cargador.close()
        pixbuf = cargador.get_pixbuf()
        if pixbuf is None:
            return None
        if pixbuf.get_width() > 640:
            alto = max(1, pixbuf.get_height() * 640 // pixbuf.get_width())
            pixbuf = pixbuf.scale_simple(640, alto, GdkPixbuf.InterpType.BILINEAR)
        if pixbuf.get_has_alpha():
            pixbuf = pixbuf.composite_color_simple(pixbuf.get_width(), pixbuf.get_height(),
                                                   GdkPixbuf.InterpType.BILINEAR, 255, 16, 0xFFFFFF, 0xFFFFFF)
        ok, buffer = pixbuf.save_to_bufferv("jpeg", ["quality"], ["75"])
        return bytes(buffer) if ok else None
    except Exception:
        return None


def crear_bloque(editor, bloque: Bloque) -> BloqueBase:
    """Fábrica: el widget que corresponde al tipo del bloque."""
    if bloque.kind in BLOQUES_DE_TEXTO:
        return BloqueTexto(editor, bloque)
    return {
        "code": BloqueCodigo, "image": BloqueImagen, "file": BloqueArchivo,
        "divider": BloqueDivisor, "embed": BloqueEnlace,
    }[bloque.kind](editor, bloque)

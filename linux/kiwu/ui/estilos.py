"""Hojas de estilo (CSS de GTK4) y fondo de la ventana."""

from __future__ import annotations

import hashlib
from pathlib import Path
from typing import Optional

import gi

gi.require_version("Gtk", "4.0")
gi.require_version("Adw", "1")
from gi.repository import Adw, Gdk, Gio, Gtk  # noqa: E402

from .. import rutas  # noqa: E402

# Colores pastel de las tareas (mismos que la versión de macOS): fondo y texto
PASTEL = [
    ((1.00, 0.85, 0.75), (0.55, 0.28, 0.12)),   # melocotón
    ((1.00, 0.90, 0.72), (0.55, 0.35, 0.05)),   # albaricoque
    ((1.00, 0.96, 0.72), (0.50, 0.42, 0.05)),   # mantequilla
    ((1.00, 0.80, 0.78), (0.60, 0.22, 0.20)),   # coral
    ((0.99, 0.83, 0.87), (0.58, 0.22, 0.35)),   # rosa
    ((0.95, 0.87, 0.78), (0.45, 0.32, 0.20)),   # arena
    ((0.96, 0.78, 0.68), (0.50, 0.25, 0.15)),   # terracota claro
    ((0.98, 0.88, 0.62), (0.50, 0.36, 0.00)),   # miel
]


def _hex(rgb) -> str:
    return "#%02x%02x%02x" % tuple(round(c * 255) for c in rgb)


def clase_pastel(id_tarea: str) -> str:
    """Cada tarea recibe un color estable según su id."""
    h = int(hashlib.md5(id_tarea.encode()).hexdigest(), 16)
    return f"pastel-{h % len(PASTEL)}"


def asignar_pasteles(ids: list[str]) -> dict[str, str]:
    """Como en macOS: si dos tareas del mismo día coinciden, la segunda pasa al siguiente color libre."""
    usados: set[int] = set()
    salida: dict[str, str] = {}
    for id_ in ids:
        indice = int(clase_pastel(id_).split("-")[1])
        if len(usados) < len(PASTEL):
            while indice in usados:
                indice = (indice + 1) % len(PASTEL)
        usados.add(indice)
        salida[id_] = f"pastel-{indice}"
    return salida


def _css_pasteles() -> str:
    return "\n".join(
        f".pastel-{i} {{ background-color: {_hex(f)}; color: {_hex(t)}; }}" for i, (f, t) in enumerate(PASTEL)
    )


CSS_BASE = """
.kiwu-rail { background-color: alpha(@window_fg_color, 0.06); }
.kiwu-sidebar { background-color: alpha(@window_fg_color, 0.04); }
.kiwu-panel { background-color: transparent; }
.kiwu-boton-modulo { min-width: 52px; min-height: 52px; border-radius: 13px; }
.kiwu-boton-modulo:checked { background-color: alpha(@accent_bg_color, 0.18); color: @accent_color; }
.kiwu-titulo { font-size: 26px; font-weight: 800; }
.kiwu-titulo-nota { font-size: 30px; font-weight: 800; background: none; box-shadow: none; padding-left: 0; }
.kiwu-titulo-tarea { font-size: 24px; font-weight: 700; background: none; box-shadow: none; padding-left: 0; }
.kiwu-seccion { font-weight: 700; opacity: 0.7; }
.kiwu-suave { opacity: 0.6; }
.kiwu-minimo { font-size: 11px; opacity: 0.6; }
.kiwu-campo-nueva { background-color: alpha(@window_fg_color, 0.07); border-radius: 8px; padding: 6px 10px; }
.kiwu-campo-nueva entry, .kiwu-campo-nueva entry:focus { background: none; box-shadow: none; outline: none; border: none; }
.kiwu-fila { border-radius: 8px; padding: 8px 10px; }
.kiwu-fila:hover { background-color: alpha(@window_fg_color, 0.05); }
.kiwu-fila.seleccionada { background-color: alpha(@window_fg_color, 0.10); }
.kiwu-hecha { opacity: 0.5; }
.kiwu-vencida { color: @error_color; }
.kiwu-hoy { color: @success_color; }
.kiwu-chip { border-radius: 99px; padding: 1px 8px; font-size: 11px; font-weight: 600;
             background-color: alpha(@accent_bg_color, 0.2); color: @accent_color; }
.kiwu-columna { background-color: alpha(@window_fg_color, 0.045); border-radius: 12px; padding: 10px; }
.kiwu-columna.resaltada { background-color: alpha(@accent_bg_color, 0.18); }
.kiwu-tarjeta { background-color: @card_bg_color; border-radius: 10px; padding: 10px;
                border: 1px solid alpha(@window_fg_color, 0.12); }
.kiwu-tarjeta.seleccionada { border: 2px solid @accent_bg_color; }
.kiwu-selector-vista { background-color: alpha(@window_fg_color, 0.08); border-radius: 8px; padding: 2px; }
.kiwu-selector-vista button { min-width: 28px; min-height: 22px; padding: 0 4px; border-radius: 6px; }
.kiwu-selector-vista button:checked { background-color: @card_bg_color; }

/* Calendario */
.kiwu-dia { border: 1px solid alpha(@window_fg_color, 0.08); padding: 4px; }
.kiwu-dia.fuera-de-mes { opacity: 0.45; }
.kiwu-dia.seleccionado { background-color: alpha(@accent_bg_color, 0.12); }
.kiwu-numero-dia { padding: 1px 6px; border-radius: 99px; font-weight: 600; }
.kiwu-numero-dia.hoy { background-color: @accent_bg_color; color: @accent_fg_color; }
.kiwu-evento { border-radius: 4px; padding: 1px 6px; font-size: 11px; }

/* Editor de notas */
.kiwu-bloque textview, .kiwu-bloque textview text { background: none; color: inherit; }
textview.kiwu-h1 { font-size: 22px; font-weight: 800; }
textview.kiwu-h2 { font-size: 18px; font-weight: 700; }
textview.kiwu-cita { font-style: italic; }
.kiwu-callout { background-color: alpha(#f6a02d, 0.16); border-radius: 8px; padding: 10px 12px; }
.kiwu-cita-caja { border-left: 3px solid alpha(@window_fg_color, 0.5); padding-left: 12px; }
.kiwu-codigo { border-radius: 8px; padding: 0; }
.kiwu-codigo-oscuro { background-color: #1e1e1e; color: #d4d4d4; }
.kiwu-codigo-claro { background-color: #f5f5f5; color: #1f1f1f; }
.kiwu-codigo textview, .kiwu-codigo textview text { background: none; }
.kiwu-codigo-oscuro textview text { color: #d4d4d4; }
.kiwu-codigo-claro textview text { color: #1f1f1f; }
.kiwu-tarjeta-enlace { background-color: alpha(@window_fg_color, 0.05); border-radius: 10px;
                       border: 1px solid alpha(@window_fg_color, 0.1); }
.kiwu-tarjeta-adjunto { background-color: alpha(@window_fg_color, 0.05); border-radius: 8px; padding: 10px;
                        border: 1px solid alpha(@window_fg_color, 0.1); }
.kiwu-barra-formato { padding: 3px; }
.kiwu-barra-formato button { min-width: 28px; min-height: 26px; padding: 0 4px; }
.kiwu-slash { padding: 4px; }
.kiwu-slash row { border-radius: 6px; padding: 3px 6px; }
.kiwu-slash-icono { background-color: alpha(@window_fg_color, 0.08); border-radius: 6px; padding: 4px; }
.kiwu-muestra-color { min-width: 30px; min-height: 30px; border-radius: 6px; }
"""


def css_dinamico(ajustes) -> str:
    """Estilos que dependen de los ajustes del usuario (translucidez, desenfoque)."""
    visibilidad = float(ajustes["fondo_visibilidad"])
    opacidad_paneles = float(ajustes["opacidad_paneles"])
    desenfoque = float(ajustes["desenfoque"])
    opacidad_ventana = float(ajustes["opacidad_ventana"])
    css = f"""
.kiwu-fondo-imagen {{ opacity: {visibilidad:.3f}; filter: blur({desenfoque:.1f}px); }}
.kiwu-velo {{ background-color: alpha(@window_bg_color, {opacidad_paneles * 0.6:.3f}); }}
"""
    if opacidad_ventana < 0.999:
        css += f"""
window.kiwu-ventana, window.kiwu-ventana > contents {{ background-color: alpha(@window_bg_color, {opacidad_ventana:.3f}); }}
window.kiwu-ventana headerbar {{ background-color: alpha(@headerbar_bg_color, {opacidad_ventana:.3f}); }}
"""
    return css


class Estilos:
    """Carga y actualiza los estilos de la aplicación."""

    def __init__(self, ajustes):
        self.ajustes = ajustes
        self.base = Gtk.CssProvider()
        self.dinamico = Gtk.CssProvider()
        self._cargar(self.base, CSS_BASE + _css_pasteles())
        pantalla = Gdk.Display.get_default()
        if pantalla is not None:
            Gtk.StyleContext.add_provider_for_display(pantalla, self.base, Gtk.STYLE_PROVIDER_PRIORITY_APPLICATION)
            Gtk.StyleContext.add_provider_for_display(pantalla, self.dinamico, Gtk.STYLE_PROVIDER_PRIORITY_APPLICATION + 1)
        self.actualizar()
        ajustes.suscribir(lambda _clave: self.actualizar())

    @staticmethod
    def _cargar(proveedor: Gtk.CssProvider, texto: str) -> None:
        if hasattr(proveedor, "load_from_string"):       # GTK 4.12+
            proveedor.load_from_string(texto)
        else:
            proveedor.load_from_data(texto.encode("utf-8"))

    def actualizar(self) -> None:
        self._cargar(self.dinamico, css_dinamico(self.ajustes))
        gestor = Adw.StyleManager.get_default()
        tema = self.ajustes["tema"]
        gestor.set_color_scheme({
            "claro": Adw.ColorScheme.FORCE_LIGHT,
            "oscuro": Adw.ColorScheme.FORCE_DARK,
        }.get(tema, Adw.ColorScheme.DEFAULT))


def registrar_esquemas_de_codigo() -> None:
    """Hace visibles los temas de colores del código (tipo VS Code Dark+ / Light+)."""
    try:
        gi.require_version("GtkSource", "5")
        from gi.repository import GtkSource
    except (ValueError, ImportError):
        return
    carpeta = Path(__file__).resolve().parent.parent / "recursos" / "esquemas"
    if carpeta.is_dir():
        GtkSource.StyleSchemeManager.get_default().append_search_path(str(carpeta))


class Fondo(Gtk.Overlay):
    """Imagen de fondo de la ventana con un velo que controla la translucidez."""

    def __init__(self, ajustes):
        super().__init__()
        self.ajustes = ajustes
        self.imagen = Gtk.Picture()
        self.imagen.set_content_fit(Gtk.ContentFit.COVER)
        self.imagen.set_can_shrink(True)
        self.imagen.set_hexpand(True)
        self.imagen.set_vexpand(True)
        self.imagen.add_css_class("kiwu-fondo-imagen")
        self.velo = Gtk.Box()
        self.velo.add_css_class("kiwu-velo")
        self.velo.set_can_target(False)
        self.set_child(self.imagen)
        self.add_overlay(self.velo)
        self.set_can_target(False)
        self.recargar()

    def recargar(self) -> None:
        ruta = rutas.ruta_fondo()
        existe = ruta.exists()
        self.imagen.set_file(None)               # fuerza a releer el archivo si se cambió la imagen
        if existe:
            try:
                self.imagen.set_file(Gio.File.new_for_path(str(ruta)))
            except Exception:
                existe = False
        self.imagen.set_visible(existe)
        self.velo.set_visible(existe)

    @staticmethod
    def instalar(origen: Path) -> bool:
        """Copia la imagen elegida a la carpeta de datos para que siga disponible."""
        import shutil
        try:
            shutil.copyfile(origen, rutas.ruta_fondo())
            return True
        except OSError:
            return False

    @staticmethod
    def quitar() -> None:
        try:
            rutas.ruta_fondo().unlink()
        except OSError:
            pass

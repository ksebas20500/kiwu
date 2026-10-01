"""Ajustes del entorno gráfico que hay que hacer ANTES de cargar GTK/WebKit."""

from __future__ import annotations

import os
from pathlib import Path


def tiene_nvidia_propietario() -> bool:
    """Driver propietario de NVIDIA cargado (no el controlador libre «nouveau»)."""
    return Path("/proc/driver/nvidia/version").exists()


def preparar() -> list[str]:
    """Devuelve las variables que ha fijado (para mostrarlas con --diagnostico).

    - En Wayland con el driver propietario de NVIDIA, WebKitGTK (usado solo para reproducir vídeos incrustados)
      puede dar pantalla en blanco con el renderizador DMABUF; se desactiva salvo que el usuario ya lo haya decidido.
    - Todo se puede anular exportando la variable antes de lanzar Kiwu.
    """
    fijadas: list[str] = []
    if os.environ.get("KIWU_SIN_AJUSTES_DE_GPU"):
        return fijadas
    if tiene_nvidia_propietario() and "WEBKIT_DISABLE_DMABUF_RENDERER" not in os.environ:
        os.environ["WEBKIT_DISABLE_DMABUF_RENDERER"] = "1"
        fijadas.append("WEBKIT_DISABLE_DMABUF_RENDERER=1")
    return fijadas


def diagnostico() -> str:
    lineas = [
        f"Sesión: {os.environ.get('XDG_SESSION_TYPE', '?')}  (WAYLAND_DISPLAY={os.environ.get('WAYLAND_DISPLAY', '-')})",
        f"Escritorio: {os.environ.get('XDG_CURRENT_DESKTOP', '?')}",
        f"NVIDIA propietario: {'sí' if tiene_nvidia_propietario() else 'no'}",
    ]
    for var in ("GDK_BACKEND", "GSK_RENDERER", "WEBKIT_DISABLE_DMABUF_RENDERER", "WEBKIT_DISABLE_COMPOSITING_MODE"):
        lineas.append(f"{var}={os.environ.get(var, '-')}")
    try:
        import gi
        gi.require_version("Gtk", "4.0")
        gi.require_version("Adw", "1")
        from gi.repository import Adw, Gtk
        lineas.append(f"GTK {Gtk.get_major_version()}.{Gtk.get_minor_version()}.{Gtk.get_micro_version()}, "
                      f"libadwaita {Adw.get_major_version()}.{Adw.get_minor_version()}.{Adw.get_micro_version()}")
    except Exception as e:  # noqa: BLE001
        lineas.append(f"GTK/libadwaita no disponibles: {e}")
    for nombre, version in (("GtkSource", "5"), ("WebKit", "6.0")):
        try:
            import gi
            gi.require_version(nombre, version)
            lineas.append(f"{nombre} {version}: disponible")
        except Exception:  # noqa: BLE001
            lineas.append(f"{nombre} {version}: NO disponible (opcional)")
    return "\n".join(lineas)

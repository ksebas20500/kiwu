"""Módulo de tareas: barra lateral + lista/tablero/calendario/archivos + panel de detalle."""

from __future__ import annotations

import os
from datetime import datetime
from typing import Optional

import gi

gi.require_version("Gtk", "4.0")
from gi.repository import GLib, Gtk  # noqa: E402

from ..modelos import Materia  # noqa: E402
from ..servicios import CALENDARIO, COMPLETADAS, HOY, PROXIMOS, TODOS, materia_item  # noqa: E402
from .archivos import ArchivosMateria  # noqa: E402
from .calendario import Calendario  # noqa: E402
from .detalle import DetalleTarea  # noqa: E402
from .lista import ListaTareas  # noqa: E402
from .sidebar import SidebarTareas  # noqa: E402
from .tablero import Tablero  # noqa: E402
from .utiles import confirmar, etiqueta, pedir_texto  # noqa: E402


class VistaTareas(Gtk.Box):
    """También hace de «contexto» para las vistas hijas: guarda la selección y ofrece las acciones comunes."""

    def __init__(self, ventana, db, servicio, ajustes):
        super().__init__()
        self.ventana, self.db, self.servicio, self.ajustes = ventana, db, servicio, ajustes
        self.item: tuple = HOY
        self.seleccion: Optional[str] = None
        self.sub = "tareas"                       # tareas | archivos (cuando hay una lista elegida)
        self._refresco_pendiente = False
        self._detalle: Optional[DetalleTarea] = None

        self.sidebar = SidebarTareas(servicio, self.ir_a, self.nueva_lista, self.renombrar_lista, self.eliminar_lista)
        self.append(self.sidebar)
        self.append(Gtk.Separator(orientation=Gtk.Orientation.VERTICAL))

        self.lista = ListaTareas(self)
        self.tablero = Tablero(self)
        self.calendario = Calendario(self)
        self.archivos = ArchivosMateria(self)
        self.centro = Gtk.Stack()
        for nombre, vista in (("lista", self.lista), ("tablero", self.tablero), ("calendario", self.calendario),
                              ("archivos", self.archivos)):
            self.centro.add_named(vista, nombre)
        self.append(self.centro)
        self.separador_detalle = Gtk.Separator(orientation=Gtk.Orientation.VERTICAL)
        self.append(self.separador_detalle)

        self.zona_detalle = Gtk.Box(orientation=Gtk.Orientation.VERTICAL)
        self.vacio = Gtk.Box(orientation=Gtk.Orientation.VERTICAL, spacing=8)
        self.vacio.set_valign(Gtk.Align.CENTER)
        self.vacio.set_vexpand(True)
        icono = Gtk.Image.new_from_icon_name("object-select-symbolic")
        icono.set_pixel_size(40)
        icono.add_css_class("kiwu-suave")
        self.vacio.append(icono)
        self.vacio.append(etiqueta("Selecciona una tarea", ["title-3", "kiwu-suave"], xalign=0.5))
        self.append(self.zona_detalle)

        db.suscribir(self._base_cambio)
        self.refrescar()

    # --- propiedades usadas por las vistas hijas -------------------------------------------------

    @property
    def vista_tareas(self) -> str:
        return self.ajustes["vista_tareas"]

    def carpeta_disponible(self, m: Materia) -> bool:
        return bool(m.carpeta) and os.path.isdir(m.carpeta)

    def _materia_actual(self) -> Optional[Materia]:
        return self.db.materia(self.item[1]) if self.item[0] == "materia" else None

    def _titulo(self) -> str:
        t = self.item[0]
        if t == "materia":
            m = self._materia_actual()
            return m.nombre if m else "Lista"
        return {"hoy": "Hoy", "proximos": "Próximos 7 días", "calendario": "Calendario", "todos": "Todas las tareas",
                "completadas": "Completadas"}[t]

    # --- acciones ---------------------------------------------------------------------------------

    def programar_refresco(self) -> None:
        """Redibuja cuando acabe la señal en curso (los widgets que la emitieron pueden desaparecer al redibujar)."""
        if not self._refresco_pendiente:
            self._refresco_pendiente = True
            GLib.idle_add(self._refresco_diferido)

    def ir_a(self, item: tuple) -> None:
        self.item, self.sub = item, "tareas"
        self.programar_refresco()

    def seleccionar(self, id_: Optional[str]) -> None:
        self.seleccion = id_
        self.programar_refresco()

    def cambiar_vista(self, vista: str) -> None:
        self.ajustes["vista_tareas"] = vista
        self.programar_refresco()

    def cambiar_sub(self, sub: str) -> None:
        self.sub = sub
        self.programar_refresco()

    def crear_tarea(self, titulo: str, materia_id: str) -> None:
        fecha = None
        if self.item == HOY:
            fecha = datetime.now().replace(hour=18, minute=0, second=0, microsecond=0).timestamp()
        d = self.servicio.crear(titulo, materia_id, fecha)
        if self.vista_tareas == "tablero" or self.item == CALENDARIO:
            return
        self.seleccion = d.id

    def eliminar_tarea(self, d) -> None:
        if self.seleccion == d.id:
            self.seleccion = None
        self.db.eliminar_deber(d.id)

    def nueva_lista(self) -> None:
        def crear(nombre: str) -> None:
            m = self.db.crear_materia(nombre)
            self.ir_a(materia_item(m.id))
        pedir_texto(self.ventana, "Nueva lista", "Nombre de la materia", crear)

    def renombrar_lista(self, m: Materia) -> None:
        pedir_texto(self.ventana, "Renombrar lista", "Nombre", lambda n: self.db.renombrar_materia(m.id, n),
                    texto_inicial=m.nombre, etiqueta_accion="Guardar")

    def eliminar_lista(self, m: Materia) -> None:
        def borrar() -> None:
            if self.item == materia_item(m.id):
                self.item = TODOS
            self.seleccion = None
            self.db.eliminar_materia(m.id)
        confirmar(self.ventana, f"¿Eliminar {m.nombre}?",
                  "Se eliminarán también todas sus tareas. Los archivos de la carpeta vinculada no se tocan.",
                  "Eliminar lista y sus tareas", borrar)

    def nueva_tarea_enfocada(self) -> None:
        """Atajo «Nueva tarea»: lleva el cursor al campo de añadir tarea."""
        if self.item == CALENDARIO:
            self.calendario.campo.enfocar()
            return
        if self.item[0] == "materia" and self.sub == "archivos":
            self.sub = "tareas"
            self.refrescar()
        if self.item == COMPLETADAS:
            self.item = TODOS
            self.refrescar()
        (self.tablero if self.vista_tareas == "tablero" else self.lista).enfocar_nueva()

    def abrir_tarea(self, id_: str) -> None:
        """Desde una notificación: muestra la tarea."""
        self.item, self.sub, self.seleccion = TODOS, "tareas", id_
        self.programar_refresco()

    # --- refresco ---------------------------------------------------------------------------------

    def _base_cambio(self, tipo: str) -> None:
        if tipo in ("deber", "materia", "importacion"):
            self.programar_refresco()

    def _refresco_diferido(self) -> bool:
        self._refresco_pendiente = False
        self.refrescar()
        return False

    def _modo_lista(self) -> bool:
        return self.item != CALENDARIO and self.sub == "tareas" and self.vista_tareas == "lista"

    def refrescar(self) -> None:
        materias = self.db.materias()
        if self.item[0] == "materia" and self._materia_actual() is None:
            self.item = TODOS
        self.sidebar.refrescar(self.item, materias)
        if self.seleccion and self.db.deber(self.seleccion) is None:
            self.seleccion = None

        materia = self._materia_actual()
        if self.item == CALENDARIO:
            self.calendario.refrescar(self.db.deberes(), materias)
            self.centro.set_visible_child_name("calendario")
        elif materia is not None and self.sub == "archivos":
            self.archivos.refrescar(materia)
            self.centro.set_visible_child_name("archivos")
        else:
            pendientes = self.servicio.pendientes(self.item)
            completados = self.servicio.completados(self.item)
            vista = self.tablero if self.vista_tareas == "tablero" else self.lista
            vista.refrescar(self._titulo(), pendientes, completados, materias, materia, self.item != COMPLETADAS)
            self.centro.set_visible_child_name("tablero" if self.vista_tareas == "tablero" else "lista")
        self._actualizar_detalle()

    def _actualizar_detalle(self) -> None:
        modo_lista = self._modo_lista()
        # En modo lista el centro tiene ancho fijo y el detalle se estira; en los demás, al revés.
        self.centro.set_hexpand(not modo_lista)
        self.centro.set_size_request(380 if modo_lista else 420, -1)
        self.zona_detalle.set_hexpand(modo_lista)
        self.zona_detalle.set_size_request(-1 if modo_lista else 400, -1)

        mostrar = bool(self.seleccion) or modo_lista
        self.zona_detalle.set_visible(mostrar)
        self.separador_detalle.set_visible(mostrar)

        if not self.seleccion:
            if self._detalle is not None:
                self._detalle.cerrar()
                self._detalle = None
            self._poner_en_detalle(self.vacio)
            return
        d = self.db.deber(self.seleccion)
        if d is None:
            return
        if self._detalle is not None and self._detalle.deber_id == d.id:
            self._detalle.sincronizar(d)
            return
        if self._detalle is not None:
            self._detalle.cerrar()
        self._detalle = DetalleTarea(self, d, cerrable=not modo_lista)
        self._poner_en_detalle(self._detalle)

    def _poner_en_detalle(self, widget: Gtk.Widget) -> None:
        hijo = self.zona_detalle.get_first_child()
        if hijo is widget:
            return
        while hijo is not None:
            siguiente = hijo.get_next_sibling()
            self.zona_detalle.remove(hijo)
            hijo = siguiente
        widget.set_vexpand(True)
        self.zona_detalle.append(widget)

    def guardar_todo(self) -> None:
        if self._detalle is not None:
            self._detalle.cerrar()

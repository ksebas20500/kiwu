"""Prueba de humo de la interfaz SIN GTK: se sustituye `gi` por un doble que acepta cualquier llamada.

No comprueba el aspecto ni el comportamiento real (para eso hay que ejecutar la app en Linux), pero sí detecta
errores de Python: nombres sin definir, llamadas a funciones propias con argumentos equivocados, atributos mal
escritos, bucles infinitos… Se puede ejecutar en cualquier sistema (macOS incluido).
"""

import os
import sys
import tempfile
import types
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

DEVUELVEN_NADA = {"get_first_child", "get_next_sibling", "get_parent", "get_root", "get_last_child",
                  "get_child", "get_previous_sibling", "get_file", "get_row_at_index"}
DEVUELVEN_TEXTO = {"get_text", "get_label", "get_string"}
DEVUELVEN_FALSO = {"get_active", "get_has_selection", "get_visible", "has_focus", "get_dark", "is_start",
                   "is_end", "has_tag", "get_modified"}
DEVUELVEN_CERO = {"compare", "get_selected", "get_value", "get_index", "get_n_items", "get_width", "get_height"}
DEVUELVEN_PAR = {"get_bounds", "window_to_buffer_coords", "buffer_to_window_coords", "get_iter_at_location",
                 "save_to_bufferv", "get_selection_bounds_falso"}


class _Meta(type):
    def __getattr__(cls, nombre):
        if nombre.startswith("__"):
            raise AttributeError(nombre)
        if nombre == "accelerator_parse":
            return lambda *a, **k: (True, _Falso(), _Falso())
        return _Falso

    def __and__(cls, otro):
        return 0

    __rand__ = __or__ = __ror__ = __and__


class _Falso(metaclass=_Meta):
    def __init__(self, *args, **kwargs):
        pass

    def __getattr__(self, nombre):
        if nombre.startswith("__"):
            raise AttributeError(nombre)
        if nombre in DEVUELVEN_NADA:
            return lambda *a, **k: None
        if nombre in DEVUELVEN_TEXTO:
            return lambda *a, **k: ""
        if nombre in DEVUELVEN_FALSO:
            return lambda *a, **k: False
        if nombre in DEVUELVEN_CERO:
            return lambda *a, **k: 0
        if nombre in DEVUELVEN_PAR:
            return lambda *a, **k: (_Falso(), _Falso())
        if nombre == "get_selection_bounds":
            return lambda *a, **k: ()
        return _Falso()

    def __call__(self, *args, **kwargs):
        return _Falso()

    def __iter__(self):
        return iter((_Falso(), _Falso()))

    def __and__(self, otro):
        return 0

    __rand__ = __or__ = __ror__ = __and__

    def __bool__(self):
        return True


def instalar_gi_falso():
    gi = types.ModuleType("gi")
    gi.require_version = lambda *a, **k: None
    repo = types.ModuleType("gi.repository")
    for nombre in ("Gtk", "Adw", "Gio", "GLib", "GObject", "Gdk", "GdkPixbuf", "Pango", "GtkSource", "WebKit"):
        setattr(repo, nombre, _Falso)
    gi.repository = repo
    sys.modules["gi"] = gi
    sys.modules["gi.repository"] = repo


instalar_gi_falso()

from kiwu import enlaces  # noqa: E402
from kiwu.ajustes import Ajustes  # noqa: E402
from kiwu.db import BaseDeDatos  # noqa: E402
from kiwu.modelos import TIPOS_BLOQUE, Bloque, Documento, TextRun  # noqa: E402
from kiwu.servicios import CALENDARIO, COMPLETADAS, HOY, PROXIMOS, TODOS, Columna, Servicio, materia_item  # noqa: E402


class Humo(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        os.environ["KIWU_DATOS"] = os.path.join(self.tmp.name, "datos")
        os.environ["KIWU_CONFIG"] = os.path.join(self.tmp.name, "config")
        self.db = BaseDeDatos(":memory:")
        self.servicio = Servicio(self.db)
        self.ajustes = Ajustes(Path(self.tmp.name) / "s.json")
        self.m = self.db.crear_materia("Universidad")
        self.otra = self.db.crear_materia("Casa")
        import time
        for i, t in enumerate(("A", "B", "C")):
            self.servicio.crear(t, self.m.id, time.time() + i * 86400)
        d = self.servicio.crear("Hecha", self.otra.id)
        self.servicio.marcar(d, True)
        c = self.db.crear_cuaderno("Notas")
        p = self.db.crear_pagina(c.id)
        self.db.actualizar_pagina(p.id, titulo="Hola", contenido=self._documento().a_json())

    def tearDown(self):
        self.tmp.cleanup()

    @staticmethod
    def _documento() -> Documento:
        bloques = []
        for tipo in TIPOS_BLOQUE:
            b = Bloque(kind=tipo, text=f"texto {tipo}")
            if tipo == "embed":
                b.url, b.text = "https://example.com", ""
                b.preview = {"titulo": "Ejemplo", "descripcion": "d", "sitio": "example.com"}
            if tipo == "file":
                b.file_name, b.path = "apuntes.pdf", "/no/existe/apuntes.pdf"
            if tipo == "paragraph":
                b.runs = [TextRun("texto ", bold=True), TextRun("paragraph", link="https://example.com")]
                b.text = "texto paragraph"
            bloques.append(b)
        return Documento(bloques)

    def test_importan_todos_los_modulos(self):
        import importlib
        for nombre in ("kiwu.ui.utiles", "kiwu.ui.estilos", "kiwu.ui.editor.texto_rico", "kiwu.ui.editor.menus",
                       "kiwu.ui.editor.bloques", "kiwu.ui.editor.editor", "kiwu.ui.widgets", "kiwu.ui.sidebar",
                       "kiwu.ui.lista", "kiwu.ui.tablero", "kiwu.ui.calendario", "kiwu.ui.detalle",
                       "kiwu.ui.archivos", "kiwu.ui.vista_tareas", "kiwu.ui.notas", "kiwu.ui.ajustes_dialogo",
                       "kiwu.ui.ventana", "kiwu.app"):
            importlib.import_module(nombre)

    def test_ventana_y_vistas(self):
        from kiwu.ui.ventana import Ventana
        v = Ventana(_Falso(), self.db, self.servicio, self.ajustes)
        t = v.tareas
        for item in (HOY, PROXIMOS, TODOS, COMPLETADAS, CALENDARIO, materia_item(self.m.id)):
            for vista in ("lista", "tablero"):
                self.ajustes["vista_tareas"] = vista
                t.ir_a(item)
                t.refrescar()          # en la app real el redibujado se programa con GLib.idle_add
        # selección, detalle, subpestañas de archivos y acciones
        d = self.db.deberes()[0]
        t.ir_a(TODOS)
        t.seleccionar(d.id)
        t.refrescar()
        t.cambiar_sub("archivos")
        t.refrescar()
        t.cambiar_sub("tareas")
        t.refrescar()
        t.crear_tarea("Nueva", self.m.id)
        t.eliminar_tarea(self.db.deber(d.id))
        t.refrescar()
        t.nueva_tarea_enfocada()
        v.notas.refrescar()
        v.notas.nueva_pagina()
        v.notas.refrescar()
        v.notas.abrir_pagina(self.db.paginas()[0].id)
        v.notas.refrescar()
        v.ir_a_modulo("notas")
        v.aplicar_atajos()
        v.nueva_tarea()
        v.tareas.guardar_todo()
        v.notas.guardar_todo()

    def test_ajustes_dialogo(self):
        from kiwu.ui.ajustes_dialogo import DialogoAjustes
        DialogoAjustes(_Falso(), self.ajustes, self.db)

    def test_editor_con_todos_los_bloques(self):
        from kiwu.ui.editor.editor import EditorBloques
        guardados = []
        ed = EditorBloques(self._documento(), guardados.append)
        self.assertGreaterEqual(len(ed.bloques), len(TIPOS_BLOQUE))
        # operaciones estructurales sobre cada bloque
        for w in list(ed.bloques):
            if w in ed.bloques:
                ed.cambiar_sangria(w, 1)
                ed.duplicar(w)
        for w in list(ed.bloques)[:6]:
            if w in ed.bloques and w.kind != "divider":
                ed.mover(w, 1)
        for tipo in ("heading", "bulletedList", "numberedList", "checklist", "quote", "callout", "code", "embed",
                     "paragraph"):
            w = ed.bloques[0]
            ed.convertir(w, tipo)
        ed.enter(ed.bloques[0], "", None)
        ed.navegar(ed.bloques[1], 1)
        ed.navegar(ed.bloques[1], -1)
        ed.al_final()
        ed.renumerar()
        while len(ed.bloques) > 1:
            ed.eliminar(ed.bloques[0])
        ed.borrar_vacio(ed.bloques[0])
        ed.cambio()
        ed.guardar_ahora()
        doc = ed.documento()
        self.assertGreaterEqual(len(doc.bloques), 1)

    def test_flujo_de_enlace(self):
        # La conversión de párrafo a tarjeta de enlace solo depende de la lógica pura
        self.assertTrue(enlaces.parece_enlace_suelto("https://example.com/a"))


if __name__ == "__main__":
    unittest.main()

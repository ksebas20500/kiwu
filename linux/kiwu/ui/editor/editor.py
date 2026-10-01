"""Editor de notas por bloques (equivalente a `NoteEditorView` de Kiwu para macOS)."""

from __future__ import annotations

import base64
import os
from typing import Callable, Optional

import gi

gi.require_version("Gtk", "4.0")
from gi.repository import Gdk, Gio, GLib, Gtk  # noqa: E402

from ...enlaces import parece_enlace_suelto, url_desde_texto  # noqa: E402
from ...modelos import BLOQUES_DE_TEXTO, Bloque, Documento, nuevo_id  # noqa: E402
from . import texto_rico  # noqa: E402
from .bloques import BloqueBase, BloqueTexto, crear_bloque  # noqa: E402
from .menus import BarraFormato, CuadroEnlace, MenuSlash  # noqa: E402

LISTAS = ("bulletedList", "numberedList", "checklist")
EXTENSIONES_IMAGEN = {".png", ".jpg", ".jpeg", ".gif", ".webp", ".bmp", ".svg"}


def preparar_imagen(ruta: str) -> Optional[str]:
    """Lee una imagen, la reduce (máx. 1600 px) y la devuelve en base64 (PNG si tiene transparencia, JPEG si no)."""
    try:
        from gi.repository import GdkPixbuf
        pixbuf = GdkPixbuf.Pixbuf.new_from_file_at_scale(ruta, 1600, 1600, True)
        formato, opciones = ("png", ([], [])) if pixbuf.get_has_alpha() else ("jpeg", (["quality"], ["85"]))
        ok, datos = pixbuf.save_to_bufferv(formato, *opciones)
        return base64.b64encode(bytes(datos)).decode("ascii") if ok else None
    except Exception:
        return None


class EditorBloques(Gtk.Box):
    def __init__(self, documento: Documento, al_cambiar: Callable[[Documento], None], retraso_ms: int = 500):
        super().__init__(orientation=Gtk.Orientation.VERTICAL)
        self.al_cambiar = al_cambiar
        self.retraso_ms = retraso_ms
        self.bloques: list[BloqueBase] = []
        self._temporizador = 0
        self._pendiente = False

        self.cuerpo = Gtk.Box(orientation=Gtk.Orientation.VERTICAL, spacing=2)
        self.append(self.cuerpo)
        pie = Gtk.Box()
        pie.set_size_request(-1, 240)
        pie.set_vexpand(True)
        clic = Gtk.GestureClick(button=Gdk.BUTTON_PRIMARY)
        clic.connect("released", lambda *_: self.al_final())
        pie.add_controller(clic)
        self.append(pie)

        self.slash = MenuSlash(self._elegir_slash)
        self.barra = BarraFormato(lambda: self.pedir_enlace(self.barra.bloque))
        self.cuadro_enlace = CuadroEnlace()

        self.cargar(documento)

        soltar = Gtk.DropTarget.new(Gdk.FileList, Gdk.DragAction.COPY)
        soltar.connect("drop", self._archivos_soltados)
        self.add_controller(soltar)

    # --- carga y guardado -----------------------------------------------------------------------

    def cargar(self, documento: Documento) -> None:
        for b in list(self.bloques):
            self._quitar_widget(b)
        for datos in documento.bloques:
            self._anadir(crear_bloque(self, datos))
        self._asegurar_final()
        self.renumerar()

    def documento(self) -> Documento:
        return Documento([b.a_bloque() for b in self.bloques])

    def cambio(self) -> None:
        self._pendiente = True
        if self._temporizador:
            GLib.source_remove(self._temporizador)
        self._temporizador = GLib.timeout_add(self.retraso_ms, self._emitir)

    def _emitir(self) -> bool:
        self._temporizador = 0
        if self._pendiente:
            self._pendiente = False
            self.al_cambiar(self.documento())
        return False

    def guardar_ahora(self) -> None:
        if self._temporizador:
            GLib.source_remove(self._temporizador)
            self._temporizador = 0
        self.slash.ocultar()
        self.barra.ocultar()
        if self._pendiente:
            self._pendiente = False
            self.al_cambiar(self.documento())

    # --- estructura -----------------------------------------------------------------------------

    def _indice(self, widget: BloqueBase) -> int:
        return self.bloques.index(widget)

    def _anadir(self, widget: BloqueBase, indice: Optional[int] = None) -> BloqueBase:
        if indice is None or indice >= len(self.bloques):
            anterior = self.bloques[-1] if self.bloques else None
            self.bloques.append(widget)
        else:
            anterior = self.bloques[indice - 1] if indice > 0 else None
            self.bloques.insert(indice, widget)
        self.cuerpo.insert_child_after(widget, anterior)
        return widget

    def _quitar_widget(self, widget: BloqueBase) -> None:
        widget.al_quitar()
        if self.slash.bloque is widget:
            self.slash.ocultar()
        if self.barra.bloque is widget:
            self.barra.ocultar()
        self.cuerpo.remove(widget)
        self.bloques.remove(widget)

    def _asegurar_final(self) -> None:
        if not self.bloques or self.bloques[-1].kind != "paragraph":
            self._anadir(crear_bloque(self, Bloque()))

    def _enfocar(self, widget: BloqueBase, al_final: bool = True) -> None:
        GLib.idle_add(lambda: (widget.enfocar(al_final), False)[1])

    def renumerar(self) -> None:
        """Numeración de las listas, con un contador por nivel de sangría."""
        contadores = [0] * 5
        for w in self.bloques:
            nivel = min(max(w.bloque.indent, 0), 4)
            if w.kind == "numberedList":
                contadores[nivel] += 1
                for mayor in range(nivel + 1, 5):
                    contadores[mayor] = 0
                w.fijar_numero(max(contadores[nivel], 1))
            else:
                contadores = [0] * 5

    def reemplazar(self, widget: BloqueBase, datos: Bloque) -> BloqueBase:
        i = self._indice(widget)
        nuevo = crear_bloque(self, datos)
        self._anadir(nuevo, i)
        self._quitar_widget(widget)
        return nuevo

    # --- operaciones pedidas por los bloques -----------------------------------------------------

    def convertir(self, widget: BloqueBase, tipo: str, texto: Optional[str] = None) -> None:
        datos = widget.a_bloque()
        if tipo == datos.kind and texto is None:
            return
        if texto is not None:
            datos.text, datos.runs = texto, None
        if tipo == "embed":
            datos.url = url_desde_texto(datos.text)
            datos.text, datos.runs, datos.preview = "", None, None
        elif datos.kind == "code":
            datos.runs = None
        datos.kind = tipo
        nuevo = self.reemplazar(widget, datos)
        self._asegurar_final()
        self.renumerar()
        self.cambio()
        if tipo != "embed":
            self._enfocar(nuevo)

    def cambiar_sangria(self, widget: BloqueBase, delta: int) -> None:
        datos = widget.a_bloque()
        nueva = min(max(datos.indent + delta, 0), 4)
        if nueva == datos.indent:
            return
        datos.indent = nueva
        widget.aplicar_sangria()
        self.renumerar()
        self.cambio()

    def mover(self, widget: BloqueBase, delta: int) -> None:
        i = self._indice(widget)
        j = i + delta
        if not 0 <= j < len(self.bloques):
            return
        self.bloques[i], self.bloques[j] = self.bloques[j], self.bloques[i]
        if delta < 0:
            self.cuerpo.reorder_child_after(widget, self.bloques[j - 1] if j > 0 else None)
        else:
            self.cuerpo.reorder_child_after(widget, self.bloques[j - 1])
        self._asegurar_final()
        self.renumerar()
        self.cambio()

    def duplicar(self, widget: BloqueBase) -> None:
        copia = Bloque.de_json(widget.a_bloque().a_json())
        copia.id = nuevo_id()
        self._anadir(crear_bloque(self, copia), self._indice(widget) + 1)
        self.renumerar()
        self.cambio()

    def eliminar(self, widget: BloqueBase) -> None:
        i = self._indice(widget)
        if len(self.bloques) == 1:
            self.reemplazar(widget, Bloque())
        else:
            self._quitar_widget(widget)
            vecino = self.bloques[min(max(i - 1, 0), len(self.bloques) - 1)]
            if vecino.es_texto:
                self._enfocar(vecino)
        self._asegurar_final()
        self.renumerar()
        self.cambio()

    def enter(self, widget: BloqueBase, resto: str, runs) -> None:
        datos = widget.a_bloque()
        i = self._indice(widget)

        # Una dirección web sola en un párrafo se convierte en tarjeta con vista previa
        if datos.kind == "paragraph" and not resto and datos.indent == 0 and parece_enlace_suelto(datos.text):
            self.convertir(widget, "embed")
            siguiente = self.bloques[i + 1] if i + 1 < len(self.bloques) else None
            if siguiente is not None:
                self._enfocar(siguiente, al_final=False)
            return

        nuevo = Bloque()
        if datos.kind in LISTAS:
            if not datos.text and not resto:
                if datos.indent > 0:
                    self.cambiar_sangria(widget, -1)
                else:
                    self.convertir(widget, "paragraph")
                return
            nuevo.kind, nuevo.indent = datos.kind, datos.indent
        nuevo.text, nuevo.runs = resto, runs
        w = self._anadir(crear_bloque(self, nuevo), i + 1)
        self.renumerar()
        self.cambio()
        self._enfocar(w, al_final=False)

    def borrar_vacio(self, widget: BloqueBase) -> None:
        datos = widget.a_bloque()
        if datos.indent > 0:
            self.cambiar_sangria(widget, -1)
            return
        if datos.kind != "paragraph":
            self.convertir(widget, "paragraph")
            return
        if len(self.bloques) <= 1:
            return
        i = self._indice(widget)
        self._quitar_widget(widget)
        previo = self.bloques[max(0, i - 1)]
        self.renumerar()
        self.cambio()
        if previo.es_texto:
            self._enfocar(previo)

    def navegar(self, widget: BloqueBase, direccion: int) -> bool:
        i = self._indice(widget) + direccion
        while 0 <= i < len(self.bloques):
            if self.bloques[i].es_texto:
                self._enfocar(self.bloques[i], al_final=direccion < 0)
                return True
            i += direccion
        return False

    def al_final(self) -> None:
        ultimo = self.bloques[-1] if self.bloques else None
        if ultimo is not None and ultimo.kind == "paragraph" and not ultimo.a_bloque().text:
            self._enfocar(ultimo)
        else:
            w = self._anadir(crear_bloque(self, Bloque()))
            self.cambio()
            self._enfocar(w)

    def enlace_a_texto(self, widget: BloqueBase) -> None:
        datos = widget.a_bloque()
        datos.kind, datos.text, datos.url, datos.preview = "paragraph", datos.url or "", None, None
        nuevo = self.reemplazar(widget, datos)
        self.cambio()
        self._enfocar(nuevo)

    # --- menú «/» y formato ----------------------------------------------------------------------

    def actualizar_slash(self, widget: BloqueBase) -> None:
        opciones = widget.opciones_slash() if isinstance(widget, BloqueTexto) else []
        if opciones:
            self.slash.mostrar(widget, opciones)
        elif self.slash.bloque is widget or self.slash.visible:
            self.slash.ocultar()

    def _elegir_slash(self, tipo: str) -> None:
        widget = self.slash.bloque
        self.slash.ocultar()
        if widget is None:
            return
        widget.buf.set_text("", -1)
        if tipo in ("image", "file"):
            self.especial(widget, tipo)
        elif tipo == "divider":
            self.insertar_tras(widget, [Bloque(kind="divider")])
        else:
            self.convertir(widget, tipo, "")

    def seleccion_cambio(self, widget: BloqueBase, hay: bool) -> None:
        if hay:
            self.barra.mostrar(widget)
        elif self.barra.bloque is widget:
            self.barra.ocultar()

    def pedir_enlace(self, widget: Optional[BloqueBase]) -> None:
        if isinstance(widget, BloqueTexto):
            self.cuadro_enlace.mostrar(widget)

    # --- imágenes, archivos y divisores ----------------------------------------------------------

    def insertar_tras(self, widget: Optional[BloqueBase], nuevos: list[Bloque]) -> None:
        """Inserta bloques tras `widget` (sustituyéndolo si es un párrafo vacío)."""
        if widget is not None and widget in self.bloques:
            i = self._indice(widget)
            datos = widget.a_bloque()
            if datos.kind == "paragraph" and not datos.text:
                self._quitar_widget(widget)
            else:
                i += 1
        else:
            i = len(self.bloques)
            if self.bloques and self.bloques[-1].kind == "paragraph" and not self.bloques[-1].a_bloque().text:
                i -= 1
        for k, datos in enumerate(nuevos):
            self._anadir(crear_bloque(self, datos), i + k)
        self._asegurar_final()
        self.renumerar()
        self.cambio()

    def especial(self, widget: Optional[BloqueBase], tipo: str) -> None:
        if tipo == "image":
            self._elegir(["image/*"], "Elige una imagen", False,
                         lambda rutas: self._anadir_imagenes(widget, rutas))
        elif tipo == "file":
            self._elegir(["application/pdf", "application/msword",
                          "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                          "application/vnd.oasis.opendocument.text"],
                         "Elige los PDF o documentos de Word que quieres adjuntar", True,
                         lambda rutas: self._anadir_archivos(widget, rutas))

    def _elegir(self, tipos_mime: list[str], titulo: str, multiple: bool, al_elegir: Callable[[list[str]], None]) -> None:
        dialogo = Gtk.FileDialog()
        dialogo.set_title(titulo)
        filtro = Gtk.FileFilter()
        filtro.set_name("Archivos admitidos")
        for mime in tipos_mime:
            filtro.add_mime_type(mime)
        filtros = Gio.ListStore.new(Gtk.FileFilter)
        filtros.append(filtro)
        dialogo.set_filters(filtros)
        dialogo.set_default_filter(filtro)

        def listo(dlg, resultado):
            try:
                if multiple:
                    modelo = dlg.open_multiple_finish(resultado)
                    archivos = [modelo.get_item(i) for i in range(modelo.get_n_items())]
                else:
                    archivos = [dlg.open_finish(resultado)]
            except GLib.Error:
                return                                   # cancelado
            rutas = [a.get_path() for a in archivos if a is not None and a.get_path()]
            if rutas:
                al_elegir(rutas)

        ventana = self.get_root()
        if multiple:
            dialogo.open_multiple(ventana, None, listo)
        else:
            dialogo.open(ventana, None, listo)

    def _anadir_imagenes(self, widget: Optional[BloqueBase], rutas: list[str]) -> None:
        nuevos = []
        for ruta in rutas:
            datos = preparar_imagen(ruta)
            if datos:
                nuevos.append(Bloque(kind="image", image_data=datos))
        if nuevos:
            self.insertar_tras(widget, nuevos)

    def _anadir_archivos(self, widget: Optional[BloqueBase], rutas: list[str]) -> None:
        self.insertar_tras(widget, [Bloque(kind="file", file_name=os.path.basename(r), path=r) for r in rutas])

    def vincular_archivo(self, widget: BloqueBase) -> None:
        """Elige de nuevo el archivo de un adjunto que no se encuentra (o que viene de macOS)."""
        def asignar(rutas: list[str]) -> None:
            datos = widget.a_bloque()
            datos.path = rutas[0]
            datos.file_name = datos.file_name or os.path.basename(rutas[0])
            self.reemplazar(widget, datos)
            self.cambio()

        self._elegir(["application/pdf", "application/msword",
                      "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                      "application/vnd.oasis.opendocument.text"], "Elige el archivo", False, asignar)

    def _archivos_soltados(self, _destino, valor, _x, _y) -> bool:
        rutas = [f.get_path() for f in valor.get_files() if f.get_path()]
        imagenes = [r for r in rutas if os.path.splitext(r)[1].lower() in EXTENSIONES_IMAGEN]
        otros = [r for r in rutas if r not in imagenes]
        if imagenes:
            self._anadir_imagenes(None, imagenes)
        if otros:
            self._anadir_archivos(None, otros)
        return bool(rutas)

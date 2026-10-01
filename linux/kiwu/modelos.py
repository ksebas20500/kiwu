"""Modelos de datos y formato de las notas.

El formato JSON de las notas es el mismo que usa Kiwu para macOS (`NoteDocument`, `NoteBlock`,
`TextRun`): una nota creada en un sistema se lee en el otro. Los lectores son tolerantes: aceptan
campos ausentes y conservan los desconocidos.
"""

from __future__ import annotations

import json
import uuid
from dataclasses import dataclass, field
from typing import Optional

TIPOS_BLOQUE = [
    "paragraph", "heading", "subheading", "bulletedList", "numberedList", "checklist",
    "quote", "callout", "code", "image", "file", "divider", "embed",
]

TITULOS_BLOQUE = {
    "paragraph": "Texto",
    "heading": "Título 1",
    "subheading": "Título 2",
    "bulletedList": "Lista con viñetas",
    "numberedList": "Lista numerada",
    "checklist": "Lista de tareas",
    "quote": "Cita",
    "callout": "Destacado",
    "code": "Código",
    "image": "Imagen",
    "file": "Archivo adjunto (PDF, Word)",
    "divider": "Divisor",
    "embed": "Enlace con vista previa (web o vídeo)",
}

ICONOS_BLOQUE = {
    "paragraph": "format-justify-left-symbolic",
    "heading": "format-text-bold-symbolic",
    "subheading": "format-text-italic-symbolic",
    "bulletedList": "view-list-symbolic",
    "numberedList": "view-list-ordered-symbolic",
    "checklist": "object-select-symbolic",
    "quote": "format-indent-more-symbolic",
    "callout": "dialog-information-symbolic",
    "code": "utilities-terminal-symbolic",
    "image": "insert-image-symbolic",
    "file": "mail-attachment-symbolic",
    "divider": "list-remove-symbolic",
    "embed": "insert-link-symbolic",
}

# Bloques cuyo contenido es texto editable
BLOQUES_DE_TEXTO = {
    "paragraph", "heading", "subheading", "bulletedList", "numberedList", "checklist", "quote", "callout",
}


def nuevo_id() -> str:
    return str(uuid.uuid4()).upper()


@dataclass
class TextRun:
    text: str
    bold: bool = False
    italic: bool = False
    underline: bool = False
    strike: bool = False
    code: bool = False
    color: Optional[str] = None
    highlight: Optional[str] = None
    link: Optional[str] = None

    def formato(self) -> tuple:
        return (self.bold, self.italic, self.underline, self.strike, self.code,
                self.color, self.highlight, self.link)

    def es_plano(self) -> bool:
        return self.formato() == (False, False, False, False, False, None, None, None)

    def a_json(self) -> dict:
        # macOS exige todas las claves booleanas; las opcionales se omiten si no hay valor.
        d = {
            "text": self.text, "bold": self.bold, "italic": self.italic, "underline": self.underline,
            "strike": self.strike, "code": self.code,
        }
        if self.color:
            d["color"] = self.color
        if self.highlight:
            d["highlight"] = self.highlight
        if self.link:
            d["link"] = self.link
        return d

    @staticmethod
    def de_json(d: dict) -> "TextRun":
        return TextRun(
            text=str(d.get("text", "")), bold=bool(d.get("bold", False)), italic=bool(d.get("italic", False)),
            underline=bool(d.get("underline", False)), strike=bool(d.get("strike", False)),
            code=bool(d.get("code", False)), color=d.get("color"), highlight=d.get("highlight"),
            link=d.get("link"),
        )


def fusionar_runs(runs: list[TextRun]) -> list[TextRun]:
    """Une tramos contiguos con el mismo formato."""
    salida: list[TextRun] = []
    for run in runs:
        if not run.text:
            continue
        if salida and salida[-1].formato() == run.formato():
            salida[-1].text += run.text
        else:
            salida.append(TextRun(**{**run.__dict__}))
    return salida


def runs_o_nada(runs: list[TextRun]) -> Optional[list[TextRun]]:
    """None si el texto no tiene ningún formato (como hace la versión de macOS)."""
    runs = fusionar_runs(runs)
    return None if all(r.es_plano() for r in runs) else runs


@dataclass
class Bloque:
    id: str = field(default_factory=nuevo_id)
    kind: str = "paragraph"
    text: str = ""
    runs: Optional[list[TextRun]] = None
    indent: int = 0
    language: Optional[str] = None
    url: Optional[str] = None
    preview: Optional[dict] = None
    is_checked: bool = False
    image_data: Optional[str] = None      # base64, igual que Data en el JSON de Swift
    bookmark: Optional[str] = None        # marcador de macOS (opaco, se conserva al reexportar)
    file_name: Optional[str] = None
    path: Optional[str] = None            # ruta del archivo adjunto en Linux
    extra: dict = field(default_factory=dict)  # campos desconocidos, para no perderlos

    def a_json(self) -> dict:
        d: dict = dict(self.extra)
        d.update({
            "id": self.id, "kind": self.kind, "text": self.text, "indent": self.indent,
            "isChecked": self.is_checked,
        })
        if self.runs:
            d["runs"] = [r.a_json() for r in self.runs]
        for clave, valor in (("language", self.language), ("url", self.url), ("preview", self.preview),
                             ("imageData", self.image_data), ("bookmark", self.bookmark),
                             ("fileName", self.file_name), ("path", self.path)):
            if valor is not None:
                d[clave] = valor
        return d

    @staticmethod
    def de_json(d: dict) -> "Bloque":
        conocidas = {"id", "kind", "text", "runs", "indent", "language", "url", "preview", "isChecked",
                     "imageData", "bookmark", "fileName", "path"}
        tipo = d.get("kind", "paragraph")
        runs = d.get("runs")
        return Bloque(
            id=str(d.get("id") or nuevo_id()),
            kind=tipo if tipo in TIPOS_BLOQUE else "paragraph",
            text=str(d.get("text", "")),
            runs=[TextRun.de_json(r) for r in runs] if isinstance(runs, list) else None,
            indent=max(0, min(4, int(d.get("indent", 0) or 0))),
            language=d.get("language"), url=d.get("url"), preview=d.get("preview"),
            is_checked=bool(d.get("isChecked", False)), image_data=d.get("imageData"),
            bookmark=d.get("bookmark"), file_name=d.get("fileName"), path=d.get("path"),
            extra={k: v for k, v in d.items() if k not in conocidas},
        )

    def texto_plano(self) -> str:
        if self.kind == "file":
            return self.file_name or ""
        if self.kind == "embed":
            return (self.preview or {}).get("titulo") or self.url or ""
        return self.text


@dataclass
class Documento:
    bloques: list[Bloque] = field(default_factory=lambda: [Bloque()])

    @staticmethod
    def vacio() -> "Documento":
        return Documento([Bloque()])

    @staticmethod
    def de_json(contenido) -> "Documento":
        """Lee un documento. Si el contenido no es válido devuelve uno vacío (nunca falla)."""
        try:
            if isinstance(contenido, (bytes, bytearray)):
                contenido = contenido.decode("utf-8")
            datos = json.loads(contenido) if isinstance(contenido, str) else contenido
            bloques = [Bloque.de_json(b) for b in datos.get("blocks", []) if isinstance(b, dict)]
        except (ValueError, AttributeError, TypeError):
            return Documento.vacio()
        return Documento(bloques or [Bloque()])

    def a_json(self) -> str:
        return json.dumps({"blocks": [b.a_json() for b in self.bloques]}, ensure_ascii=False, separators=(",", ":"))

    def texto_plano(self) -> str:
        return "\n".join(t for t in (b.texto_plano() for b in self.bloques) if t)


# --- Entidades de la base de datos -------------------------------------------------------------

@dataclass
class Materia:
    id: str
    nombre: str
    icono: str = "book"
    orden: int = 0
    carpeta: Optional[str] = None
    creada: float = 0.0
    actualizada: float = 0.0


@dataclass
class Deber:
    id: str
    materia_id: str
    titulo: str
    completado: bool = False
    estado: int = 0                       # 0 por hacer, 1 en curso (Hecho se deduce de `completado`)
    fecha_entrega: Optional[float] = None
    recordatorio_horas: int = 24
    fecha_completado: Optional[float] = None
    nota: str = '{"blocks":[]}'
    creada: float = 0.0
    actualizada: float = 0.0
    recordado: Optional[float] = None


@dataclass
class Cuaderno:
    id: str
    nombre: str
    icono: str = "📓"
    orden: int = 0
    creada: float = 0.0
    actualizada: float = 0.0


@dataclass
class Pagina:
    id: str
    cuaderno_id: str
    titulo: str = ""
    icono: str = ""
    contenido: str = '{"blocks":[]}'
    texto_plano: str = ""
    favorita: bool = False
    creada: float = 0.0
    actualizada: float = 0.0

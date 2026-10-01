"""Lenguajes de los bloques de código y detección automática (misma lista que Kiwu para macOS)."""

from __future__ import annotations

import json
import re

# id de Kiwu, nombre, id en GtkSourceView 5 (None si no hay soporte)
LENGUAJES = [
    ("plano", "Texto plano", None),
    ("swift", "Swift", "swift"),
    ("python", "Python", "python3"),
    ("javascript", "JavaScript", "js"),
    ("typescript", "TypeScript", "typescript"),
    ("json", "JSON", "json"),
    ("html", "HTML / XML", "html"),
    ("css", "CSS", "css"),
    ("bash", "Bash / Shell", "sh"),
    ("c", "C / C++", "cpp"),
    ("java", "Java", "java"),
    ("csharp", "C#", "c-sharp"),
    ("go", "Go", "go"),
    ("rust", "Rust", "rust"),
    ("sql", "SQL", "sql"),
]

_POR_ID = {i: (n, g) for i, n, g in LENGUAJES}


def nombre(id_: str) -> str:
    return _POR_ID.get(id_, (id_, None))[0]


def id_gtksource(id_: str):
    return _POR_ID.get(id_, (None, None))[1]


def detectar(texto: str) -> str:
    t = (texto or "").strip()
    if not t:
        return "plano"
    if t[0] in "{[":
        try:
            json.loads(t)
            return "json"
        except ValueError:
            pass

    def tiene(patron: str, flags: int = re.MULTILINE) -> bool:
        return re.search(patron, t, flags) is not None

    if tiene(r"^#!.*(sh|bash|zsh)"):
        return "bash"
    if tiene(r"^\s*<(!DOCTYPE|html|div|span|\?xml|head|body)", re.IGNORECASE):
        return "html"
    if tiene(r"\b(select\s.+\sfrom|insert\s+into|create\s+table|update\s+\w+\s+set)\b", re.IGNORECASE | re.DOTALL):
        return "sql"
    if tiene(r"\b(import\s+(SwiftUI|Foundation|UIKit|AppKit)|func\s+\w+\(|guard\s+let|@State|struct\s+\w+\s*:)"):
        return "swift"
    if tiene(r"(^|\n)\s*(def\s+\w+\(|class\s+\w+.*:\s*$|import\s+\w+\s*$|from\s+\w+\s+import|print\()"):
        return "python"
    if tiene(r"\b(fn\s+\w+|let\s+mut|impl\s|pub\s+(fn|struct))"):
        return "rust"
    if tiene(r"\bpackage\s+\w+\s*$|\bfunc\s+\w+\(.*\)\s*.*\{|:="):
        return "go"
    if tiene(r"\b(public\s+static\s+void|System\.out\.print|import\s+java\.)"):
        return "java"
    if tiene(r"\b(using\s+System|Console\.WriteLine|namespace\s+\w+\s*\{?)"):
        return "csharp"
    if tiene(r"#include\s*[<\"]|\bstd::|\bint\s+main\s*\("):
        return "c"
    if tiene(r"\b(const|let|var)\s+\w+\s*=|=>|\bfunction\b|console\.log|require\("):
        return "typescript" if tiene(r":\s*(string|number|boolean)\b|\binterface\s+\w+") else "javascript"
    if tiene(r"(^|\n)\s*[.#]?[A-Za-z][\w\-\s,.#]*\{[^}]*:[^}]*;"):
        return "css"
    if tiene(r"^(\$ |sudo |cd |ls |echo |git |npm |brew |pacman |yay |curl )"):
        return "bash"
    return "plano"

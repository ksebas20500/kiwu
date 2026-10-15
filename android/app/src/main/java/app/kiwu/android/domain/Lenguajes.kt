package app.kiwu.android.domain

import kotlinx.serialization.json.Json

/** Lenguajes de los bloques de código y detección automática (misma lista y mismas reglas que Linux y macOS). */
object Lenguajes {
    /** id de Kiwu a nombre visible. */
    val nombres = linkedMapOf(
        "plano" to "Texto plano",
        "swift" to "Swift",
        "python" to "Python",
        "javascript" to "JavaScript",
        "typescript" to "TypeScript",
        "json" to "JSON",
        "html" to "HTML / XML",
        "css" to "CSS",
        "bash" to "Bash / Shell",
        "c" to "C / C++",
        "java" to "Java",
        "csharp" to "C#",
        "go" to "Go",
        "rust" to "Rust",
        "sql" to "SQL",
    )

    fun nombre(id: String?): String = nombres[id] ?: id ?: nombres.getValue("plano")

    private fun tiene(texto: String, patron: String, vararg opciones: RegexOption): Boolean =
        Regex(patron, setOf(RegexOption.MULTILINE, *opciones)).containsMatchIn(texto)

    fun detectar(codigo: String?): String {
        val t = (codigo ?: "").trim()
        if (t.isEmpty()) return "plano"
        if (t[0] == '{' || t[0] == '[') {
            try {
                Json.parseToJsonElement(t)
                return "json"
            } catch (e: Exception) {
                // no es JSON: se sigue con el resto de reglas
            }
        }
        val i = RegexOption.IGNORE_CASE
        val punto = RegexOption.DOT_MATCHES_ALL
        return when {
            tiene(t, """^#!.*(sh|bash|zsh)""") -> "bash"
            tiene(t, """^\s*<(!DOCTYPE|html|div|span|\?xml|head|body)""", i) -> "html"
            tiene(t, """\b(select\s.+\sfrom|insert\s+into|create\s+table|update\s+\w+\s+set)\b""", i, punto) -> "sql"
            tiene(t, """\b(import\s+(SwiftUI|Foundation|UIKit|AppKit)|func\s+\w+\(|guard\s+let|@State|struct\s+\w+\s*:)""") -> "swift"
            tiene(t, """(^|\n)\s*(def\s+\w+\(|class\s+\w+.*:\s*$|import\s+\w+\s*$|from\s+\w+\s+import|print\()""") -> "python"
            tiene(t, """\b(fn\s+\w+|let\s+mut|impl\s|pub\s+(fn|struct))""") -> "rust"
            tiene(t, """\bpackage\s+\w+\s*$|\bfunc\s+\w+\(.*\)\s*.*\{|:=""") -> "go"
            tiene(t, """\b(public\s+static\s+void|System\.out\.print|import\s+java\.)""") -> "java"
            tiene(t, """\b(using\s+System|Console\.WriteLine|namespace\s+\w+\s*\{?)""") -> "csharp"
            tiene(t, """#include\s*[<"]|\bstd::|\bint\s+main\s*\(""") -> "c"
            tiene(t, """\b(const|let|var)\s+\w+\s*=|=>|\bfunction\b|console\.log|require\(""") ->
                if (tiene(t, """:\s*(string|number|boolean)\b|\binterface\s+\w+""")) "typescript" else "javascript"
            tiene(t, """(^|\n)\s*[.#]?[A-Za-z][\w\-\s,.#]*\{[^}]*:[^}]*;""") -> "css"
            tiene(t, """^(\$ |sudo |cd |ls |echo |git |npm |brew |pacman |yay |curl )""") -> "bash"
            else -> "plano"
        }
    }
}

package app.kiwu.android.domain

/**
 * Resaltado de sintaxis por expresiones regulares (mismas reglas que `Resaltador.swift` en macOS).
 * Devuelve tramos con su tipo; los colores (VS Code Dark+ y Light+) se eligen en la interfaz.
 */
object Resaltador {
    enum class Token { PALABRA, CONTROL, CADENA, COMENTARIO, NUMERO, TIPO, FUNCION, VARIABLE, ETIQUETA, ATRIBUTO, PROPIEDAD, DECORADOR }

    class Tramo(val inicio: Int, val fin: Int, val token: Token)

    private class Regla(val regex: Regex, val token: Token)

    private const val MAX_CARACTERES = 60_000

    private fun kw(vararg palabras: String) = "\\b(?:" + palabras.joinToString("|") + ")\\b"

    private const val COMILLA_DOBLE = "\"(?:\\\\.|[^\"\\\\\\n])*\""
    private const val COMILLA_SIMPLE = "'(?:\\\\.|[^'\\\\\\n])*'"
    private const val COMILLA_INVERSA = "`(?:\\\\.|[^`\\\\])*`"
    private const val COMENTARIO_BLOQUE = "/\\*[\\s\\S]*?(?:\\*/|$)"
    private const val COMENTARIO_LINEA = "//[^\\n]*"
    private const val NUMERO = "\\b(?:0[xX][0-9a-fA-F_]+|0[bB][01_]+|\\d[\\d_]*(?:\\.\\d+)?(?:[eE][+-]?\\d+)?)\\b"
    private const val LLAMADA = "\\b[a-zA-Z_][A-Za-z0-9_]*(?=\\s*\\()"
    private const val TIPO_MAYUS = "\\b[A-Z][A-Za-z0-9_]*\\b"

    private fun r(patron: String, token: Token, vararg opciones: RegexOption) = Regla(Regex(patron, opciones.toSet()), token)

    private val cache = HashMap<String, List<Regla>>()

    private fun reglas(id: String): List<Regla> = synchronized(cache) { cache.getOrPut(id) { definiciones(id) } }

    private fun definiciones(id: String): List<Regla> = when (id) {
        "swift" -> listOf(
            r(COMENTARIO_BLOQUE, Token.COMENTARIO), r(COMENTARIO_LINEA, Token.COMENTARIO),
            r("\"\"\"[\\s\\S]*?(?:\"\"\"|$)", Token.CADENA), r(COMILLA_DOBLE, Token.CADENA),
            r("@[A-Za-z_][A-Za-z0-9_]*", Token.DECORADOR),
            r(
                kw("if", "else", "guard", "switch", "case", "default", "for", "while", "repeat", "do", "catch", "return",
                    "break", "continue", "defer", "fallthrough", "throw", "try", "await"),
                Token.CONTROL,
            ),
            r(
                kw("import", "class", "struct", "enum", "protocol", "extension", "func", "var", "let", "init", "deinit",
                    "subscript", "typealias", "static", "private", "public", "internal", "fileprivate", "open", "final",
                    "override", "mutating", "lazy", "weak", "unowned", "where", "self", "Self", "super", "nil", "true",
                    "false", "in", "is", "as", "async", "throws", "rethrows", "inout", "some", "any", "associatedtype",
                    "convenience", "required", "indirect", "nonisolated", "actor"),
                Token.PALABRA,
            ),
            r(NUMERO, Token.NUMERO), r(LLAMADA, Token.FUNCION), r(TIPO_MAYUS, Token.TIPO),
        )
        "python" -> listOf(
            r("#[^\\n]*", Token.COMENTARIO),
            r("[rRbBfFuU]{0,2}(?:\"\"\"[\\s\\S]*?(?:\"\"\"|$)|'''[\\s\\S]*?(?:'''|$))", Token.CADENA),
            r("[rRbBfFuU]{0,2}$COMILLA_DOBLE", Token.CADENA), r("[rRbBfFuU]{0,2}$COMILLA_SIMPLE", Token.CADENA),
            r("^\\s*@[A-Za-z_][\\w.]*", Token.DECORADOR, RegexOption.MULTILINE),
            r(
                kw("if", "elif", "else", "for", "while", "try", "except", "finally", "return", "break", "continue",
                    "match", "case", "raise", "yield", "pass", "with", "assert", "del"),
                Token.CONTROL,
            ),
            r(
                kw("def", "class", "lambda", "import", "from", "as", "global", "nonlocal", "None", "True", "False",
                    "and", "or", "not", "in", "is", "async", "await", "self", "cls"),
                Token.PALABRA,
            ),
            r(NUMERO, Token.NUMERO), r(LLAMADA, Token.FUNCION), r(TIPO_MAYUS, Token.TIPO),
        )
        "javascript", "typescript" -> listOf(
            r(COMENTARIO_BLOQUE, Token.COMENTARIO), r(COMENTARIO_LINEA, Token.COMENTARIO),
            r(COMILLA_INVERSA, Token.CADENA), r(COMILLA_DOBLE, Token.CADENA), r(COMILLA_SIMPLE, Token.CADENA),
            r(
                kw("if", "else", "for", "while", "do", "switch", "case", "default", "break", "continue", "return",
                    "try", "catch", "finally", "throw", "await", "yield"),
                Token.CONTROL,
            ),
            r(
                kw("const", "let", "var", "function", "class", "extends", "new", "this", "super", "import", "export",
                    "from", "as", "typeof", "instanceof", "void", "delete", "in", "of", "async", "static", "get", "set",
                    "null", "undefined", "true", "false", "interface", "type", "enum", "implements", "public", "private",
                    "protected", "readonly", "declare", "namespace", "abstract", "keyof"),
                Token.PALABRA,
            ),
            r(NUMERO, Token.NUMERO), r(LLAMADA, Token.FUNCION), r(TIPO_MAYUS, Token.TIPO),
        )
        "json" -> listOf(
            r("\"(?:\\\\.|[^\"\\\\])*\"(?=\\s*:)", Token.PROPIEDAD),
            r("\"(?:\\\\.|[^\"\\\\])*\"", Token.CADENA),
            r("\\b(?:true|false|null)\\b", Token.PALABRA),
            r("-?\\b\\d+(?:\\.\\d+)?(?:[eE][+-]?\\d+)?\\b", Token.NUMERO),
        )
        "html" -> listOf(
            r("<!--[\\s\\S]*?(?:-->|$)", Token.COMENTARIO),
            r("\"[^\"]*\"", Token.CADENA), r("'[^']*'", Token.CADENA),
            r("</?[A-Za-z][\\w:.-]*", Token.ETIQUETA), r("/?>", Token.ETIQUETA),
            r("[A-Za-z_:@][\\w:.-]*(?=\\s*=)", Token.ATRIBUTO),
            r("&[A-Za-z#0-9]+;", Token.NUMERO),
        )
        "css" -> listOf(
            r(COMENTARIO_BLOQUE, Token.COMENTARIO), r(COMILLA_DOBLE, Token.CADENA), r(COMILLA_SIMPLE, Token.CADENA),
            r("@[A-Za-z-]+", Token.CONTROL),
            r("#[0-9a-fA-F]{3,8}\\b", Token.NUMERO),
            r("-?\\b\\d+(?:\\.\\d+)?(?:px|em|rem|%|vh|vw|s|ms|deg|fr)?\\b", Token.NUMERO),
            r("[A-Za-z-]+(?=\\s*:)", Token.PROPIEDAD),
            r("[.#][A-Za-z_-][\\w-]*", Token.TIPO),
            r("\\b[a-z][a-z0-9]*(?=\\s*[{,:.])", Token.ETIQUETA),
            r("\\b[a-zA-Z-]+(?=\\()", Token.FUNCION),
        )
        "bash" -> listOf(
            r("(?:^|(?<=\\s))#[^\\n]*", Token.COMENTARIO, RegexOption.MULTILINE),
            r(COMILLA_DOBLE, Token.CADENA), r(COMILLA_SIMPLE, Token.CADENA),
            r("\\$\\{[^}]*\\}|\\$[A-Za-z_][A-Za-z0-9_]*|\\$[0-9@#?!*]", Token.VARIABLE),
            r(
                kw("if", "then", "else", "elif", "fi", "for", "while", "until", "do", "done", "case", "esac", "in",
                    "function", "return", "exit", "break", "continue"),
                Token.CONTROL,
            ),
            r(kw("export", "local", "echo", "cd", "source", "alias", "unset", "set", "readonly", "declare", "sudo"), Token.PALABRA),
            r("(?<=\\s)--?[A-Za-z][\\w-]*", Token.ATRIBUTO),
            r(NUMERO, Token.NUMERO), r("^\\s*[a-zA-Z_][\\w./-]*", Token.FUNCION, RegexOption.MULTILINE),
        )
        "c", "java", "csharp", "go", "rust" -> listOf(
            r(COMENTARIO_BLOQUE, Token.COMENTARIO), r(COMENTARIO_LINEA, Token.COMENTARIO),
            r(COMILLA_DOBLE, Token.CADENA), r(COMILLA_SIMPLE, Token.CADENA),
            r("^\\s*#\\s*\\w+", Token.DECORADOR, RegexOption.MULTILINE),
            r("@[A-Za-z_]\\w*", Token.DECORADOR),
            r(
                kw("if", "else", "for", "while", "do", "switch", "case", "default", "break", "continue", "return", "goto",
                    "try", "catch", "finally", "throw", "match", "loop", "select", "defer", "go"),
                Token.CONTROL,
            ),
            r(
                kw("int", "char", "float", "double", "void", "long", "short", "unsigned", "signed", "bool", "const",
                    "static", "struct", "class", "enum", "union", "typedef", "namespace", "using", "template", "typename",
                    "public", "private", "protected", "virtual", "override", "final", "new", "delete", "this", "nullptr",
                    "NULL", "null", "true", "false", "sizeof", "extern", "inline", "auto", "volatile", "import", "package",
                    "interface", "extends", "implements", "throws", "abstract", "synchronized", "fn", "let", "mut", "pub",
                    "impl", "trait", "use", "mod", "func", "var", "chan", "range", "map", "string", "byte", "async",
                    "await", "readonly", "sealed", "record", "self", "Self", "type", "as", "in", "where"),
                Token.PALABRA,
            ),
            r(NUMERO, Token.NUMERO), r(LLAMADA, Token.FUNCION), r(TIPO_MAYUS, Token.TIPO),
        )
        "sql" -> listOf(
            r("--[^\\n]*", Token.COMENTARIO), r(COMENTARIO_BLOQUE, Token.COMENTARIO), r(COMILLA_SIMPLE, Token.CADENA),
            r(
                "\\b(?:select|from|where|and|or|not|insert|into|values|update|set|delete|create|table|alter|drop|" +
                    "index|view|join|inner|left|right|outer|full|cross|on|as|group|by|order|having|limit|offset|union|all|" +
                    "distinct|null|is|in|like|between|exists|case|when|then|else|end|primary|key|foreign|references|" +
                    "default|constraint|unique|asc|desc|begin|commit|rollback|if|with)\\b",
                Token.PALABRA, RegexOption.IGNORE_CASE,
            ),
            r(NUMERO, Token.NUMERO), r("\\b[a-z_]+(?=\\s*\\()", Token.FUNCION, RegexOption.IGNORE_CASE),
        )
        else -> emptyList()
    }

    /** Tramos coloreados del código. El primero que cubre un trozo gana (las reglas van de más a menos específica). */
    fun tramos(texto: String, lenguaje: String): List<Tramo> {
        if (texto.isEmpty() || texto.length >= MAX_CARACTERES) return emptyList()
        val ocupado = BooleanArray(texto.length)
        val salida = ArrayList<Tramo>()
        for (regla in reglas(lenguaje)) {
            for (m in regla.regex.findAll(texto)) {
                val rango = m.range
                if (rango.isEmpty()) continue
                var libre = true
                for (i in rango) if (ocupado[i]) { libre = false; break }
                if (!libre) continue
                for (i in rango) ocupado[i] = true
                salida.add(Tramo(rango.first, rango.last + 1, regla.token))
            }
        }
        return salida
    }
}

import AppKit

/// Resaltado de sintaxis por expresiones regulares, con los colores de VS Code (Dark+ y Light+).
enum Resaltador {
    enum Token {
        case palabra, control, cadena, comentario, numero, tipo, funcion, variable, etiqueta, atributo, propiedad, decorador
    }

    struct Lenguaje: Identifiable, Hashable {
        let id: String
        let nombre: String
    }

    static let lenguajes: [Lenguaje] = [
        Lenguaje(id: "plano", nombre: "Texto plano"),
        Lenguaje(id: "swift", nombre: "Swift"),
        Lenguaje(id: "python", nombre: "Python"),
        Lenguaje(id: "javascript", nombre: "JavaScript"),
        Lenguaje(id: "typescript", nombre: "TypeScript"),
        Lenguaje(id: "json", nombre: "JSON"),
        Lenguaje(id: "html", nombre: "HTML / XML"),
        Lenguaje(id: "css", nombre: "CSS"),
        Lenguaje(id: "bash", nombre: "Bash / Shell"),
        Lenguaje(id: "c", nombre: "C / C++"),
        Lenguaje(id: "java", nombre: "Java"),
        Lenguaje(id: "csharp", nombre: "C#"),
        Lenguaje(id: "go", nombre: "Go"),
        Lenguaje(id: "rust", nombre: "Rust"),
        Lenguaje(id: "sql", nombre: "SQL")
    ]

    static func nombre(de id: String) -> String { lenguajes.first { $0.id == id }?.nombre ?? id }

    // MARK: Colores

    private static func hex(_ valor: UInt32) -> NSColor {
        NSColor(srgbRed: CGFloat((valor >> 16) & 0xFF) / 255, green: CGFloat((valor >> 8) & 0xFF) / 255,
                blue: CGFloat(valor & 0xFF) / 255, alpha: 1)
    }

    static func color(_ token: Token?, oscuro: Bool) -> NSColor {
        guard let token else { return oscuro ? hex(0xD4D4D4) : hex(0x1F1F1F) }
        switch token {
        case .palabra: return oscuro ? hex(0x569CD6) : hex(0x0000FF)
        case .control: return oscuro ? hex(0xC586C0) : hex(0xAF00DB)
        case .cadena: return oscuro ? hex(0xCE9178) : hex(0xA31515)
        case .comentario: return oscuro ? hex(0x6A9955) : hex(0x008000)
        case .numero: return oscuro ? hex(0xB5CEA8) : hex(0x098658)
        case .tipo: return oscuro ? hex(0x4EC9B0) : hex(0x267F99)
        case .funcion: return oscuro ? hex(0xDCDCAA) : hex(0x795E26)
        case .variable: return oscuro ? hex(0x9CDCFE) : hex(0x001080)
        case .etiqueta: return oscuro ? hex(0x569CD6) : hex(0x800000)
        case .atributo: return oscuro ? hex(0x9CDCFE) : hex(0xE50000)
        case .propiedad: return oscuro ? hex(0x9CDCFE) : hex(0x0451A5)
        case .decorador: return oscuro ? hex(0xDCDCAA) : hex(0x795E26)
        }
    }

    static func fondo(oscuro: Bool) -> NSColor { oscuro ? hex(0x1E1E1E) : hex(0xF5F5F5) }

    // MARK: Aplicar

    static func aplicar(_ ts: NSTextStorage, lenguaje: String, oscuro: Bool, fuente: NSFont) {
        let todo = NSRange(location: 0, length: ts.length)
        ts.beginEditing()
        ts.setAttributes([.font: fuente, .foregroundColor: color(nil, oscuro: oscuro)], range: todo)
        if ts.length > 0, ts.length < 60_000 {
            var ocupado = IndexSet()
            for regla in reglas(para: lenguaje) {
                regla.regex.enumerateMatches(in: ts.string, range: todo) { m, _, _ in
                    guard let m else { return }
                    let r = regla.grupo < m.numberOfRanges ? m.range(at: regla.grupo) : m.range
                    guard r.location != NSNotFound, r.length > 0 else { return }
                    let ind = r.location..<NSMaxRange(r)
                    guard !ocupado.intersects(integersIn: ind) else { return }
                    ocupado.insert(integersIn: ind)
                    ts.addAttribute(.foregroundColor, value: color(regla.token, oscuro: oscuro), range: r)
                }
            }
        }
        ts.endEditing()
    }

    // MARK: Detección automática

    static func detectar(_ texto: String) -> String {
        let t = texto.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !t.isEmpty else { return "plano" }
        if (t.hasPrefix("{") || t.hasPrefix("[")),
           (try? JSONSerialization.jsonObject(with: Data(t.utf8))) != nil { return "json" }
        func tiene(_ patron: String) -> Bool { t.range(of: patron, options: .regularExpression) != nil }
        if t.hasPrefix("#!") && tiene("^#!.*(sh|bash|zsh)") { return "bash" }
        if tiene("^\\s*<(!DOCTYPE|html|div|span|\\?xml|head|body)") { return "html" }
        if tiene("(?i)\\b(select\\s.+\\sfrom|insert\\s+into|create\\s+table|update\\s+\\w+\\s+set)\\b") { return "sql" }
        if tiene("\\b(import\\s+(SwiftUI|Foundation|UIKit|AppKit)|func\\s+\\w+\\(|guard\\s+let|@State|struct\\s+\\w+\\s*:)") { return "swift" }
        if tiene("(^|\\n)\\s*(def\\s+\\w+\\(|class\\s+\\w+.*:\\s*$|import\\s+\\w+\\s*$|from\\s+\\w+\\s+import|print\\()") { return "python" }
        if tiene("\\b(fn\\s+\\w+|let\\s+mut|impl\\s|pub\\s+(fn|struct))") { return "rust" }
        if tiene("\\bpackage\\s+\\w+\\s*$|\\bfunc\\s+\\w+\\(.*\\)\\s*.*\\{|:=") { return "go" }
        if tiene("\\b(public\\s+static\\s+void|System\\.out\\.print|import\\s+java\\.)") { return "java" }
        if tiene("\\b(using\\s+System|Console\\.WriteLine|namespace\\s+\\w+\\s*\\{?)") { return "csharp" }
        if tiene("#include\\s*[<\"]|\\bstd::|\\bint\\s+main\\s*\\(") { return "c" }
        if tiene("\\b(const|let|var)\\s+\\w+\\s*=|=>|\\bfunction\\b|console\\.log|require\\(") {
            return tiene(":\\s*(string|number|boolean)\\b|\\binterface\\s+\\w+") ? "typescript" : "javascript"
        }
        if tiene("(^|\\n)\\s*[.#]?[A-Za-z][\\w\\-\\s,.#]*\\{[^}]*:[^}]*;") { return "css" }
        if tiene("^(\\$ |sudo |cd |ls |echo |git |npm |brew |curl )") { return "bash" }
        return "plano"
    }

    // MARK: Reglas

    private struct Regla {
        let regex: NSRegularExpression
        let token: Token
        let grupo: Int
    }

    nonisolated(unsafe) private static var cache: [String: [Regla]] = [:]

    private static func reglas(para id: String) -> [Regla] {
        if let hecho = cache[id] { return hecho }
        let definicion = definiciones(id).compactMap { (patron, token, opciones, grupo) -> Regla? in
            guard let re = try? NSRegularExpression(pattern: patron, options: opciones) else { return nil }
            return Regla(regex: re, token: token, grupo: grupo)
        }
        cache[id] = definicion
        return definicion
    }

    private static func kw(_ palabras: [String]) -> String { "\\b(?:" + palabras.joined(separator: "|") + ")\\b" }

    private typealias Def = (String, Token, NSRegularExpression.Options, Int)

    private static func d(_ patron: String, _ token: Token, _ opciones: NSRegularExpression.Options = [], grupo: Int = 0) -> Def {
        (patron, token, opciones, grupo)
    }

    private static let comillaDoble = "\"(?:\\\\.|[^\"\\\\\\n])*\""
    private static let comillaSimple = "'(?:\\\\.|[^'\\\\\\n])*'"
    private static let comillaInversa = "`(?:\\\\.|[^`\\\\])*`"
    private static let comentarioBloque = "/\\*[\\s\\S]*?(?:\\*/|$)"
    private static let comentarioLinea = "//[^\\n]*"
    private static let numero = "\\b(?:0[xX][0-9a-fA-F_]+|0[bB][01_]+|\\d[\\d_]*(?:\\.\\d+)?(?:[eE][+-]?\\d+)?)\\b"
    private static let llamada = "\\b[a-zA-Z_][A-Za-z0-9_]*(?=\\s*\\()"
    private static let tipoMayus = "\\b[A-Z][A-Za-z0-9_]*\\b"

    private static func definiciones(_ id: String) -> [Def] {
        switch id {
        case "swift":
            return [
                d(comentarioBloque, .comentario), d(comentarioLinea, .comentario),
                d("\"\"\"[\\s\\S]*?(?:\"\"\"|$)", .cadena), d(comillaDoble, .cadena),
                d("@[A-Za-z_][A-Za-z0-9_]*", .decorador),
                d(kw(["if", "else", "guard", "switch", "case", "default", "for", "while", "repeat", "do", "catch", "return",
                      "break", "continue", "defer", "fallthrough", "throw", "try", "await"]), .control),
                d(kw(["import", "class", "struct", "enum", "protocol", "extension", "func", "var", "let", "init", "deinit",
                      "subscript", "typealias", "static", "private", "public", "internal", "fileprivate", "open", "final",
                      "override", "mutating", "lazy", "weak", "unowned", "where", "self", "Self", "super", "nil", "true",
                      "false", "in", "is", "as", "async", "throws", "rethrows", "inout", "some", "any", "associatedtype",
                      "convenience", "required", "indirect", "nonisolated", "actor"]), .palabra),
                d(numero, .numero), d(llamada, .funcion), d(tipoMayus, .tipo)
            ]
        case "python":
            return [
                d("#[^\\n]*", .comentario),
                d("[rRbBfFuU]{0,2}(?:\"\"\"[\\s\\S]*?(?:\"\"\"|$)|'''[\\s\\S]*?(?:'''|$))", .cadena),
                d("[rRbBfFuU]{0,2}" + comillaDoble, .cadena), d("[rRbBfFuU]{0,2}" + comillaSimple, .cadena),
                d("^\\s*@[A-Za-z_][\\w.]*", .decorador, [.anchorsMatchLines]),
                d(kw(["if", "elif", "else", "for", "while", "try", "except", "finally", "return", "break", "continue",
                      "match", "case", "raise", "yield", "pass", "with", "assert", "del"]), .control),
                d(kw(["def", "class", "lambda", "import", "from", "as", "global", "nonlocal", "None", "True", "False",
                      "and", "or", "not", "in", "is", "async", "await", "self", "cls"]), .palabra),
                d(numero, .numero), d(llamada, .funcion), d(tipoMayus, .tipo)
            ]
        case "javascript", "typescript":
            return [
                d(comentarioBloque, .comentario), d(comentarioLinea, .comentario),
                d(comillaInversa, .cadena), d(comillaDoble, .cadena), d(comillaSimple, .cadena),
                d(kw(["if", "else", "for", "while", "do", "switch", "case", "default", "break", "continue", "return",
                      "try", "catch", "finally", "throw", "await", "yield"]), .control),
                d(kw(["const", "let", "var", "function", "class", "extends", "new", "this", "super", "import", "export",
                      "from", "as", "typeof", "instanceof", "void", "delete", "in", "of", "async", "static", "get", "set",
                      "null", "undefined", "true", "false", "interface", "type", "enum", "implements", "public", "private",
                      "protected", "readonly", "declare", "namespace", "abstract", "keyof"]), .palabra),
                d(numero, .numero), d(llamada, .funcion), d(tipoMayus, .tipo)
            ]
        case "json":
            return [
                d("\"(?:\\\\.|[^\"\\\\])*\"(?=\\s*:)", .propiedad),
                d("\"(?:\\\\.|[^\"\\\\])*\"", .cadena),
                d("\\b(?:true|false|null)\\b", .palabra),
                d("-?\\b\\d+(?:\\.\\d+)?(?:[eE][+-]?\\d+)?\\b", .numero)
            ]
        case "html":
            return [
                d("<!--[\\s\\S]*?(?:-->|$)", .comentario),
                d("\"[^\"]*\"", .cadena), d("'[^']*'", .cadena),
                d("</?[A-Za-z][\\w:.-]*", .etiqueta), d("/?>", .etiqueta),
                d("[A-Za-z_:@][\\w:.-]*(?=\\s*=)", .atributo),
                d("&[A-Za-z#0-9]+;", .numero)
            ]
        case "css":
            return [
                d(comentarioBloque, .comentario), d(comillaDoble, .cadena), d(comillaSimple, .cadena),
                d("@[A-Za-z-]+", .control),
                d("#[0-9a-fA-F]{3,8}\\b", .numero),
                d("-?\\b\\d+(?:\\.\\d+)?(?:px|em|rem|%|vh|vw|s|ms|deg|fr)?\\b", .numero),
                d("[A-Za-z-]+(?=\\s*:)", .propiedad),
                d("[.#][A-Za-z_-][\\w-]*", .tipo),
                d("\\b[a-z][a-z0-9]*(?=\\s*[{,:.])", .etiqueta),
                d("\\b[a-zA-Z-]+(?=\\()", .funcion)
            ]
        case "bash":
            return [
                d("(?:^|(?<=\\s))#[^\\n]*", .comentario, [.anchorsMatchLines]),
                d(comillaDoble, .cadena), d(comillaSimple, .cadena),
                d("\\$\\{[^}]*\\}|\\$[A-Za-z_][A-Za-z0-9_]*|\\$[0-9@#?!*]", .variable),
                d(kw(["if", "then", "else", "elif", "fi", "for", "while", "until", "do", "done", "case", "esac", "in",
                      "function", "return", "exit", "break", "continue"]), .control),
                d(kw(["export", "local", "echo", "cd", "source", "alias", "unset", "set", "readonly", "declare", "sudo"]), .palabra),
                d("(?<=\\s)--?[A-Za-z][\\w-]*", .atributo),
                d(numero, .numero), d("^\\s*[a-zA-Z_][\\w./-]*", .funcion, [.anchorsMatchLines])
            ]
        case "c", "java", "csharp", "go", "rust":
            return [
                d(comentarioBloque, .comentario), d(comentarioLinea, .comentario),
                d(comillaDoble, .cadena), d(comillaSimple, .cadena),
                d("^\\s*#\\s*\\w+", .decorador, [.anchorsMatchLines]),
                d("@[A-Za-z_]\\w*", .decorador),
                d(kw(["if", "else", "for", "while", "do", "switch", "case", "default", "break", "continue", "return", "goto",
                      "try", "catch", "finally", "throw", "match", "loop", "select", "defer", "go"]), .control),
                d(kw(["int", "char", "float", "double", "void", "long", "short", "unsigned", "signed", "bool", "const",
                      "static", "struct", "class", "enum", "union", "typedef", "namespace", "using", "template", "typename",
                      "public", "private", "protected", "virtual", "override", "final", "new", "delete", "this", "nullptr",
                      "NULL", "null", "true", "false", "sizeof", "extern", "inline", "auto", "volatile", "import", "package",
                      "interface", "extends", "implements", "throws", "abstract", "synchronized", "fn", "let", "mut", "pub",
                      "impl", "trait", "use", "mod", "func", "var", "chan", "range", "map", "string", "byte", "async",
                      "await", "readonly", "sealed", "record", "self", "Self", "type", "as", "in", "where"]), .palabra),
                d(numero, .numero), d(llamada, .funcion), d(tipoMayus, .tipo)
            ]
        case "sql":
            return [
                d("--[^\\n]*", .comentario), d(comentarioBloque, .comentario), d(comillaSimple, .cadena),
                d("(?i)\\b(?:select|from|where|and|or|not|insert|into|values|update|set|delete|create|table|alter|drop|"
                  + "index|view|join|inner|left|right|outer|full|cross|on|as|group|by|order|having|limit|offset|union|all|"
                  + "distinct|null|is|in|like|between|exists|case|when|then|else|end|primary|key|foreign|references|"
                  + "default|constraint|unique|asc|desc|begin|commit|rollback|if|with)\\b", .palabra),
                d(numero, .numero), d("(?i)\\b[a-z_]+(?=\\s*\\()", .funcion)
            ]
        default:
            return []
        }
    }
}

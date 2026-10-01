import SwiftUI
import AppKit
import Combine

// MARK: - Colores de texto

/// Paleta de colores de texto y resaltado (adaptada a modo claro y oscuro).
enum NotaColor: String, CaseIterable, Identifiable {
    case gris, marron, naranja, amarillo, verde, azul, morado, rosa, rojo
    var id: String { rawValue }

    var titulo: String {
        switch self {
        case .gris: return "Gris"
        case .marron: return "Marrón"
        case .naranja: return "Naranja"
        case .amarillo: return "Amarillo"
        case .verde: return "Verde"
        case .azul: return "Azul"
        case .morado: return "Morado"
        case .rosa: return "Rosa"
        case .rojo: return "Rojo"
        }
    }

    private var claro: (CGFloat, CGFloat, CGFloat) {
        switch self {
        case .gris: return (0.45, 0.45, 0.45)
        case .marron: return (0.60, 0.40, 0.27)
        case .naranja: return (0.85, 0.45, 0.05)
        case .amarillo: return (0.74, 0.58, 0.00)
        case .verde: return (0.18, 0.55, 0.34)
        case .azul: return (0.20, 0.45, 0.80)
        case .morado: return (0.55, 0.35, 0.75)
        case .rosa: return (0.80, 0.30, 0.55)
        case .rojo: return (0.80, 0.25, 0.20)
        }
    }

    private var oscuro: (CGFloat, CGFloat, CGFloat) {
        switch self {
        case .gris: return (0.62, 0.62, 0.62)
        case .marron: return (0.76, 0.56, 0.42)
        case .naranja: return (0.96, 0.62, 0.27)
        case .amarillo: return (0.96, 0.80, 0.27)
        case .verde: return (0.42, 0.76, 0.52)
        case .azul: return (0.42, 0.66, 0.96)
        case .morado: return (0.72, 0.57, 0.92)
        case .rosa: return (0.96, 0.52, 0.72)
        case .rojo: return (0.96, 0.46, 0.42)
        }
    }

    var texto: NSColor {
        let (c, o) = (claro, oscuro)
        return NSColor(name: nil) { ap in
            let d = ap.bestMatch(from: [.darkAqua, .aqua]) == .darkAqua
            let v = d ? o : c
            return NSColor(srgbRed: v.0, green: v.1, blue: v.2, alpha: 1)
        }
    }

    var fondo: NSColor {
        let (c, o) = (claro, oscuro)
        return NSColor(name: nil) { ap in
            let d = ap.bestMatch(from: [.darkAqua, .aqua]) == .darkAqua
            let v = d ? o : c
            return NSColor(srgbRed: v.0, green: v.1, blue: v.2, alpha: d ? 0.32 : 0.24)
        }
    }

    var swiftUI: Color { Color(nsColor: texto) }
}

extension Notification.Name {
    /// Un campo de texto de una nota ha recibido el foco.
    static let campoActivo = Notification.Name("kiwu.campoActivo")
}

// MARK: - Atributos semánticos

extension NSAttributedString.Key {
    static let tfBold = NSAttributedString.Key("tf.bold")
    static let tfItalic = NSAttributedString.Key("tf.italic")
    static let tfUnderline = NSAttributedString.Key("tf.underline")
    static let tfStrike = NSAttributedString.Key("tf.strike")
    static let tfCode = NSAttributedString.Key("tf.code")
    static let tfColor = NSAttributedString.Key("tf.color")
    static let tfBg = NSAttributedString.Key("tf.bg")
}

/// Aspecto base de un bloque (fuente, color y si va tachado por completo).
struct EstiloBloque: Equatable {
    var fuente: NSFont
    var color: NSColor
    var tachado = false

    var base: [NSAttributedString.Key: Any] { [.font: fuente, .foregroundColor: color] }
}

/// Convierte entre el texto con formato del editor y los `TextRun` que se guardan.
enum CodecTexto {
    static func atributado(texto: String, runs: [TextRun]?, estilo: EstiloBloque) -> NSMutableAttributedString {
        let resultado = NSMutableAttributedString()
        if let runs, runs.map(\.text).joined() == texto {
            for run in runs { resultado.append(NSAttributedString(string: run.text, attributes: semanticos(run))) }
        } else {
            resultado.append(NSAttributedString(string: texto))
        }
        visual(resultado, NSRange(location: 0, length: resultado.length), estilo)
        return resultado
    }

    private static func semanticos(_ r: TextRun) -> [NSAttributedString.Key: Any] {
        var a: [NSAttributedString.Key: Any] = [:]
        if r.bold { a[.tfBold] = true }
        if r.italic { a[.tfItalic] = true }
        if r.underline { a[.tfUnderline] = true }
        if r.strike { a[.tfStrike] = true }
        if r.code { a[.tfCode] = true }
        if let c = r.color { a[.tfColor] = c }
        if let h = r.highlight { a[.tfBg] = h }
        if let l = r.link { a[.link] = URL(string: l) ?? l }
        return a
    }

    /// nil si el texto no tiene ningún formato.
    static func runs(de texto: NSAttributedString) -> [TextRun]? {
        guard texto.length > 0 else { return nil }
        var salida: [TextRun] = []
        var hayFormato = false
        let cadena = texto.string as NSString
        texto.enumerateAttributes(in: NSRange(location: 0, length: texto.length), options: []) { a, r, _ in
            var run = TextRun(text: cadena.substring(with: r))
            run.bold = a[.tfBold] != nil
            run.italic = a[.tfItalic] != nil
            run.underline = a[.tfUnderline] != nil
            run.strike = a[.tfStrike] != nil
            run.code = a[.tfCode] != nil
            run.color = a[.tfColor] as? String
            run.highlight = a[.tfBg] as? String
            if let url = a[.link] as? URL { run.link = url.absoluteString } else { run.link = a[.link] as? String }
            if run != TextRun(text: run.text) { hayFormato = true }
            salida.append(run)
        }
        return hayFormato ? salida : nil
    }

    /// Recalcula fuente, color, fondo, subrayado y tachado a partir de los atributos semánticos.
    static func visual(_ s: NSMutableAttributedString, _ rango: NSRange, _ estilo: EstiloBloque) {
        guard rango.length > 0, NSMaxRange(rango) <= s.length else { return }
        var tramos: [(NSRange, [NSAttributedString.Key: Any])] = []
        s.enumerateAttributes(in: rango, options: []) { a, r, _ in tramos.append((r, a)) }
        let fm = NSFontManager.shared
        for (r, a) in tramos {
            var fuente = estilo.fuente
            if a[.tfBold] != nil { fuente = fm.convert(fuente, toHaveTrait: .boldFontMask) }
            if a[.tfItalic] != nil { fuente = fm.convert(fuente, toHaveTrait: .italicFontMask) }
            let esCodigo = a[.tfCode] != nil
            if esCodigo {
                fuente = NSFont.monospacedSystemFont(ofSize: estilo.fuente.pointSize * 0.88, weight: a[.tfBold] != nil ? .bold : .regular)
            }
            s.addAttribute(.font, value: fuente, range: r)

            let color = (a[.tfColor] as? String).flatMap(NotaColor.init(rawValue:))?.texto
                ?? (esCodigo ? NotaColor.rojo.texto : estilo.color)
            s.addAttribute(.foregroundColor, value: color, range: r)

            if let fondo = (a[.tfBg] as? String).flatMap(NotaColor.init(rawValue:))?.fondo {
                s.addAttribute(.backgroundColor, value: fondo, range: r)
            } else if esCodigo {
                s.addAttribute(.backgroundColor, value: NSColor.labelColor.withAlphaComponent(0.08), range: r)
            } else {
                s.removeAttribute(.backgroundColor, range: r)
            }

            if a[.tfStrike] != nil || estilo.tachado {
                s.addAttribute(.strikethroughStyle, value: NSUnderlineStyle.single.rawValue, range: r)
            } else {
                s.removeAttribute(.strikethroughStyle, range: r)
            }
            if a[.tfUnderline] != nil {
                s.addAttribute(.underlineStyle, value: NSUnderlineStyle.single.rawValue, range: r)
            } else {
                s.removeAttribute(.underlineStyle, range: r)
            }
        }
    }
}

// MARK: - Enfoque entre bloques

final class EnfoqueCtl: ObservableObject {
    struct Solicitud: Equatable {
        let id: UUID
        let alFinal: Bool
    }

    @Published var solicitud: Solicitud?

    func pedir(_ id: UUID, alFinal: Bool = true) { solicitud = Solicitud(id: id, alFinal: alFinal) }
}

// MARK: - Menú contextual del bloque

struct ItemMenuBloque {
    var titulo: String
    var icono: String? = nil
    var sub: [ItemMenuBloque]? = nil
    var destructivo = false
    var accion: () -> Void = {}
}

private final class ItemConAccion: NSMenuItem {
    private var cierre: (() -> Void)?

    convenience init(_ item: ItemMenuBloque) {
        self.init(title: item.titulo, action: #selector(ejecutar), keyEquivalent: "")
        target = self
        cierre = item.accion
        if let icono = item.icono { image = NSImage(systemSymbolName: icono, accessibilityDescription: nil) }
        if let sub = item.sub {
            let menu = NSMenu()
            sub.forEach { menu.addItem(ItemConAccion($0)) }
            submenu = menu
            action = nil
        }
    }

    @objc private func ejecutar() { cierre?() }
}

// MARK: - Campo de texto (NSTextView)

final class CampoTexto: NSTextView {
    var estilo = EstiloBloque(fuente: .systemFont(ofSize: 14), color: .labelColor)
    var placeholder = ""
    var esCodigo = false
    var alEnfocar: (() -> Void)?
    var bloqueID: UUID?
    var alPedirEnlace: (() -> Void)?
    var alPegarEnlaceSolo: ((String) -> Void)?
    var itemsMenu: (() -> [ItemMenuBloque])?

    override func setFrameSize(_ newSize: NSSize) {
        super.setFrameSize(newSize)
        if let tc = textContainer, abs(tc.containerSize.width - newSize.width) > 0.5 {
            tc.containerSize = NSSize(width: newSize.width, height: .greatestFiniteMagnitude)
        }
    }

    override func becomeFirstResponder() -> Bool {
        let ok = super.becomeFirstResponder()
        if ok {
            ControladorFormato.shared.activo = self
            alEnfocar?()
            NotificationCenter.default.post(name: .campoActivo, object: self)
        }
        return ok
    }

    override func draw(_ dirtyRect: NSRect) {
        super.draw(dirtyRect)
        if string.isEmpty, !placeholder.isEmpty {
            NSAttributedString(string: placeholder, attributes: [.font: estilo.fuente, .foregroundColor: NSColor.placeholderTextColor])
                .draw(at: textContainerOrigin)
        }
    }

    // MARK: Atajos de formato

    override func performKeyEquivalent(with event: NSEvent) -> Bool {
        guard !esCodigo, window?.firstResponder === self else { return super.performKeyEquivalent(with: event) }
        let f = event.modifierFlags.intersection(.deviceIndependentFlagsMask)
        let tecla = event.charactersIgnoringModifiers?.lowercased() ?? ""
        if f == .command {
            switch tecla {
            case "b": alternar(.tfBold); return true
            case "i": alternar(.tfItalic); return true
            case "u": alternar(.tfUnderline); return true
            case "e": alternar(.tfCode); return true
            case "k": alPedirEnlace?(); return true
            default: break
            }
        }
        if f == [.command, .shift], tecla == "x" { alternar(.tfStrike); return true }
        return super.performKeyEquivalent(with: event)
    }

    override func paste(_ sender: Any?) {
        guard !esCodigo else { pasteAsPlainText(sender); return }
        let copiado = NSPasteboard.general.string(forType: .string)?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        if let url = URL(string: copiado), ["http", "https"].contains(url.scheme ?? ""), url.host != nil, !copiado.contains(where: \.isWhitespace) {
            if selectedRange().length > 0 { aplicarEnlace(copiado); return }
            if string.isEmpty { alPegarEnlaceSolo?(copiado); return }
        }
        pasteAsPlainText(sender)
    }

    override func menu(for event: NSEvent) -> NSMenu? {
        let menu = super.menu(for: event) ?? NSMenu()
        let extras = itemsMenu?() ?? []
        if !extras.isEmpty {
            menu.addItem(.separator())
            extras.forEach { menu.addItem(ItemConAccion($0)) }
        }
        return menu
    }

    // MARK: Líneas (para moverse entre bloques con las flechas)

    var enPrimeraLinea: Bool {
        guard let lm = layoutManager, !string.isEmpty else { return true }
        let n = string.utf16.count
        let pos = selectedRange().location
        if pos >= n { return false == string.hasSuffix("\n") && lineaDeGlifo(lm, n - 1).location == 0 }
        return lineaDeGlifo(lm, pos).location == 0
    }

    var enUltimaLinea: Bool {
        guard let lm = layoutManager, !string.isEmpty else { return true }
        let n = string.utf16.count
        let pos = selectedRange().location
        if pos >= n { return true }
        return NSMaxRange(lineaDeGlifo(lm, pos)) >= lm.numberOfGlyphs
    }

    private func lineaDeGlifo(_ lm: NSLayoutManager, _ caracter: Int) -> NSRange {
        let g = lm.glyphIndexForCharacter(at: max(0, min(caracter, string.utf16.count - 1)))
        var rango = NSRange()
        lm.lineFragmentRect(forGlyphAt: g, effectiveRange: &rango)
        return rango
    }

    // MARK: Formato

    func alternar(_ clave: NSAttributedString.Key) {
        let r = selectedRange()
        if r.length == 0 {
            var t = typingAttributes
            t[clave] = t[clave] == nil ? true : nil
            typingAttributes = tipeo(t)
            return
        }
        var todo = true
        textStorage?.enumerateAttribute(clave, in: r) { v, _, parar in
            if v == nil { todo = false; parar.pointee = true }
        }
        cambiar(r) { ts in
            if todo { ts.removeAttribute(clave, range: r) } else { ts.addAttribute(clave, value: true, range: r) }
        }
    }

    func colorear(_ token: String?, fondo: Bool) {
        let clave: NSAttributedString.Key = fondo ? .tfBg : .tfColor
        let r = selectedRange()
        if r.length == 0 {
            var t = typingAttributes
            t[clave] = token
            typingAttributes = tipeo(t)
            return
        }
        cambiar(r) { ts in
            if let token { ts.addAttribute(clave, value: token, range: r) } else { ts.removeAttribute(clave, range: r) }
        }
    }

    func aplicarEnlace(_ url: String?) {
        let r = selectedRange()
        guard r.length > 0 else { return }
        cambiar(r) { ts in
            if let url, !url.isEmpty {
                ts.addAttribute(.link, value: URL(string: url) ?? url, range: r)
            } else {
                ts.removeAttribute(.link, range: r)
            }
        }
    }

    func limpiarFormato() {
        let r = selectedRange()
        guard r.length > 0 else { return }
        cambiar(r) { ts in
            for clave in [NSAttributedString.Key.tfBold, .tfItalic, .tfUnderline, .tfStrike, .tfCode, .tfColor, .tfBg, .link] {
                ts.removeAttribute(clave, range: r)
            }
        }
    }

    /// Enlace que hay bajo la selección (para rellenar el cuadro de ⌘K).
    var enlaceActual: String? {
        let r = selectedRange()
        guard r.length > 0, r.location < string.utf16.count else { return nil }
        let v = textStorage?.attribute(.link, at: r.location, effectiveRange: nil)
        return (v as? URL)?.absoluteString ?? v as? String
    }

    private func cambiar(_ r: NSRange, _ cuerpo: (NSTextStorage) -> Void) {
        guard let ts = textStorage, shouldChangeText(in: r, replacementString: nil) else { return }
        ts.beginEditing()
        cuerpo(ts)
        CodecTexto.visual(ts, r, estilo)
        ts.endEditing()
        didChangeText()
    }

    /// Atributos de escritura con la parte visual recalculada.
    private func tipeo(_ t: [NSAttributedString.Key: Any]) -> [NSAttributedString.Key: Any] {
        let temporal = NSMutableAttributedString(string: " ", attributes: t)
        CodecTexto.visual(temporal, NSRange(location: 0, length: 1), estilo)
        return temporal.attributes(at: 0, effectiveRange: nil)
    }
}

/// Punto de acceso de la barra de formato al campo que está escribiendo el usuario.
final class ControladorFormato {
    static let shared = ControladorFormato()
    weak var activo: CampoTexto?

    func alternar(_ clave: NSAttributedString.Key) { usar { $0.alternar(clave) } }
    func colorear(_ token: String?, fondo: Bool) { usar { $0.colorear(token, fondo: fondo) } }
    func enlazar(_ url: String?) { usar { $0.aplicarEnlace(url) } }
    func limpiar() { usar { $0.limpiarFormato() } }
    var enlaceActual: String? { activo?.enlaceActual }

    private func usar(_ accion: (CampoTexto) -> Void) {
        guard let campo = activo else { return }
        accion(campo)
        campo.window?.makeFirstResponder(campo)
    }
}

// MARK: - Puente con SwiftUI

struct AccionesCampo {
    var enter: (String, [TextRun]?) -> Void = { _, _ in }
    var borrarVacio: () -> Void = {}
    var retrocederInicio: () -> Bool = { false }
    var navegar: (Int) -> Bool = { _ in false }
    var slashMover: (Int) -> Void = { _ in }
    var tab: (Int) -> Void = { _ in }
    var escape: () -> Void = {}
    var seleccion: (Bool) -> Void = { _ in }
    var enlaceSolo: (String) -> Void = { _ in }
    var pedirEnlace: () -> Void = {}
    var menu: () -> [ItemMenuBloque] = { [] }
}

struct CampoRico: NSViewRepresentable {
    @Binding var texto: String
    @Binding var runs: [TextRun]?
    let id: UUID
    let estilo: EstiloBloque
    let placeholder: String
    /// nil = texto con formato; un valor = bloque de código con ese lenguaje (resuelto).
    var codigo: CodigoConfig? = nil
    @ObservedObject var enfoque: EnfoqueCtl
    var slashActivo = false
    var acciones: AccionesCampo

    struct CodigoConfig: Equatable {
        var lenguaje: String
        var oscuro: Bool
    }

    func makeNSView(context: Context) -> CampoTexto {
        let campo = CampoTexto(usingTextLayoutManager: false)
        campo.delegate = context.coordinator
        campo.isRichText = true
        campo.allowsUndo = true
        campo.drawsBackground = false
        campo.isEditable = true
        campo.isSelectable = true
        campo.importsGraphics = false
        campo.textContainerInset = .zero
        campo.textContainer?.lineFragmentPadding = 0
        campo.isHorizontallyResizable = false
        campo.isVerticallyResizable = false
        campo.textContainer?.widthTracksTextView = true
        campo.isAutomaticQuoteSubstitutionEnabled = false
        campo.isAutomaticDashSubstitutionEnabled = false
        campo.isAutomaticSpellingCorrectionEnabled = false
        campo.isAutomaticTextReplacementEnabled = false
        campo.isAutomaticLinkDetectionEnabled = false
        campo.linkTextAttributes = [
            .foregroundColor: NSColor.linkColor,
            .underlineStyle: NSUnderlineStyle.single.rawValue,
            .cursor: NSCursor.pointingHand
        ]
        return campo
    }

    func updateNSView(_ campo: CampoTexto, context: Context) {
        let c = context.coordinator
        c.parent = self

        let estiloCambio = campo.estilo != estilo || campo.esCodigo != (codigo != nil)
        campo.estilo = estilo
        campo.placeholder = placeholder
        campo.esCodigo = codigo != nil
        campo.bloqueID = id
        campo.alPedirEnlace = acciones.pedirEnlace
        campo.alPegarEnlaceSolo = acciones.enlaceSolo
        campo.itemsMenu = acciones.menu

        if !campo.hasMarkedText() {
            let runsActuales = codigo == nil ? CodecTexto.runs(de: campo.attributedString()) : nil
            if campo.string != texto || estiloCambio || (codigo == nil && runsActuales != runs) {
                c.aplicando = true
                let seleccion = campo.selectedRange()
                if codigo == nil {
                    campo.textStorage?.setAttributedString(CodecTexto.atributado(texto: texto, runs: runs, estilo: estilo))
                } else {
                    campo.textStorage?.setAttributedString(NSAttributedString(string: texto, attributes: estilo.base))
                }
                let n = campo.string.utf16.count
                campo.setSelectedRange(NSRange(location: min(seleccion.location, n), length: 0))
                campo.typingAttributes = estilo.base
                c.aplicando = false
                c.ultimoCodigo = nil
            }
        }

        if let codigo, c.ultimoCodigo != codigo || c.ultimoTexto != texto {
            c.ultimoCodigo = codigo
            c.ultimoTexto = texto
            c.aplicando = true
            if let ts = campo.textStorage {
                Resaltador.aplicar(ts, lenguaje: codigo.lenguaje, oscuro: codigo.oscuro, fuente: estilo.fuente)
            }
            campo.typingAttributes = estilo.base
            c.aplicando = false
        }

        if let s = enfoque.solicitud, s.id == id {
            c.enfocar(campo, solicitud: s, intentos: 6)
        }
    }

    func sizeThatFits(_ proposal: ProposedViewSize, nsView campo: CampoTexto, context: Context) -> CGSize? {
        guard let ancho = proposal.width, ancho.isFinite, ancho > 1 else { return nil }
        // Se mide en una pila de texto aparte: SwiftUI propone anchos provisionales y no deben quedarse
        // en el contenedor real (así el texto siempre usa todo el ancho y la altura coincide).
        let medidor = context.coordinator.medidor
        medidor.storage.setAttributedString(campo.attributedString())
        medidor.contenedor.containerSize = NSSize(width: ancho, height: .greatestFiniteMagnitude)
        medidor.layout.ensureLayout(for: medidor.contenedor)
        let minimo = medidor.layout.defaultLineHeight(for: campo.estilo.fuente)
        return CGSize(width: ancho, height: ceil(max(medidor.layout.usedRect(for: medidor.contenedor).height, minimo)))
    }

    func makeCoordinator() -> Coordinador { Coordinador(self) }

    final class Coordinador: NSObject, NSTextViewDelegate {
        var parent: CampoRico
        var aplicando = false
        var ultimoCodigo: CodigoConfig?
        var ultimoTexto: String?

        /// Pila de TextKit usada solo para calcular la altura.
        let medidor: (storage: NSTextStorage, layout: NSLayoutManager, contenedor: NSTextContainer) = {
            let storage = NSTextStorage()
            let layout = NSLayoutManager()
            let contenedor = NSTextContainer(size: NSSize(width: 400, height: CGFloat.greatestFiniteMagnitude))
            contenedor.lineFragmentPadding = 0
            layout.addTextContainer(contenedor)
            storage.addLayoutManager(layout)
            return (storage, layout, contenedor)
        }()

        init(_ parent: CampoRico) { self.parent = parent }

        func enfocar(_ campo: CampoTexto, solicitud: EnfoqueCtl.Solicitud, intentos: Int) {
            DispatchQueue.main.async { [weak self, weak campo] in
                guard let self, let campo, self.parent.enfoque.solicitud == solicitud else { return }
                guard let ventana = campo.window else {
                    if intentos > 0 { self.enfocar(campo, solicitud: solicitud, intentos: intentos - 1) }
                    return
                }
                self.parent.enfoque.solicitud = nil
                ventana.makeFirstResponder(campo)
                campo.setSelectedRange(NSRange(location: solicitud.alFinal ? campo.string.utf16.count : 0, length: 0))
            }
        }

        func textDidChange(_ notification: Notification) {
            guard !aplicando, let campo = notification.object as? CampoTexto else { return }
            if !campo.esCodigo { autoenlazar(campo) }
            parent.texto = campo.string
            if campo.esCodigo {
                parent.runs = nil
            } else {
                parent.runs = CodecTexto.runs(de: campo.attributedString())
            }
        }

        func textViewDidChangeSelection(_ notification: Notification) {
            guard let campo = notification.object as? CampoTexto else { return }
            ControladorFormato.shared.activo = campo
            let hay = campo.selectedRange().length > 0 && !campo.esCodigo
            DispatchQueue.main.async { self.parent.acciones.seleccion(hay) }
        }

        func textView(_ textView: NSTextView, shouldChangeTextIn range: NSRange, replacementString: String?) -> Bool {
            // Al escribir justo detrás de un enlace, el texto nuevo no debe seguir siendo enlace.
            if let texto = replacementString, !texto.isEmpty, range.length == 0, range.location > 0,
               let ts = textView.textStorage, range.location <= ts.length,
               ts.attribute(.link, at: range.location - 1, effectiveRange: nil) != nil,
               range.location == ts.length || ts.attribute(.link, at: range.location, effectiveRange: nil) == nil {
                textView.typingAttributes.removeValue(forKey: .link)
            }
            return true
        }

        func textView(_ textView: NSTextView, clickedOnLink link: Any, at charIndex: Int) -> Bool {
            let url = (link as? URL) ?? (link as? String).flatMap(URL.init(string:))
            if let url { NSWorkspace.shared.open(url) }
            return true
        }

        func textView(_ textView: NSTextView, doCommandBy selector: Selector) -> Bool {
            guard let campo = textView as? CampoTexto else { return false }
            let a = parent.acciones
            switch selector {
            case #selector(NSResponder.insertNewline(_:)):
                if campo.esCodigo { return false }
                if parent.slashActivo { a.enter("", nil); return true }
                // Enter en mitad del texto parte el bloque: lo que queda tras el cursor pasa al bloque nuevo.
                var seleccion = campo.selectedRange()
                if seleccion.length > 0 {
                    campo.insertText("", replacementRange: seleccion)
                    seleccion = campo.selectedRange()
                }
                let total = campo.string.utf16.count
                var resto = ""
                var runsResto: [TextRun]?
                if seleccion.location < total {
                    let cola = NSRange(location: seleccion.location, length: total - seleccion.location)
                    let atributada = campo.attributedString().attributedSubstring(from: cola)
                    resto = atributada.string
                    runsResto = CodecTexto.runs(de: atributada)
                    aplicando = true
                    campo.textStorage?.deleteCharacters(in: cola)
                    aplicando = false
                }
                parent.texto = campo.string
                parent.runs = CodecTexto.runs(de: campo.attributedString())
                a.enter(resto, runsResto)
                return true
            case #selector(NSResponder.deleteBackward(_:)):
                if campo.string.isEmpty { a.borrarVacio(); return true }
                if campo.selectedRange() == NSRange(location: 0, length: 0) { return a.retrocederInicio() }
                return false
            case #selector(NSResponder.moveUp(_:)):
                if parent.slashActivo { a.slashMover(-1); return true }
                return campo.enPrimeraLinea ? a.navegar(-1) : false
            case #selector(NSResponder.moveDown(_:)):
                if parent.slashActivo { a.slashMover(1); return true }
                return campo.enUltimaLinea ? a.navegar(1) : false
            case #selector(NSResponder.insertTab(_:)):
                if campo.esCodigo {
                    campo.insertText("    ", replacementRange: campo.selectedRange())
                } else {
                    a.tab(1)
                }
                return true
            case #selector(NSResponder.insertBacktab(_:)):
                if !campo.esCodigo { a.tab(-1) }
                return true
            case #selector(NSResponder.cancelOperation(_:)):
                a.escape()
                return true
            default:
                return false
            }
        }

        /// Convierte en enlace las direcciones web escritas, al pulsar espacio o salto de línea.
        private func autoenlazar(_ campo: CampoTexto) {
            guard let ts = campo.textStorage, ts.length > 0 else { return }
            let ultimo = (ts.string as NSString).substring(from: max(0, campo.selectedRange().location - 1)).first
            guard let ultimo, ultimo == " " || ultimo == "\n" else { return }
            guard let detector = try? NSDataDetector(types: NSTextCheckingResult.CheckingType.link.rawValue) else { return }
            let rango = NSRange(location: 0, length: ts.length)
            var cambios: [(NSRange, URL)] = []
            detector.enumerateMatches(in: ts.string, range: rango) { m, _, _ in
                guard let m, let url = m.url, ["http", "https"].contains(url.scheme ?? ""),
                      ts.attribute(.link, at: m.range.location, effectiveRange: nil) == nil else { return }
                cambios.append((m.range, url))
            }
            guard !cambios.isEmpty else { return }
            ts.beginEditing()
            for (r, url) in cambios { ts.addAttribute(.link, value: url, range: r) }
            ts.endEditing()
        }
    }
}

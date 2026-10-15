import SwiftUI
import AppKit

/// Color de la app (preferencia local: no viaja en la copia de seguridad ni se sincroniza).
/// Valores guardados: un nombre predefinido, `"sistema"` o `"#RRGGBB"`.
enum ColorAcento {
    static let predeterminado = "coral"

    static let predefinidos: [(nombre: String, titulo: String, hex: String)] = [
        ("coral", "Coral", "#E8705A"), ("rojo", "Rojo", "#D0382E"), ("naranja", "Naranja", "#E07B10"),
        ("verde", "Verde", "#2E8B57"), ("azul", "Azul", "#2F6FD0"), ("morado", "Morado", "#8A55C2"),
        ("rosa", "Rosa", "#D14A8B"), ("grafito", "Grafito", "#5F6B73")
    ]

    static let sistema = "sistema"

    /// El color en hexadecimal que corresponde al valor guardado (nil para «sistema» o un valor no válido).
    static func hex(de valor: String) -> String? {
        if let p = predefinidos.first(where: { $0.nombre == valor }) { return p.hex }
        return rgb(hex: valor) != nil ? valor.uppercased() : nil
    }

    static func rgb(hex: String) -> (r: Double, g: Double, b: Double)? {
        guard hex.hasPrefix("#"), hex.count == 7, let n = UInt32(hex.dropFirst(), radix: 16) else { return nil }
        return (Double((n >> 16) & 0xFF) / 255, Double((n >> 8) & 0xFF) / 255, Double(n & 0xFF) / 255)
    }

    static func hex(r: Double, g: Double, b: Double) -> String {
        func canal(_ v: Double) -> Int { Int((min(max(v, 0), 1) * 255).rounded()) }
        return String(format: "#%02X%02X%02X", canal(r), canal(g), canal(b))
    }

    /// Luminancia relativa (WCAG).
    static func luminancia(r: Double, g: Double, b: Double) -> Double {
        func lineal(_ c: Double) -> Double { c <= 0.03928 ? c / 12.92 : pow((c + 0.055) / 1.055, 2.4) }
        return 0.2126 * lineal(r) + 0.7152 * lineal(g) + 0.0722 * lineal(b)
    }

    /// Razón de contraste (1...21) entre dos luminancias.
    static func contraste(_ a: Double, _ b: Double) -> Double {
        (max(a, b) + 0.05) / (min(a, b) + 0.05)
    }

    /// Fondo de referencia contra el que debe leerse el color: blanco en claro y casi negro en oscuro.
    private static func luminanciaFondo(oscuro: Bool) -> Double { oscuro ? luminancia(r: 0.12, g: 0.12, b: 0.12) : 1 }

    /// Si el color no llega a contraste 4,5:1 con el fondo (con un pequeño margen por el redondeo a 8 bits), lo oscurece (modo claro) o aclara (modo oscuro) hasta lograrlo.
    static func ajustado(hex: String, oscuro: Bool) -> String {
        guard var c = rgb(hex: hex) else { return hex }
        let fondo = luminanciaFondo(oscuro: oscuro)
        for _ in 0..<40 where contraste(luminancia(r: c.r, g: c.g, b: c.b), fondo) < 4.6 {
            if oscuro { c = (c.r + (1 - c.r) * 0.05, c.g + (1 - c.g) * 0.05, c.b + (1 - c.b) * 0.05) }
            else { c = (c.r * 0.97, c.g * 0.97, c.b * 0.97) }
        }
        return Self.hex(r: c.r, g: c.g, b: c.b)
    }

    /// El color con el que se tiñe la app, o el del sistema.
    static func color(para valor: String, oscuro: Bool) -> Color {
        guard let h = hex(de: valor), let c = rgb(hex: ajustado(hex: h, oscuro: oscuro)) else {
            return Color(nsColor: .controlAccentColor)
        }
        return Color(red: c.r, green: c.g, blue: c.b)
    }

    static func color(hex: String) -> Color {
        guard let c = rgb(hex: hex) else { return .accentColor }
        return Color(red: c.r, green: c.g, blue: c.b)
    }
}

import SwiftUI

/// Colores cálidos pastel para distinguir tareas. Compartido entre la app y los widgets.
struct ColorPastel {
    let fondo: Color
    let texto: Color

    init(_ r: Double, _ g: Double, _ b: Double, texto t: (Double, Double, Double)) {
        fondo = Color(red: r, green: g, blue: b)
        texto = Color(red: t.0, green: t.1, blue: t.2)
    }
}

enum PaletaPastel {
    static let colores: [ColorPastel] = [
        ColorPastel(1.00, 0.85, 0.75, texto: (0.55, 0.28, 0.12)),  // melocotón
        ColorPastel(1.00, 0.90, 0.72, texto: (0.55, 0.35, 0.05)),  // albaricoque
        ColorPastel(1.00, 0.96, 0.72, texto: (0.50, 0.42, 0.05)),  // mantequilla
        ColorPastel(1.00, 0.80, 0.78, texto: (0.60, 0.22, 0.20)),  // coral
        ColorPastel(0.99, 0.83, 0.87, texto: (0.58, 0.22, 0.35)),  // rosa
        ColorPastel(0.95, 0.87, 0.78, texto: (0.45, 0.32, 0.20)),  // arena
        ColorPastel(0.96, 0.78, 0.68, texto: (0.50, 0.25, 0.15)),  // terracota claro
        ColorPastel(0.98, 0.88, 0.62, texto: (0.50, 0.36, 0.00))   // miel
    ]

    /// Cada tarea recibe un color estable (según su id); si dos tareas del mismo día
    /// coinciden, la segunda pasa al siguiente color libre.
    static func asignar(ids: [UUID]) -> [UUID: ColorPastel] {
        var usados = Set<Int>()
        var resultado: [UUID: ColorPastel] = [:]
        for id in ids {
            var indice = preferido(id)
            if usados.count < colores.count {
                while usados.contains(indice) { indice = (indice + 1) % colores.count }
            }
            usados.insert(indice)
            resultado[id] = colores[indice]
        }
        return resultado
    }

    private static func preferido(_ id: UUID) -> Int {
        let suma = withUnsafeBytes(of: id.uuid) { $0.reduce(0) { ($0 &* 31) &+ Int($1) } }
        return abs(suma) % colores.count
    }
}

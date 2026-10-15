import Foundation

/// Comparación de texto sin distinguir mayúsculas ni tildes: «cod» encuentra «Código» (igual que en Android).
enum BusquedaTexto {
    static func plegar(_ texto: String) -> String {
        texto.folding(options: [.diacriticInsensitive, .caseInsensitive], locale: .current)
    }

    /// `true` si `texto` contiene `buscado`; un `buscado` vacío coincide con todo.
    static func contiene(_ texto: String, _ buscado: String) -> Bool {
        let b = plegar(buscado)
        return b.isEmpty || plegar(texto).contains(b)
    }
}

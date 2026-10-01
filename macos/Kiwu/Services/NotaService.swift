import AppKit

enum NotaService {
    static func leer(_ data: Data) -> NoteDocument {
        var doc = (try? JSONDecoder().decode(NoteDocument.self, from: data)) ?? .empty
        if doc.blocks.isEmpty { doc.blocks = [NoteBlock()] }
        return doc
    }

    /// Reduce la imagen a un máximo de `maxLado` px y la guarda como JPEG.
    static func imagenOptimizada(from url: URL, maxLado: CGFloat = 1600) -> Data? {
        guard let original = NSImage(contentsOf: url),
              let rep = original.representations.first else { return nil }
        let ancho = CGFloat(rep.pixelsWide), alto = CGFloat(rep.pixelsHigh)
        guard ancho > 0, alto > 0 else { return nil }
        let escala = min(1, maxLado / max(ancho, alto))
        let tamano = NSSize(width: ancho * escala, height: alto * escala)

        let destino = NSImage(size: tamano)
        destino.lockFocus()
        original.draw(in: NSRect(origin: .zero, size: tamano))
        destino.unlockFocus()

        guard let tiff = destino.tiffRepresentation,
              let bitmap = NSBitmapImageRep(data: tiff) else { return nil }
        return bitmap.representation(using: .jpeg, properties: [.compressionFactor: 0.8])
    }
}

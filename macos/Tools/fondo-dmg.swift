import AppKit

// Dibuja el fondo de la ventana del instalador (.dmg): 660 × 400 puntos, a doble resolución.
// Uso: fondo-dmg <salida.png> <versión>
let argumentos = CommandLine.arguments
guard argumentos.count >= 3 else {
    FileHandle.standardError.write(Data("Uso: fondo-dmg <salida.png> <versión>\n".utf8))
    exit(1)
}
let ancho = 660.0, alto = 400.0, escala = 2.0

func color(_ hex: UInt32, _ alfa: CGFloat = 1) -> NSColor {
    NSColor(srgbRed: CGFloat((hex >> 16) & 0xFF) / 255, green: CGFloat((hex >> 8) & 0xFF) / 255,
            blue: CGFloat(hex & 0xFF) / 255, alpha: alfa)
}

guard let rep = NSBitmapImageRep(
    bitmapDataPlanes: nil, pixelsWide: Int(ancho * escala), pixelsHigh: Int(alto * escala), bitsPerSample: 8,
    samplesPerPixel: 4, hasAlpha: true, isPlanar: false, colorSpaceName: .deviceRGB, bytesPerRow: 0, bitsPerPixel: 0
) else { exit(1) }
rep.size = NSSize(width: ancho, height: alto)   // 144 ppp: Finder lo muestra a 660 × 400

NSGraphicsContext.saveGraphicsState()
NSGraphicsContext.current = NSGraphicsContext(bitmapImageRep: rep)

// Fondo cálido, a juego con el icono.
NSGradient(colors: [color(0xFFF6EF), color(0xFCE0D2)])!.draw(in: NSRect(x: 0, y: 0, width: ancho, height: alto), angle: -90)

// Círculos suaves de adorno.
color(0xF4A38C, 0.16).setFill()
NSBezierPath(ovalIn: NSRect(x: -90, y: -120, width: 300, height: 300)).fill()
color(0xF7C6A8, 0.22).setFill()
NSBezierPath(ovalIn: NSRect(x: 470, y: 230, width: 280, height: 280)).fill()

func texto(_ cadena: String, tamano: CGFloat, peso: NSFont.Weight, color c: NSColor, centroY: CGFloat) {
    let atributos: [NSAttributedString.Key: Any] = [.font: NSFont.systemFont(ofSize: tamano, weight: peso), .foregroundColor: c]
    let t = NSAttributedString(string: cadena, attributes: atributos)
    let medida = t.size()
    t.draw(at: NSPoint(x: (ancho - medida.width) / 2, y: centroY - medida.height / 2))
}

texto("Arrastra Kiwu a Aplicaciones", tamano: 24, peso: .semibold, color: color(0x5A2E22), centroY: alto - 62)
texto("Tus tareas y notas, sin cuentas ni nube", tamano: 13, peso: .regular, color: color(0x8A6A5E), centroY: alto - 92)
texto("Versión \(argumentos[2])", tamano: 11, peso: .regular, color: color(0x9C7F73), centroY: 24)

// Flecha entre los dos iconos (el centro de los iconos está a 190 pt del borde superior).
let yFlecha = alto - 190
let flecha = NSBezierPath()
flecha.lineWidth = 5
flecha.lineCapStyle = .round
flecha.lineJoinStyle = .round
flecha.move(to: NSPoint(x: 262, y: yFlecha))
flecha.curve(to: NSPoint(x: 392, y: yFlecha), controlPoint1: NSPoint(x: 305, y: yFlecha + 26), controlPoint2: NSPoint(x: 350, y: yFlecha + 26))
color(0xE8705A).setStroke()
flecha.stroke()
let punta = NSBezierPath()
punta.lineWidth = 5
punta.lineCapStyle = .round
punta.lineJoinStyle = .round
punta.move(to: NSPoint(x: 378, y: yFlecha + 14))
punta.line(to: NSPoint(x: 394, y: yFlecha + 1))
punta.line(to: NSPoint(x: 376, y: yFlecha - 10))
punta.stroke()

NSGraphicsContext.restoreGraphicsState()

guard let datos = rep.representation(using: .png, properties: [:]) else { exit(1) }
try datos.write(to: URL(fileURLWithPath: argumentos[1]))

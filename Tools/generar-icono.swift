import AppKit
import CoreGraphics

// Dibuja el icono de TaskFlow a un tamaño dado (lienzo lógico de 1024 px).
func dibujar(lado: Int) -> Data {
    let escala = CGFloat(lado) / 1024
    let espacio = CGColorSpace(name: CGColorSpace.sRGB)!
    let ctx = CGContext(data: nil, width: lado, height: lado, bitsPerComponent: 8, bytesPerRow: 0, space: espacio,
                        bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue)!
    ctx.scaleBy(x: escala, y: escala)
    ctx.setAllowsAntialiasing(true)
    ctx.interpolationQuality = .high

    func color(_ r: CGFloat, _ g: CGFloat, _ b: CGFloat, _ a: CGFloat = 1) -> CGColor {
        CGColor(colorSpace: espacio, components: [r / 255, g / 255, b / 255, a])!
    }

    // 1. Cuerpo del icono (cuadrado redondeado con margen, como la plantilla de macOS) y sombra
    let cuerpo = CGRect(x: 100, y: 100, width: 824, height: 824)
    let rutaCuerpo = CGPath(roundedRect: cuerpo, cornerWidth: 186, cornerHeight: 186, transform: nil)

    ctx.saveGState()
    ctx.setShadow(offset: CGSize(width: 0, height: -14), blur: 30, color: color(60, 20, 10, 0.32))
    ctx.addPath(rutaCuerpo)
    ctx.setFillColor(color(255, 140, 110))
    ctx.fillPath()
    ctx.restoreGState()

    // 2. Degradado cálido dentro del cuerpo
    ctx.saveGState()
    ctx.addPath(rutaCuerpo)
    ctx.clip()
    let degradado = CGGradient(colorsSpace: espacio, colors: [color(255, 208, 160), color(255, 122, 104)] as CFArray, locations: [0, 1])!
    ctx.drawLinearGradient(degradado, start: CGPoint(x: 300, y: 924), end: CGPoint(x: 724, y: 100), options: [.drawsBeforeStartLocation, .drawsAfterEndLocation])
    // brillo suave arriba
    let brillo = CGGradient(colorsSpace: espacio, colors: [color(255, 255, 255, 0.28), color(255, 255, 255, 0)] as CFArray, locations: [0, 1])!
    ctx.drawLinearGradient(brillo, start: CGPoint(x: 512, y: 924), end: CGPoint(x: 512, y: 560), options: [.drawsBeforeStartLocation])
    ctx.restoreGState()

    // 3. Hoja de calendario (blanca) con sombra
    let hoja = CGRect(x: 246, y: 236, width: 532, height: 540)
    let rutaHoja = CGPath(roundedRect: hoja, cornerWidth: 78, cornerHeight: 78, transform: nil)
    ctx.saveGState()
    ctx.setShadow(offset: CGSize(width: 0, height: -12), blur: 24, color: color(120, 40, 20, 0.30))
    ctx.addPath(rutaHoja)
    ctx.setFillColor(color(255, 253, 250))
    ctx.fillPath()
    ctx.restoreGState()

    // 4. Banda superior de la hoja
    ctx.saveGState()
    ctx.addPath(rutaHoja)
    ctx.clip()
    let banda = CGRect(x: hoja.minX, y: hoja.maxY - 132, width: hoja.width, height: 132)
    let bandaGrad = CGGradient(colorsSpace: espacio, colors: [color(255, 138, 110), color(244, 98, 88)] as CFArray, locations: [0, 1])!
    ctx.saveGState()
    ctx.clip(to: banda)
    ctx.drawLinearGradient(bandaGrad, start: CGPoint(x: 0, y: banda.maxY), end: CGPoint(x: 0, y: banda.minY), options: [])
    ctx.restoreGState()
    ctx.restoreGState()

    // 5. Anillas de la hoja
    for x in [hoja.minX + 132, hoja.maxX - 132 - 34] {
        let anilla = CGRect(x: x, y: hoja.maxY - 62, width: 34, height: 96)
        let rutaAnilla = CGPath(roundedRect: anilla, cornerWidth: 17, cornerHeight: 17, transform: nil)
        ctx.saveGState()
        ctx.setShadow(offset: CGSize(width: 0, height: -4), blur: 8, color: color(90, 30, 20, 0.35))
        ctx.addPath(rutaAnilla)
        ctx.setFillColor(color(255, 246, 236))
        ctx.fillPath()
        ctx.restoreGState()
    }

    // 6. Marca de verificación
    let marca = CGMutablePath()
    marca.move(to: CGPoint(x: 366, y: 470))
    marca.addLine(to: CGPoint(x: 466, y: 372))
    marca.addLine(to: CGPoint(x: 662, y: 566))
    ctx.saveGState()
    ctx.setShadow(offset: CGSize(width: 0, height: -6), blur: 10, color: color(200, 70, 50, 0.25))
    ctx.addPath(marca)
    ctx.setLineWidth(70)
    ctx.setLineCap(.round)
    ctx.setLineJoin(.round)
    ctx.setStrokeColor(color(244, 98, 88))
    ctx.strokePath()
    ctx.restoreGState()

    let imagen = ctx.makeImage()!
    let rep = NSBitmapImageRep(cgImage: imagen)
    return rep.representation(using: .png, properties: [:])!
}

let salida = CommandLine.arguments[1]
try FileManager.default.createDirectory(atPath: salida, withIntermediateDirectories: true)
let tamanos: [(nombre: String, lado: Int)] = [
    ("icon_16x16", 16), ("icon_16x16@2x", 32), ("icon_32x32", 32), ("icon_32x32@2x", 64),
    ("icon_128x128", 128), ("icon_128x128@2x", 256), ("icon_256x256", 256), ("icon_256x256@2x", 512),
    ("icon_512x512", 512), ("icon_512x512@2x", 1024)
]
for t in tamanos {
    try dibujar(lado: t.lado).write(to: URL(fileURLWithPath: "\(salida)/\(t.nombre).png"))
}
print("ok")

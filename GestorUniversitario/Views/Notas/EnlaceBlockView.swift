import SwiftUI
import AppKit
import WebKit

// MARK: - Obtención de la vista previa

enum ServicioEnlaces {
    /// Normaliza lo que escribió el usuario a una URL http(s).
    static func url(desde texto: String) -> URL? {
        var t = texto.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !t.isEmpty, !t.contains(where: \.isWhitespace) else { return nil }
        if !t.contains("://") { t = "https://" + t }
        guard let url = URL(string: t), ["http", "https"].contains(url.scheme ?? ""),
              let host = url.host, host.contains(".") else { return nil }
        return url
    }

    static func videoYouTube(_ url: URL) -> String? {
        let host = (url.host ?? "").replacingOccurrences(of: "www.", with: "").replacingOccurrences(of: "m.", with: "")
        let id: String?
        if host == "youtu.be" {
            id = url.pathComponents.dropFirst().first
        } else if host.hasSuffix("youtube.com") || host.hasSuffix("youtube-nocookie.com") {
            if let v = URLComponents(url: url, resolvingAgainstBaseURL: false)?.queryItems?.first(where: { $0.name == "v" })?.value {
                id = v
            } else if url.pathComponents.count >= 3, ["shorts", "embed", "live", "v"].contains(url.pathComponents[1]) {
                id = url.pathComponents[2]
            } else {
                id = nil
            }
        } else {
            id = nil
        }
        guard let id, id.range(of: "^[A-Za-z0-9_-]{6,15}$", options: .regularExpression) != nil else { return nil }
        return id
    }

    static func videoVimeo(_ url: URL) -> String? {
        guard (url.host ?? "").hasSuffix("vimeo.com"),
              let id = url.pathComponents.last(where: { $0.allSatisfy(\.isNumber) && !$0.isEmpty }) else { return nil }
        return id
    }

    static func obtener(_ url: URL) async -> LinkPreview? {
        if let id = videoYouTube(url) { return await youtube(url, id: id) }
        if let id = videoVimeo(url) { return await vimeo(url, id: id) }
        return await pagina(url)
    }

    // MARK: Proveedores

    private static func youtube(_ url: URL, id: String) async -> LinkPreview {
        var vp = LinkPreview(titulo: nil, descripcion: nil, sitio: "YouTube", imagen: nil, proveedor: "youtube", videoID: id)
        if let oembed = URL(string: "https://www.youtube.com/oembed?format=json&url=" + (url.absoluteString.addingPercentEncoding(withAllowedCharacters: .alphanumerics) ?? "")),
           let datos = await descargar(oembed),
           let json = try? JSONSerialization.jsonObject(with: datos) as? [String: Any] {
            vp.titulo = json["title"] as? String
            vp.descripcion = json["author_name"] as? String
        }
        for calidad in ["maxresdefault", "hqdefault"] {
            if let miniatura = URL(string: "https://img.youtube.com/vi/\(id)/\(calidad).jpg"),
               let datos = await descargar(miniatura, maxBytes: 4_000_000), let reducida = reducir(datos) {
                vp.imagen = reducida
                break
            }
        }
        return vp
    }

    private static func vimeo(_ url: URL, id: String) async -> LinkPreview {
        var vp = LinkPreview(titulo: nil, descripcion: nil, sitio: "Vimeo", imagen: nil, proveedor: "vimeo", videoID: id)
        if let oembed = URL(string: "https://vimeo.com/api/oembed.json?url=" + (url.absoluteString.addingPercentEncoding(withAllowedCharacters: .alphanumerics) ?? "")),
           let datos = await descargar(oembed),
           let json = try? JSONSerialization.jsonObject(with: datos) as? [String: Any] {
            vp.titulo = json["title"] as? String
            vp.descripcion = json["author_name"] as? String
            if let miniatura = (json["thumbnail_url"] as? String).flatMap(URL.init(string:)),
               let datos = await descargar(miniatura, maxBytes: 4_000_000) {
                vp.imagen = reducir(datos)
            }
        }
        return vp
    }

    private static func pagina(_ url: URL) async -> LinkPreview? {
        guard let datos = await descargar(url, maxBytes: 1_500_000) else { return nil }
        let html = String(decoding: datos, as: UTF8.self)
        func meta(_ claves: [String]) -> String? {
            for clave in claves {
                let patrones = [
                    "<meta[^>]+(?:property|name)=[\"']\(clave)[\"'][^>]+content=[\"']([^\"']*)[\"']",
                    "<meta[^>]+content=[\"']([^\"']*)[\"'][^>]+(?:property|name)=[\"']\(clave)[\"']"
                ]
                for patron in patrones {
                    if let r = html.range(of: patron, options: [.regularExpression, .caseInsensitive]),
                       let contenido = capturar(String(html[r]), "content=[\"']([^\"']*)[\"']") {
                        let limpio = decodificar(contenido).trimmingCharacters(in: .whitespacesAndNewlines)
                        if !limpio.isEmpty { return limpio }
                    }
                }
            }
            return nil
        }
        var titulo = meta(["og:title", "twitter:title"])
        if titulo == nil, let r = html.range(of: "<title[^>]*>([\\s\\S]*?)</title>", options: [.regularExpression, .caseInsensitive]) {
            titulo = capturar(String(html[r]), ">([\\s\\S]*?)<").map { decodificar($0).trimmingCharacters(in: .whitespacesAndNewlines) }
        }
        let descripcion = meta(["og:description", "twitter:description", "description"])
        let sitio = meta(["og:site_name"]) ?? url.host?.replacingOccurrences(of: "www.", with: "")
        var imagen: Data?
        if let ruta = meta(["og:image", "twitter:image"]), let imagenURL = URL(string: ruta, relativeTo: url)?.absoluteURL,
           let datos = await descargar(imagenURL, maxBytes: 5_000_000) {
            imagen = reducir(datos)
        }
        guard titulo != nil || descripcion != nil else { return nil }
        return LinkPreview(titulo: titulo, descripcion: descripcion, sitio: sitio, imagen: imagen, proveedor: nil, videoID: nil)
    }

    // MARK: Utilidades

    private static func descargar(_ url: URL, maxBytes: Int = 1_000_000) async -> Data? {
        var peticion = URLRequest(url: url, timeoutInterval: 12)
        peticion.setValue("Mozilla/5.0 (Macintosh; Intel Mac OS X 14_0) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Safari/605.1.15",
                          forHTTPHeaderField: "User-Agent")
        peticion.setValue("es,en;q=0.8", forHTTPHeaderField: "Accept-Language")
        guard let (datos, respuesta) = try? await URLSession.shared.data(for: peticion),
              (respuesta as? HTTPURLResponse).map({ (200..<300).contains($0.statusCode) }) ?? true,
              datos.count <= maxBytes else { return nil }
        return datos
    }

    private static func capturar(_ texto: String, _ patron: String) -> String? {
        guard let re = try? NSRegularExpression(pattern: patron, options: [.caseInsensitive]),
              let m = re.firstMatch(in: texto, range: NSRange(texto.startIndex..., in: texto)), m.numberOfRanges > 1,
              let r = Range(m.range(at: 1), in: texto) else { return nil }
        return String(texto[r])
    }

    private static func decodificar(_ s: String) -> String {
        var r = s
        for (a, b) in [("&amp;", "&"), ("&quot;", "\""), ("&#39;", "'"), ("&#x27;", "'"), ("&lt;", "<"), ("&gt;", ">"), ("&nbsp;", " ")] {
            r = r.replacingOccurrences(of: a, with: b)
        }
        return r
    }

    /// Reduce la imagen a 640 px de ancho y la guarda como JPEG para no inflar la nota.
    private static func reducir(_ datos: Data) -> Data? {
        guard let imagen = NSImage(data: datos), imagen.size.width > 1 else { return nil }
        let ancho = min(640, imagen.size.width)
        let alto = imagen.size.height * ancho / imagen.size.width
        guard let rep = NSBitmapImageRep(bitmapDataPlanes: nil, pixelsWide: Int(ancho), pixelsHigh: Int(alto), bitsPerSample: 8,
                                         samplesPerPixel: 4, hasAlpha: true, isPlanar: false, colorSpaceName: .deviceRGB,
                                         bytesPerRow: 0, bitsPerPixel: 0) else { return nil }
        NSGraphicsContext.saveGraphicsState()
        NSGraphicsContext.current = NSGraphicsContext(bitmapImageRep: rep)
        NSColor.black.setFill()
        NSRect(x: 0, y: 0, width: ancho, height: alto).fill()
        imagen.draw(in: NSRect(x: 0, y: 0, width: ancho, height: alto))
        NSGraphicsContext.restoreGraphicsState()
        return rep.representation(using: .jpeg, properties: [.compressionFactor: 0.75])
    }
}

// MARK: - Bloque de enlace

struct EnlaceBlockView: View {
    @Binding var block: NoteBlock
    let onQuitar: () -> Void
    let onConvertirEnTexto: () -> Void

    @State private var direccion = ""
    @State private var cargando = false
    @State private var fallo = false
    @State private var reproduciendo = false
    @State private var hover = false
    @FocusState private var campoEnfocado: Bool

    private var url: URL? { block.url.flatMap { URL(string: $0) } }
    private var esVideo: Bool { block.preview?.proveedor != nil }

    var body: some View {
        Group {
            if let url {
                tarjeta(url)
            } else {
                entrada
            }
        }
        .padding(.vertical, 2)
        .onHover { hover = $0 }
        .task(id: block.url) {
            if block.url != nil, block.preview == nil, !fallo { await cargar() }
        }
    }

    // MARK: Entrada

    private var entrada: some View {
        HStack(spacing: 8) {
            Image(systemName: "link").foregroundStyle(.secondary)
            TextField("Pega un enlace de una página o un vídeo y pulsa Intro", text: $direccion)
                .textFieldStyle(.plain)
                .focused($campoEnfocado)
                .onSubmit(confirmar)
            if !direccion.isEmpty {
                Button("Añadir", action: confirmar)
            }
            Button(action: onQuitar) { Image(systemName: "xmark") }
                .buttonStyle(.plain)
                .foregroundStyle(.secondary)
        }
        .padding(10)
        .background(RoundedRectangle(cornerRadius: 8).fill(Color.primary.opacity(0.06)))
        .onAppear { campoEnfocado = true }
    }

    private func confirmar() {
        guard let nueva = ServicioEnlaces.url(desde: direccion) else {
            NSSound.beep()
            return
        }
        fallo = false
        block.url = nueva.absoluteString
        block.preview = nil
    }

    // MARK: Tarjeta

    @ViewBuilder
    private func tarjeta(_ url: URL) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            if reproduciendo, let vp = block.preview, let id = vp.videoID, let proveedor = vp.proveedor {
                ReproductorWeb(proveedor: proveedor, id: id)
                    .frame(height: 300)
                    .clipShape(RoundedRectangle(cornerRadius: 8))
                HStack {
                    Text("¿No se reproduce? Algunos vídeos no permiten verse fuera de su web.")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                    Spacer()
                    Button("Abrir en el navegador") { NSWorkspace.shared.open(url) }
                        .font(.caption)
                }
                .padding(.horizontal, 12)
                .padding(.vertical, 8)
            } else if esVideo {
                videoPortada
            } else {
                paginaTarjeta(url)
            }
        }
        .background(RoundedRectangle(cornerRadius: 10).fill(Color.primary.opacity(0.05)))
        .overlay(RoundedRectangle(cornerRadius: 10).stroke(Color.primary.opacity(0.1), lineWidth: 0.5))
        .clipShape(RoundedRectangle(cornerRadius: 10))
        .overlay(alignment: .topTrailing) {
            if hover {
                HStack(spacing: 10) {
                    if cargando { ProgressView().controlSize(.small) }
                    Button { NSWorkspace.shared.open(url) } label: { Image(systemName: "arrow.up.right.square") }
                        .help("Abrir en el navegador")
                    Button { Task { await cargar() } } label: { Image(systemName: "arrow.clockwise") }
                        .help("Actualizar vista previa")
                    Button(action: onConvertirEnTexto) { Image(systemName: "text.alignleft") }
                        .help("Convertir en texto")
                    Button(action: onQuitar) { Image(systemName: "xmark") }
                        .help("Quitar")
                }
                .buttonStyle(.plain)
                .padding(6)
                .background(Capsule().fill(.regularMaterial))
                .padding(8)
            }
        }
    }

    private var videoPortada: some View {
        VStack(alignment: .leading, spacing: 0) {
            ZStack {
                if let datos = block.preview?.imagen, let imagen = NSImage(data: datos) {
                    Image(nsImage: imagen).resizable().scaledToFill()
                } else {
                    Rectangle().fill(Color.black.opacity(0.8))
                }
                Button { reproduciendo = true } label: {
                    Image(systemName: "play.circle.fill")
                        .font(.system(size: 54))
                        .foregroundStyle(.white)
                        .shadow(radius: 6)
                }
                .buttonStyle(.plain)
            }
            .frame(maxWidth: .infinity)
            .frame(height: 220)
            .clipped()

            textos(block.preview?.titulo ?? block.url ?? "", block.preview?.descripcion, block.preview?.sitio)
                .padding(12)
        }
    }

    private func paginaTarjeta(_ url: URL) -> some View {
        Button { NSWorkspace.shared.open(url) } label: {
            HStack(spacing: 0) {
                textos(block.preview?.titulo ?? (cargando ? "Cargando vista previa…" : url.absoluteString),
                       block.preview?.descripcion,
                       block.preview?.sitio ?? url.host)
                    .padding(12)
                    .frame(maxWidth: .infinity, alignment: .leading)
                if let datos = block.preview?.imagen, let imagen = NSImage(data: datos) {
                    Image(nsImage: imagen)
                        .resizable()
                        .scaledToFill()
                        .frame(width: 150, height: 100)
                        .clipped()
                }
            }
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }

    private func textos(_ titulo: String, _ descripcion: String?, _ sitio: String?) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(titulo).font(.system(size: 14, weight: .semibold)).lineLimit(2)
            if let descripcion, !descripcion.isEmpty {
                Text(descripcion).font(.caption).foregroundStyle(.secondary).lineLimit(2)
            }
            HStack(spacing: 4) {
                Image(systemName: esVideo ? "play.rectangle" : "globe").font(.caption2)
                Text(sitio ?? "").lineLimit(1)
                if fallo { Text("· No se pudo cargar la vista previa").foregroundStyle(.orange) }
            }
            .font(.caption2)
            .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private func cargar() async {
        guard let url else { return }
        cargando = true
        fallo = false
        let resultado = await ServicioEnlaces.obtener(url)
        cargando = false
        if let resultado { block.preview = resultado } else { fallo = true }
    }
}

// MARK: - Reproductor incrustado

private struct ReproductorWeb: NSViewRepresentable {
    let proveedor: String
    let id: String

    /// YouTube exige que las apps se identifiquen con un `Referer` propio (si no, responde con el error 152/153).
    private var identidad: String { "https://" + (Bundle.main.bundleIdentifier ?? "com.ksebas.gestoruniversitario").lowercased() }

    func makeNSView(context: Context) -> WKWebView {
        let config = WKWebViewConfiguration()
        config.mediaTypesRequiringUserActionForPlayback = []
        let web = WKWebView(frame: .zero, configuration: config)
        web.setValue(false, forKey: "drawsBackground")

        let direccion: String
        if proveedor == "youtube" {
            direccion = "https://www.youtube.com/embed/\(id)?autoplay=1&rel=0&playsinline=1&enablejsapi=1"
                + "&origin=\(identidad)&widget_referrer=\(identidad)"
        } else {
            direccion = "https://player.vimeo.com/video/\(id)?autoplay=1"
        }
        if let url = URL(string: direccion) {
            var peticion = URLRequest(url: url)
            peticion.setValue(identidad + "/", forHTTPHeaderField: "Referer")
            web.load(peticion)
        }
        return web
    }

    func updateNSView(_ nsView: WKWebView, context: Context) {}
}

import AppKit

/// Recibe los enlaces `taskflow://…` (widgets) directamente desde macOS, tanto si la app
/// estaba cerrada como si ya estaba abierta, y la trae al frente en el punto indicado.
final class AppDelegate: NSObject, NSApplicationDelegate {
    private static var ultimoEnlace: (texto: String, fecha: Date)?

    func application(_ application: NSApplication, open urls: [URL]) {
        urls.forEach { Self.procesar($0, origen: "AppDelegate") }
    }

    /// Punto único de entrada para cualquier enlace. Ignora el mismo enlace repetido en menos de 0.6 s
    /// (macOS puede entregarlo por dos caminos a la vez).
    static func procesar(_ url: URL, origen: String) {
        let texto = url.absoluteString
        if let ultimo = ultimoEnlace, ultimo.texto == texto, Date().timeIntervalSince(ultimo.fecha) < 0.6 {
            NSLog("TaskFlow [URL] duplicado ignorado (%@): %@", origen, texto)
            return
        }
        ultimoEnlace = (texto, Date())

        guard let destino = Enrutador.shared.abrir(url) else {
            NSLog("TaskFlow [URL] no reconocido (%@): %@", origen, texto)
            return
        }
        NSLog("TaskFlow [URL] aceptado (%@): %@ -> %@", origen, texto, destino.rawValue)
        UserDefaults.standard.set(destino.rawValue, forKey: "modulo")
        traerAlFrente()
    }

    /// Muestra la ventana existente de la app (también si estaba minimizada o detrás de otras).
    static func traerAlFrente() {
        if #available(macOS 14.0, *) {
            NSApp.activate()
        } else {
            NSApp.activate(ignoringOtherApps: true)
        }
        for ventana in NSApp.windows where ventana.canBecomeMain {
            if ventana.isMiniaturized { ventana.deminiaturize(nil) }
            ventana.makeKeyAndOrderFront(nil)
        }
    }
}

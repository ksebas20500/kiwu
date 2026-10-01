import SwiftUI
import AppKit
import Combine

/// Acciones de la app que se pueden lanzar con un atajo de teclado.
enum AccionApp: String, CaseIterable, Identifiable, Codable {
    case irTareas, irNotas, nuevaTarea, vistaLista, vistaTablero, irHoy, irCalendario, abrirAjustes
    var id: String { rawValue }

    var titulo: String {
        switch self {
        case .irTareas: return "Ir a Tareas"
        case .irNotas: return "Ir a Notas"
        case .nuevaTarea: return "Nueva tarea"
        case .vistaLista: return "Vista de lista"
        case .vistaTablero: return "Vista de tablero"
        case .irHoy: return "Ir a Hoy"
        case .irCalendario: return "Ir al Calendario"
        case .abrirAjustes: return "Abrir ajustes"
        }
    }

    var atajoPredeterminado: Atajo {
        switch self {
        case .irTareas: return Atajo("1", comando: true)
        case .irNotas: return Atajo("2", comando: true)
        case .nuevaTarea: return Atajo("n", comando: true)
        case .vistaLista: return Atajo("l", comando: true, opcion: true)
        case .vistaTablero: return Atajo("b", comando: true, opcion: true)
        case .irHoy: return Atajo("t", comando: true, mayus: true)
        case .irCalendario: return Atajo("c", comando: true, mayus: true)
        case .abrirAjustes: return Atajo(",", comando: true)
        }
    }
}

struct Atajo: Codable, Equatable {
    var tecla: String
    var comando = false
    var mayus = false
    var opcion = false
    var control = false

    init(_ tecla: String, comando: Bool = false, mayus: Bool = false, opcion: Bool = false, control: Bool = false) {
        self.tecla = tecla
        self.comando = comando
        self.mayus = mayus
        self.opcion = opcion
        self.control = control
    }

    var equivalente: KeyEquivalent { KeyEquivalent(tecla.first ?? " ") }

    var modificadores: EventModifiers {
        var m: EventModifiers = []
        if comando { m.insert(.command) }
        if mayus { m.insert(.shift) }
        if opcion { m.insert(.option) }
        if control { m.insert(.control) }
        return m
    }

    var texto: String {
        (control ? "⌃" : "") + (opcion ? "⌥" : "") + (mayus ? "⇧" : "") + (comando ? "⌘" : "") + tecla.uppercased()
    }
}

enum TemaApp: String, CaseIterable, Identifiable {
    case sistema, claro, oscuro
    var id: String { rawValue }
    var titulo: String {
        switch self {
        case .sistema: return "Sistema"
        case .claro: return "Claro"
        case .oscuro: return "Oscuro"
        }
    }
    var esquema: ColorScheme? {
        switch self {
        case .sistema: return nil
        case .claro: return .light
        case .oscuro: return .dark
        }
    }
}

/// Preferencias de la app: atajos, tema y fondo de pantalla.
@MainActor
final class AjustesStore: ObservableObject {
    static let shared = AjustesStore()

    /// Emite cada acción lanzada desde un atajo o un botón.
    let acciones = PassthroughSubject<AccionApp, Never>()

    @Published private(set) var atajos: [AccionApp: Atajo]
    @Published private(set) var fondo: NSImage?
    @Published var tema: TemaApp { didSet { defaults.set(tema.rawValue, forKey: "ajustes.tema") } }
    /// 0...1: cuánto se ve la imagen de fondo.
    @Published var visibilidadFondo: Double { didSet { defaults.set(visibilidadFondo, forKey: "ajustes.visibilidadFondo") } }
    /// 0...1: opacidad de la capa que cubre la imagen para que el contenido se lea (0 = muy translúcido).
    @Published var opacidadPaneles: Double { didSet { defaults.set(opacidadPaneles, forKey: "ajustes.opacidadPaneles") } }
    /// 0...30: desenfoque del fondo.
    @Published var desenfoque: Double { didSet { defaults.set(desenfoque, forKey: "ajustes.desenfoque") } }

    private let defaults = UserDefaults.standard

    private init() {
        var guardados: [AccionApp: Atajo] = [:]
        if let data = defaults.data(forKey: "ajustes.atajos"),
           let dic = try? JSONDecoder().decode([String: Atajo].self, from: data) {
            for (clave, atajo) in dic { if let accion = AccionApp(rawValue: clave) { guardados[accion] = atajo } }
        }
        atajos = Dictionary(uniqueKeysWithValues: AccionApp.allCases.map { ($0, guardados[$0] ?? $0.atajoPredeterminado) })
        tema = TemaApp(rawValue: defaults.string(forKey: "ajustes.tema") ?? "") ?? .sistema
        visibilidadFondo = defaults.object(forKey: "ajustes.visibilidadFondo") as? Double ?? 0.5
        opacidadPaneles = defaults.object(forKey: "ajustes.opacidadPaneles") as? Double ?? 0.45
        desenfoque = defaults.object(forKey: "ajustes.desenfoque") as? Double ?? 0
        fondo = Self.urlFondo.flatMap { NSImage(contentsOf: $0) }
    }

    // MARK: Atajos

    func atajo(_ accion: AccionApp) -> Atajo { atajos[accion] ?? accion.atajoPredeterminado }

    func disparar(_ accion: AccionApp) { acciones.send(accion) }

    /// Devuelve la acción que ya usa ese atajo, si existe.
    func conflicto(de atajo: Atajo, excepto accion: AccionApp) -> AccionApp? {
        AccionApp.allCases.first { $0 != accion && self.atajo($0) == atajo }
    }

    func asignar(_ atajo: Atajo, a accion: AccionApp) {
        atajos[accion] = atajo
        guardarAtajos()
    }

    func restaurar(_ accion: AccionApp) {
        atajos[accion] = accion.atajoPredeterminado
        guardarAtajos()
    }

    func restaurarTodos() {
        atajos = Dictionary(uniqueKeysWithValues: AccionApp.allCases.map { ($0, $0.atajoPredeterminado) })
        guardarAtajos()
    }

    private func guardarAtajos() {
        let dic = Dictionary(uniqueKeysWithValues: atajos.map { ($0.key.rawValue, $0.value) })
        defaults.set(try? JSONEncoder().encode(dic), forKey: "ajustes.atajos")
    }

    // MARK: Fondo

    private static var carpeta: URL { CarpetaDatos.url }

    private static var urlFondo: URL? {
        let url = carpeta.appendingPathComponent("fondo")
        return FileManager.default.fileExists(atPath: url.path) ? url : nil
    }

    /// Copia la imagen elegida a la carpeta de la app (así sigue disponible aunque muevas el original).
    func elegirFondo(_ origen: URL) {
        let acceso = origen.startAccessingSecurityScopedResource()
        defer { if acceso { origen.stopAccessingSecurityScopedResource() } }
        guard let imagen = NSImage(contentsOf: origen) else { return }
        let destino = Self.carpeta.appendingPathComponent("fondo")
        try? FileManager.default.createDirectory(at: Self.carpeta, withIntermediateDirectories: true)
        try? FileManager.default.removeItem(at: destino)
        do {
            try FileManager.default.copyItem(at: origen, to: destino)
            fondo = imagen
        } catch {
            NSLog("Kiwu: no se pudo copiar el fondo: \(error.localizedDescription)")
        }
    }

    func quitarFondo() {
        if let url = Self.urlFondo { try? FileManager.default.removeItem(at: url) }
        fondo = nil
    }
}

/// Capas de fondo de la ventana: imagen + velo que controla la translucidez.
struct FondoApp: View {
    @ObservedObject var ajustes: AjustesStore

    var body: some View {
        ZStack {
            Color(nsColor: .windowBackgroundColor)
            if let imagen = ajustes.fondo {
                GeometryReader { geo in
                    Image(nsImage: imagen)
                        .resizable()
                        .scaledToFill()
                        .frame(width: geo.size.width, height: geo.size.height)
                        .clipped()
                        .blur(radius: ajustes.desenfoque)
                        .opacity(ajustes.visibilidadFondo)
                }
                Color(nsColor: .windowBackgroundColor).opacity(ajustes.opacidadPaneles * 0.6)
            }
        }
        .ignoresSafeArea()
        .allowsHitTesting(false)
    }
}

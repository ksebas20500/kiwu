import Foundation

/// Constantes y datos compartidos entre la app y la extensión de widgets.
/// Este archivo se compila en ambos targets.
enum TaskFlowShared {
    /// App Group con prefijo del equipo (`<TeamID>.com.ksebas.taskflow`). macOS solo deja al widget
    /// leer los datos de la app con este formato. El valor real llega desde el Info.plist de cada target.
    static let appGroup: String = {
        if let id = Bundle.main.object(forInfoDictionaryKey: "AppGroupIdentifier") as? String,
           !id.isEmpty, !id.contains("$(") {
            return id
        }
        return "group.com.ksebas.taskflow"
    }()
    static let esquemaURL = "taskflow"

    /// Carpeta compartida entre la app y los widgets.
    ///
    /// - Compilación normal (firmada con un equipo de desarrollo): carpeta del App Group.
    /// - Compilación `DIRECT_DISTRIBUTION` (instalador descargable, firma ad hoc): macOS no permite usar
    ///   App Groups sin equipo de desarrollo, así que se usa una carpeta real de Application Support,
    ///   autorizada en el sandbox de ambos con una excepción (ver `Config/Direct-*.entitlements`).
    static var carpeta: URL? {
        #if DIRECT_DISTRIBUTION
        guard let usuario = getpwuid(getuid()) else { return nil }
        let carpeta = URL(fileURLWithPath: String(cString: usuario.pointee.pw_dir), isDirectory: true)
            .appendingPathComponent("Library/Application Support/TaskFlow", isDirectory: true)
        try? FileManager.default.createDirectory(at: carpeta, withIntermediateDirectories: true)
        return carpeta
        #else
        return FileManager.default.containerURL(forSecurityApplicationGroupIdentifier: appGroup)
        #endif
    }

    static func urlDeber(_ id: UUID) -> URL {
        URL(string: "\(esquemaURL)://deber/\(id.uuidString)")!
    }

    private static let formatoDia: DateFormatter = {
        let f = DateFormatter()
        f.locale = Locale(identifier: "en_US_POSIX")
        f.calendar = Calendar.current
        f.timeZone = TimeZone.current
        f.dateFormat = "yyyy-MM-dd"
        return f
    }()

    /// Abre el calendario de la app; con `dia`, se despliega ese día.
    static func urlCalendario(dia: Date?) -> URL {
        let consulta = dia.map { "?dia=\(formatoDia.string(from: $0))" } ?? ""
        return URL(string: "\(esquemaURL)://calendario\(consulta)")!
    }

    static func dia(desde texto: String) -> Date? {
        formatoDia.date(from: texto)
    }

    /// Abre la app en un módulo ("tareas" o "notas") sin crear nada.
    static func urlModulo(_ nombre: String) -> URL {
        URL(string: "\(esquemaURL)://\(nombre)")!
    }

    static func urlNota(_ id: UUID) -> URL {
        URL(string: "\(esquemaURL)://nota/\(id.uuidString)")!
    }

    /// Crea una nota nueva en el cuaderno indicado (o en el primero si es nil).
    static func urlNuevaNota(cuaderno: UUID?) -> URL {
        let consulta = cuaderno.map { "?cuaderno=\($0.uuidString)" } ?? ""
        return URL(string: "\(esquemaURL)://nota/nueva\(consulta)")!
    }
}

struct WidgetDeber: Codable, Identifiable, Hashable {
    let id: UUID
    let titulo: String
    let materia: String
    let materiaID: UUID?
    let fechaEntrega: Date?
}

struct WidgetLista: Codable, Identifiable, Hashable {
    let id: UUID
    let nombre: String
}

struct WidgetCuaderno: Codable, Identifiable, Hashable {
    let id: UUID
    let nombre: String
    let icono: String
}

struct WidgetNota: Codable, Identifiable, Hashable {
    let id: UUID
    let titulo: String
    let icono: String
    let resumen: String
    let cuadernoID: UUID
    let fecha: Date
    let favorita: Bool
}

struct WidgetSnapshot: Codable {
    var generado: Date
    /// Deberes pendientes, ordenados por fecha de entrega (los que no tienen fecha, al final).
    var pendientes: [WidgetDeber]
    var totalPendientes: Int
    var listas: [WidgetLista]
    var cuadernos: [WidgetCuaderno]
    /// Notas más recientes primero.
    var notas: [WidgetNota]

    init(generado: Date, pendientes: [WidgetDeber], totalPendientes: Int,
         listas: [WidgetLista], cuadernos: [WidgetCuaderno], notas: [WidgetNota]) {
        self.generado = generado
        self.pendientes = pendientes
        self.totalPendientes = totalPendientes
        self.listas = listas
        self.cuadernos = cuadernos
        self.notas = notas
    }

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        generado = try c.decode(Date.self, forKey: .generado)
        pendientes = try c.decode([WidgetDeber].self, forKey: .pendientes)
        totalPendientes = try c.decode(Int.self, forKey: .totalPendientes)
        listas = try c.decodeIfPresent([WidgetLista].self, forKey: .listas) ?? []
        cuadernos = try c.decodeIfPresent([WidgetCuaderno].self, forKey: .cuadernos) ?? []
        notas = try c.decodeIfPresent([WidgetNota].self, forKey: .notas) ?? []
    }
}

/// Almacén en archivos JSON dentro del App Group. La app escribe la instantánea;
/// el widget la lee y deja en una cola los deberes que el usuario completa,
/// para que la app los aplique a su base de datos.
enum AlmacenWidget {
    private static var archivoSnapshot: URL? { TaskFlowShared.carpeta?.appendingPathComponent("widget-snapshot.json") }
    private static var archivoCola: URL? { TaskFlowShared.carpeta?.appendingPathComponent("widget-completados.json") }

    // MARK: Instantánea

    static func leer() -> WidgetSnapshot? {
        guard let url = archivoSnapshot, let data = try? Data(contentsOf: url) else { return nil }
        let decoder = JSONDecoder()
        decoder.dateDecodingStrategy = .iso8601
        return try? decoder.decode(WidgetSnapshot.self, from: data)
    }

    static func escribir(_ snapshot: WidgetSnapshot) {
        guard let url = archivoSnapshot else { return }
        let encoder = JSONEncoder()
        encoder.dateEncodingStrategy = .iso8601
        guard let data = try? encoder.encode(snapshot) else { return }
        coordinar(url) { try? data.write(to: $0, options: .atomic) }
    }

    // MARK: Cola de deberes completados desde el widget

    static func completadosPendientes() -> [UUID] {
        guard let url = archivoCola else { return [] }
        var ids: [UUID] = []
        coordinar(url) { ids = leerCola($0) }
        return ids
    }

    /// Lo llama el widget: quita el deber de la instantánea y lo añade a la cola.
    static func marcarCompletado(_ id: UUID) {
        guard let cola = archivoCola else { return }
        coordinar(cola) { url in
            var ids = leerCola(url)
            if !ids.contains(id) { ids.append(id) }
            try? JSONEncoder().encode(ids).write(to: url, options: .atomic)
        }
        if var snapshot = leer() {
            snapshot.pendientes.removeAll { $0.id == id }
            snapshot.totalPendientes = max(0, snapshot.totalPendientes - 1)
            escribir(snapshot)
        }
    }

    /// Lo llama la app: devuelve la cola y la vacía.
    static func tomarCompletadosPendientes() -> [UUID] {
        guard let url = archivoCola else { return [] }
        var ids: [UUID] = []
        coordinar(url) { destino in
            ids = leerCola(destino)
            if !ids.isEmpty { try? FileManager.default.removeItem(at: destino) }
        }
        return ids
    }

    // MARK: Mes mostrado en el widget de calendario

    private static var archivoMes: URL? { TaskFlowShared.carpeta?.appendingPathComponent("widget-calendario-mes.txt") }

    /// Meses de diferencia respecto al mes actual que muestra el widget de calendario.
    static var desplazamientoMes: Int {
        guard let url = archivoMes else { return 0 }
        var valor = 0
        coordinar(url) { valor = (try? String(contentsOf: $0, encoding: .utf8)).flatMap { Int($0) } ?? 0 }
        return valor
    }

    /// `delta` 0 vuelve al mes actual.
    static func cambiarMes(_ delta: Int) {
        guard let url = archivoMes else { return }
        coordinar(url) { destino in
            let actual = (try? String(contentsOf: destino, encoding: .utf8)).flatMap { Int($0) } ?? 0
            let nuevo = delta == 0 ? 0 : max(-24, min(24, actual + delta))
            try? String(nuevo).write(to: destino, atomically: true, encoding: .utf8)
        }
    }

    // MARK: Utilidades

    private static func leerCola(_ url: URL) -> [UUID] {
        guard let data = try? Data(contentsOf: url) else { return [] }
        return (try? JSONDecoder().decode([UUID].self, from: data)) ?? []
    }

    /// El widget y la app son procesos distintos: se coordina el acceso al mismo archivo.
    private static func coordinar(_ url: URL, _ bloque: (URL) -> Void) {
        var error: NSError?
        NSFileCoordinator(filePresenter: nil).coordinate(writingItemAt: url, options: .forMerging, error: &error) { destino in
            bloque(destino)
        }
    }
}

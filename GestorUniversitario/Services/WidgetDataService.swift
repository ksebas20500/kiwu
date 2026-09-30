import CoreData
import WidgetKit
import Combine

/// Publica los deberes pendientes para el widget y aplica los que se completaron desde él.
enum WidgetDataService {
    private static var trabajoPendiente: DispatchWorkItem?

    /// Agrupa varias escrituras seguidas (p. ej. al teclear un título) en una sola actualización.
    static func programarActualizacion(_ context: NSManagedObjectContext) {
        trabajoPendiente?.cancel()
        let trabajo = DispatchWorkItem { actualizar(context) }
        trabajoPendiente = trabajo
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.0, execute: trabajo)
    }

    static func actualizar(_ context: NSManagedObjectContext) {
        guard TaskFlowShared.carpeta != nil else { return }

        let peticion = Deber.fetchRequest()
        peticion.predicate = NSPredicate(format: "completado == NO")
        let excluidos = Set(AlmacenWidget.completadosPendientes())
        let deberes = ((try? context.fetch(peticion)) ?? []).filter { !excluidos.contains($0.id) }

        let ordenados = deberes.sorted {
            ($0.fechaEntrega ?? .distantFuture) < ($1.fechaEntrega ?? .distantFuture)
        }
        let items = ordenados.prefix(200).map {
            WidgetDeber(id: $0.id, titulo: $0.titulo, materia: $0.materia.nombre, materiaID: $0.materia.id, fechaEntrega: $0.fechaEntrega)
        }

        let peticionListas = Materia.fetchRequest()
        peticionListas.sortDescriptors = [NSSortDescriptor(key: "orden", ascending: true), NSSortDescriptor(key: "nombre", ascending: true)]
        let listas = ((try? context.fetch(peticionListas)) ?? []).map { WidgetLista(id: $0.id, nombre: $0.nombre) }

        let peticionCuadernos = NSFetchRequest<Cuaderno>(entityName: "Cuaderno")
        peticionCuadernos.sortDescriptors = [NSSortDescriptor(key: "orden", ascending: true), NSSortDescriptor(key: "nombre", ascending: true)]
        let cuadernos = ((try? context.fetch(peticionCuadernos)) ?? []).map { WidgetCuaderno(id: $0.id, nombre: $0.nombre, icono: $0.icono) }

        let peticionNotas = NSFetchRequest<Pagina>(entityName: "Pagina")
        peticionNotas.sortDescriptors = [NSSortDescriptor(key: "fechaActualizacion", ascending: false)]
        peticionNotas.fetchLimit = 60
        let notas = ((try? context.fetch(peticionNotas)) ?? []).map {
            WidgetNota(
                id: $0.id,
                titulo: $0.titulo,
                icono: $0.icono,
                resumen: String($0.textoPlano.replacingOccurrences(of: "\n", with: " ").prefix(90)),
                cuadernoID: $0.cuaderno.id,
                fecha: $0.fechaActualizacion,
                favorita: $0.favorita
            )
        }

        AlmacenWidget.escribir(WidgetSnapshot(
            generado: .now,
            pendientes: Array(items),
            totalPendientes: deberes.count,
            listas: listas,
            cuadernos: cuadernos,
            notas: notas
        ))
        WidgetCenter.shared.reloadAllTimelines()
    }

    /// Marca como completados los deberes que el usuario resolvió desde el widget.
    @MainActor
    static func aplicarCompletadosDelWidget(context: NSManagedObjectContext, service: DeberService) {
        let ids = AlmacenWidget.tomarCompletadosPendientes()
        guard !ids.isEmpty else { return }
        let peticion = Deber.fetchRequest()
        peticion.predicate = NSPredicate(format: "id IN %@", ids as [UUID])
        for deber in (try? context.fetch(peticion)) ?? [] where !deber.completado {
            service.marcar(deber, completado: true)
        }
        actualizar(context)
    }
}

struct PeticionNuevaNota: Equatable {
    let cuaderno: UUID?
    let id = UUID()
}

/// Navegación pedida desde fuera de la app (enlaces `taskflow://...` de los widgets).
final class Enrutador: ObservableObject {
    static let shared = Enrutador()
    @Published var deberPendiente: UUID?
    @Published var notaPendiente: UUID?
    @Published var nuevaNota: PeticionNuevaNota?
    @Published var diaCalendario: Date?

    /// Interpreta la URL y devuelve el módulo que debe mostrarse (nil si no es un enlace de TaskFlow).
    func abrir(_ url: URL) -> Modulo? {
        guard url.scheme == TaskFlowShared.esquemaURL else { return nil }
        switch url.host {
        case "tareas":
            return .tareas
        case "calendario":
            let texto = URLComponents(url: url, resolvingAgainstBaseURL: false)?
                .queryItems?.first { $0.name == "dia" }?.value
            diaCalendario = texto.flatMap { TaskFlowShared.dia(desde: $0) } ?? Calendar.current.startOfDay(for: .now)
            return .tareas
        case "notas":
            return .notas
        case "deber":
            guard let id = UUID(uuidString: url.lastPathComponent) else { return nil }
            deberPendiente = id
            return .tareas
        case "nota":
            if url.lastPathComponent == "nueva" {
                let cuaderno = URLComponents(url: url, resolvingAgainstBaseURL: false)?
                    .queryItems?.first { $0.name == "cuaderno" }?.value
                    .flatMap { UUID(uuidString: $0) }
                nuevaNota = PeticionNuevaNota(cuaderno: cuaderno)
            } else if let id = UUID(uuidString: url.lastPathComponent) {
                notaPendiente = id
            } else {
                return nil
            }
            return .notas
        default:
            return nil
        }
    }
}

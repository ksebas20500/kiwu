import SwiftUI
import CoreData
import Combine

struct ContentView: View {
    @FetchRequest(
        sortDescriptors: [NSSortDescriptor(keyPath: \Materia.orden, ascending: true), NSSortDescriptor(keyPath: \Materia.nombre, ascending: true)],
        animation: .default
    ) private var materias: FetchedResults<Materia>

    @FetchRequest(
        sortDescriptors: [NSSortDescriptor(keyPath: \Deber.fechaEntrega, ascending: true)],
        animation: .default
    ) private var deberes: FetchedResults<Deber>

    @ObservedObject private var enrutador = Enrutador.shared
    @State private var sidebar: SidebarItem = .hoy
    @State private var deberSeleccionado: UUID?
    @State private var vistaMateria: VistaMateria = .tareas
    @State private var mostrandoNuevaMateria = false
    @State private var materiaAEliminar: Materia?

    private let materiaService = MateriaService(persistence: .shared, reminders: ReminderService())
    private let deberService = DeberService(persistence: .shared, reminders: ReminderService())
    private var cal: Calendar { .current }

    // MARK: Filtrado

    private var materiaActual: Materia? {
        if case .materia(let id) = sidebar { return materias.first { $0.id == id } }
        return nil
    }

    private var deberActual: Deber? {
        deberes.first { $0.id == deberSeleccionado }
    }

    private func enAlcance(_ deber: Deber, _ item: SidebarItem) -> Bool {
        switch item {
        case .todos, .calendario:
            return true
        case .completadas:
            return deber.completado
        case .materia(let id):
            return deber.materia.id == id
        case .hoy:
            guard let fecha = deber.fechaEntrega else { return false }
            return fecha <= cal.finDelDia(.now)
        case .proximos:
            guard let fecha = deber.fechaEntrega,
                  let limite = cal.date(byAdding: .day, value: 6, to: .now) else { return false }
            return fecha >= cal.startOfDay(for: .now) && fecha <= cal.finDelDia(limite)
        }
    }

    private var pendientes: [Deber] {
        deberes
            .filter { !$0.completado && enAlcance($0, sidebar) }
            .sorted { ($0.fechaEntrega ?? .distantFuture) < ($1.fechaEntrega ?? .distantFuture) }
    }

    private var completados: [Deber] {
        let lista: [Deber]
        switch sidebar {
        case .hoy:
            lista = deberes.filter { $0.completado && ($0.fechaCompletado.map(cal.isDateInToday) ?? false) }
        case .proximos, .calendario:
            lista = []
        default:
            lista = deberes.filter { $0.completado && enAlcance($0, sidebar) }
        }
        return lista.sorted { ($0.fechaCompletado ?? .distantPast) > ($1.fechaCompletado ?? .distantPast) }
    }

    private func conteo(_ item: SidebarItem) -> Int {
        switch item {
        case .calendario, .completadas:
            return 0
        default:
            return deberes.filter { !$0.completado && enAlcance($0, item) }.count
        }
    }

    private var titulo: String {
        switch sidebar {
        case .hoy: return "Hoy"
        case .proximos: return "Próximos 7 días"
        case .calendario: return "Calendario"
        case .todos: return "Todas las tareas"
        case .completadas: return "Completadas"
        case .materia: return materiaActual?.nombre ?? "Lista"
        }
    }

    // MARK: Vista

    var body: some View {
        HStack(spacing: 0) {
            SidebarView(
                seleccion: Binding(get: { sidebar }, set: { sidebar = $0; deberSeleccionado = nil; vistaMateria = .tareas }),
                materias: Array(materias),
                conteo: conteo,
                onNuevaMateria: { mostrandoNuevaMateria = true },
                onEliminarMateria: { materiaAEliminar = $0 }
            )
            .frame(width: 230)
            Divider()

            if sidebar == .calendario {
                CalendarView(
                    deberes: Array(deberes),
                    materias: Array(materias),
                    seleccion: $deberSeleccionado,
                    service: deberService,
                    onCrear: { titulo, materia, fecha in
                        deberSeleccionado = deberService.crear(titulo: titulo, en: materia, fecha: fecha).id
                    }
                )
                if let deber = deberActual {
                    Divider()
                    detalle(deber).frame(width: 400)
                }
            } else if let materia = materiaActual, vistaMateria == .archivos {
                CarpetaMateriaView(materia: materia, vista: $vistaMateria, service: materiaService)
            } else {
                TaskListView(
                    titulo: titulo,
                    pendientes: pendientes,
                    completados: completados,
                    materias: Array(materias),
                    materiaFija: materiaActual,
                    mostrarMateria: materiaActual == nil,
                    permiteCrear: sidebar != .completadas,
                    seleccion: $deberSeleccionado,
                    service: deberService,
                    onCrear: crear,
                    onEliminar: eliminar,
                    vista: materiaActual == nil ? nil : $vistaMateria,
                    carpetaAlerta: materiaActual.map { $0.bookmarkCarpeta != nil && !$0.carpetaDisponible } ?? false
                )
                .frame(width: 360)
                Divider()
                if let deber = deberActual {
                    detalle(deber)
                } else {
                    EmptyStateView(titulo: "Selecciona una tarea", icono: "checklist")
                }
            }
        }
        .frame(minWidth: 1000, minHeight: 620)
        .onReceive(enrutador.$diaCalendario.compactMap { $0 }) { _ in
            sidebar = .calendario
            deberSeleccionado = nil
        }
        .onReceive(enrutador.$deberPendiente.compactMap { $0 }) { id in
            sidebar = .todos
            deberSeleccionado = id
            enrutador.deberPendiente = nil
        }
        .sheet(isPresented: $mostrandoNuevaMateria) {
            NuevaMateriaView { nombre in
                sidebar = .materia(materiaService.crear(nombre: nombre).id)
                deberSeleccionado = nil
            }
        }
        .confirmationDialog(
            "¿Eliminar \(materiaAEliminar?.nombre ?? "la lista")?",
            isPresented: Binding(get: { materiaAEliminar != nil }, set: { if !$0 { materiaAEliminar = nil } }),
            titleVisibility: .visible
        ) {
            Button("Eliminar lista y sus tareas", role: .destructive) {
                if let materia = materiaAEliminar {
                    sidebar = .todos
                    deberSeleccionado = nil
                    materiaService.eliminar(materia)
                }
                materiaAEliminar = nil
            }
        } message: {
            Text("Se eliminarán también todas sus tareas. Los archivos del Finder no se tocan.")
        }
    }

    private func detalle(_ deber: Deber) -> some View {
        TaskDetailView(
            deber: deber,
            materias: Array(materias),
            service: deberService,
            onDelete: { eliminar(deber) }
        )
        .id(deber.id)
    }

    private func crear(_ titulo: String, en materia: Materia) {
        let fecha: Date? = sidebar == .hoy ? cal.date(bySettingHour: 18, minute: 0, second: 0, of: .now) : nil
        deberService.crear(titulo: titulo, en: materia, fecha: fecha)
    }

    private func eliminar(_ deber: Deber) {
        if deberSeleccionado == deber.id { deberSeleccionado = nil }
        deberService.eliminar(deber)
    }
}

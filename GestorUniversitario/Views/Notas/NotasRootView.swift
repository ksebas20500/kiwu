import SwiftUI
import CoreData
import Combine

enum NotasSidebarItem: Hashable {
    case todas, favoritas
    case cuaderno(UUID)
}

private enum HojaCuaderno: Identifiable {
    case nuevo
    case editar(Cuaderno)

    var id: String {
        switch self {
        case .nuevo: return "nuevo"
        case .editar(let cuaderno): return cuaderno.id.uuidString
        }
    }
}

struct NotasRootView: View {
    @FetchRequest(
        sortDescriptors: [NSSortDescriptor(keyPath: \Cuaderno.orden, ascending: true), NSSortDescriptor(keyPath: \Cuaderno.nombre, ascending: true)],
        animation: .default
    ) private var cuadernos: FetchedResults<Cuaderno>

    @FetchRequest(
        sortDescriptors: [NSSortDescriptor(keyPath: \Pagina.fechaActualizacion, ascending: false)],
        animation: .default
    ) private var paginas: FetchedResults<Pagina>

    @ObservedObject private var enrutador = Enrutador.shared
    @State private var sidebar: NotasSidebarItem = .todas
    @State private var paginaSeleccionada: UUID?
    @State private var busqueda = ""
    @State private var hoja: HojaCuaderno?
    @State private var cuadernoAEliminar: Cuaderno?

    private let service = CuadernoService(persistence: .shared)

    private var cuadernoActual: Cuaderno? {
        if case .cuaderno(let id) = sidebar { return cuadernos.first { $0.id == id } }
        return nil
    }

    private var paginaActual: Pagina? {
        paginas.first { $0.id == paginaSeleccionada }
    }

    private var enAlcance: [Pagina] {
        paginas.filter { pagina in
            switch sidebar {
            case .todas: return true
            case .favoritas: return pagina.favorita
            case .cuaderno(let id): return pagina.cuaderno.id == id
            }
        }
    }

    private var visibles: [Pagina] {
        let texto = busqueda.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !texto.isEmpty else { return enAlcance }
        return enAlcance.filter { $0.titulo.localizedStandardContains(texto) || $0.textoPlano.localizedStandardContains(texto) }
    }

    private var titulo: String {
        switch sidebar {
        case .todas: return "Todas las notas"
        case .favoritas: return "Favoritas"
        case .cuaderno: return cuadernoActual?.nombre ?? "Cuaderno"
        }
    }

    private func conteo(_ item: NotasSidebarItem) -> Int {
        switch item {
        case .todas: return paginas.count
        case .favoritas: return paginas.filter(\.favorita).count
        case .cuaderno(let id): return paginas.filter { $0.cuaderno.id == id }.count
        }
    }

    var body: some View {
        HStack(spacing: 0) {
            NotasSidebarView(
                seleccion: Binding(get: { sidebar }, set: { sidebar = $0; paginaSeleccionada = nil }),
                cuadernos: Array(cuadernos),
                conteo: conteo,
                onNuevo: { hoja = .nuevo },
                onEditar: { hoja = .editar($0) },
                onEliminar: { cuadernoAEliminar = $0 }
            )
            .frame(width: 230)
            Divider()

            PaginaListView(
                titulo: titulo,
                paginas: visibles,
                mostrarCuaderno: cuadernoActual == nil,
                busqueda: $busqueda,
                seleccion: $paginaSeleccionada,
                puedeCrear: !cuadernos.isEmpty,
                onNueva: crearPagina,
                onEliminar: eliminar
            )
            .frame(width: 340)
            Divider()

            if let pagina = paginaActual {
                PaginaDetailView(
                    pagina: pagina,
                    cuadernos: Array(cuadernos),
                    service: service,
                    onDelete: { eliminar(pagina) }
                )
                .id(pagina.id)
            } else {
                EmptyStateView(titulo: "Selecciona o crea una nota", icono: "note.text")
            }
        }
        .onAppear(perform: asegurarCuadernoInicial)
        .onReceive(enrutador.$notaPendiente.compactMap { $0 }) { id in
            sidebar = .todas
            paginaSeleccionada = id
            enrutador.notaPendiente = nil
        }
        .onReceive(enrutador.$nuevaNota.compactMap { $0 }) { peticion in
            let destino = peticion.cuaderno.flatMap { service.cuaderno(id: $0) } ?? service.primerCuaderno()
            // Si ya hay una nota en blanco, se reutiliza: pulsar varias veces no llena la lista de notas vacías.
            let pagina = service.paginaEnBlanco(en: destino) ?? service.crearPagina(en: destino)
            sidebar = .cuaderno(destino.id)
            paginaSeleccionada = pagina.id
            enrutador.nuevaNota = nil
        }
        .sheet(item: $hoja) { hoja in
            switch hoja {
            case .nuevo:
                CuadernoFormView(titulo: "Nuevo cuaderno", nombre: "", emoji: "📓") { nombre, emoji in
                    sidebar = .cuaderno(service.crearCuaderno(nombre: nombre, icono: emoji, orden: cuadernos.count).id)
                    paginaSeleccionada = nil
                }
            case .editar(let cuaderno):
                CuadernoFormView(titulo: "Editar cuaderno", nombre: cuaderno.nombre, emoji: cuaderno.icono) { nombre, emoji in
                    service.editar(cuaderno, nombre: nombre, icono: emoji)
                }
            }
        }
        .confirmationDialog(
            "¿Eliminar \(cuadernoAEliminar?.nombre ?? "el cuaderno")?",
            isPresented: Binding(get: { cuadernoAEliminar != nil }, set: { if !$0 { cuadernoAEliminar = nil } }),
            titleVisibility: .visible
        ) {
            Button("Eliminar cuaderno y sus notas", role: .destructive) {
                if let cuaderno = cuadernoAEliminar {
                    sidebar = .todas
                    paginaSeleccionada = nil
                    service.eliminar(cuaderno)
                }
                cuadernoAEliminar = nil
            }
        } message: {
            Text("Se eliminarán también todas las notas del cuaderno. Los archivos adjuntos del Finder no se tocan.")
        }
    }

    private func asegurarCuadernoInicial() {
        _ = service.primerCuaderno()
    }

    private func crearPagina() {
        guard let destino = cuadernoActual ?? cuadernos.first else { return }
        paginaSeleccionada = service.crearPagina(en: destino).id
    }

    private func eliminar(_ pagina: Pagina) {
        if paginaSeleccionada == pagina.id { paginaSeleccionada = nil }
        service.eliminar(pagina)
    }
}

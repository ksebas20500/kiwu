import SwiftUI
import UniformTypeIdentifiers

enum VistaMateria: String, CaseIterable, Identifiable {
    case tareas, archivos
    var id: String { rawValue }
}

/// Selector "Tareas | Archivos" de una lista.
struct VistaMateriaPicker: View {
    @Binding var vista: VistaMateria
    var carpetaAlerta = false

    var body: some View {
        Picker("", selection: $vista) {
            Text("Tareas").tag(VistaMateria.tareas)
            Text(carpetaAlerta ? "Archivos ⚠︎" : "Archivos").tag(VistaMateria.archivos)
        }
        .pickerStyle(.segmented)
        .labelsHidden()
        .frame(width: 200)
    }
}

/// Explorador de la carpeta del Finder vinculada a una lista. Los archivos no se copian:
/// se leen de su ubicación original y se abren con su aplicación predeterminada.
struct CarpetaMateriaView: View {
    @ObservedObject var materia: Materia
    @Binding var vista: VistaMateria
    let service: MateriaService

    @State private var raiz: URL?
    @State private var ruta: [URL] = []
    @State private var elementos: [ElementoCarpeta] = []
    @State private var error: String?
    @State private var busqueda = ""
    @State private var seleccion: URL?
    @State private var accesoActivo = false

    private let finder = FinderService()

    private var actual: URL? { ruta.last ?? raiz }
    private var enlazada: Bool { materia.bookmarkCarpeta != nil }
    private var alerta: Bool { enlazada && !materia.carpetaDisponible }

    private var visibles: [ElementoCarpeta] {
        let texto = busqueda.trimmingCharacters(in: .whitespacesAndNewlines)
        return texto.isEmpty ? elementos : elementos.filter { $0.nombre.localizedStandardContains(texto) }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            cabecera
            Divider()
            if !enlazada {
                sinCarpeta
            } else if alerta || raiz == nil {
                noDisponible
            } else {
                explorador
            }
        }
        .onAppear(perform: abrir)
        .onDisappear(perform: cerrar)
        .onChange(of: materia.bookmarkCarpeta) { _ in abrir() }
    }

    // MARK: Cabecera

    private var cabecera: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                Text(materia.nombre)
                    .font(.system(size: 26, weight: .bold))
                    .lineLimit(1)
                Spacer()
                if enlazada {
                    Menu {
                        Button("Abrir en el Finder") { abrirEnFinder() }.disabled(alerta || actual == nil)
                        Button("Actualizar") { cargar() }.disabled(alerta || actual == nil)
                        Divider()
                        Button("Cambiar carpeta…") { elegir() }
                        Button("Desvincular carpeta", role: .destructive) {
                            service.desvincularCarpeta(materia)
                        }
                    } label: {
                        Image(systemName: "ellipsis.circle").font(.system(size: 18))
                    }
                    .menuStyle(.borderlessButton)
                    .fixedSize()
                }
            }
            VistaMateriaPicker(vista: $vista, carpetaAlerta: alerta)

            if enlazada, !alerta, let raiz {
                migas(raiz)
            }
        }
        .padding(.horizontal, 20)
        .padding(.top, 20)
        .padding(.bottom, 12)
    }

    private func migas(_ raiz: URL) -> some View {
        HStack(spacing: 4) {
            Button {
                ruta = []
                cargar()
            } label: {
                Label(raiz.lastPathComponent, systemImage: "folder")
            }
            .buttonStyle(.plain)
            .foregroundColor(ruta.isEmpty ? .primary : .accentColor)

            ForEach(Array(ruta.enumerated()), id: \.offset) { indice, url in
                Image(systemName: "chevron.right").font(.caption2).foregroundStyle(.secondary)
                Button(url.lastPathComponent) {
                    ruta = Array(ruta.prefix(indice + 1))
                    cargar()
                }
                .buttonStyle(.plain)
                .foregroundColor(indice == ruta.count - 1 ? .primary : .accentColor)
            }
            Spacer()
        }
        .font(.callout)
        .lineLimit(1)
    }

    // MARK: Estados

    private var sinCarpeta: some View {
        VStack(spacing: 14) {
            Image(systemName: "folder.badge.plus")
                .font(.system(size: 44))
                .foregroundStyle(.secondary)
            Text("Vincula una carpeta del Finder")
                .font(.title3.weight(.semibold))
            Text("Junta aquí los PDF, Word y demás archivos de esta materia. No se copian: se leen desde su carpeta original.")
                .font(.callout)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
                .frame(maxWidth: 380)
            Button("Elegir carpeta…", action: elegir)
                .buttonStyle(.borderedProminent)
                .controlSize(.large)
            Text("También puedes arrastrar la carpeta hasta aquí.")
                .font(.caption)
                .foregroundStyle(.tertiary)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .onDrop(of: [.fileURL], isTargeted: nil) { proveedores in
            guard let proveedor = proveedores.first else { return false }
            _ = proveedor.loadObject(ofClass: URL.self) { url, _ in
                guard let url, (try? url.resourceValues(forKeys: [.isDirectoryKey]))?.isDirectory == true else { return }
                DispatchQueue.main.async { service.vincularCarpeta(materia, url) }
            }
            return true
        }
    }

    private var noDisponible: some View {
        VStack(spacing: 14) {
            Image(systemName: "exclamationmark.triangle.fill")
                .font(.system(size: 40))
                .foregroundColor(.orange)
            Text("No se encuentra la carpeta")
                .font(.title3.weight(.semibold))
            Text("Se movió, se renombró o ya no está disponible. Vuelve a seleccionarla o desvincúlala.")
                .font(.callout)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
                .frame(maxWidth: 380)
            HStack {
                Button("Volver a seleccionar…", action: elegir)
                    .buttonStyle(.borderedProminent)
                Button("Desvincular", role: .destructive) { service.desvincularCarpeta(materia) }
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    // MARK: Explorador

    private var explorador: some View {
        VStack(spacing: 0) {
            HStack(spacing: 8) {
                Button {
                    ruta.removeLast()
                    cargar()
                } label: {
                    Image(systemName: "chevron.left")
                }
                .buttonStyle(.plain)
                .disabled(ruta.isEmpty)
                .help("Carpeta anterior")

                Image(systemName: "magnifyingglass").foregroundStyle(.secondary)
                TextField("Buscar en esta carpeta", text: $busqueda)
                    .textFieldStyle(.plain)
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
            .background(RoundedRectangle(cornerRadius: 8).fill(Color.primary.opacity(0.06)))
            .padding(.horizontal, 20)
            .padding(.vertical, 10)

            ScrollView {
                LazyVStack(spacing: 2) {
                    ForEach(visibles) { elemento in
                        fila(elemento)
                            .onTapGesture(count: 2) { abrirElemento(elemento) }
                            .onTapGesture { seleccion = elemento.url }
                    }
                }
                .padding(.horizontal, 12)
                .padding(.bottom, 20)
            }
            .overlay {
                if let error {
                    EmptyStateView(titulo: error, icono: "exclamationmark.triangle")
                } else if visibles.isEmpty {
                    EmptyStateView(titulo: busqueda.isEmpty ? "Carpeta vacía" : "Sin resultados", icono: "folder")
                }
            }
        }
    }

    private func fila(_ elemento: ElementoCarpeta) -> some View {
        HStack(spacing: 12) {
            Image(nsImage: NSWorkspace.shared.icon(forFile: elemento.url.path))
                .resizable()
                .frame(width: 28, height: 28)
            Text(elemento.nombre)
                .lineLimit(1)
            Spacer(minLength: 8)
            if let modificado = elemento.modificado {
                Text(modificado, format: .dateTime.day().month(.abbreviated).year())
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            if !elemento.esCarpeta, let tamano = elemento.tamano {
                Text(ByteCountFormatter.string(fromByteCount: tamano, countStyle: .file))
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    .frame(width: 70, alignment: .trailing)
            } else if elemento.esCarpeta {
                Image(systemName: "chevron.right")
                    .font(.caption)
                    .foregroundStyle(.tertiary)
                    .frame(width: 70, alignment: .trailing)
            }
        }
        .padding(.horizontal, 10)
        .padding(.vertical, 6)
        .background(RoundedRectangle(cornerRadius: 8).fill(seleccion == elemento.url ? Color.primary.opacity(0.08) : .clear))
        .contentShape(Rectangle())
        .contextMenu {
            Button(elemento.esCarpeta ? "Abrir carpeta" : "Abrir") { abrirElemento(elemento) }
            Button("Mostrar en el Finder") { NSWorkspace.shared.activateFileViewerSelecting([elemento.url]) }
        }
    }

    // MARK: Acciones

    private func elegir() {
        guard let url = finder.elegirCarpeta(mensaje: "Elige la carpeta de \(materia.nombre)") else { return }
        service.vincularCarpeta(materia, url)
    }

    private func abrirElemento(_ elemento: ElementoCarpeta) {
        if elemento.esCarpeta {
            ruta.append(elemento.url)
            busqueda = ""
            cargar()
        } else {
            finder.open(elemento.url)
        }
    }

    private func abrirEnFinder() {
        guard let actual else { return }
        finder.open(actual)
    }

    /// Resuelve el marcador y mantiene el acceso a la carpeta mientras la vista está abierta.
    private func abrir() {
        cerrar()
        guard let data = materia.bookmarkCarpeta else { return }
        guard let resuelto = try? finder.resolveBookmark(data),
              resuelto.url.startAccessingSecurityScopedResource() else {
            service.comprobarCarpeta(materia)
            return
        }
        accesoActivo = true
        raiz = resuelto.url
        ruta = []
        busqueda = ""
        service.comprobarCarpeta(materia)
        cargar()
    }

    private func cerrar() {
        if accesoActivo { raiz?.stopAccessingSecurityScopedResource() }
        accesoActivo = false
        raiz = nil
        elementos = []
    }

    private func cargar() {
        guard let carpeta = actual else { return }
        do {
            elementos = try finder.listar(carpeta)
            error = nil
        } catch {
            elementos = []
            self.error = "No se pudo leer la carpeta"
            service.comprobarCarpeta(materia)
        }
    }
}

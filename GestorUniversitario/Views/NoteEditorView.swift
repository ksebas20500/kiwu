import SwiftUI
import Combine
import UniformTypeIdentifiers

final class NoteEditorModel: ObservableObject {
    @Published var doc: NoteDocument
    private let vigente: () -> Bool
    private let actual: () -> Data
    private let escribir: (NoteDocument, Data) -> Void
    private var cancellable: AnyCancellable?

    init(datos: Data, vigente: @escaping () -> Bool, actual: @escaping () -> Data, escribir: @escaping (NoteDocument, Data) -> Void) {
        self.vigente = vigente
        self.actual = actual
        self.escribir = escribir
        doc = NotaService.leer(datos)
        cancellable = $doc
            .dropFirst()
            .debounce(for: .seconds(0.5), scheduler: RunLoop.main)
            .sink { [weak self] _ in self?.guardar() }
    }

    convenience init(deber: Deber) {
        self.init(
            datos: deber.contenidoNota,
            vigente: { !deber.estaEliminado },
            actual: { deber.contenidoNota },
            escribir: { _, data in
                deber.contenidoNota = data
                deber.fechaActualizacion = .now
                PersistenceController.shared.save()
            }
        )
    }

    convenience init(pagina: Pagina) {
        self.init(
            datos: pagina.contenido,
            vigente: { !pagina.estaEliminado },
            actual: { pagina.contenido },
            escribir: { doc, data in
                pagina.contenido = data
                pagina.textoPlano = doc.textoPlano
                pagina.fechaActualizacion = .now
                PersistenceController.shared.save()
            }
        )
    }

    /// Inserta bloques tras el bloque `id` (sustituyéndolo si es un párrafo vacío),
    /// o antes del párrafo vacío final. Siempre deja un párrafo al final para seguir escribiendo.
    func insertar(_ bloques: [NoteBlock], despuesDe id: UUID? = nil) {
        if let id, let i = doc.blocks.firstIndex(where: { $0.id == id }) {
            let bloque = doc.blocks[i]
            if bloque.kind == .paragraph && bloque.text.isEmpty {
                doc.blocks.remove(at: i)
                doc.blocks.insert(contentsOf: bloques, at: i)
            } else {
                doc.blocks.insert(contentsOf: bloques, at: i + 1)
            }
        } else if let ultimo = doc.blocks.last, ultimo.kind == .paragraph, ultimo.text.isEmpty {
            doc.blocks.insert(contentsOf: bloques, at: doc.blocks.count - 1)
        } else {
            doc.blocks.append(contentsOf: bloques)
        }
        if doc.blocks.last?.kind != .paragraph { doc.blocks.append(NoteBlock()) }
    }

    func adjuntarArchivos(_ urls: [URL], despuesDe id: UUID? = nil) {
        let finder = FinderService()
        let bloques: [NoteBlock] = urls.compactMap { url in
            let marcador: Data
            do {
                marcador = try finder.makeBookmark(for: url)
            } catch {
                NSLog("No se pudo adjuntar \(url.lastPathComponent): \(error.localizedDescription)")
                return nil
            }
            var bloque = NoteBlock()
            bloque.kind = .file
            bloque.fileName = url.lastPathComponent
            bloque.bookmark = marcador
            return bloque
        }
        if !bloques.isEmpty { insertar(bloques, despuesDe: id) }
    }

    func elegirArchivos(despuesDe id: UUID? = nil) {
        let panel = NSOpenPanel()
        panel.allowedContentTypes = [.pdf, UTType(filenameExtension: "doc"), UTType(filenameExtension: "docx")].compactMap { $0 }
        panel.allowsMultipleSelection = true
        panel.message = "Elige los PDF o documentos de Word que quieres adjuntar"
        if panel.runModal() == .OK { adjuntarArchivos(panel.urls, despuesDe: id) }
    }

    func guardar() {
        guard vigente() else { return }
        let data = doc.data
        guard data != actual() else { return }
        escribir(doc, data)
    }
}

struct NoteEditorView: View {
    @ObservedObject var model: NoteEditorModel
    @FocusState private var enfoque: UUID?

    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            ForEach($model.doc.blocks) { $block in
                BlockRow(
                    block: $block,
                    numero: numero(de: block.id),
                    enfoque: $enfoque,
                    onEnter: { enter(block.id) },
                    onBorrar: { borrar(block.id) },
                    onEliminar: { quitar(block.id) },
                    onEspecial: { especial(block.id, $0) }
                )
            }

            Color.clear
                .frame(height: 100)
                .contentShape(Rectangle())
                .onTapGesture(perform: alFinal)
                .overlay(alignment: .topLeading) {
                    Menu {
                        ForEach(NoteBlockKind.allCases) { tipo in
                            Button {
                                agregar(tipo)
                            } label: {
                                Label(tipo.titulo, systemImage: tipo.icono)
                            }
                        }
                    } label: {
                        Label("Añadir bloque", systemImage: "plus")
                            .font(.caption)
                    }
                    .menuStyle(.borderlessButton)
                    .fixedSize()
                    .padding(.top, 8)
                }
        }
        .onDrop(of: [.fileURL], isTargeted: nil) { proveedores in
            for proveedor in proveedores {
                _ = proveedor.loadObject(ofClass: URL.self) { url, _ in
                    guard let url else { return }
                    DispatchQueue.main.async { model.adjuntarArchivos([url]) }
                }
            }
            return true
        }
    }

    private func especial(_ id: UUID, _ tipo: NoteBlockKind) {
        switch tipo {
        case .image:
            elegirImagen(despuesDe: id)
        case .file:
            model.elegirArchivos(despuesDe: id)
        case .divider:
            var divisor = NoteBlock()
            divisor.kind = .divider
            model.insertar([divisor], despuesDe: id)
        default:
            break
        }
    }

    private func quitar(_ id: UUID) {
        guard let i = model.doc.blocks.firstIndex(where: { $0.id == id }) else { return }
        if model.doc.blocks.count > 1 {
            model.doc.blocks.remove(at: i)
        } else {
            model.doc.blocks = [NoteBlock()]
        }
    }

    private func numero(de id: UUID) -> Int {
        var n = 0
        for bloque in model.doc.blocks {
            n = bloque.kind == .numberedList ? n + 1 : 0
            if bloque.id == id { return max(n, 1) }
        }
        return 1
    }

    private func enfocar(_ id: UUID) {
        DispatchQueue.main.async { enfoque = id }
    }

    private func enter(_ id: UUID) {
        guard let i = model.doc.blocks.firstIndex(where: { $0.id == id }) else { return }
        let actual = model.doc.blocks[i]
        var nuevo = NoteBlock()
        switch actual.kind {
        case .bulletedList, .numberedList, .checklist:
            if actual.text.isEmpty {
                model.doc.blocks[i].kind = .paragraph
                enfocar(id)
                return
            }
            nuevo.kind = actual.kind
        default:
            break
        }
        model.doc.blocks.insert(nuevo, at: i + 1)
        enfocar(nuevo.id)
    }

    private func borrar(_ id: UUID) {
        guard let i = model.doc.blocks.firstIndex(where: { $0.id == id }) else { return }
        if model.doc.blocks[i].kind != .paragraph {
            model.doc.blocks[i].kind = .paragraph
            enfocar(id)
            return
        }
        guard model.doc.blocks.count > 1 else { return }
        model.doc.blocks.remove(at: i)
        enfocar(model.doc.blocks[max(0, i - 1)].id)
    }

    private func alFinal() {
        if let ultimo = model.doc.blocks.last, ultimo.kind == .paragraph, ultimo.text.isEmpty {
            enfocar(ultimo.id)
        } else {
            let nuevo = NoteBlock()
            model.doc.blocks.append(nuevo)
            enfocar(nuevo.id)
        }
    }

    private func agregar(_ tipo: NoteBlockKind) {
        if tipo == .image {
            elegirImagen()
            return
        }
        if tipo == .file {
            model.elegirArchivos()
            return
        }
        if tipo != .divider, let ultimo = model.doc.blocks.last, ultimo.kind == .paragraph, ultimo.text.isEmpty {
            model.doc.blocks[model.doc.blocks.count - 1].kind = tipo
            enfocar(ultimo.id)
            return
        }
        var bloque = NoteBlock()
        bloque.kind = tipo
        model.doc.blocks.append(bloque)
        if tipo == .divider {
            let siguiente = NoteBlock()
            model.doc.blocks.append(siguiente)
            enfocar(siguiente.id)
        } else {
            enfocar(bloque.id)
        }
    }

    private func elegirImagen(despuesDe id: UUID? = nil) {
        let panel = NSOpenPanel()
        panel.allowedContentTypes = [.image]
        panel.allowsMultipleSelection = false
        guard panel.runModal() == .OK, let url = panel.url,
              let data = NotaService.imagenOptimizada(from: url) else { return }

        var bloque = NoteBlock()
        bloque.kind = .image
        bloque.imageData = data
        model.insertar([bloque], despuesDe: id)
    }
}

private struct BlockRow: View {
    @Binding var block: NoteBlock
    let numero: Int
    var enfoque: FocusState<UUID?>.Binding
    let onEnter: () -> Void
    let onBorrar: () -> Void
    let onEliminar: () -> Void
    let onEspecial: (NoteBlockKind) -> Void

    @State private var hover = false

    var body: some View {
        HStack(alignment: .top, spacing: 6) {
            Menu {
                ForEach(NoteBlockKind.allCases.filter { $0 != .image && $0 != .file }) { tipo in
                    Button {
                        convertir(a: tipo)
                    } label: {
                        Label(tipo.titulo, systemImage: tipo.icono)
                    }
                }
                Divider()
                Button("Eliminar bloque", role: .destructive, action: onEliminar)
            } label: {
                Image(systemName: "line.3.horizontal")
                    .foregroundStyle(.secondary)
            }
            .menuStyle(.borderlessButton)
            .menuIndicator(.hidden)
            .fixedSize()
            .opacity(hover ? 1 : 0)
            .padding(.top, 3)

            contenido
                .overlay(alignment: .topLeading) {
                    if !opcionesSlash.isEmpty {
                        SlashMenu(opciones: opcionesSlash, onElegir: elegirSlash)
                            .offset(y: 30)
                    }
                }
        }
        .zIndex(opcionesSlash.isEmpty ? 0 : 10)
        .onHover { hover = $0 }
    }

    // MARK: Menú "/"

    private var opcionesSlash: [NoteBlockKind] {
        guard block.kind == .paragraph, block.text.hasPrefix("/") else { return [] }
        let filtro = String(block.text.dropFirst())
        return NoteBlockKind.allCases.filter {
            $0 != .paragraph && (filtro.isEmpty || $0.titulo.localizedCaseInsensitiveContains(filtro))
        }
    }

    private func elegirSlash(_ tipo: NoteBlockKind) {
        block.text = ""
        switch tipo {
        case .image, .file, .divider:
            onEspecial(tipo)
        default:
            block.kind = tipo
            reenfocar()
        }
    }

    @ViewBuilder
    private var contenido: some View {
        switch block.kind {
        case .paragraph:
            campo("Escribe algo, o pulsa / para ver los bloques")
        case .heading:
            campo("Título")
                .font(.system(size: 21, weight: .bold))
                .padding(.top, 6)
        case .subheading:
            campo("Título 2")
                .font(.system(size: 17, weight: .semibold))
                .padding(.top, 4)
        case .callout:
            HStack(alignment: .top, spacing: 10) {
                Text("💡")
                campo("Escribe una nota destacada")
            }
            .padding(12)
            .background(RoundedRectangle(cornerRadius: 8).fill(Color.orange.opacity(0.14)))
            .padding(.vertical, 2)
        case .bulletedList:
            HStack(alignment: .top, spacing: 8) {
                Text("•").padding(.top, 2)
                campo("Elemento de lista")
            }
        case .numberedList:
            HStack(alignment: .top, spacing: 8) {
                Text("\(numero).")
                    .foregroundStyle(.secondary)
                    .padding(.top, 2)
                    .frame(minWidth: 18, alignment: .trailing)
                campo("Elemento de lista")
            }
        case .checklist:
            HStack(alignment: .top, spacing: 8) {
                CheckboxView(marcado: block.isChecked) { block.isChecked.toggle() }
                    .padding(.top, 1)
                campo("Tarea")
                    .strikethrough(block.isChecked)
                    .foregroundColor(block.isChecked ? .secondary : .primary)
            }
        case .quote:
            campo("Cita")
                .italic()
                .padding(.leading, 12)
                .overlay(alignment: .leading) {
                    Rectangle().fill(Color.secondary.opacity(0.6)).frame(width: 3)
                }
        case .code:
            TextField("Código", text: $block.text, axis: .vertical)
                .textFieldStyle(.plain)
                .font(.system(.body, design: .monospaced))
                .focused(enfoque, equals: block.id)
                .modifier(BorrarVacioModifier(texto: block.text, accion: onBorrar))
                .padding(10)
                .background(RoundedRectangle(cornerRadius: 6).fill(Color.primary.opacity(0.06)))
        case .image:
            if let data = block.imageData, let imagen = NSImage(data: data) {
                Image(nsImage: imagen)
                    .resizable()
                    .scaledToFit()
                    .frame(maxHeight: 380)
                    .clipShape(RoundedRectangle(cornerRadius: 8))
                    .padding(.vertical, 4)
            } else {
                Text("Imagen no disponible").foregroundStyle(.secondary)
            }
        case .file:
            FileBlockView(block: $block, onQuitar: onEliminar)
        case .divider:
            Divider().padding(.vertical, 10)
        }
    }

    private func campo(_ placeholder: String) -> some View {
        TextField(placeholder, text: $block.text)
            .textFieldStyle(.plain)
            .focused(enfoque, equals: block.id)
            .onSubmit {
                if let primero = opcionesSlash.first { elegirSlash(primero) } else { onEnter() }
            }
            .onChange(of: block.text) { aplicarAtajo($0) }
            .modifier(BorrarVacioModifier(texto: block.text, accion: onBorrar))
    }

    private func convertir(a tipo: NoteBlockKind) {
        block.kind = tipo
        if tipo == .divider { block.text = "" }
        reenfocar()
    }

    private func reenfocar() {
        let id = block.id
        DispatchQueue.main.async { enfoque.wrappedValue = id }
    }

    private func aplicarAtajo(_ texto: String) {
        guard block.kind == .paragraph else { return }
        let atajos: [(String, NoteBlockKind)] = [
            ("# ", .heading), ("## ", .subheading), ("- ", .bulletedList), ("* ", .bulletedList),
            ("1. ", .numberedList), ("[] ", .checklist), ("> ", .quote), ("```", .code)
        ]
        for (prefijo, tipo) in atajos where texto.hasPrefix(prefijo) {
            block.kind = tipo
            block.text = String(texto.dropFirst(prefijo.count))
            reenfocar()
            return
        }
    }
}

private struct SlashMenu: View {
    let opciones: [NoteBlockKind]
    let onElegir: (NoteBlockKind) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            ForEach(Array(opciones.prefix(8).enumerated()), id: \.element) { indice, tipo in
                Button { onElegir(tipo) } label: {
                    Label(tipo.titulo, systemImage: tipo.icono)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(.horizontal, 10)
                        .padding(.vertical, 6)
                        .background(indice == 0 ? Color.accentColor.opacity(0.15) : .clear)
                        .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
            }
        }
        .padding(4)
        .frame(width: 250)
        .background(RoundedRectangle(cornerRadius: 8).fill(Color(nsColor: .windowBackgroundColor)))
        .overlay(RoundedRectangle(cornerRadius: 8).stroke(Color.primary.opacity(0.15), lineWidth: 0.5))
        .shadow(color: .black.opacity(0.2), radius: 10, y: 4)
    }
}

/// Borrar en un bloque vacío lo convierte en texto o lo elimina (macOS 14+).
private struct BorrarVacioModifier: ViewModifier {
    let texto: String
    let accion: () -> Void

    func body(content: Content) -> some View {
        if #available(macOS 14.0, *) {
            content.onKeyPress(.delete) {
                if texto.isEmpty {
                    accion()
                    return .handled
                }
                return .ignored
            }
        } else {
            content
        }
    }
}

private struct FileBlockView: View {
    @Binding var block: NoteBlock
    let onQuitar: () -> Void

    @State private var disponible = true
    @State private var tamano: String?
    @State private var hover = false

    private let finder = FinderService()

    private var extensionArchivo: String {
        URL(fileURLWithPath: block.fileName ?? "").pathExtension.lowercased()
    }

    private var icono: (nombre: String, color: Color) {
        switch extensionArchivo {
        case "pdf": return ("doc.richtext.fill", .red)
        case "doc", "docx": return ("doc.text.fill", .blue)
        default: return ("doc.fill", .gray)
        }
    }

    var body: some View {
        HStack(spacing: 12) {
            HStack(spacing: 12) {
                Image(systemName: icono.nombre)
                    .font(.system(size: 24))
                    .foregroundColor(disponible ? icono.color : .gray)
                    .frame(width: 32)
                VStack(alignment: .leading, spacing: 2) {
                    Text(block.fileName ?? "Archivo")
                        .lineLimit(1)
                        .foregroundColor(disponible ? .primary : .red)
                    Text(disponible ? (tamano ?? extensionArchivo.uppercased()) : "No se encuentra el archivo. Vuelve a dejarlo en su sitio o elimina el adjunto.")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                        .lineLimit(2)
                }
                Spacer(minLength: 0)
            }
            .contentShape(Rectangle())
            .onTapGesture(perform: abrir)

            if hover {
                Button(action: mostrarEnFinder) { Image(systemName: "folder") }
                    .buttonStyle(.plain)
                    .help("Mostrar en Finder")
                    .disabled(!disponible)
                Button(action: onQuitar) { Image(systemName: "xmark") }
                    .buttonStyle(.plain)
                    .help("Quitar adjunto (no borra el archivo)")
            }
        }
        .padding(10)
        .background(RoundedRectangle(cornerRadius: 8).fill(Color.primary.opacity(0.05)))
        .overlay(RoundedRectangle(cornerRadius: 8).stroke(Color.primary.opacity(0.1), lineWidth: 0.5))
        .padding(.vertical, 2)
        .onHover { hover = $0 }
        .onAppear(perform: comprobar)
    }

    private func comprobar() {
        guard let data = block.bookmark, let resuelto = try? finder.resolveBookmark(data) else {
            disponible = false
            return
        }
        let url = resuelto.url
        let acceso = url.startAccessingSecurityScopedResource()
        defer { if acceso { url.stopAccessingSecurityScopedResource() } }

        disponible = FileManager.default.fileExists(atPath: url.path)
        if let bytes = (try? url.resourceValues(forKeys: [.fileSizeKey]))?.fileSize {
            tamano = ByteCountFormatter.string(fromByteCount: Int64(bytes), countStyle: .file)
        }
        if disponible, resuelto.isStale {
            block.bookmark = try? finder.makeBookmark(for: url)
        }
    }

    private func abrir() {
        guard let data = block.bookmark, let resuelto = try? finder.resolveBookmark(data) else {
            disponible = false
            return
        }
        let url = resuelto.url
        let acceso = url.startAccessingSecurityScopedResource()
        finder.open(url)
        if acceso {
            DispatchQueue.main.asyncAfter(deadline: .now() + 5) { url.stopAccessingSecurityScopedResource() }
        }
    }

    private func mostrarEnFinder() {
        guard let data = block.bookmark, let resuelto = try? finder.resolveBookmark(data) else { return }
        let url = resuelto.url
        let acceso = url.startAccessingSecurityScopedResource()
        NSWorkspace.shared.activateFileViewerSelecting([url])
        if acceso {
            DispatchQueue.main.asyncAfter(deadline: .now() + 5) { url.stopAccessingSecurityScopedResource() }
        }
    }
}

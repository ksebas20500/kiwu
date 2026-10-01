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

/// Operaciones que una fila de bloque pide al editor (ya atadas al id del bloque).
private struct OpsBloque {
    var enter: (String, [TextRun]?) -> Void
    var borrar: () -> Void
    var eliminar: () -> Void
    var especial: (NoteBlockKind) -> Void
    var navegar: (Int) -> Bool
    var mover: (Int) -> Void
    var duplicar: () -> Void
    var aEnlace: (String) -> Void
}

struct NoteEditorView: View {
    @ObservedObject var model: NoteEditorModel
    @StateObject private var enfoque = EnfoqueCtl()

    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            ForEach($model.doc.blocks) { $block in
                let id = block.id
                BlockRow(
                    block: $block,
                    numero: numero(de: id),
                    enfoque: enfoque,
                    ops: OpsBloque(
                        enter: { enter(id, resto: $0, runs: $1) },
                        borrar: { borrar(id) },
                        eliminar: { quitar(id) },
                        especial: { especial(id, $0) },
                        navegar: { navegar(id, $0) },
                        mover: { mover(id, $0) },
                        duplicar: { duplicar(id) },
                        aEnlace: { aEnlace(id, $0) }
                    )
                )
            }

            Color.clear
                .frame(height: 240)
                .contentShape(Rectangle())
                .onTapGesture(perform: alFinal)
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

    private func indice(_ id: UUID) -> Int? { model.doc.blocks.firstIndex { $0.id == id } }

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
            if let i = indice(id), i + 1 < model.doc.blocks.count {
                enfoque.pedir(model.doc.blocks[model.doc.blocks.count - 1].id, alFinal: false)
            }
        default:
            break
        }
    }

    private func quitar(_ id: UUID) {
        guard let i = indice(id) else { return }
        if model.doc.blocks.count > 1 {
            model.doc.blocks.remove(at: i)
        } else {
            model.doc.blocks = [NoteBlock()]
        }
    }

    /// Numeración de las listas, con un contador por nivel de sangría.
    private func numero(de id: UUID) -> Int {
        var contadores = Array(repeating: 0, count: 5)
        for bloque in model.doc.blocks {
            let nivel = min(max(bloque.indent, 0), 4)
            if bloque.kind == .numberedList {
                contadores[nivel] += 1
                for mayor in (nivel + 1)..<5 { contadores[mayor] = 0 }
            } else {
                contadores = Array(repeating: 0, count: 5)
            }
            if bloque.id == id { return max(contadores[nivel], 1) }
        }
        return 1
    }

    private func puedeEscribirse(_ tipo: NoteBlockKind) -> Bool {
        ![.image, .file, .divider, .embed].contains(tipo)
    }

    private func navegar(_ id: UUID, _ direccion: Int) -> Bool {
        guard var i = indice(id) else { return false }
        i += direccion
        while model.doc.blocks.indices.contains(i) {
            if puedeEscribirse(model.doc.blocks[i].kind) {
                enfoque.pedir(model.doc.blocks[i].id, alFinal: direccion < 0)
                return true
            }
            i += direccion
        }
        return false
    }

    private func mover(_ id: UUID, _ delta: Int) {
        guard let i = indice(id), model.doc.blocks.indices.contains(i + delta) else { return }
        withAnimation(.easeInOut(duration: 0.15)) {
            model.doc.blocks.swapAt(i, i + delta)
        }
    }

    private func duplicar(_ id: UUID) {
        guard let i = indice(id) else { return }
        var copia = model.doc.blocks[i]
        copia.id = UUID()
        model.doc.blocks.insert(copia, at: i + 1)
    }

    private func aEnlace(_ id: UUID, _ texto: String) {
        guard let i = indice(id), let url = ServicioEnlaces.url(desde: texto) else { return }
        model.doc.blocks[i].kind = .embed
        model.doc.blocks[i].text = ""
        model.doc.blocks[i].runs = nil
        model.doc.blocks[i].url = url.absoluteString
        model.doc.blocks[i].preview = nil
        if model.doc.blocks.last?.kind != .paragraph { model.doc.blocks.append(NoteBlock()) }
    }

    private func enter(_ id: UUID, resto: String, runs: [TextRun]?) {
        guard let i = indice(id) else { return }
        let actual = model.doc.blocks[i]

        // Una dirección web sola en un párrafo se convierte en enlace con vista previa.
        if actual.kind == .paragraph, resto.isEmpty, actual.indent == 0,
           actual.text.hasPrefix("http://") || actual.text.hasPrefix("https://") || actual.text.hasPrefix("www."),
           ServicioEnlaces.url(desde: actual.text) != nil {
            aEnlace(id, actual.text)
            if let siguiente = model.doc.blocks.indices.contains(i + 1) ? model.doc.blocks[i + 1] : nil {
                enfoque.pedir(siguiente.id, alFinal: false)
            }
            return
        }

        var nuevo = NoteBlock()
        switch actual.kind {
        case .bulletedList, .numberedList, .checklist:
            if actual.text.isEmpty && resto.isEmpty {
                if actual.indent > 0 {
                    model.doc.blocks[i].indent -= 1
                } else {
                    model.doc.blocks[i].kind = .paragraph
                }
                enfoque.pedir(id)
                return
            }
            nuevo.kind = actual.kind
            nuevo.indent = actual.indent
        default:
            break
        }
        nuevo.text = resto
        nuevo.runs = runs
        model.doc.blocks.insert(nuevo, at: i + 1)
        enfoque.pedir(nuevo.id, alFinal: false)
    }

    private func borrar(_ id: UUID) {
        guard let i = indice(id) else { return }
        if model.doc.blocks[i].indent > 0 {
            model.doc.blocks[i].indent -= 1
            enfoque.pedir(id)
            return
        }
        if model.doc.blocks[i].kind != .paragraph {
            model.doc.blocks[i].kind = .paragraph
            enfoque.pedir(id)
            return
        }
        guard model.doc.blocks.count > 1 else { return }
        model.doc.blocks.remove(at: i)
        enfoque.pedir(model.doc.blocks[max(0, i - 1)].id)
    }

    private func alFinal() {
        if let ultimo = model.doc.blocks.last, ultimo.kind == .paragraph, ultimo.text.isEmpty {
            enfoque.pedir(ultimo.id)
        } else {
            let nuevo = NoteBlock()
            model.doc.blocks.append(nuevo)
            enfoque.pedir(nuevo.id)
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

// MARK: - Fila de bloque

private struct BlockRow: View {
    @Binding var block: NoteBlock
    let numero: Int
    @ObservedObject var enfoque: EnfoqueCtl
    let ops: OpsBloque

    @Environment(\.colorScheme) private var esquema
    @State private var indiceSlash = 0
    @State private var slashDescartado = false
    @State private var haySeleccion = false
    @State private var pidiendoEnlace = false

    var body: some View {
        contenido
            .padding(.leading, CGFloat(block.indent) * 26)
            .overlay(alignment: .topLeading) {
                if !opcionesSlash.isEmpty {
                    SlashMenu(opciones: opcionesSlash, seleccionado: indiceSlash, onElegir: elegirSlash, onPasar: { indiceSlash = $0 })
                        .offset(y: 30)
                }
            }
            .overlay(alignment: .topLeading) {
                if haySeleccion {
                    BarraFormato(pidiendoEnlace: $pidiendoEnlace)
                        .offset(y: -44)
                }
            }
            .zIndex(opcionesSlash.isEmpty && !haySeleccion ? 0 : 10)
            .onChange(of: block.text) { _ in
                indiceSlash = 0
                slashDescartado = false
                aplicarAtajo(block.text)
            }
            .onReceive(NotificationCenter.default.publisher(for: .campoActivo)) { nota in
                if (nota.object as? CampoTexto)?.bloqueID != block.id { haySeleccion = false }
            }
    }

    // MARK: Menú "/"

    private var opcionesSlash: [NoteBlockKind] {
        guard !slashDescartado, block.kind == .paragraph, block.text.hasPrefix("/"), !block.text.contains(" ") || block.text.count < 14 else { return [] }
        let filtro = String(block.text.dropFirst())
        return NoteBlockKind.allCases.filter {
            $0 != .paragraph && (filtro.isEmpty || $0.titulo.localizedCaseInsensitiveContains(filtro))
        }
    }

    private func elegirSlash(_ tipo: NoteBlockKind) {
        block.text = ""
        block.runs = nil
        switch tipo {
        case .image, .file, .divider:
            ops.especial(tipo)
        default:
            block.kind = tipo
            if tipo != .embed { enfoque.pedir(block.id) }
        }
    }

    // MARK: Acciones del campo de texto

    private var acciones: AccionesCampo {
        AccionesCampo(
            enter: { resto, runs in
                if opcionesSlash.indices.contains(indiceSlash) {
                    elegirSlash(opcionesSlash[indiceSlash])
                } else {
                    ops.enter(resto, runs)
                }
            },
            borrarVacio: ops.borrar,
            retrocederInicio: {
                if block.indent > 0 { block.indent -= 1; return true }
                if block.kind != .paragraph { block.kind = .paragraph; return true }
                return false
            },
            navegar: ops.navegar,
            slashMover: { delta in
                let n = opcionesSlash.count
                guard n > 0 else { return }
                indiceSlash = min(max(indiceSlash + delta, 0), n - 1)
            },
            tab: { delta in block.indent = min(max(block.indent + delta, 0), 4) },
            escape: { if !opcionesSlash.isEmpty { slashDescartado = true } },
            seleccion: { haySeleccion = $0 },
            enlaceSolo: ops.aEnlace,
            pedirEnlace: { if haySeleccion { pidiendoEnlace = true } },
            menu: menuBloque
        )
    }

    private func menuBloque() -> [ItemMenuBloque] {
        let convertibles = NoteBlockKind.allCases.filter { $0 != .image && $0 != .file && $0 != .divider }
        return [
            ItemMenuBloque(titulo: "Convertir en", icono: "arrow.triangle.2.circlepath",
                           sub: convertibles.map { tipo in ItemMenuBloque(titulo: tipo.titulo, icono: tipo.icono) { convertir(a: tipo) } }),
            ItemMenuBloque(titulo: "Aumentar sangría", icono: "increase.indent") { block.indent = min(block.indent + 1, 4) },
            ItemMenuBloque(titulo: "Reducir sangría", icono: "decrease.indent") { block.indent = max(block.indent - 1, 0) },
            ItemMenuBloque(titulo: "Mover arriba", icono: "arrow.up") { ops.mover(-1) },
            ItemMenuBloque(titulo: "Mover abajo", icono: "arrow.down") { ops.mover(1) },
            ItemMenuBloque(titulo: "Duplicar", icono: "plus.square.on.square", accion: ops.duplicar),
            ItemMenuBloque(titulo: "Eliminar bloque", icono: "trash", destructivo: true, accion: ops.eliminar)
        ]
    }

    private func convertir(a tipo: NoteBlockKind) {
        if tipo == .embed {
            block.url = ServicioEnlaces.url(desde: block.text)?.absoluteString
            block.preview = nil
            block.text = ""
            block.runs = nil
        }
        block.kind = tipo
        if tipo != .embed { enfoque.pedir(block.id) }
    }

    private func campo(_ placeholder: String, fuente: NSFont = .systemFont(ofSize: 14), color: NSColor = .labelColor, tachado: Bool = false) -> some View {
        CampoRico(
            texto: $block.text,
            runs: $block.runs,
            id: block.id,
            estilo: EstiloBloque(fuente: fuente, color: color, tachado: tachado),
            placeholder: placeholder,
            enfoque: enfoque,
            slashActivo: !opcionesSlash.isEmpty,
            acciones: acciones
        )
    }

    private func cursiva(_ fuente: NSFont) -> NSFont { NSFontManager.shared.convert(fuente, toHaveTrait: .italicFontMask) }

    // MARK: Contenido

    @ViewBuilder
    private var contenido: some View {
        switch block.kind {
        case .paragraph:
            campo("Escribe algo, o pulsa / para ver los bloques")
        case .heading:
            campo("Título", fuente: .systemFont(ofSize: 22, weight: .bold))
                .padding(.top, 8)
        case .subheading:
            campo("Título 2", fuente: .systemFont(ofSize: 18, weight: .semibold))
                .padding(.top, 5)
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
                Text(["•", "◦", "▪", "•", "◦"][min(block.indent, 4)]).padding(.top, 1).frame(width: 12)
                campo("Elemento de lista")
            }
        case .numberedList:
            HStack(alignment: .top, spacing: 8) {
                Text("\(numero).")
                    .foregroundStyle(.secondary)
                    .padding(.top, 1)
                    .frame(minWidth: 18, alignment: .trailing)
                campo("Elemento de lista")
            }
        case .checklist:
            HStack(alignment: .top, spacing: 8) {
                CheckboxView(marcado: block.isChecked) { block.isChecked.toggle() }
                    .padding(.top, 0)
                campo("Tarea", color: block.isChecked ? .secondaryLabelColor : .labelColor, tachado: block.isChecked)
            }
        case .quote:
            campo("Cita", fuente: cursiva(.systemFont(ofSize: 14)))
                .padding(.leading, 12)
                .overlay(alignment: .leading) {
                    Rectangle().fill(Color.secondary.opacity(0.6)).frame(width: 3)
                }
        case .code:
            bloqueCodigo
        case .image:
            if let data = block.imageData, let imagen = NSImage(data: data) {
                Image(nsImage: imagen)
                    .resizable()
                    .scaledToFit()
                    .frame(maxHeight: 380)
                    .clipShape(RoundedRectangle(cornerRadius: 8))
                    .padding(.vertical, 4)
                    .contextMenu { Button("Eliminar imagen", role: .destructive, action: ops.eliminar) }
            } else {
                Text("Imagen no disponible").foregroundStyle(.secondary)
            }
        case .file:
            FileBlockView(block: $block, onQuitar: ops.eliminar)
        case .embed:
            EnlaceBlockView(block: $block, onQuitar: ops.eliminar) {
                block.kind = .paragraph
                block.text = block.url ?? ""
                block.url = nil
                block.preview = nil
            }
        case .divider:
            Divider()
                .padding(.vertical, 10)
                .contextMenu { Button("Eliminar divisor", role: .destructive, action: ops.eliminar) }
        }
    }

    // MARK: Código

    private var bloqueCodigo: some View {
        let oscuro = esquema == .dark
        let detectado = Resaltador.detectar(block.text)
        let lenguaje = block.language ?? detectado
        let fuente = NSFont.monospacedSystemFont(ofSize: 13, weight: .regular)
        return VStack(alignment: .leading, spacing: 0) {
            HStack(spacing: 10) {
                Menu {
                    Button("Automático") { block.language = nil }
                    Divider()
                    ForEach(Resaltador.lenguajes) { l in
                        Button(l.nombre) { block.language = l.id }
                    }
                } label: {
                    Text(block.language == nil ? "Auto · \(Resaltador.nombre(de: lenguaje))" : Resaltador.nombre(de: lenguaje))
                        .font(.caption)
                }
                .menuStyle(.borderlessButton)
                .menuIndicator(.hidden)
                .fixedSize()
                Spacer()
                Button {
                    NSPasteboard.general.clearContents()
                    NSPasteboard.general.setString(block.text, forType: .string)
                } label: {
                    Label("Copiar", systemImage: "doc.on.doc").font(.caption)
                }
                .buttonStyle(.plain)
            }
            .foregroundStyle(.secondary)
            .padding(.horizontal, 12)
            .padding(.top, 8)

            CampoRico(
                texto: $block.text,
                runs: $block.runs,
                id: block.id,
                estilo: EstiloBloque(fuente: fuente, color: Resaltador.color(nil, oscuro: oscuro)),
                placeholder: "Escribe o pega tu código",
                codigo: .init(lenguaje: lenguaje, oscuro: oscuro),
                enfoque: enfoque,
                acciones: acciones
            )
            .padding(12)
        }
        .background(RoundedRectangle(cornerRadius: 8).fill(Color(nsColor: Resaltador.fondo(oscuro: oscuro))))
        .overlay(RoundedRectangle(cornerRadius: 8).stroke(Color.primary.opacity(0.08), lineWidth: 0.5))
        .padding(.vertical, 2)
    }

    // MARK: Atajos de Markdown

    private func aplicarAtajo(_ texto: String) {
        guard block.kind == .paragraph else { return }
        let atajos: [(String, NoteBlockKind)] = [
            ("# ", .heading), ("## ", .subheading), ("- ", .bulletedList), ("* ", .bulletedList),
            ("1. ", .numberedList), ("[] ", .checklist), ("> ", .quote), ("```", .code)
        ]
        for (prefijo, tipo) in atajos where texto.hasPrefix(prefijo) {
            block.kind = tipo
            block.text = String(texto.dropFirst(prefijo.count))
            block.runs = nil
            enfoque.pedir(block.id)
            return
        }
    }
}

// MARK: - Menú "/"

private struct SlashMenu: View {
    let opciones: [NoteBlockKind]
    let seleccionado: Int
    let onElegir: (NoteBlockKind) -> Void
    let onPasar: (Int) -> Void

    var body: some View {
        ScrollViewReader { proxy in
            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    ForEach(Array(opciones.enumerated()), id: \.element) { indice, tipo in
                        Button { onElegir(tipo) } label: {
                            HStack(spacing: 10) {
                                Image(systemName: tipo.icono)
                                    .frame(width: 26, height: 26)
                                    .background(RoundedRectangle(cornerRadius: 6).fill(Color.primary.opacity(0.08)))
                                Text(tipo.titulo).lineLimit(1)
                                Spacer(minLength: 0)
                            }
                            .padding(.horizontal, 8)
                            .padding(.vertical, 4)
                            .background(RoundedRectangle(cornerRadius: 6).fill(indice == seleccionado ? Color.accentColor.opacity(0.18) : .clear))
                            .contentShape(Rectangle())
                        }
                        .buttonStyle(.plain)
                        .id(indice)
                        .onHover { if $0 { onPasar(indice) } }
                    }
                }
                .padding(4)
            }
            .onChange(of: seleccionado) { nuevo in
                withAnimation(.easeOut(duration: 0.1)) { proxy.scrollTo(nuevo, anchor: nil) }
            }
        }
        .frame(width: 290)
        .frame(height: min(CGFloat(opciones.count) * 36 + 8, 300))
        .background(RoundedRectangle(cornerRadius: 10).fill(Color(nsColor: .windowBackgroundColor)))
        .overlay(RoundedRectangle(cornerRadius: 10).stroke(Color.primary.opacity(0.15), lineWidth: 0.5))
        .shadow(color: .black.opacity(0.22), radius: 12, y: 5)
    }
}

// MARK: - Barra de formato

private struct BarraFormato: View {
    @Binding var pidiendoEnlace: Bool
    @State private var mostrandoColores = false
    @State private var direccion = ""

    private let control = ControladorFormato.shared

    var body: some View {
        HStack(spacing: 2) {
            boton("bold", "Negrita  ⌘B") { control.alternar(.tfBold) }
            boton("italic", "Cursiva  ⌘I") { control.alternar(.tfItalic) }
            boton("underline", "Subrayado  ⌘U") { control.alternar(.tfUnderline) }
            boton("strikethrough", "Tachado  ⌘⇧X") { control.alternar(.tfStrike) }
            boton("chevron.left.forwardslash.chevron.right", "Código en línea  ⌘E") { control.alternar(.tfCode) }
            Divider().frame(height: 16).padding(.horizontal, 4)
            boton("paintpalette", "Color y resaltado") { mostrandoColores.toggle() }
                .popover(isPresented: $mostrandoColores, arrowEdge: .bottom) { paleta }
            boton("link", "Enlace  ⌘K") {
                direccion = control.enlaceActual ?? ""
                pidiendoEnlace = true
            }
            .popover(isPresented: $pidiendoEnlace, arrowEdge: .bottom) { panelEnlace }
            boton("eraser", "Quitar formato") { control.limpiar() }
        }
        .padding(4)
        .background(RoundedRectangle(cornerRadius: 9).fill(.regularMaterial))
        .overlay(RoundedRectangle(cornerRadius: 9).stroke(Color.primary.opacity(0.15), lineWidth: 0.5))
        .shadow(color: .black.opacity(0.18), radius: 8, y: 3)
        .fixedSize()
        .onChange(of: pidiendoEnlace) { abierto in
            if abierto && direccion.isEmpty { direccion = control.enlaceActual ?? "" }
        }
    }

    private func boton(_ icono: String, _ ayuda: String, _ accion: @escaping () -> Void) -> some View {
        Button(action: accion) {
            Image(systemName: icono)
                .font(.system(size: 13, weight: .medium))
                .frame(width: 28, height: 26)
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .help(ayuda)
    }

    private var paleta: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("Color del texto").font(.caption).foregroundStyle(.secondary)
            LazyVGrid(columns: Array(repeating: GridItem(.fixed(30), spacing: 6), count: 5), spacing: 6) {
                Button { control.colorear(nil, fondo: false); mostrandoColores = false } label: {
                    Text("A").frame(width: 30, height: 30)
                        .background(RoundedRectangle(cornerRadius: 6).stroke(Color.primary.opacity(0.3)))
                }
                .buttonStyle(.plain)
                .help("Predeterminado")
                ForEach(NotaColor.allCases) { color in
                    Button { control.colorear(color.rawValue, fondo: false); mostrandoColores = false } label: {
                        Text("A").fontWeight(.semibold).foregroundStyle(color.swiftUI).frame(width: 30, height: 30)
                            .background(RoundedRectangle(cornerRadius: 6).fill(Color.primary.opacity(0.06)))
                    }
                    .buttonStyle(.plain)
                    .help(color.titulo)
                }
            }
            Text("Resaltado").font(.caption).foregroundStyle(.secondary)
            LazyVGrid(columns: Array(repeating: GridItem(.fixed(30), spacing: 6), count: 5), spacing: 6) {
                Button { control.colorear(nil, fondo: true); mostrandoColores = false } label: {
                    Image(systemName: "nosign").frame(width: 30, height: 30)
                        .background(RoundedRectangle(cornerRadius: 6).stroke(Color.primary.opacity(0.3)))
                }
                .buttonStyle(.plain)
                .help("Sin resaltado")
                ForEach(NotaColor.allCases) { color in
                    Button { control.colorear(color.rawValue, fondo: true); mostrandoColores = false } label: {
                        RoundedRectangle(cornerRadius: 6).fill(Color(nsColor: color.fondo)).frame(width: 30, height: 30)
                    }
                    .buttonStyle(.plain)
                    .help(color.titulo)
                }
            }
        }
        .padding(14)
    }

    private var panelEnlace: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("Enlace").font(.headline)
            TextField("https://…", text: $direccion)
                .textFieldStyle(.roundedBorder)
                .frame(width: 260)
                .onSubmit(aplicar)
            HStack {
                Button("Quitar enlace") { control.enlazar(nil); pidiendoEnlace = false }
                Spacer()
                Button("Aplicar", action: aplicar)
                    .keyboardShortcut(.defaultAction)
                    .disabled(ServicioEnlaces.url(desde: direccion) == nil)
            }
        }
        .padding(14)
    }

    private func aplicar() {
        guard let url = ServicioEnlaces.url(desde: direccion) else { return }
        control.enlazar(url.absoluteString)
        pidiendoEnlace = false
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

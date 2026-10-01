import SwiftUI

struct PaginaDetailView: View {
    @ObservedObject var pagina: Pagina
    let cuadernos: [Cuaderno]
    let service: CuadernoService
    let onDelete: () -> Void

    @StateObject private var model: NoteEditorModel
    @State private var mostrandoIconos = false
    @FocusState private var tituloEnfocado: Bool

    init(pagina: Pagina, cuadernos: [Cuaderno], service: CuadernoService, onDelete: @escaping () -> Void) {
        self.pagina = pagina
        self.cuadernos = cuadernos
        self.service = service
        self.onDelete = onDelete
        _model = StateObject(wrappedValue: NoteEditorModel(pagina: pagina))
    }

    var body: some View {
        if pagina.estaEliminado {
            Color.clear
        } else {
            VStack(spacing: 0) {
                barraSuperior
                Divider()
                ScrollView {
                    VStack(alignment: .leading, spacing: 10) {
                        cabecera
                        NoteEditorView(model: model)
                    }
                    .frame(maxWidth: 760, alignment: .leading)
                    .padding(.horizontal, 48)
                    .padding(.vertical, 32)
                    .frame(maxWidth: .infinity)
                }
            }
            .onAppear {
                if pagina.titulo.isEmpty {
                    DispatchQueue.main.asyncAfter(deadline: .now() + 0.15) { tituloEnfocado = true }
                }
            }
            .onDisappear {
                model.guardar()
                service.guardar(pagina)
            }
        }
    }

    private var barraSuperior: some View {
        HStack(spacing: 14) {
            Menu {
                ForEach(cuadernos) { cuaderno in
                    Button {
                        service.mover(pagina, a: cuaderno)
                    } label: {
                        Text("\(cuaderno.icono) \(cuaderno.nombre)")
                    }
                }
            } label: {
                Text("\(pagina.cuaderno.icono) \(pagina.cuaderno.nombre)")
            }
            .menuStyle(.borderlessButton)
            .fixedSize()
            .help("Mover a otro cuaderno")

            Spacer()

            Text("Editada \(pagina.fechaActualizacion.formatted(date: .abbreviated, time: .shortened))")
                .font(.caption)
                .foregroundStyle(.secondary)

            Button { service.alternarFavorita(pagina) } label: {
                Image(systemName: pagina.favorita ? "star.fill" : "star")
                    .foregroundColor(pagina.favorita ? .yellow : .secondary)
            }
            .buttonStyle(.plain)
            .help(pagina.favorita ? "Quitar de favoritas" : "Marcar como favorita")

            Button { model.elegirArchivos() } label: { Image(systemName: "paperclip") }
                .buttonStyle(.plain)
                .help("Adjuntar PDF o Word")

            Menu {
                Button("Eliminar nota", role: .destructive, action: onDelete)
            } label: {
                Image(systemName: "ellipsis")
            }
            .menuStyle(.borderlessButton)
            .fixedSize()
        }
        .padding(.horizontal, 20)
        .padding(.vertical, 12)
    }

    private var cabecera: some View {
        VStack(alignment: .leading, spacing: 8) {
            Button { mostrandoIconos.toggle() } label: {
                if pagina.icono.isEmpty {
                    Label("Añadir icono", systemImage: "face.smiling")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                } else {
                    Text(pagina.icono).font(.system(size: 52))
                }
            }
            .buttonStyle(.plain)
            .popover(isPresented: $mostrandoIconos, arrowEdge: .bottom) {
                EmojiPicker(
                    onElegir: { pagina.icono = $0; service.guardar(pagina); mostrandoIconos = false },
                    onQuitar: pagina.icono.isEmpty ? nil : { pagina.icono = ""; service.guardar(pagina); mostrandoIconos = false }
                )
            }

            TextField("Sin título", text: Binding(
                get: { pagina.titulo },
                set: { pagina.titulo = $0; service.guardar(pagina) }
            ))
            .textFieldStyle(.plain)
            .font(.system(size: 36, weight: .bold))
            .focused($tituloEnfocado)
        }
        .padding(.bottom, 8)
    }
}

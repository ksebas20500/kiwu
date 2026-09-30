import SwiftUI

struct NotasSidebarView: View {
    @Binding var seleccion: NotasSidebarItem
    let cuadernos: [Cuaderno]
    let conteo: (NotasSidebarItem) -> Int
    let onNuevo: () -> Void
    let onEditar: (Cuaderno) -> Void
    let onEliminar: (Cuaderno) -> Void

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 2) {
                SidebarFila(activo: seleccion == .todas, icono: .simbolo("note.text"), titulo: "Todas las notas", cuenta: conteo(.todas)) {
                    seleccion = .todas
                }
                SidebarFila(activo: seleccion == .favoritas, icono: .simbolo("star"), titulo: "Favoritas", cuenta: conteo(.favoritas)) {
                    seleccion = .favoritas
                }

                HStack {
                    Text("Cuadernos")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                    Spacer()
                    Button(action: onNuevo) { Image(systemName: "plus") }
                        .buttonStyle(.plain)
                        .foregroundStyle(.secondary)
                        .help("Nuevo cuaderno")
                }
                .padding(.horizontal, 10)
                .padding(.top, 18)
                .padding(.bottom, 4)

                ForEach(cuadernos) { cuaderno in
                    let item = NotasSidebarItem.cuaderno(cuaderno.id)
                    SidebarFila(activo: seleccion == item, icono: .emoji(cuaderno.icono), titulo: cuaderno.nombre, cuenta: conteo(item)) {
                        seleccion = item
                    }
                    .contextMenu {
                        Button("Editar cuaderno") { onEditar(cuaderno) }
                        Button("Eliminar cuaderno", role: .destructive) { onEliminar(cuaderno) }
                    }
                }
            }
            .padding(10)
        }
        .background(Color.primary.opacity(0.04))
    }
}

struct CuadernoFormView: View {
    @Environment(\.dismiss) private var dismiss
    let titulo: String
    @State var nombre: String
    @State var emoji: String
    let onGuardar: (String, String) -> Void

    private var limpio: String { nombre.trimmingCharacters(in: .whitespacesAndNewlines) }

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            Text(titulo).font(.title2.bold())

            HStack(spacing: 10) {
                Text(emoji).font(.system(size: 28))
                TextField("Nombre del cuaderno", text: $nombre)
                    .onSubmit(guardar)
            }

            EmojiPicker(onElegir: { emoji = $0 })
                .background(RoundedRectangle(cornerRadius: 8).fill(Color.primary.opacity(0.05)))

            HStack {
                Spacer()
                Button("Cancelar") { dismiss() }
                Button("Guardar", action: guardar)
                    .keyboardShortcut(.defaultAction)
                    .disabled(limpio.isEmpty)
            }
        }
        .padding(24)
        .frame(width: 360)
    }

    private func guardar() {
        guard !limpio.isEmpty else { return }
        onGuardar(limpio, emoji)
        dismiss()
    }
}

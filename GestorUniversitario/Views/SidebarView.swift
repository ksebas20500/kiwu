import SwiftUI

struct SidebarView: View {
    @Binding var seleccion: SidebarItem
    let materias: [Materia]
    let conteo: (SidebarItem) -> Int
    let onNuevaMateria: () -> Void
    let onEliminarMateria: (Materia) -> Void

    var body: some View {
        VStack(spacing: 0) {
            ScrollView {
                VStack(alignment: .leading, spacing: 2) {
                    fila(.hoy, "Hoy", "sun.max")
                    fila(.proximos, "Próximos 7 días", "calendar.badge.clock")
                    fila(.calendario, "Calendario", "calendar")
                    fila(.todos, "Todas las tareas", "tray.full")

                    HStack {
                        Text("Listas")
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                        Spacer()
                        Button(action: onNuevaMateria) {
                            Image(systemName: "plus")
                        }
                        .buttonStyle(.plain)
                        .foregroundStyle(.secondary)
                        .help("Nueva lista")
                    }
                    .padding(.horizontal, 10)
                    .padding(.top, 18)
                    .padding(.bottom, 4)

                    ForEach(materias) { materia in
                        fila(.materia(materia.id), materia.nombre, materia.icono, alerta: materia.bookmarkCarpeta != nil && !materia.carpetaDisponible)
                            .contextMenu {
                                Button("Eliminar lista", role: .destructive) { onEliminarMateria(materia) }
                            }
                    }

                    Divider().padding(.vertical, 10)
                    fila(.completadas, "Completadas", "checkmark.circle")
                }
                .padding(10)
            }
        }
        .background(Color.primary.opacity(0.04))
    }

    private func fila(_ item: SidebarItem, _ titulo: String, _ icono: String, alerta: Bool = false) -> some View {
        let activo = seleccion == item
        let cuenta = conteo(item)
        return Button {
            seleccion = item
        } label: {
            HStack(spacing: 10) {
                Image(systemName: icono)
                    .frame(width: 20)
                    .foregroundStyle(activo ? Color.accentColor : .secondary)
                Text(titulo)
                    .lineLimit(1)
                Spacer()
                if alerta {
                    Image(systemName: "exclamationmark.triangle.fill")
                        .font(.caption)
                        .foregroundColor(.orange)
                        .help("La carpeta vinculada no está disponible")
                }
                if cuenta > 0 {
                    Text("\(cuenta)")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
            }
            .padding(.horizontal, 10)
            .padding(.vertical, 7)
            .background(RoundedRectangle(cornerRadius: 8).fill(activo ? Color.accentColor.opacity(0.15) : .clear))
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }
}

struct NuevaMateriaView: View {
    @Environment(\.dismiss) private var dismiss
    @State private var nombre = ""
    let onCreate: (String) -> Void

    private var limpio: String { nombre.trimmingCharacters(in: .whitespacesAndNewlines) }

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("Nueva lista")
                .font(.title2.bold())
            TextField("Nombre de la materia", text: $nombre)
                .onSubmit(crear)
            HStack {
                Spacer()
                Button("Cancelar") { dismiss() }
                Button("Crear", action: crear)
                    .keyboardShortcut(.defaultAction)
                    .disabled(limpio.isEmpty)
            }
        }
        .padding(24)
        .frame(width: 360)
    }

    private func crear() {
        guard !limpio.isEmpty else { return }
        onCreate(limpio)
        dismiss()
    }
}

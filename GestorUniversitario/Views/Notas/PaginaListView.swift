import SwiftUI

struct PaginaListView: View {
    let titulo: String
    let paginas: [Pagina]
    let mostrarCuaderno: Bool
    @Binding var busqueda: String
    @Binding var seleccion: UUID?
    let puedeCrear: Bool
    let onNueva: () -> Void
    let onEliminar: (Pagina) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack {
                Text(titulo)
                    .font(.system(size: 26, weight: .bold))
                    .lineLimit(1)
                Spacer()
                Button(action: onNueva) {
                    Image(systemName: "square.and.pencil")
                        .font(.system(size: 17))
                }
                .buttonStyle(.plain)
                .disabled(!puedeCrear)
                .help("Nueva nota")
            }
            .padding(.horizontal, 20)
            .padding(.top, 20)
            .padding(.bottom, 12)

            HStack(spacing: 8) {
                Image(systemName: "magnifyingglass").foregroundStyle(.secondary)
                TextField("Buscar en las notas", text: $busqueda)
                    .textFieldStyle(.plain)
                if !busqueda.isEmpty {
                    Button { busqueda = "" } label: { Image(systemName: "xmark.circle.fill") }
                        .buttonStyle(.plain)
                        .foregroundStyle(.secondary)
                }
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
            .background(RoundedRectangle(cornerRadius: 8).fill(Color.primary.opacity(0.06)))
            .padding(.horizontal, 20)
            .padding(.bottom, 10)

            ScrollView {
                LazyVStack(spacing: 2) {
                    ForEach(paginas) { pagina in
                        PaginaRow(pagina: pagina, seleccionada: seleccion == pagina.id, mostrarCuaderno: mostrarCuaderno) {
                            onEliminar(pagina)
                        }
                        .onTapGesture { seleccion = pagina.id }
                    }
                }
                .padding(.horizontal, 10)
                .padding(.bottom, 20)
            }
            .overlay {
                if paginas.isEmpty {
                    EmptyStateView(titulo: busqueda.isEmpty ? "Sin notas" : "Sin resultados", icono: busqueda.isEmpty ? "note.text" : "magnifyingglass")
                }
            }
        }
    }
}

private struct PaginaRow: View {
    @ObservedObject var pagina: Pagina
    let seleccionada: Bool
    let mostrarCuaderno: Bool
    let onEliminar: () -> Void

    var body: some View {
        if pagina.estaEliminado {
            EmptyView()
        } else {
            HStack(alignment: .top, spacing: 10) {
                if pagina.icono.isEmpty {
                    Image(systemName: "doc.text")
                        .foregroundStyle(.secondary)
                        .frame(width: 24, height: 24)
                } else {
                    Text(pagina.icono)
                        .font(.system(size: 20))
                        .frame(width: 24, height: 24)
                }

                VStack(alignment: .leading, spacing: 3) {
                    Text(pagina.titulo.isEmpty ? "Sin título" : pagina.titulo)
                        .fontWeight(.medium)
                        .foregroundColor(pagina.titulo.isEmpty ? .secondary : .primary)
                        .lineLimit(1)
                    if !pagina.textoPlano.isEmpty {
                        Text(pagina.textoPlano.replacingOccurrences(of: "\n", with: " ").prefix(120))
                            .font(.caption)
                            .foregroundStyle(.secondary)
                            .lineLimit(2)
                    }
                    HStack(spacing: 6) {
                        Text(pagina.fechaActualizacion, style: .relative)
                        if mostrarCuaderno {
                            Text("· \(pagina.cuaderno.nombre)")
                        }
                    }
                    .font(.caption2)
                    .foregroundStyle(.tertiary)
                }
                Spacer(minLength: 0)
                if pagina.favorita {
                    Image(systemName: "star.fill")
                        .font(.caption)
                        .foregroundColor(.yellow)
                }
            }
            .padding(.horizontal, 10)
            .padding(.vertical, 9)
            .background(RoundedRectangle(cornerRadius: 8).fill(seleccionada ? Color.primary.opacity(0.08) : .clear))
            .contentShape(Rectangle())
            .contextMenu {
                Button("Eliminar nota", role: .destructive, action: onEliminar)
            }
        }
    }
}

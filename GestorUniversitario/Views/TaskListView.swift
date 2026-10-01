import SwiftUI

struct TaskListView: View {
    let titulo: String
    let pendientes: [Deber]
    let completados: [Deber]
    let materias: [Materia]
    let materiaFija: Materia?
    let mostrarMateria: Bool
    let permiteCrear: Bool
    @Binding var seleccion: UUID?
    @Binding var vistaTareas: VistaTareas
    let service: DeberService
    let onCrear: (String, Materia) -> Void
    let onEliminar: (Deber) -> Void
    var vista: Binding<VistaMateria>? = nil
    var carpetaAlerta = false

    @State private var nuevoTitulo = ""
    @State private var destinoID: UUID?
    @State private var completadosVisibles = true
    @FocusState private var campoEnfocado: Bool
    @ObservedObject private var ajustes = AjustesStore.shared

    private var destino: Materia? {
        materiaFija ?? materias.first { $0.id == destinoID } ?? materias.first
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack(spacing: 8) {
                Text(titulo)
                    .font(.system(size: 26, weight: .bold))
                    .lineLimit(1)
                Spacer()
                SelectorVistaTareas(vista: $vistaTareas)
            }
            .padding(.horizontal, 20)
            .padding(.top, 20)
            .padding(.bottom, vista == nil ? 12 : 8)

            if let vista {
                VistaMateriaPicker(vista: vista, carpetaAlerta: carpetaAlerta)
                    .padding(.horizontal, 20)
                    .padding(.bottom, 12)
            }

            if permiteCrear { campoNuevaTarea }

            ScrollView {
                LazyVStack(alignment: .leading, spacing: 2) {
                    ForEach(pendientes) { fila($0) }

                    if !completados.isEmpty {
                        Button {
                            withAnimation { completadosVisibles.toggle() }
                        } label: {
                            HStack(spacing: 6) {
                                Image(systemName: completadosVisibles ? "chevron.down" : "chevron.right")
                                    .font(.caption)
                                Text("Completada")
                                    .font(.headline)
                                Text("\(completados.count)")
                                    .foregroundStyle(.secondary)
                            }
                            .padding(.top, 16)
                            .padding(.horizontal, 10)
                        }
                        .buttonStyle(.plain)

                        if completadosVisibles {
                            ForEach(completados) { fila($0) }
                        }
                    }
                }
                .padding(.horizontal, 10)
                .padding(.bottom, 20)
            }
            .overlay {
                if pendientes.isEmpty && completados.isEmpty {
                    EmptyStateView(titulo: "Sin tareas", icono: "checklist")
                }
            }
        }
        .onReceive(ajustes.acciones) { if $0 == .nuevaTarea && permiteCrear { campoEnfocado = true } }
    }

    private var campoNuevaTarea: some View {
        HStack(spacing: 8) {
            Image(systemName: "plus")
                .foregroundStyle(.secondary)
            TextField(destino == nil ? "Crea una lista primero" : "Añadir tarea", text: $nuevoTitulo)
                .textFieldStyle(.plain)
                .focused($campoEnfocado)
                .onSubmit(crear)
                .disabled(destino == nil)

            if materiaFija == nil, let destino {
                Menu {
                    ForEach(materias) { materia in
                        Button(materia.nombre) { destinoID = materia.id }
                    }
                } label: {
                    Label(destino.nombre, systemImage: destino.icono)
                        .font(.caption)
                }
                .menuStyle(.borderlessButton)
                .fixedSize()
            }
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 10)
        .background(RoundedRectangle(cornerRadius: 8).fill(Color.primary.opacity(0.06)))
        .padding(.horizontal, 20)
        .padding(.bottom, 10)
    }

    private func fila(_ deber: Deber) -> some View {
        TaskRow(
            deber: deber,
            seleccionado: seleccion == deber.id,
            mostrarMateria: mostrarMateria,
            service: service,
            onEliminar: { onEliminar(deber) }
        )
        .onTapGesture { seleccion = deber.id }
    }

    private func crear() {
        let limpio = nuevoTitulo.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !limpio.isEmpty, let destino else { return }
        onCrear(limpio, destino)
        nuevoTitulo = ""
    }
}

private struct TaskRow: View {
    @ObservedObject var deber: Deber
    let seleccionado: Bool
    let mostrarMateria: Bool
    let service: DeberService
    let onEliminar: () -> Void

    var body: some View {
        if deber.estaEliminado {
            EmptyView()
        } else {
            HStack(spacing: 10) {
                CheckboxView(marcado: deber.completado) {
                    service.marcar(deber, completado: !deber.completado)
                }
                Text(deber.titulo)
                    .lineLimit(1)
                    .strikethrough(deber.completado)
                if deber.columna == .enCurso {
                    Text("En curso")
                        .font(.caption2.weight(.medium))
                        .padding(.horizontal, 6)
                        .padding(.vertical, 2)
                        .background(Capsule().fill(Color.accentColor.opacity(0.18)))
                        .foregroundColor(.accentColor)
                }
                Spacer(minLength: 8)
                if mostrarMateria {
                    Text(deber.materia.nombre)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
                if let fecha = deber.fechaEntrega {
                    Text(FechaFormato.corto(fecha))
                        .font(.caption)
                        .foregroundColor(deber.vencido ? .red : .secondary)
                }
            }
            .opacity(deber.completado ? 0.5 : 1)
            .padding(.horizontal, 10)
            .padding(.vertical, 9)
            .background(RoundedRectangle(cornerRadius: 8).fill(seleccionado ? Color.primary.opacity(0.08) : .clear))
            .contentShape(Rectangle())
            .contextMenu {
                ForEach(ColumnaFlujo.allCases.filter { $0 != deber.columna }) { c in
                    Button("Mover a «\(c.titulo)»") { service.mover(deber, a: c) }
                }
                Divider()
                Button("Eliminar tarea", role: .destructive, action: onEliminar)
            }
        }
    }
}

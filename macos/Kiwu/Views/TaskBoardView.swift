import SwiftUI

enum VistaTareas: String {
    case lista, tablero
}

/// Conmutador Lista / Tablero.
struct SelectorVistaTareas: View {
    @Binding var vista: VistaTareas

    var body: some View {
        HStack(spacing: 2) {
            boton(.lista, "list.bullet", "Vista de lista")
            boton(.tablero, "rectangle.split.3x1", "Vista de tablero")
        }
        .padding(2)
        .background(RoundedRectangle(cornerRadius: 8).fill(Color.primary.opacity(0.08)))
    }

    private func boton(_ destino: VistaTareas, _ icono: String, _ ayuda: String) -> some View {
        Button { vista = destino } label: {
            Image(systemName: icono)
                .frame(width: 28, height: 22)
                .background(RoundedRectangle(cornerRadius: 6).fill(vista == destino ? Color(nsColor: .controlBackgroundColor) : .clear))
        }
        .buttonStyle(.plain)
        .foregroundStyle(vista == destino ? Color.primary : .secondary)
        .help(ayuda)
    }
}

/// Tablero con tres columnas (Por hacer, En curso, Hecho). Las tarjetas se arrastran entre columnas.
struct TaskBoardView: View {
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
    @FocusState private var campoEnfocado: Bool
    @ObservedObject private var ajustes = AjustesStore.shared
    @State private var refresco = 0

    private var destino: Materia? {
        materiaFija ?? materias.first { $0.id == destinoID } ?? materias.first
    }

    private func tarjetas(_ columna: ColumnaFlujo) -> [Deber] {
        (pendientes + completados).filter { $0.columna == columna }
    }

    var body: some View {
        let _ = refresco
        VStack(alignment: .leading, spacing: 0) {
            HStack(spacing: 12) {
                Text(titulo).font(.system(size: 26, weight: .bold)).lineLimit(1)
                Spacer()
                SelectorVistaTareas(vista: $vistaTareas)
            }
            .padding(.horizontal, 20)
            .padding(.top, 20)
            .padding(.bottom, 10)

            if let vista {
                VistaMateriaPicker(vista: vista, carpetaAlerta: carpetaAlerta)
                    .padding(.horizontal, 20)
                    .padding(.bottom, 10)
            }

            if permiteCrear { campoNueva }

            GeometryReader { geo in
                // Las tres columnas se reparten el ancho; si no caben (panel de detalle abierto) se desplaza en horizontal.
                let ancho = max(200, (geo.size.width - 40 - 28) / 3)
                ScrollView(.horizontal) {
                    HStack(alignment: .top, spacing: 14) {
                        ForEach(ColumnaFlujo.allCases) { columna in
                            ColumnaView(columna: columna, tarjetas: tarjetas(columna), seleccion: $seleccion,
                                        mostrarMateria: mostrarMateria, service: service, onEliminar: onEliminar)
                                .frame(width: ancho)
                        }
                    }
                    .padding(.horizontal, 20)
                    .padding(.bottom, 20)
                    .frame(minHeight: geo.size.height, alignment: .top)
                    .background(Color.clear.contentShape(Rectangle()).onTapGesture { seleccion = nil })
                }
            }
        }
        .onReceive(ajustes.acciones) { if $0 == .nuevaTarea && permiteCrear { campoEnfocado = true } }
    }

    private var campoNueva: some View {
        HStack(spacing: 8) {
            Image(systemName: "plus").foregroundStyle(.secondary)
            TextField(destino == nil ? "Crea una lista primero" : "Añadir tarea a «Por hacer»", text: $nuevoTitulo)
                .textFieldStyle(.plain)
                .focused($campoEnfocado)
                .onSubmit(crear)
                .disabled(destino == nil)
            if materiaFija == nil, let destino {
                Menu {
                    ForEach(materias) { materia in Button(materia.nombre) { destinoID = materia.id } }
                } label: {
                    Label(destino.nombre, systemImage: destino.icono).font(.caption)
                }
                .menuStyle(.borderlessButton)
                .fixedSize()
            }
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 10)
        .background(RoundedRectangle(cornerRadius: 8).fill(Color.primary.opacity(0.06)))
        .frame(maxWidth: 480)
        .padding(.horizontal, 20)
        .padding(.bottom, 12)
    }

    private func crear() {
        let limpio = nuevoTitulo.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !limpio.isEmpty, let destino else { return }
        onCrear(limpio, destino)
        nuevoTitulo = ""
    }
}

private struct ColumnaView: View {
    let columna: ColumnaFlujo
    let tarjetas: [Deber]
    @Binding var seleccion: UUID?
    let mostrarMateria: Bool
    let service: DeberService
    let onEliminar: (Deber) -> Void

    @State private var resaltada = false

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(spacing: 6) {
                Text(columna.titulo).font(.headline).foregroundStyle(.secondary)
                Text("\(tarjetas.count)").font(.caption).foregroundStyle(.tertiary)
            }
            .padding(.horizontal, 4)

            ScrollView {
                LazyVStack(spacing: 8) {
                    ForEach(tarjetas) { deber in
                        TarjetaView(deber: deber, seleccionada: seleccion == deber.id, mostrarMateria: mostrarMateria,
                                    service: service, onEliminar: { onEliminar(deber) })
                            .onTapGesture { seleccion = deber.id }
                            .draggable(deber.id.uuidString)
                    }
                    if tarjetas.isEmpty {
                        Text("Arrastra tareas aquí")
                            .font(.caption)
                            .foregroundStyle(.tertiary)
                            .frame(maxWidth: .infinity, minHeight: 60)
                    }
                }
                .padding(.bottom, 4)
            }
        }
        .padding(10)
        .frame(maxHeight: .infinity, alignment: .top)
        .background(RoundedRectangle(cornerRadius: 12).fill(Color.primary.opacity(resaltada ? 0.12 : 0.04)))
        .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color.accentColor.opacity(resaltada ? 0.6 : 0), lineWidth: 2))
        .dropDestination(for: String.self) { ids, _ in
            var movido = false
            for texto in ids {
                guard let id = UUID(uuidString: texto),
                      let deber = (try? service.persistence.container.viewContext.fetch(Deber.fetchRequest()))?.first(where: { $0.id == id })
                else { continue }
                service.mover(deber, a: columna)
                movido = true
            }
            return movido
        } isTargeted: { resaltada = $0 }
    }
}

private struct TarjetaView: View {
    @ObservedObject var deber: Deber
    let seleccionada: Bool
    let mostrarMateria: Bool
    let service: DeberService
    let onEliminar: () -> Void

    var body: some View {
        if deber.estaEliminado {
            EmptyView()
        } else {
            HStack(alignment: .top, spacing: 10) {
                CheckboxView(marcado: deber.completado) {
                    service.marcar(deber, completado: !deber.completado)
                }
                VStack(alignment: .leading, spacing: 3) {
                    Text(deber.titulo)
                        .strikethrough(deber.completado)
                        .frame(maxWidth: .infinity, alignment: .leading)
                    HStack(spacing: 6) {
                        if let fecha = deber.fechaEntrega {
                            Label(FechaFormato.corto(fecha), systemImage: "calendar")
                                .foregroundColor(deber.vencido ? .red : (Calendar.current.isDateInToday(fecha) ? .green : .secondary))
                        }
                        if mostrarMateria {
                            Text(deber.materia.nombre).foregroundStyle(.secondary)
                        }
                    }
                    .font(.caption)
                }
            }
            .opacity(deber.completado ? 0.55 : 1)
            .padding(10)
            .background(RoundedRectangle(cornerRadius: 10).fill(Color(nsColor: .controlBackgroundColor)))
            .overlay(RoundedRectangle(cornerRadius: 10).stroke(seleccionada ? Color.accentColor : Color.primary.opacity(0.1), lineWidth: seleccionada ? 2 : 1))
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

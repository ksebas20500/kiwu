import SwiftUI
import Combine

struct CalendarView: View {
    let deberes: [Deber]
    let materias: [Materia]
    @Binding var seleccion: UUID?
    let service: DeberService
    let onCrear: (String, Materia, Date) -> Void

    @ObservedObject private var enrutador = Enrutador.shared
    @State private var diaExpandido: Date?
    @State private var mes = Calendar.current.dateInterval(of: .month, for: .now)?.start ?? .now
    @State private var dia = Calendar.current.startOfDay(for: .now)
    @State private var nuevoTitulo = ""
    @State private var destinoID: UUID?

    private var cal: Calendar { .current }

    private var destino: Materia? {
        materias.first { $0.id == destinoID } ?? materias.first
    }

    private var desfase: Int {
        (cal.component(.weekday, from: mes) - cal.firstWeekday + 7) % 7
    }

    private var filas: Int {
        let diasMes = cal.range(of: .day, in: .month, for: mes)?.count ?? 30
        return (desfase + diasMes + 6) / 7
    }

    private var dias: [Date] {
        let primero = cal.date(byAdding: .day, value: -desfase, to: mes) ?? mes
        return (0..<(filas * 7)).compactMap { cal.date(byAdding: .day, value: $0, to: primero) }
    }

    private var porDia: [Date: [Deber]] {
        Dictionary(grouping: deberes.filter { !$0.estaEliminado && $0.fechaEntrega != nil }) {
            cal.startOfDay(for: $0.fechaEntrega!)
        }
    }

    private var simbolosSemana: [String] {
        let s = cal.shortStandaloneWeekdaySymbols
        let i = cal.firstWeekday - 1
        return Array(s[i...] + s[..<i])
    }

    var body: some View {
        VStack(spacing: 0) {
            cabecera
            HStack(spacing: 0) {
                ForEach(simbolosSemana, id: \.self) { simbolo in
                    Text(simbolo.capitalized)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                        .frame(maxWidth: .infinity)
                }
            }
            .padding(.bottom, 6)
            Divider()
            cuadricula
            Divider()
            barraNuevaTarea
        }
    }

    private var cabecera: some View {
        HStack(spacing: 12) {
            Text(mes.formatted(.dateTime.month(.wide).year()).capitalized)
                .font(.system(size: 26, weight: .bold))
            Spacer()
            Button { cambiarMes(-1) } label: { Image(systemName: "chevron.left") }
            Button("Hoy") {
                mes = cal.dateInterval(of: .month, for: .now)?.start ?? .now
                dia = cal.startOfDay(for: .now)
            }
            Button { cambiarMes(1) } label: { Image(systemName: "chevron.right") }
        }
        .padding(.horizontal, 20)
        .padding(.top, 20)
        .padding(.bottom, 12)
    }

    private var cuadricula: some View {
        let agrupados = porDia
        let lista = dias
        return GeometryReader { geo in
            let anchoCelda = geo.size.width / 7
            let altoCelda = geo.size.height / CGFloat(filas)

            ZStack(alignment: .topLeading) {
                VStack(spacing: 0) {
                    ForEach(0..<filas, id: \.self) { fila in
                        HStack(spacing: 0) {
                            ForEach(0..<7, id: \.self) { columna in
                                let fecha = lista[fila * 7 + columna]
                                DayCell(
                                    fecha: fecha,
                                    delMes: cal.isDate(fecha, equalTo: mes, toGranularity: .month),
                                    seleccionado: cal.isDate(fecha, inSameDayAs: dia),
                                    tareas: ordenadas(agrupados[fecha] ?? []),
                                    seleccion: $seleccion
                                )
                                .onTapGesture(count: 2) {
                                    dia = fecha
                                    withAnimation(.spring(response: 0.3, dampingFraction: 0.85)) { diaExpandido = fecha }
                                }
                                .onTapGesture {
                                    dia = fecha
                                    withAnimation(.easeOut(duration: 0.15)) { diaExpandido = nil }
                                }
                            }
                        }
                    }
                }

                if let expandido = diaExpandido, let indice = lista.firstIndex(of: expandido) {
                    let fila = CGFloat(indice / 7), columna = CGFloat(indice % 7)
                    let tareas = ordenadas(agrupados[expandido] ?? [])
                    let ancho = min(geo.size.width, max(anchoCelda, 280))
                    let alto = min(geo.size.height, max(altoCelda, CGFloat(64 + tareas.count * 38)))
                    let x = min(columna * anchoCelda, geo.size.width - ancho)
                    let y = min(fila * altoCelda, geo.size.height - alto)
                    let ancla = UnitPoint(
                        x: (columna * anchoCelda + anchoCelda / 2 - x) / ancho,
                        y: (fila * altoCelda + altoCelda / 2 - y) / alto
                    )

                    ExpandedDayCard(
                        fecha: expandido,
                        tareas: tareas,
                        seleccion: $seleccion,
                        service: service,
                        onCerrar: { withAnimation(.easeOut(duration: 0.15)) { diaExpandido = nil } }
                    )
                    .frame(width: ancho, height: alto)
                    .offset(x: x, y: y)
                    .transition(.scale(scale: 0.4, anchor: ancla).combined(with: .opacity))
                    .zIndex(1)
                }
            }
        }
        .onExitCommand { withAnimation { diaExpandido = nil } }
        .onReceive(enrutador.$diaCalendario.compactMap { $0 }) { fecha in
            let inicio = cal.startOfDay(for: fecha)
            mes = cal.dateInterval(of: .month, for: inicio)?.start ?? inicio
            dia = inicio
            enrutador.diaCalendario = nil
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.25) {
                withAnimation(.spring(response: 0.3, dampingFraction: 0.85)) { diaExpandido = inicio }
            }
        }
    }

    private func ordenadas(_ tareas: [Deber]) -> [Deber] {
        tareas.sorted {
            ($0.fechaEntrega ?? .distantFuture, $0.fechaCreacion) < ($1.fechaEntrega ?? .distantFuture, $1.fechaCreacion)
        }
    }

    private var barraNuevaTarea: some View {
        HStack(spacing: 8) {
            Image(systemName: "plus").foregroundStyle(.secondary)
            TextField(destino == nil ? "Crea una lista primero" : "Añadir tarea el \(FechaFormato.corto(dia))", text: $nuevoTitulo)
                .textFieldStyle(.plain)
                .onSubmit(crear)
                .disabled(destino == nil)
            if let destino {
                Menu {
                    ForEach(materias) { materia in
                        Button(materia.nombre) { destinoID = materia.id }
                    }
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
        .padding(12)
    }

    private func cambiarMes(_ delta: Int) {
        mes = cal.date(byAdding: .month, value: delta, to: mes) ?? mes
    }

    private func crear() {
        let limpio = nuevoTitulo.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !limpio.isEmpty, let destino else { return }
        let fecha = cal.date(bySettingHour: 9, minute: 0, second: 0, of: dia) ?? dia
        onCrear(limpio, destino, fecha)
        nuevoTitulo = ""
    }
}

private struct DayCell: View {
    let fecha: Date
    let delMes: Bool
    let seleccionado: Bool
    let tareas: [Deber]
    @Binding var seleccion: UUID?

    private var esHoy: Bool { Calendar.current.isDateInToday(fecha) }

    var body: some View {
        VStack(alignment: .leading, spacing: 3) {
            Text("\(Calendar.current.component(.day, from: fecha))")
                .font(.system(size: 12, weight: esHoy ? .bold : .regular))
                .foregroundColor(esHoy ? .white : (delMes ? .primary : .secondary))
                .frame(width: 22, height: 22)
                .background(Circle().fill(esHoy ? Color.accentColor : .clear))

            let colores = PaletaPastel.asignar(a: tareas)
            ForEach(tareas.prefix(3)) { deber in
                ChipView(deber: deber, color: colores[deber.id] ?? PaletaPastel.colores[0], seleccionado: seleccion == deber.id) {
                    seleccion = deber.id
                }
            }
            if tareas.count > 3 {
                Text("+\(tareas.count - 3) más")
                    .font(.caption2)
                    .foregroundStyle(.secondary)
            }
            Spacer(minLength: 0)
        }
        .padding(4)
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
        .background(seleccionado ? Color.accentColor.opacity(0.08) : .clear)
        .overlay(Rectangle().stroke(Color.primary.opacity(0.08), lineWidth: 0.5))
        .opacity(delMes ? 1 : 0.55)
        .contentShape(Rectangle())
    }
}

private struct ChipView: View {
    @ObservedObject var deber: Deber
    let color: ColorPastel
    let seleccionado: Bool
    let onTap: () -> Void

    var body: some View {
        if deber.estaEliminado {
            EmptyView()
        } else {
            HStack(spacing: 4) {
                if deber.vencido {
                    Circle().fill(Color.red).frame(width: 6, height: 6)
                }
                Text(deber.titulo)
                    .font(.caption)
                    .lineLimit(1)
                    .strikethrough(deber.completado)
            }
            .foregroundColor(color.texto)
            .padding(.horizontal, 5)
            .padding(.vertical, 2)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(RoundedRectangle(cornerRadius: 4).fill(color.fondo))
            .overlay(RoundedRectangle(cornerRadius: 4).stroke(color.texto, lineWidth: seleccionado ? 1.5 : 0))
            .opacity(deber.completado ? 0.55 : 1)
            .contentShape(Rectangle())
            .onTapGesture(perform: onTap)
        }
    }
}

private struct ExpandedDayCard: View {
    let fecha: Date
    let tareas: [Deber]
    @Binding var seleccion: UUID?
    let service: DeberService
    let onCerrar: () -> Void

    var body: some View {
        let colores = PaletaPastel.asignar(a: tareas)
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text(fecha.formatted(.dateTime.weekday(.wide).day().month(.wide)).capitalized)
                    .font(.headline)
                Spacer()
                Button(action: onCerrar) {
                    Image(systemName: "xmark.circle.fill").foregroundStyle(.secondary)
                }
                .buttonStyle(.plain)
                .help("Cerrar (Esc)")
            }

            if tareas.isEmpty {
                Text("Sin tareas este día")
                    .foregroundStyle(.secondary)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else {
                ScrollView {
                    VStack(spacing: 6) {
                        ForEach(tareas) { deber in
                            ExpandedTaskRow(
                                deber: deber,
                                color: colores[deber.id] ?? PaletaPastel.colores[0],
                                seleccionado: seleccion == deber.id,
                                service: service
                            ) { seleccion = deber.id }
                        }
                    }
                }
            }
        }
        .padding(12)
        .background(RoundedRectangle(cornerRadius: 12).fill(Color(nsColor: .windowBackgroundColor)))
        .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color.primary.opacity(0.15), lineWidth: 0.5))
        .shadow(color: .black.opacity(0.25), radius: 14, y: 6)
    }
}

private struct ExpandedTaskRow: View {
    @ObservedObject var deber: Deber
    let color: ColorPastel
    let seleccionado: Bool
    let service: DeberService
    let onTap: () -> Void

    var body: some View {
        if deber.estaEliminado {
            EmptyView()
        } else {
            HStack(spacing: 8) {
                CheckboxView(marcado: deber.completado) {
                    service.marcar(deber, completado: !deber.completado)
                }
                VStack(alignment: .leading, spacing: 1) {
                    Text(deber.titulo)
                        .lineLimit(2)
                        .strikethrough(deber.completado)
                    Text("\(deber.materia.nombre)" + (deber.fechaEntrega.map { " · " + $0.formatted(date: .omitted, time: .shortened) } ?? ""))
                        .font(.caption2)
                        .opacity(0.75)
                }
                Spacer(minLength: 0)
                if deber.vencido {
                    Circle().fill(Color.red).frame(width: 7, height: 7)
                }
            }
            .foregroundColor(color.texto)
            .padding(.horizontal, 10)
            .padding(.vertical, 7)
            .background(RoundedRectangle(cornerRadius: 8).fill(color.fondo))
            .overlay(RoundedRectangle(cornerRadius: 8).stroke(color.texto, lineWidth: seleccionado ? 1.5 : 0))
            .opacity(deber.completado ? 0.55 : 1)
            .contentShape(Rectangle())
            .onTapGesture(perform: onTap)
        }
    }
}

extension PaletaPastel {
    static func asignar(a tareas: [Deber]) -> [UUID: ColorPastel] {
        asignar(ids: tareas.filter { !$0.estaEliminado }.map(\.id))
    }
}

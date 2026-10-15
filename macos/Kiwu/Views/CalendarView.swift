import SwiftUI
import Combine

struct CalendarView: View {
    let deberes: [Deber]
    let materias: [Materia]
    @Binding var seleccion: UUID?
    let service: DeberService
    let onCrear: (String, Materia, Date) -> Void

    @ObservedObject private var enrutador = Enrutador.shared
    @State private var mes = Calendar.current.dateInterval(of: .month, for: .now)?.start ?? .now
    @State private var dia = Calendar.current.startOfDay(for: .now)
    @State private var nuevoTitulo = ""
    @State private var destinoID: UUID?
    @State private var hora = Calendar.current.date(bySettingHour: 18, minute: 0, second: 0, of: .now) ?? .now

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
                .frame(minHeight: 230)
                .layoutPriority(1)
            Divider()
            detalleDelDia
                .frame(minHeight: 150, idealHeight: 210, maxHeight: 280)
        }
        .onReceive(enrutador.$diaCalendario.compactMap { $0 }) { fecha in
            let inicio = cal.startOfDay(for: fecha)
            mes = cal.dateInterval(of: .month, for: inicio)?.start ?? inicio
            dia = inicio
            enrutador.diaCalendario = nil
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

    /// Cada día muestra su número y un punto de color por tarea (hasta 4 y un «+» si hay más).
    private var cuadricula: some View {
        let agrupados = porDia
        let lista = dias
        return VStack(spacing: 0) {
            ForEach(0..<filas, id: \.self) { fila in
                HStack(spacing: 0) {
                    ForEach(0..<7, id: \.self) { columna in
                        let fecha = lista[fila * 7 + columna]
                        DayCell(
                            fecha: fecha,
                            delMes: cal.isDate(fecha, equalTo: mes, toGranularity: .month),
                            seleccionado: cal.isDate(fecha, inSameDayAs: dia),
                            tareas: ordenadas(agrupados[fecha] ?? [])
                        )
                        .onTapGesture { dia = fecha }
                    }
                }
            }
        }
    }

    private func ordenadas(_ tareas: [Deber]) -> [Deber] {
        tareas.sorted {
            ($0.fechaEntrega ?? .distantFuture, $0.fechaCreacion) < ($1.fechaEntrega ?? .distantFuture, $1.fechaCreacion)
        }
    }

    /// Detalle del día seleccionado: ocupa el espacio restante, se desplaza y permite añadir una tarea con hora.
    private var detalleDelDia: some View {
        let tareas = ordenadas(porDia[dia] ?? [])
        let colores = PaletaPastel.asignar(a: tareas)
        return VStack(alignment: .leading, spacing: 8) {
            Text(dia.formatted(.dateTime.weekday(.wide).day().month(.wide)).capitalized)
                .font(.headline)
                .padding(.horizontal, 16)
                .padding(.top, 10)
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
                    .padding(.horizontal, 16)
                }
            }
            barraNuevaTarea
        }
    }

    private var barraNuevaTarea: some View {
        HStack(spacing: 8) {
            Image(systemName: "plus").foregroundStyle(.secondary)
            TextField(destino == nil ? "Crea una lista primero" : "Añadir tarea el \(FechaFormato.corto(dia))", text: $nuevoTitulo)
                .textFieldStyle(.plain)
                .onSubmit(crear)
                .disabled(destino == nil)
            DatePicker("Hora", selection: $hora, displayedComponents: .hourAndMinute)
                .labelsHidden()
                .help("Hora de entrega")
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
        let h = cal.dateComponents([.hour, .minute], from: hora)
        let fecha = cal.date(bySettingHour: h.hour ?? 18, minute: h.minute ?? 0, second: 0, of: dia) ?? dia
        onCrear(limpio, destino, fecha)
        nuevoTitulo = ""
    }
}

private struct DayCell: View {
    let fecha: Date
    let delMes: Bool
    let seleccionado: Bool
    let tareas: [Deber]

    private var esHoy: Bool { Calendar.current.isDateInToday(fecha) }

    var body: some View {
        VStack(spacing: 4) {
            Text("\(Calendar.current.component(.day, from: fecha))")
                .font(.system(size: 13, weight: esHoy ? .bold : .regular))
                .foregroundColor(esHoy ? .white : (delMes ? .primary : .secondary))
                .frame(width: 24, height: 24)
                .background(Circle().fill(esHoy ? Color.accentColor : .clear))

            let colores = PaletaPastel.asignar(a: tareas)
            HStack(spacing: 3) {
                ForEach(tareas.prefix(4)) { deber in
                    Circle()
                        .fill((colores[deber.id] ?? PaletaPastel.colores[0]).texto)
                        .frame(width: 6, height: 6)
                        .opacity(deber.completado ? 0.4 : 1)
                }
                if tareas.count > 4 {
                    Text("+").font(.caption2.bold()).foregroundStyle(.secondary)
                }
            }
            .frame(height: 8)
            Spacer(minLength: 0)
        }
        .padding(.top, 6)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(seleccionado ? Color.accentColor.opacity(0.14) : .clear)
        .overlay(Rectangle().stroke(Color.primary.opacity(0.08), lineWidth: 0.5))
        .opacity(delMes ? 1 : 0.55)
        .contentShape(Rectangle())
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

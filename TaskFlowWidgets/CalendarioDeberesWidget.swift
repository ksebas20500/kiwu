import WidgetKit
import SwiftUI

struct CalendarioEntry: TimelineEntry {
    let date: Date
    /// Primer día del mes que se muestra.
    let mes: Date
    /// Nombre de la lista elegida; nil si se muestran todas.
    let lista: String?
    let pendientes: [WidgetDeber]
    let hayDatos: Bool
}

struct CalendarioProvider: AppIntentTimelineProvider {
    typealias Intent = SeleccionListaIntent

    func placeholder(in context: Context) -> CalendarioEntry {
        let ahora = Date.now
        let ejemplo = [
            WidgetDeber(id: UUID(), titulo: "Informe de laboratorio", materia: "Química", materiaID: nil, fechaEntrega: ahora.addingTimeInterval(3600 * 5)),
            WidgetDeber(id: UUID(), titulo: "Ejercicios del capítulo 4", materia: "Cálculo", materiaID: nil, fechaEntrega: ahora.addingTimeInterval(86400 * 2)),
            WidgetDeber(id: UUID(), titulo: "Resumen de lectura", materia: "Historia", materiaID: nil, fechaEntrega: ahora.addingTimeInterval(86400 * 4))
        ]
        return CalendarioEntry(date: ahora, mes: Self.inicioDeMes(desplazado: 0), lista: nil, pendientes: ejemplo, hayDatos: true)
    }

    func snapshot(for configuration: SeleccionListaIntent, in context: Context) async -> CalendarioEntry {
        context.isPreview ? placeholder(in: context) : entrada(configuration)
    }

    func timeline(for configuration: SeleccionListaIntent, in context: Context) async -> Timeline<CalendarioEntry> {
        // Se refresca a medianoche para que el día actual se marque bien.
        let cal = Calendar.current
        let manana = cal.startOfDay(for: cal.date(byAdding: .day, value: 1, to: .now) ?? .now)
        return Timeline(entries: [entrada(configuration)], policy: .after(manana))
    }

    private func entrada(_ configuracion: SeleccionListaIntent) -> CalendarioEntry {
        let mes = Self.inicioDeMes(desplazado: AlmacenWidget.desplazamientoMes)
        guard let snapshot = AlmacenWidget.leer() else {
            return CalendarioEntry(date: .now, mes: mes, lista: nil, pendientes: [], hayDatos: false)
        }
        guard let elegida = configuracion.lista else {
            return CalendarioEntry(date: .now, mes: mes, lista: nil, pendientes: snapshot.pendientes, hayDatos: true)
        }
        let nombre = snapshot.listas.first { $0.id == elegida.id }?.nombre ?? elegida.nombre
        return CalendarioEntry(
            date: .now, mes: mes, lista: nombre,
            pendientes: snapshot.pendientes.filter { $0.materiaID == elegida.id },
            hayDatos: true
        )
    }

    private static func inicioDeMes(desplazado meses: Int) -> Date {
        let cal = Calendar.current
        let actual = cal.dateInterval(of: .month, for: .now)?.start ?? .now
        return cal.date(byAdding: .month, value: meses, to: actual) ?? actual
    }
}

struct CalendarioDeberesWidget: Widget {
    let kind = "CalendarioDeberes"

    var body: some WidgetConfiguration {
        AppIntentConfiguration(kind: kind, intent: SeleccionListaIntent.self, provider: CalendarioProvider()) { entrada in
            CalendarioWidgetView(entrada: entrada)
                .containerBackground(.fill.tertiary, for: .widget)
        }
        .configurationDisplayName("Calendario de deberes")
        .description("El mes con tus fechas de entrega marcadas. Pulsa un día para verlo en TaskFlow.")
        .supportedFamilies([.systemSmall, .systemMedium, .systemLarge])
    }
}

struct CalendarioWidgetView: View {
    let entrada: CalendarioEntry
    @Environment(\.widgetFamily) private var familia

    private var cal: Calendar { .current }
    private var compacto: Bool { familia == .systemMedium }

    // MARK: Datos derivados

    private var porDia: [Date: [WidgetDeber]] {
        Dictionary(grouping: entrada.pendientes.filter { $0.fechaEntrega != nil }) {
            cal.startOfDay(for: $0.fechaEntrega!)
        }
    }

    private var hoy: Date { cal.startOfDay(for: entrada.date) }

    private var desfase: Int {
        (cal.component(.weekday, from: entrada.mes) - cal.firstWeekday + 7) % 7
    }

    private var filas: Int {
        let diasMes = cal.range(of: .day, in: .month, for: entrada.mes)?.count ?? 30
        return (desfase + diasMes + 6) / 7
    }

    private var dias: [Date] {
        let primero = cal.date(byAdding: .day, value: -desfase, to: entrada.mes) ?? entrada.mes
        return (0..<(filas * 7)).compactMap { cal.date(byAdding: .day, value: $0, to: primero) }
    }

    private var iniciales: [String] {
        let s = cal.veryShortStandaloneWeekdaySymbols
        let i = cal.firstWeekday - 1
        return Array(s[i...] + s[..<i])
    }

    /// Deberes a mostrar en la agenda: primero los vencidos y luego los próximos, por fecha.
    private var agenda: [WidgetDeber] {
        entrada.pendientes
            .filter { $0.fechaEntrega != nil }
            .sorted { $0.fechaEntrega! < $1.fechaEntrega! }
    }

    // MARK: Cuerpo

    var body: some View {
        switch familia {
        case .systemSmall:
            pequeno.widgetURL(TaskFlowShared.urlCalendario(dia: hoy))
        case .systemMedium:
            HStack(alignment: .top, spacing: 12) {
                calendario.frame(maxWidth: .infinity)
                listaAgenda(maximo: 3)
                    .frame(width: 138)
            }
            .widgetURL(TaskFlowShared.urlCalendario(dia: nil))
        default:
            VStack(alignment: .leading, spacing: 8) {
                calendario
                Divider()
                listaAgenda(maximo: 3)
            }
            .widgetURL(TaskFlowShared.urlCalendario(dia: nil))
        }
    }

    // MARK: Widget pequeño: el día de hoy

    private var pequeno: some View {
        let deHoy = porDia[hoy] ?? []
        let siguiente = agenda.first { ($0.fechaEntrega ?? .distantPast) >= entrada.date }
        return VStack(alignment: .leading, spacing: 2) {
            Text(entrada.date.formatted(.dateTime.weekday(.wide)).uppercased())
                .font(.caption.weight(.semibold))
                .foregroundStyle(Color.accentColor)
            Text(entrada.date.formatted(.dateTime.day()))
                .font(.system(size: 44, weight: .light))
            Text(entrada.date.formatted(.dateTime.month(.wide)).capitalized)
                .font(.caption)
                .foregroundStyle(.secondary)
            Spacer(minLength: 0)
            if !entrada.hayDatos {
                Text("Abre TaskFlow para sincronizar")
                    .font(.caption2)
                    .foregroundStyle(.secondary)
            } else {
                Text(deHoy.isEmpty ? "Sin entregas hoy" : "\(deHoy.count) \(deHoy.count == 1 ? "entrega" : "entregas") hoy")
                    .font(.caption.weight(.semibold))
                    .foregroundColor(deHoy.isEmpty ? .secondary : .red)
                if let siguiente {
                    Text(siguiente.titulo)
                        .font(.caption2)
                        .foregroundStyle(.secondary)
                        .lineLimit(1)
                }
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
    }

    // MARK: Cuadrícula del mes

    private var calendario: some View {
        VStack(spacing: compacto ? 2 : 4) {
            cabecera
            HStack(spacing: 0) {
                ForEach(Array(iniciales.enumerated()), id: \.offset) { _, letra in
                    Text(letra)
                        .font(.system(size: compacto ? 8 : 10))
                        .foregroundStyle(.secondary)
                        .frame(maxWidth: .infinity)
                }
            }
            VStack(spacing: compacto ? 0 : 1) {
                ForEach(0..<filas, id: \.self) { fila in
                    HStack(spacing: 0) {
                        ForEach(0..<7, id: \.self) { columna in
                            celda(dias[fila * 7 + columna])
                        }
                    }
                }
            }
        }
    }

    private var cabecera: some View {
        HStack(spacing: 6) {
            Button(intent: CambiarMesIntent(delta: 0)) {
                Text((entrada.lista.map { "\($0) · " } ?? "") + entrada.mes.formatted(.dateTime.month(.wide).year()).capitalized)
                    .font(.system(size: compacto ? 11 : 13, weight: .semibold))
                    .lineLimit(1)
            }
            .buttonStyle(.plain)
            Spacer(minLength: 0)
            Button(intent: CambiarMesIntent(delta: -1)) {
                Image(systemName: "chevron.left").font(.system(size: compacto ? 10 : 12, weight: .semibold))
            }
            .buttonStyle(.plain)
            Button(intent: CambiarMesIntent(delta: 1)) {
                Image(systemName: "chevron.right").font(.system(size: compacto ? 10 : 12, weight: .semibold))
            }
            .buttonStyle(.plain)
        }
    }

    private func celda(_ dia: Date) -> some View {
        let esHoy = cal.isDate(dia, inSameDayAs: hoy)
        let delMes = cal.isDate(dia, equalTo: entrada.mes, toGranularity: .month)
        let tareas = porDia[dia] ?? []
        let vencido = dia < hoy
        let colores = PaletaPastel.asignar(ids: tareas.map(\.id))
        let lado: CGFloat = compacto ? 12 : 18

        return Link(destination: TaskFlowShared.urlCalendario(dia: dia)) {
            VStack(spacing: compacto ? 0 : 1) {
                Text("\(cal.component(.day, from: dia))")
                    .font(.system(size: compacto ? 8.5 : 11, weight: esHoy ? .bold : .regular))
                    .foregroundColor(esHoy ? .white : (delMes ? .primary : Color.secondary.opacity(0.5)))
                    .frame(width: lado, height: lado)
                    .background(Circle().fill(esHoy ? Color.accentColor : .clear))
                HStack(spacing: 2) {
                    ForEach(Array(tareas.prefix(3).enumerated()), id: \.offset) { _, deber in
                        Circle()
                            .fill(vencido ? Color.red : (colores[deber.id]?.texto ?? .orange))
                            .frame(width: compacto ? 3 : 4, height: compacto ? 3 : 4)
                    }
                }
                .frame(height: compacto ? 3 : 4)
                .opacity(delMes ? 1 : 0.4)
            }
            .frame(maxWidth: .infinity)
            .contentShape(Rectangle())
        }
    }

    // MARK: Agenda

    private func listaAgenda(maximo: Int) -> some View {
        VStack(alignment: .leading, spacing: compacto ? 5 : 6) {
            if !entrada.hayDatos {
                Text("Abre TaskFlow para sincronizar tus deberes")
                    .font(.caption2)
                    .foregroundStyle(.secondary)
            } else if agenda.isEmpty {
                Label("Sin entregas pendientes", systemImage: "checkmark.seal")
                    .font(.caption2)
                    .foregroundStyle(.secondary)
            } else {
                Text("Próximas entregas")
                    .font(.system(size: 10, weight: .semibold))
                    .foregroundStyle(.secondary)
                ForEach(agenda.prefix(maximo)) { deber in
                    fila(deber)
                }
            }
            Spacer(minLength: 0)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
    }

    private func fila(_ deber: WidgetDeber) -> some View {
        let vencido = deber.fechaEntrega.map { $0 < entrada.date } ?? false
        return HStack(alignment: .top, spacing: 6) {
            Button(intent: CompletarDeberIntent(id: deber.id.uuidString)) {
                Image(systemName: "circle")
                    .font(.system(size: 13))
                    .foregroundStyle(vencido ? Color.red : Color.accentColor)
            }
            .buttonStyle(.plain)

            Link(destination: TaskFlowShared.urlDeber(deber.id)) {
                VStack(alignment: .leading, spacing: 0) {
                    Text(deber.titulo)
                        .font(.system(size: 11, weight: .medium))
                        .lineLimit(1)
                    if let fecha = deber.fechaEntrega {
                        Text(FechaWidget.texto(fecha, ahora: entrada.date))
                            .font(.system(size: 9))
                            .foregroundColor(vencido ? .red : .secondary)
                            .lineLimit(1)
                    }
                }
            }
        }
    }
}

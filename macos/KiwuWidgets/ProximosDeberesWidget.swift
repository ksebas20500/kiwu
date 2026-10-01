import WidgetKit
import SwiftUI

struct DeberesEntry: TimelineEntry {
    let date: Date
    /// Nombre de la lista elegida; nil si se muestran todas.
    let lista: String?
    let pendientes: [WidgetDeber]
    let total: Int
    let hayDatos: Bool
}

struct DeberesProvider: AppIntentTimelineProvider {
    typealias Intent = SeleccionListaIntent

    func placeholder(in context: Context) -> DeberesEntry {
        DeberesEntry(date: .now, lista: nil, pendientes: Self.ejemplo, total: Self.ejemplo.count, hayDatos: true)
    }

    func snapshot(for configuration: SeleccionListaIntent, in context: Context) async -> DeberesEntry {
        context.isPreview ? placeholder(in: context) : entrada(en: .now, snapshot: AlmacenWidget.leer(), configuracion: configuration)
    }

    func timeline(for configuration: SeleccionListaIntent, in context: Context) async -> Timeline<DeberesEntry> {
        let snapshot = AlmacenWidget.leer()
        let ahora = Date.now
        let base = entrada(en: ahora, snapshot: snapshot, configuracion: configuration)

        // Una entrada por cada próxima fecha de entrega: así un deber pasa a "vencido" en su hora exacta.
        let cambios = Set(base.pendientes.compactMap(\.fechaEntrega).filter { $0 > ahora })
            .sorted()
            .prefix(8)
        let entradas = [base] + cambios.map { entrada(en: $0, snapshot: snapshot, configuracion: configuration) }
        return Timeline(entries: entradas, policy: .after(ahora.addingTimeInterval(30 * 60)))
    }

    private func entrada(en fecha: Date, snapshot: WidgetSnapshot?, configuracion: SeleccionListaIntent) -> DeberesEntry {
        guard let snapshot else {
            return DeberesEntry(date: fecha, lista: nil, pendientes: [], total: 0, hayDatos: false)
        }
        guard let elegida = configuracion.lista else {
            return DeberesEntry(date: fecha, lista: nil, pendientes: snapshot.pendientes, total: snapshot.totalPendientes, hayDatos: true)
        }
        let deLaLista = snapshot.pendientes.filter { $0.materiaID == elegida.id }
        let nombre = snapshot.listas.first { $0.id == elegida.id }?.nombre ?? elegida.nombre
        return DeberesEntry(date: fecha, lista: nombre, pendientes: deLaLista, total: deLaLista.count, hayDatos: true)
    }

    private static let ejemplo = [
        WidgetDeber(id: UUID(), titulo: "Informe de laboratorio", materia: "Química", materiaID: nil, fechaEntrega: .now.addingTimeInterval(3600 * 5)),
        WidgetDeber(id: UUID(), titulo: "Ejercicios del capítulo 4", materia: "Cálculo", materiaID: nil, fechaEntrega: .now.addingTimeInterval(86400)),
        WidgetDeber(id: UUID(), titulo: "Resumen de lectura", materia: "Historia", materiaID: nil, fechaEntrega: .now.addingTimeInterval(86400 * 3))
    ]
}

struct ProximosDeberesWidget: Widget {
    let kind = "ProximosDeberes"

    var body: some WidgetConfiguration {
        AppIntentConfiguration(kind: kind, intent: SeleccionListaIntent.self, provider: DeberesProvider()) { entrada in
            DeberesWidgetView(entrada: entrada)
                .containerBackground(.fill.tertiary, for: .widget)
        }
        .configurationDisplayName("Deberes")
        .description("Tus deberes pendientes. Elige una lista concreta o muestra todas, y márcalos como completados.")
        .supportedFamilies([.systemSmall, .systemMedium, .systemLarge])
    }
}

struct DeberesWidgetView: View {
    let entrada: DeberesEntry
    @Environment(\.widgetFamily) private var familia

    private var maximo: Int {
        switch familia {
        case .systemSmall: return 2
        case .systemMedium: return 3
        default: return 8
        }
    }

    private var visibles: [WidgetDeber] { Array(entrada.pendientes.prefix(maximo)) }

    var body: some View {
        let contenido = VStack(alignment: .leading, spacing: 8) {
            cabecera
            if !entrada.hayDatos {
                mensaje("Abre Kiwu para sincronizar tus deberes", icono: "arrow.triangle.2.circlepath")
            } else if visibles.isEmpty {
                mensaje(entrada.lista == nil ? "Todo al día" : "Sin deberes pendientes en esta lista", icono: "checkmark.seal.fill")
            } else {
                ForEach(visibles) { deber in fila(deber) }
                Spacer(minLength: 0)
                if entrada.total > visibles.count {
                    Text("+\(entrada.total - visibles.count) más")
                        .font(.caption2)
                        .foregroundStyle(.secondary)
                }
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)

        // En el widget pequeño solo se admite un enlace para todo el widget.
        if familia == .systemSmall, let primero = visibles.first {
            contenido.widgetURL(KiwuShared.urlDeber(primero.id))
        } else {
            // Los enlaces de cada fila tienen prioridad; el resto del widget abre la app en Tareas.
            contenido.widgetURL(KiwuShared.urlModulo("tareas"))
        }
    }

    private var cabecera: some View {
        HStack(spacing: 6) {
            Image(systemName: "checkmark.circle.fill")
                .foregroundStyle(Color.accentColor)
            Text(entrada.lista ?? "Deberes")
                .font(.headline)
                .lineLimit(1)
            Spacer()
            if entrada.total > 0 {
                Text("\(entrada.total)")
                    .font(.caption.weight(.semibold))
                    .padding(.horizontal, 7)
                    .padding(.vertical, 2)
                    .background(Capsule().fill(Color.accentColor.opacity(0.18)))
            }
        }
    }

    private func fila(_ deber: WidgetDeber) -> some View {
        let vencido = deber.fechaEntrega.map { $0 < entrada.date } ?? false
        let detalle = HStack(alignment: .top, spacing: 8) {
            VStack(alignment: .leading, spacing: 1) {
                Text(deber.titulo)
                    .font(.callout.weight(.medium))
                    .lineLimit(1)
                HStack(spacing: 4) {
                    Text(deber.materia)
                    if let fecha = deber.fechaEntrega {
                        Text("· " + FechaWidget.texto(fecha, ahora: entrada.date))
                            .foregroundColor(vencido ? .red : .secondary)
                    }
                }
                .font(.caption2)
                .foregroundStyle(.secondary)
                .lineLimit(1)
            }
            Spacer(minLength: 0)
        }

        return HStack(alignment: .top, spacing: 8) {
            Button(intent: CompletarDeberIntent(id: deber.id.uuidString)) {
                Image(systemName: "circle")
                    .font(.system(size: 17))
                    .foregroundStyle(vencido ? Color.red : Color.accentColor)
            }
            .buttonStyle(.plain)

            if familia == .systemSmall {
                detalle
            } else {
                Link(destination: KiwuShared.urlDeber(deber.id)) { detalle }
            }
        }
    }

    private func mensaje(_ texto: String, icono: String) -> some View {
        VStack(spacing: 6) {
            Image(systemName: icono)
                .font(.title2)
            Text(texto)
                .font(.caption)
                .multilineTextAlignment(.center)
        }
        .foregroundStyle(.secondary)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

enum FechaWidget {
    static func texto(_ fecha: Date, ahora: Date) -> String {
        let cal = Calendar.current
        let hora = fecha.formatted(date: .omitted, time: .shortened)
        if cal.isDate(fecha, inSameDayAs: ahora) { return "Hoy \(hora)" }
        if let manana = cal.date(byAdding: .day, value: 1, to: ahora), cal.isDate(fecha, inSameDayAs: manana) { return "Mañana" }
        if let ayer = cal.date(byAdding: .day, value: -1, to: ahora), cal.isDate(fecha, inSameDayAs: ayer) { return "Ayer" }
        return fecha.formatted(.dateTime.day().month(.abbreviated))
    }
}

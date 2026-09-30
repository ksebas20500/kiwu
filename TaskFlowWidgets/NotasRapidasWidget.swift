import WidgetKit
import SwiftUI

struct NotasEntry: TimelineEntry {
    let date: Date
    let cuaderno: UUID?
    let titulo: String
    let notas: [WidgetNota]
    let hayDatos: Bool
}

struct NotasProvider: AppIntentTimelineProvider {
    typealias Intent = SeleccionCuadernoIntent

    func placeholder(in context: Context) -> NotasEntry {
        NotasEntry(date: .now, cuaderno: nil, titulo: "Notas rápidas", notas: Self.ejemplo, hayDatos: true)
    }

    func snapshot(for configuration: SeleccionCuadernoIntent, in context: Context) async -> NotasEntry {
        context.isPreview ? placeholder(in: context) : entrada(configuration)
    }

    func timeline(for configuration: SeleccionCuadernoIntent, in context: Context) async -> Timeline<NotasEntry> {
        Timeline(entries: [entrada(configuration)], policy: .after(Date.now.addingTimeInterval(30 * 60)))
    }

    private func entrada(_ configuracion: SeleccionCuadernoIntent) -> NotasEntry {
        guard let snapshot = AlmacenWidget.leer() else {
            return NotasEntry(date: .now, cuaderno: nil, titulo: "Notas rápidas", notas: [], hayDatos: false)
        }
        guard let elegido = configuracion.cuaderno,
              let cuaderno = snapshot.cuadernos.first(where: { $0.id == elegido.id }) else {
            return NotasEntry(date: .now, cuaderno: nil, titulo: "Notas rápidas", notas: snapshot.notas, hayDatos: true)
        }
        return NotasEntry(
            date: .now,
            cuaderno: cuaderno.id,
            titulo: "\(cuaderno.icono) \(cuaderno.nombre)",
            notas: snapshot.notas.filter { $0.cuadernoID == cuaderno.id },
            hayDatos: true
        )
    }

    private static let ejemplo = [
        WidgetNota(id: UUID(), titulo: "Conceptos de termodinámica", icono: "🔬", resumen: "Primera ley: la energía se conserva…", cuadernoID: UUID(), fecha: .now, favorita: false),
        WidgetNota(id: UUID(), titulo: "Ideas para el proyecto", icono: "💡", resumen: "Investigar sobre redes neuronales", cuadernoID: UUID(), fecha: .now, favorita: true)
    ]
}

struct NotasRapidasWidget: Widget {
    let kind = "NotasRapidas"

    var body: some WidgetConfiguration {
        AppIntentConfiguration(kind: kind, intent: SeleccionCuadernoIntent.self, provider: NotasProvider()) { entrada in
            NotasWidgetView(entrada: entrada)
                .containerBackground(.fill.tertiary, for: .widget)
        }
        .configurationDisplayName("Nota rápida")
        .description("Crea una nota nueva con un toque y accede a las más recientes de un cuaderno.")
        .supportedFamilies([.systemSmall, .systemMedium, .systemLarge])
    }
}

struct NotasWidgetView: View {
    let entrada: NotasEntry
    @Environment(\.widgetFamily) private var familia

    private var maximo: Int {
        switch familia {
        case .systemSmall: return 1
        case .systemMedium: return 2
        default: return 6
        }
    }

    private var nuevaNota: URL { TaskFlowShared.urlNuevaNota(cuaderno: entrada.cuaderno) }
    private var visibles: [WidgetNota] { Array(entrada.notas.prefix(maximo)) }

    var body: some View {
        let contenido = VStack(alignment: .leading, spacing: 8) {
            HStack(spacing: 6) {
                Image(systemName: "note.text")
                    .foregroundStyle(Color.accentColor)
                Text(entrada.titulo)
                    .font(.headline)
                    .lineLimit(1)
                Spacer()
            }

            if !entrada.hayDatos {
                Text("Abre TaskFlow para sincronizar tus notas")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            } else if visibles.isEmpty {
                Text("Aún no hay notas")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            } else {
                ForEach(visibles) { nota in fila(nota) }
            }

            Spacer(minLength: 0)
            botonNueva
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)

        // En el widget pequeño solo se admite un enlace para todo el widget: crear nota.
        if familia == .systemSmall {
            contenido.widgetURL(nuevaNota)
        } else {
            // Los enlaces de cada nota y del botón tienen prioridad; el resto del widget abre Notas.
            contenido.widgetURL(TaskFlowShared.urlModulo("notas"))
        }
    }

    private var botonNueva: some View {
        let etiqueta = Label("Nueva nota", systemImage: "square.and.pencil")
            .font(.caption.weight(.semibold))
            .foregroundStyle(.white)
            .padding(.horizontal, 10)
            .padding(.vertical, 6)
            .frame(maxWidth: familia == .systemSmall ? .infinity : nil)
            .background(Capsule().fill(Color.accentColor))

        return Group {
            if familia == .systemSmall {
                etiqueta
            } else {
                Link(destination: nuevaNota) { etiqueta }
            }
        }
    }

    @ViewBuilder
    private func fila(_ nota: WidgetNota) -> some View {
        let detalle = HStack(alignment: .top, spacing: 8) {
            if nota.icono.isEmpty {
                Image(systemName: "doc.text").foregroundStyle(.secondary)
            } else {
                Text(nota.icono)
            }
            VStack(alignment: .leading, spacing: 1) {
                Text(nota.titulo.isEmpty ? "Sin título" : nota.titulo)
                    .font(.callout.weight(.medium))
                    .lineLimit(1)
                if !nota.resumen.isEmpty && familia != .systemSmall {
                    Text(nota.resumen)
                        .font(.caption2)
                        .foregroundStyle(.secondary)
                        .lineLimit(1)
                }
            }
            Spacer(minLength: 0)
        }

        if familia == .systemSmall {
            detalle
        } else {
            Link(destination: TaskFlowShared.urlNota(nota.id)) { detalle }
        }
    }
}

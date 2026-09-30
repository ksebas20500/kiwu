import SwiftUI
import CoreData

enum SidebarItem: Hashable {
    case hoy, proximos, calendario, todos, completadas
    case materia(UUID)
}

extension Calendar {
    func finDelDia(_ fecha: Date) -> Date {
        date(byAdding: DateComponents(day: 1, second: -1), to: startOfDay(for: fecha)) ?? fecha
    }
}

extension NSManagedObject {
    var estaEliminado: Bool { isDeleted || managedObjectContext == nil }
}

extension Deber {
    var vencido: Bool { !completado && (fechaEntrega.map { $0 < .now } ?? false) }
}

enum FechaFormato {
    static func corto(_ fecha: Date) -> String {
        let cal = Calendar.current
        if cal.isDateInToday(fecha) { return "Hoy" }
        if cal.isDateInTomorrow(fecha) { return "Mañana" }
        if cal.isDateInYesterday(fecha) { return "Ayer" }
        return fecha.formatted(.dateTime.day().month(.abbreviated))
    }

    static func completo(_ fecha: Date) -> String {
        "\(corto(fecha)), \(fecha.formatted(date: .omitted, time: .shortened))"
    }
}

extension NoteBlockKind {
    var titulo: String {
        switch self {
        case .paragraph: return "Texto"
        case .heading: return "Título 1"
        case .subheading: return "Título 2"
        case .bulletedList: return "Lista con viñetas"
        case .numberedList: return "Lista numerada"
        case .checklist: return "Lista de tareas"
        case .quote: return "Cita"
        case .callout: return "Destacado"
        case .code: return "Código"
        case .image: return "Imagen"
        case .file: return "Archivo adjunto (PDF, Word)"
        case .divider: return "Divisor"
        }
    }

    var icono: String {
        switch self {
        case .paragraph: return "text.alignleft"
        case .heading: return "textformat.size"
        case .subheading: return "textformat.size.smaller"
        case .bulletedList: return "list.bullet"
        case .numberedList: return "list.number"
        case .checklist: return "checklist"
        case .quote: return "text.quote"
        case .callout: return "lightbulb"
        case .code: return "chevron.left.forwardslash.chevron.right"
        case .image: return "photo"
        case .file: return "paperclip"
        case .divider: return "minus"
        }
    }
}

struct EmptyStateView: View {
    let titulo: String
    let icono: String

    var body: some View {
        VStack(spacing: 8) {
            Image(systemName: icono)
                .font(.system(size: 36))
            Text(titulo)
                .font(.title3)
        }
        .foregroundStyle(.secondary)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

struct CheckboxView: View {
    let marcado: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Image(systemName: marcado ? "checkmark.square.fill" : "square")
                .font(.system(size: 17))
                .foregroundColor(marcado ? .accentColor : .secondary)
        }
        .buttonStyle(.plain)
    }
}

enum IconoLateral {
    case simbolo(String)
    case emoji(String)
}

struct SidebarFila: View {
    let activo: Bool
    let icono: IconoLateral
    let titulo: String
    let cuenta: Int
    let accion: () -> Void

    var body: some View {
        Button(action: accion) {
            HStack(spacing: 10) {
                Group {
                    switch icono {
                    case .simbolo(let nombre):
                        Image(systemName: nombre)
                            .foregroundStyle(activo ? Color.accentColor : .secondary)
                    case .emoji(let texto):
                        Text(texto)
                    }
                }
                .frame(width: 20)
                Text(titulo).lineLimit(1)
                Spacer()
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

struct EmojiPicker: View {
    let onElegir: (String) -> Void
    var onQuitar: (() -> Void)?

    private static let emojis = [
        "📝", "📖", "📚", "💡", "🧠", "🔬", "🧪", "📐", "💻", "🧮", "📊", "🗂️",
        "📌", "⭐️", "🎯", "🔥", "✅", "❓", "📅", "🗒️", "🎓", "🏛️", "⚖️", "🩺",
        "🌍", "🎨", "🎵", "⚙️", "🔍", "📎", "🧾", "🖋️", "🚀", "🧬", "📈", "🗣️"
    ]

    var body: some View {
        VStack(spacing: 8) {
            LazyVGrid(columns: Array(repeating: GridItem(.fixed(34)), count: 6), spacing: 6) {
                ForEach(Self.emojis, id: \.self) { emoji in
                    Button { onElegir(emoji) } label: {
                        Text(emoji).font(.system(size: 22))
                    }
                    .buttonStyle(.plain)
                }
            }
            if let onQuitar {
                Divider()
                Button("Quitar icono", action: onQuitar)
            }
        }
        .padding(12)
    }
}

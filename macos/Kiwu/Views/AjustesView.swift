import SwiftUI
import AppKit
import UniformTypeIdentifiers

private enum PestanaAjustes: String, CaseIterable, Identifiable {
    case apariencia = "Apariencia"
    case atajos = "Atajos"
    case copia = "Copia"
    var id: String { rawValue }
    var icono: String {
        switch self {
        case .apariencia: return "paintpalette"
        case .atajos: return "keyboard"
        case .copia: return "externaldrive"
        }
    }
}

struct AjustesView: View {
    @Environment(\.dismiss) private var dismiss
    @ObservedObject private var ajustes = AjustesStore.shared
    @State private var pestana: PestanaAjustes = .apariencia

    var body: some View {
        VStack(spacing: 0) {
            HStack {
                Text("Ajustes").font(.title2.bold())
                Spacer()
                Picker("", selection: $pestana) {
                    ForEach(PestanaAjustes.allCases) { Label($0.rawValue, systemImage: $0.icono).tag($0) }
                }
                .pickerStyle(.segmented)
                .labelsHidden()
                .frame(width: 330)
                Spacer()
                Button("Listo") { dismiss() }
                    .keyboardShortcut(.defaultAction)
            }
            .padding(20)
            Divider()

            ScrollView {
                Group {
                    switch pestana {
                    case .apariencia: AparienciaAjustes(ajustes: ajustes)
                    case .atajos: AtajosAjustes(ajustes: ajustes)
                    case .copia: CopiaAjustes()
                    }
                }
                .padding(24)
                .frame(maxWidth: .infinity, alignment: .leading)
            }
        }
        .frame(width: 620, height: 560)
    }
}

// MARK: Apariencia

private struct AparienciaAjustes: View {
    @ObservedObject var ajustes: AjustesStore

    var body: some View {
        VStack(alignment: .leading, spacing: 22) {
            seccion("Tema") {
                Picker("Tema", selection: $ajustes.tema) {
                    ForEach(TemaApp.allCases) { Text($0.titulo).tag($0) }
                }
                .pickerStyle(.segmented)
                .labelsHidden()
                .frame(width: 280)
            }

            seccion("Color de la app") {
                SelectorAcento(acento: $ajustes.acento)
            }

            seccion("Fondo de la app") {
                HStack(alignment: .top, spacing: 16) {
                    vistaPrevia
                    VStack(alignment: .leading, spacing: 8) {
                        Button("Elegir imagen…", action: elegir)
                        if ajustes.fondo != nil {
                            Button("Quitar fondo", role: .destructive) { ajustes.quitarFondo() }
                        }
                        Text("Se copia a la carpeta de la app; puedes mover o borrar el original.")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                            .fixedSize(horizontal: false, vertical: true)
                    }
                }

                control("Visibilidad del fondo", valor: $ajustes.visibilidadFondo, rango: 0...1, formato: porcentaje)
                control("Opacidad de los paneles", valor: $ajustes.opacidadPaneles, rango: 0...1, formato: porcentaje,
                        ayuda: "Menos opacidad = se ve más el fondo a través de la app.")
                control("Desenfoque", valor: $ajustes.desenfoque, rango: 0...30, formato: { "\(Int($0)) px" })
            }
        }
    }

    private var vistaPrevia: some View {
        ZStack {
            FondoApp(ajustes: ajustes)
            HStack(spacing: 0) {
                Color.primary.opacity(0.06).frame(width: 34)
                VStack(alignment: .leading, spacing: 6) {
                    RoundedRectangle(cornerRadius: 3).fill(Color.primary.opacity(0.25)).frame(width: 60, height: 6)
                    RoundedRectangle(cornerRadius: 3).fill(Color.primary.opacity(0.15)).frame(width: 90, height: 6)
                    RoundedRectangle(cornerRadius: 3).fill(Color.primary.opacity(0.15)).frame(width: 75, height: 6)
                }
                .padding(10)
                Spacer()
            }
        }
        .frame(width: 180, height: 110)
        .clipShape(RoundedRectangle(cornerRadius: 10))
        .overlay(RoundedRectangle(cornerRadius: 10).stroke(Color.primary.opacity(0.15)))
    }

    private func elegir() {
        let panel = NSOpenPanel()
        panel.allowedContentTypes = [.image]
        panel.allowsMultipleSelection = false
        panel.canChooseDirectories = false
        panel.message = "Elige la imagen de fondo"
        if panel.runModal() == .OK, let url = panel.url { ajustes.elegirFondo(url) }
    }

    private func porcentaje(_ v: Double) -> String { "\(Int((v * 100).rounded())) %" }

    private func seccion(_ titulo: String, @ViewBuilder contenido: () -> some View) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(titulo).font(.headline)
            contenido()
        }
    }

    private func control(_ titulo: String, valor: Binding<Double>, rango: ClosedRange<Double>,
                         formato: @escaping (Double) -> String, ayuda: String? = nil) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            HStack {
                Text(titulo)
                Spacer()
                Text(formato(valor.wrappedValue)).foregroundStyle(.secondary).monospacedDigit()
            }
            Slider(value: valor, in: rango)
            if let ayuda { Text(ayuda).font(.caption).foregroundStyle(.secondary) }
        }
        .opacity(ajustes.fondo == nil ? 0.5 : 1)
        .disabled(ajustes.fondo == nil)
    }
}

// MARK: Color de la app

private struct SelectorAcento: View {
    @Binding var acento: String

    private var personalizado: Binding<Color> {
        Binding(
            get: { ColorAcento.hex(de: acento).map { ColorAcento.color(hex: $0) } ?? .accentColor },
            set: { nuevo in
                if let c = NSColor(nuevo).usingColorSpace(.sRGB) {
                    acento = ColorAcento.hex(r: Double(c.redComponent), g: Double(c.greenComponent), b: Double(c.blueComponent))
                }
            }
        )
    }

    private var esPredefinido: Bool { ColorAcento.predefinidos.contains { $0.nombre == acento } }

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(spacing: 10) {
                ForEach(ColorAcento.predefinidos, id: \.nombre) { p in
                    muestra(ColorAcento.color(hex: p.hex), activo: acento == p.nombre, ayuda: p.titulo) { acento = p.nombre }
                }
            }
            HStack(spacing: 12) {
                Button(acento == ColorAcento.sistema ? "✓ Colores del sistema" : "Colores del sistema") { acento = ColorAcento.sistema }
                ColorPicker("Otro", selection: personalizado, supportsOpacity: false)
                    .fixedSize()
                if !esPredefinido && acento != ColorAcento.sistema { Text("Personalizado").font(.caption).foregroundStyle(.secondary) }
            }
            Text("Solo cambia este Mac: no se incluye en la copia de seguridad.")
                .font(.caption)
                .foregroundStyle(.secondary)
        }
    }

    private func muestra(_ color: Color, activo: Bool, ayuda: String, accion: @escaping () -> Void) -> some View {
        Button(action: accion) {
            Circle().fill(color).frame(width: 26, height: 26)
                .overlay(Circle().stroke(Color.primary.opacity(activo ? 0.9 : 0.15), lineWidth: activo ? 3 : 1))
        }
        .buttonStyle(.plain)
        .help(ayuda)
    }
}

// MARK: Copia de seguridad

private struct CopiaAjustes: View {
    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            Text("Copia de seguridad").font(.headline)
            Text("Guarda tus listas, tareas, cuadernos, notas y horarios en un archivo .kiwu.json. Es el mismo formato de Kiwu para Linux y Android, así que sirve para pasar tus datos de un dispositivo a otro.")
                .font(.callout)
                .foregroundStyle(.secondary)
                .fixedSize(horizontal: false, vertical: true)
            HStack {
                Button("Exportar copia…") { CopiaUI.exportar() }
                Button("Importar copia…") { CopiaUI.importar() }
            }
            Text("Al importar no se modifica ni se duplica lo que ya tienes: solo se añade lo nuevo. Las carpetas vinculadas a una lista no se incluyen.")
                .font(.caption)
                .foregroundStyle(.secondary)
                .fixedSize(horizontal: false, vertical: true)
        }
    }
}

// MARK: Atajos

private struct AtajosAjustes: View {
    @ObservedObject var ajustes: AjustesStore
    @State private var grabando: AccionApp?
    @State private var aviso: String?
    @State private var monitor: Any?

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            Text("Pulsa «Cambiar» y luego la combinación que quieras (debe incluir ⌘, ⌃ o ⌥). Esc cancela.")
                .font(.callout)
                .foregroundStyle(.secondary)

            VStack(spacing: 0) {
                ForEach(AccionApp.allCases) { accion in
                    fila(accion)
                    if accion != AccionApp.allCases.last { Divider() }
                }
            }
            .background(RoundedRectangle(cornerRadius: 10).fill(Color.primary.opacity(0.05)))

            HStack {
                if let aviso { Label(aviso, systemImage: "exclamationmark.triangle.fill").foregroundStyle(.orange).font(.callout) }
                Spacer()
                Button("Restaurar todos") { ajustes.restaurarTodos(); aviso = nil }
            }
        }
        .onDisappear(perform: detener)
    }

    private func fila(_ accion: AccionApp) -> some View {
        let esta = grabando == accion
        let atajo = ajustes.atajo(accion)
        return HStack {
            Text(accion.titulo)
            Spacer()
            Text(esta ? "Pulsa la combinación…" : atajo.texto)
                .font(.system(.body, design: .rounded).weight(.medium))
                .padding(.horizontal, 10)
                .padding(.vertical, 4)
                .background(RoundedRectangle(cornerRadius: 6).fill(esta ? Color.accentColor.opacity(0.25) : Color.primary.opacity(0.08)))
            Button(esta ? "Cancelar" : "Cambiar") { esta ? detener() : empezar(accion) }
            Button {
                ajustes.restaurar(accion)
            } label: { Image(systemName: "arrow.counterclockwise") }
                .help("Restaurar el atajo predeterminado")
                .disabled(atajo == accion.atajoPredeterminado)
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 8)
    }

    private func empezar(_ accion: AccionApp) {
        detener()
        aviso = nil
        grabando = accion
        monitor = NSEvent.addLocalMonitorForEvents(matching: .keyDown) { evento in
            if evento.keyCode == 53 { detener(); return nil }  // Esc
            guard let tecla = evento.charactersIgnoringModifiers?.lowercased(), tecla.count == 1,
                  tecla.unicodeScalars.first.map({ !CharacterSet.controlCharacters.contains($0) }) ?? false else { return nil }
            let f = evento.modifierFlags
            let nuevo = Atajo(tecla, comando: f.contains(.command), mayus: f.contains(.shift),
                              opcion: f.contains(.option), control: f.contains(.control))
            guard nuevo.comando || nuevo.control || nuevo.opcion else {
                aviso = "Incluye ⌘, ⌃ o ⌥ para no chocar con la escritura."
                return nil
            }
            if let otra = ajustes.conflicto(de: nuevo, excepto: accion) {
                aviso = "\(nuevo.texto) ya se usa en «\(otra.titulo)»."
                return nil
            }
            ajustes.asignar(nuevo, a: accion)
            detener()
            return nil
        }
    }

    private func detener() {
        if let monitor { NSEvent.removeMonitor(monitor) }
        monitor = nil
        grabando = nil
    }
}

// MARK: Comandos de menú (aplican los atajos configurados)

struct ComandosApp: Commands {
    @ObservedObject var ajustes = AjustesStore.shared

    var body: some Commands {
        CommandGroup(after: .newItem) {
            Divider()
            Button("Exportar copia…") { CopiaUI.exportar() }
            Button("Importar copia…") { CopiaUI.importar() }
        }
        CommandMenu("Kiwu") {
            ForEach(AccionApp.allCases) { accion in
                let atajo = ajustes.atajo(accion)
                Button(accion.titulo) { ajustes.disparar(accion) }
                    .keyboardShortcut(atajo.equivalente, modifiers: atajo.modificadores)
            }
        }
    }
}

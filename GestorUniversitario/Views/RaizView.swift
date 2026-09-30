import SwiftUI

enum Modulo: String {
    case tareas, notas
}

/// Raíz de la app: barra lateral de módulos (Tareas / Notas) y el módulo activo.
struct RaizView: View {
    @AppStorage("modulo") private var modulo: Modulo = .tareas
    @Environment(\.managedObjectContext) private var context
    private let deberService = DeberService(persistence: .shared, reminders: ReminderService())
    private let materiaService = MateriaService(persistence: .shared, reminders: ReminderService())

    var body: some View {
        HStack(spacing: 0) {
            VStack(spacing: 10) {
                boton(.tareas, icono: "checkmark.square", ayuda: "Tareas")
                boton(.notas, icono: "note.text", ayuda: "Notas")
                Spacer()
            }
            .padding(.top, 16)
            .frame(width: 56)
            .background(Color.primary.opacity(0.06))

            Divider()

            switch modulo {
            case .tareas: ContentView()
            case .notas: NotasRootView()
            }
        }
        .frame(minWidth: 1060, minHeight: 620)
        .onOpenURL { AppDelegate.procesar($0, origen: "onOpenURL") }
        .onAppear(perform: sincronizarWidget)
        .onReceive(NotificationCenter.default.publisher(for: NSApplication.didBecomeActiveNotification)) { _ in
            sincronizarWidget()
        }
    }

    /// Aplica los deberes completados desde el widget y refresca sus datos.
    private func sincronizarWidget() {
        materiaService.comprobarTodas((try? context.fetch(Materia.fetchRequest())) ?? [])
        WidgetDataService.aplicarCompletadosDelWidget(context: context, service: deberService)
        WidgetDataService.actualizar(context)
    }

    private func boton(_ destino: Modulo, icono: String, ayuda: String) -> some View {
        Button {
            modulo = destino
        } label: {
            Image(systemName: icono)
                .font(.system(size: 19))
                .foregroundStyle(modulo == destino ? Color.accentColor : .secondary)
                .frame(width: 40, height: 40)
                .background(RoundedRectangle(cornerRadius: 10).fill(modulo == destino ? Color.accentColor.opacity(0.15) : .clear))
        }
        .buttonStyle(.plain)
        .help(ayuda)
    }
}

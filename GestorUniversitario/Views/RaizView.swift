import SwiftUI

enum Modulo: String {
    case tareas, notas
}

/// Raíz de la app: barra lateral de módulos (Tareas / Notas) y el módulo activo.
struct RaizView: View {
    @AppStorage("modulo") private var modulo: Modulo = .tareas
    @Environment(\.managedObjectContext) private var context
    @ObservedObject private var ajustes = AjustesStore.shared
    @State private var mostrandoAjustes = false
    private let deberService = DeberService(persistence: .shared, reminders: ReminderService())
    private let materiaService = MateriaService(persistence: .shared, reminders: ReminderService())

    var body: some View {
        HStack(spacing: 0) {
            VStack(spacing: 10) {
                boton(.tareas, icono: "checkmark.square", ayuda: "Tareas")
                boton(.notas, icono: "note.text", ayuda: "Notas")
                Spacer()
                Button {
                    mostrandoAjustes = true
                } label: {
                    Image(systemName: "gearshape")
                        .font(.system(size: 22))
                        .foregroundStyle(.secondary)
                        .frame(width: 52, height: 52)
                }
                .buttonStyle(.plain)
                .help("Ajustes")
            }
            .padding(.top, 16)
            .padding(.bottom, 12)
            .frame(width: 92)
            .background(Color.primary.opacity(0.06))

            Divider()

            switch modulo {
            case .tareas: ContentView()
            case .notas: NotasRootView()
            }
        }
        .frame(minWidth: 1060, minHeight: 620)
        .background(FondoApp(ajustes: ajustes))
        .preferredColorScheme(ajustes.tema.esquema)
        .sheet(isPresented: $mostrandoAjustes) {
            AjustesView().preferredColorScheme(ajustes.tema.esquema)
        }
        .onReceive(ajustes.acciones) { accion in
            switch accion {
            case .irTareas: modulo = .tareas
            case .irNotas: modulo = .notas
            case .abrirAjustes: mostrandoAjustes = true
            case .irHoy, .irCalendario, .vistaLista, .vistaTablero, .nuevaTarea:
                if modulo != .tareas { modulo = .tareas }
            }
        }
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
                .font(.system(size: 22))
                .foregroundStyle(modulo == destino ? Color.accentColor : .secondary)
                .frame(width: 52, height: 52)
                .background(RoundedRectangle(cornerRadius: 13).fill(modulo == destino ? Color.accentColor.opacity(0.15) : .clear))
        }
        .buttonStyle(.plain)
        .help(ayuda)
    }
}

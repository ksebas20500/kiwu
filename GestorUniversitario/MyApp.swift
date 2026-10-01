import SwiftUI

@main
struct KiwuApp: App {
    @NSApplicationDelegateAdaptor(AppDelegate.self) private var appDelegate
    private let persistenceController = PersistenceController.shared

    var body: some Scene {
        Window("Kiwu", id: "principal") {
            RaizView()
        }
        .defaultSize(width: 1240, height: 760)
        .commands { ComandosApp() }
        .environment(\.managedObjectContext, persistenceController.container.viewContext)
    }
}

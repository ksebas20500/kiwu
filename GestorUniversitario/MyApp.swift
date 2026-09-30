import SwiftUI

@main
struct TaskFlowApp: App {
    @NSApplicationDelegateAdaptor(AppDelegate.self) private var appDelegate
    private let persistenceController = PersistenceController.shared

    var body: some Scene {
        Window("TaskFlow", id: "principal") {
            RaizView()
        }
        .defaultSize(width: 1240, height: 760)
        .environment(\.managedObjectContext, persistenceController.container.viewContext)
    }
}

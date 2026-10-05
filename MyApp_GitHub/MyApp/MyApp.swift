import SwiftUI

@main
struct MyApp: App {
    init() {
        _ = ProactiveMessageManager.shared
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .onAppear {
                    ProactiveMessageManager.shared.start(chatName: "luo")
                }
        }
    }
}

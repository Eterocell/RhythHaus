import Shared
import SwiftUI
import UIKit

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context _: Self.Context) -> UIViewController {
        let controller = MainViewControllerKt.MainViewController()
        RhythHausViewControllerRegistry.presenter = controller
        return controller
    }

    func updateUIViewController(_: UIViewController, context _: Self.Context) {}
}

struct ContentView: View {
    var body: some View {
        ComposeView()
            .ignoresSafeArea()
    }
}

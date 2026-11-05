import SwiftUI
import UIKit
import ComposeApp

@main
struct iOSApp: App {
    
    let permissionManager = IOSPermissionManager()
    let reviewLauncher = IOSReviewLauncher()
    let appleSignInCoordinator = IOSAppleSignInCoordinator()
    let secureSessionStorage = IOSSecureSessionStorage()
    private let nativeViewFactory = IOSNativeViewFactory.shared
    private let iOSAppComponent: IosAppComponent

    init() {
        self.iOSAppComponent = create(
            permissionManager: permissionManager,
            reviewLauncher: reviewLauncher,
            appleSignInCoordinator: appleSignInCoordinator,
            secureSessionStorage: secureSessionStorage,
            nativeViewFactory: nativeViewFactory
        )
        iOSAppComponent.telemetry.initialize()
        // Construct every @AutoInit singleton up front — resolving the set is
        // what forces construction (AppEventDispatcher's lifecycle attach etc.).
        _ = iOSAppComponent.autoInits
        ShakeWindow.install()
    }
    
    var body: some Scene {
        WindowGroup {
            RootComposeView(
                appComponent: iOSAppComponent,
                nativeViewFactory: nativeViewFactory
            )
            .onOpenURL { url in
                // Forward URLs from custom-scheme links and Universal Links
                // into the Kotlin DeepLinkBridge — App.kt collects from it
                // and calls navController.handleDeepLink.
                iOSAppComponent.deepLinkBridge.emit(url: url.absoluteString)
            }
        }
    }
}

struct RootComposeView: View {
    @Environment(\.scenePhase) private var scenePhase
    let appComponent: IosAppComponent
    let nativeViewFactory: KMPTemplateNativeViewFactory

    var body: some View {
        ComposeView(
            appComponent: appComponent,
            nativeViewFactory: nativeViewFactory
        )
        .ignoresSafeArea()
    }
}

struct ComposeView: UIViewControllerRepresentable {
    let appComponent: IosAppComponent
    let nativeViewFactory: KMPTemplateNativeViewFactory

    func makeUIViewController(context: Context) -> UIViewController {
        // iOS 26 gets the system's Liquid Glass tab bar over Compose (hidden until Kotlin renders
        // tabs into it); before it, an app with tabs draws its own in Compose.
        if #available(iOS 26, *) {
            let shell = GlassShellController()
            shell.setContent(
                MainViewControllerKt.MainViewController(appComponent: appComponent, systemTabBar: shell)
            )
            return shell
        }
        return MainViewControllerKt.MainViewController(appComponent: appComponent, systemTabBar: nil)
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}


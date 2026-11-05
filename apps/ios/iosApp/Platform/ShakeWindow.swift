import ObjectiveC
import UIKit
import ComposeApp

/// Hands UIKit's shake gesture to Kotlin (`IosShakeDetector`), which decides what a shake opens.
///
/// The window, because a shake goes to the first responder and climbs the responder chain from
/// there. In this app the first responder is whatever Compose made it (a text field's input view,
/// or nothing at all), never a view or controller of ours, and with no first responder the event
/// goes straight to the window. The window is the one responder every shake reaches.
///
/// SwiftUI owns the window's class, so no subclass. And not an `extension UIWindow` override
/// either: Wiretap (debug builds) already overrides `motionEnded` in one, and with two the
/// runtime keeps one of them, so ours silently never ran. This wraps whatever implementation the
/// window has at launch, Wiretap's included, and calls it after posting.
enum ShakeWindow {
    private static var installed = false

    static func install() {
        guard !installed else { return }
        installed = true

        let selector = #selector(UIResponder.motionEnded(_:with:))
        guard let method = class_getInstanceMethod(UIWindow.self, selector) else { return }
        typealias MotionEnded = @convention(c) (UIWindow, Selector, UIEvent.EventSubtype, UIEvent?) -> Void
        let previous = unsafeBitCast(method_getImplementation(method), to: MotionEnded.self)
        let wrapped: @convention(block) (UIWindow, UIEvent.EventSubtype, UIEvent?) -> Void = { window, motion, event in
            if motion == .motionShake {
                SystemShake.shared.post()
            }
            previous(window, selector, motion, event)
        }
        let implementation = imp_implementationWithBlock(wrapped)
        // UIWindow may only inherit the method from UIResponder: add it to UIWindow then, so the
        // wrapper doesn't land on every responder in the app.
        if !class_addMethod(UIWindow.self, selector, implementation, method_getTypeEncoding(method)) {
            method_setImplementation(method, implementation)
        }
    }
}

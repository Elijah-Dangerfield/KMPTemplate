import UIKit
import ComposeApp

/// The Compose root with iOS 26's Liquid Glass tab bar and + laid over it.
///
/// Kotlin owns the tabs (`SystemTabBar`): this draws whatever `render` last handed it and
/// reports taps, nothing more. The bar is a plain `UITabBar` and the + a `.prominentGlass()`
/// button, both siblings of the Compose view, so the glass samples the Compose pixels beneath
/// it. Nothing shows until Kotlin renders a visible state.
///
/// While the bar shows, the Compose controller gets the bar's height as extra bottom safe area,
/// which is how Compose's `WindowInsets` learn about it: a tab screen pads its list by that
/// inset, so the list scrolls under the glass instead of stopping above it.
@available(iOS 26, *)
final class GlassShellController: UIViewController, KMPTemplateSystemTabBar, UITabBarDelegate {

    /// Gap between the bar's capsule and the +, as between Music's tabs and its search circle.
    private static let addSpacing: CGFloat = 12

    private let tabBar = LayoutReportingTabBar()
    private let addButton = UIButton(configuration: .prominentGlass())
    private var content: UIViewController?
    private var listener: KMPTemplateSystemTabBarListener?
    private var state: KMPTemplateSystemTabBarState?
    private var shown = false

    // The + is a circle as tall as the bar's glass capsule, on its center line, as far from the
    // screen edge as the capsule is from the other one. UIKit places the capsule inside the bar's
    // bounds (at the top on iPhone, with room for the home indicator under it), so these start at
    // the iPhone portrait values and follow the capsule's real frame once it is laid out.
    private lazy var addTop = addButton.topAnchor.constraint(equalTo: tabBar.topAnchor)
    private lazy var addHeight = addButton.heightAnchor.constraint(equalToConstant: 62)
    private lazy var addTrailing = addButton.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -21)
    private lazy var tabBarTrailing = tabBar.trailingAnchor.constraint(equalTo: addButton.leadingAnchor, constant: 21 - Self.addSpacing)
    private lazy var tabBarToEdge = tabBar.trailingAnchor.constraint(equalTo: view.trailingAnchor)

    func setContent(_ controller: UIViewController) {
        content = controller
        loadViewIfNeeded()
        addChild(controller)
        controller.view.frame = view.bounds
        controller.view.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        view.insertSubview(controller.view, at: 0)
        controller.didMove(toParent: self)
    }

    override func viewDidLoad() {
        super.viewDidLoad()
        tabBar.delegate = self
        tabBar.onLayout = { [weak self] in self?.alignAddWithPlatter() }
        tabBar.translatesAutoresizingMaskIntoConstraints = false
        addButton.translatesAutoresizingMaskIntoConstraints = false
        addButton.configuration?.image = UIImage(systemName: "plus", withConfiguration: UIImage.SymbolConfiguration(pointSize: 22, weight: .semibold))
        addButton.addTarget(self, action: #selector(addTapped), for: .touchUpInside)
        view.addSubview(tabBar)
        view.addSubview(addButton)
        NSLayoutConstraint.activate([
            tabBar.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            tabBarTrailing,
            tabBar.bottomAnchor.constraint(equalTo: view.safeAreaLayoutGuide.bottomAnchor),
            addTrailing,
            addTop,
            addHeight,
            addButton.widthAnchor.constraint(equalTo: addButton.heightAnchor),
        ])
        applyVisibility(animated: false)
    }

    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        updateContentInset()
    }

    override var childForStatusBarStyle: UIViewController? { content }
    override var childForHomeIndicatorAutoHidden: UIViewController? { content }
    override var childForScreenEdgesDeferringSystemGestures: UIViewController? { content }

    // MARK: KMPTemplateSystemTabBar

    func render(state: KMPTemplateSystemTabBarState) {
        let previous = self.state
        self.state = state
        if previous?.labels != state.labels || previous?.symbols != state.symbols {
            tabBar.items = state.labels.enumerated().map { index, label in
                let symbol = index < state.symbols.count ? state.symbols[index] : "circle"
                return UITabBarItem(title: label, image: UIImage(systemName: symbol), tag: index)
            }
        }
        applySelection()

        let style: UIUserInterfaceStyle = state.dark ? .dark : .light
        tabBar.overrideUserInterfaceStyle = style
        addButton.overrideUserInterfaceStyle = style
        tabBar.tintColor = UIColor(argb: state.accentArgb)
        addButton.tintColor = UIColor(argb: state.accentArgb)
        addButton.configuration?.baseForegroundColor = UIColor(argb: state.onAccentArgb)
        addButton.accessibilityLabel = state.addLabel

        if state.visible != shown || (state.addLabel == nil) != (previous?.addLabel == nil) {
            shown = state.visible
            applyVisibility(animated: previous != nil)
        }
    }

    func setListener(listener: KMPTemplateSystemTabBarListener?) {
        self.listener = listener
    }

    // MARK: UITabBarDelegate

    /// Called for the selected tab too, which is what re-tap (scroll Today to the top) needs.
    func tabBar(_ tabBar: UITabBar, didSelect item: UITabBarItem) {
        listener?.onTabTapped(index: Int32(item.tag))
        // UIKit has already moved the selection. Kotlin answers with a render if the tab really
        // changed; if it did not (the navigator refused), put back what Kotlin last said.
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.4) { [weak self] in
            self?.applySelection()
        }
    }

    @objc private func addTapped() {
        listener?.onAddTapped()
    }

    // MARK: Layout

    /// The capsule is UIKit's private `_UITabBarPlatterView`. If a release renames it, the + keeps
    /// its last (or starting) placement instead of breaking.
    ///
    /// Only the capsule's top, height and leading inset are read, none of which depend on the
    /// bar's width, so moving the bar's trailing edge here cannot feed back into the next pass.
    /// The bar insets the capsule the same on both sides, so its leading inset stands for the
    /// trailing one. Frames from a half-built capsule (no items yet, mid-animation) are skipped.
    private func alignAddWithPlatter() {
        guard let platter = tabBar.subviews.first(where: { String(describing: type(of: $0)).contains("Platter") }) else { return }
        let frame = platter.frame
        guard frame.minX >= 0, frame.maxX <= tabBar.bounds.width, frame.width > frame.height, frame.height > 0 else { return }
        set(addTop, frame.minY)
        set(addHeight, frame.height)
        set(addTrailing, -frame.minX)
        set(tabBarTrailing, frame.minX - Self.addSpacing)
    }

    private func set(_ constraint: NSLayoutConstraint, _ constant: CGFloat) {
        if abs(constraint.constant - constant) > 0.5 { constraint.constant = constant }
    }

    private func applySelection() {
        guard let state, let items = tabBar.items, items.indices.contains(Int(state.selectedIndex)) else { return }
        let item = items[Int(state.selectedIndex)]
        if tabBar.selectedItem !== item {
            tabBar.selectedItem = item
        }
    }

    private func applyVisibility(animated: Bool) {
        let visible = shown
        let addVisible = shown && state?.addLabel != nil
        // Without a +, the bar takes the full width. Deactivate before activating, or the two
        // trailing constraints meet for a moment and UIKit logs the conflict.
        if addVisible {
            tabBarToEdge.isActive = false
            tabBarTrailing.isActive = true
        } else if state != nil {
            tabBarTrailing.isActive = false
            tabBarToEdge.isActive = true
        }
        let offset = CGAffineTransform(translationX: 0, y: tabBar.bounds.height + view.safeAreaInsets.bottom)
        tabBar.isUserInteractionEnabled = visible
        addButton.isUserInteractionEnabled = addVisible
        // Accessibility follows at once: a bar on its way out is not something to land on.
        tabBar.accessibilityElementsHidden = !visible
        addButton.accessibilityElementsHidden = !addVisible
        if visible {
            tabBar.isHidden = false
        }
        if addVisible {
            addButton.isHidden = false
        }
        let changes = {
            self.tabBar.alpha = visible ? 1 : 0
            self.addButton.alpha = addVisible ? 1 : 0
            self.tabBar.transform = visible ? .identity : offset
            self.addButton.transform = addVisible ? .identity : offset
        }
        let done: (Bool) -> Void = { _ in
            if !self.shown {
                self.tabBar.isHidden = true
            }
            if !self.shown || self.state?.addLabel == nil {
                self.addButton.isHidden = true
            }
        }
        if animated {
            UIView.animate(withDuration: 0.25, delay: 0, options: [.beginFromCurrentState, .curveEaseInOut], animations: changes, completion: done)
        } else {
            changes()
            done(true)
        }
        updateContentInset()
        UIAccessibility.post(notification: .layoutChanged, argument: nil)
    }

    private func updateContentInset() {
        guard let content else { return }
        let bottom = shown ? tabBar.bounds.height : 0
        if content.additionalSafeAreaInsets.bottom != bottom {
            content.additionalSafeAreaInsets.bottom = bottom
        }
    }
}

/// Lays out its capsule in its own pass (items arriving, rotation), which the controller's
/// `viewDidLayoutSubviews` does not see.
private final class LayoutReportingTabBar: UITabBar {
    var onLayout: (() -> Void)?

    override func layoutSubviews() {
        super.layoutSubviews()
        onLayout?()
    }
}

private extension UIColor {
    convenience init(argb: Int32) {
        let value = UInt32(bitPattern: argb)
        self.init(
            red: CGFloat((value >> 16) & 0xFF) / 255,
            green: CGFloat((value >> 8) & 0xFF) / 255,
            blue: CGFloat(value & 0xFF) / 255,
            alpha: CGFloat((value >> 24) & 0xFF) / 255
        )
    }
}

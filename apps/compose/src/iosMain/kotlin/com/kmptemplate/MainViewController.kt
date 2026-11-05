package com.kmptemplate

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.window.ComposeUIViewController
import com.kmptemplate.libraries.ui.nativeviews.LocalNativeViewFactory
import com.kmptemplate.libraries.ui.nativeviews.NativeViewFactory
import platform.UIKit.UIViewController

/**
 * The Compose root. [systemTabBar] is iOS 26's Liquid Glass bar, which Swift lays over this
 * controller's view (`GlassShellController`); Swift passes null before iOS 26.
 */
fun MainViewController(
    appComponent: IosAppComponent,
    systemTabBar: SystemTabBar?,
): UIViewController {
    return ComposeUIViewController {
        CompositionLocalProvider(
            LocalNativeViewFactory provides appComponent.nativeViewFactory,
            LocalSystemTabBar provides systemTabBar,
        ) {
            App(appComponent = appComponent)
        }
    }
}

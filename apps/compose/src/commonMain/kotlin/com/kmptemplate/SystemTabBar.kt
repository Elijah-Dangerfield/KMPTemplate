@file:OptIn(ExperimentalObjCName::class)

package com.kmptemplate

import androidx.compose.runtime.staticCompositionLocalOf
import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName

/**
 * The platform's own tab bar, drawn natively over the Compose root: iOS 26's Liquid Glass bar,
 * built by Swift (`GlassShellController`). Null where there is none (Android, iOS before 26), and
 * an app with tabs draws a Compose bar instead.
 *
 * Kotlin owns the tabs. The bar draws what [render] last gave it and reports taps through the
 * listener; it never switches a tab on its own say. Until something renders a visible state it
 * stays hidden, so an app without tabs can ignore it.
 *
 * The bar sits above all of Compose, sheets and dialogs included, so render it hidden while one
 * is up. Its height reaches Compose as extra bottom safe area: a tab root puts its bottom inset in
 * its list's content padding, or the rows stop above the glass instead of running under it.
 */
@ObjCName("KMPTemplateSystemTabBar", exact = true)
interface SystemTabBar {
    fun render(state: SystemTabBarState)

    fun setListener(listener: SystemTabBarListener?)
}

@ObjCName("KMPTemplateSystemTabBarListener", exact = true)
interface SystemTabBarListener {
    /** A tab was tapped, by its place in [SystemTabBarState.labels]; also when it is the selected one. */
    fun onTabTapped(index: Int)

    fun onAddTapped()
}

/**
 * Everything the native bar shows. [labels] are the tabs in bar order and double as their
 * VoiceOver names; [symbols] are their SF Symbol names in the same order. [addLabel] names the
 * + button beside the bar, and null leaves the + out. Colors are ARGB, so the bar follows the
 * app's theme rather than the phone's when the two differ ([dark]).
 */
@ObjCName("KMPTemplateSystemTabBarState", exact = true)
data class SystemTabBarState(
    val visible: Boolean,
    val selectedIndex: Int,
    val labels: List<String>,
    val symbols: List<String>,
    val addLabel: String?,
    val accentArgb: Int,
    val onAccentArgb: Int,
    val dark: Boolean,
)

val LocalSystemTabBar = staticCompositionLocalOf<SystemTabBar?> { null }

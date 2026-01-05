package com.dangerfield.goodtimes.libraries.navigation

import com.dangerfield.goodtimes.libraries.core.Catching

fun interface WebLinkLauncher {
    fun open(url: String): Catching<Unit>
}

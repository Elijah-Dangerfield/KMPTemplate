package com.dangerfield.merizo.libraries.navigation

import com.dangerfield.merizo.libraries.core.Catching

fun interface WebLinkLauncher {
    fun open(url: String): Catching<Unit>
}

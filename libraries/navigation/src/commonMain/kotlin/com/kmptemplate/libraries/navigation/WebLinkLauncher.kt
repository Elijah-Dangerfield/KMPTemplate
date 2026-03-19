package com.kmptemplate.libraries.navigation

import com.kmptemplate.libraries.core.Catching

fun interface WebLinkLauncher {
    fun open(url: String): Catching<Unit>
}

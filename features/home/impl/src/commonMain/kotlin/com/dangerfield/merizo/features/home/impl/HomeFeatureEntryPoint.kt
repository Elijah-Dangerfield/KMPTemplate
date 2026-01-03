package com.dangerfield.merizo.features.home.impl

import androidx.navigation.NavGraphBuilder
import com.dangerfield.libraries.ui.components.text.Text
import com.dangerfield.merizo.features.home.HomeRoute
import com.dangerfield.merizo.libraries.navigation.FeatureEntryPoint
import com.dangerfield.merizo.libraries.navigation.Router
import com.dangerfield.merizo.libraries.navigation.screen
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, multibinding = true)
@Inject
class HomeFeatureEntryPoint(

) : FeatureEntryPoint {

    override fun NavGraphBuilder.buildNavGraph(router: Router) {
        screen<HomeRoute> {
            Text("Home Screen")
        }
    }
}

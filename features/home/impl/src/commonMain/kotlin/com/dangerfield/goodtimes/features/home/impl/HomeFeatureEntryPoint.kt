package com.dangerfield.goodtimes.features.home.impl

import androidx.navigation.NavGraphBuilder
import com.dangerfield.goodtimes.features.home.HomeRoute
import com.dangerfield.goodtimes.features.home.SettingsRoute
import com.dangerfield.goodtimes.libraries.navigation.FeatureEntryPoint
import com.dangerfield.goodtimes.libraries.navigation.Router
import com.dangerfield.goodtimes.libraries.navigation.screen
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn
import kotlin.time.Instant

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, multibinding = true)
@Inject
class HomeFeatureEntryPoint(

) : FeatureEntryPoint {

    override fun NavGraphBuilder.buildNavGraph(router: Router) {
        screen<HomeRoute> {
            HomeScreen(
                items = listOf(
                    PastTabPreview(
                        name = null,
                        total = 123.45,
                        peopleCount = 2,
                        createdAt = Instant.parse("2023-01-01T00:00:00.000Z")
                    ),
                    PastTabPreview(
                        name = null,
                        total = 123.45,
                        peopleCount = 2,
                        createdAt = Instant.parse("2023-01-01T00:00:00.000Z")
                    ),
                    PastTabPreview(
                        name = null,
                        total = 123.45,
                        peopleCount = 2,
                        createdAt = Instant.parse("2023-01-01T00:00:00.000Z")
                    ),
                    PastTabPreview(
                        name = null,
                        total = 123.45,
                        peopleCount = 2,
                        createdAt = Instant.parse("2023-01-01T00:00:00.000Z")
                    ),
                ),
                onNewBillClicked = {  },
                onSettingsClicked = {  },
                onPastTabClicked = {  }
            )
        }




        screen<SettingsRoute> {
            SettingsScreen()
        }
    }
}

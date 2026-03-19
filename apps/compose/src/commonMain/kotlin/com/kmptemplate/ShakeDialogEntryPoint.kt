package com.kmptemplate

import androidx.navigation.NavGraphBuilder
import com.kmptemplate.features.profile.BugReportRoute
import com.kmptemplate.libraries.navigation.FeatureEntryPoint
import com.kmptemplate.libraries.navigation.Router
import com.kmptemplate.libraries.navigation.ShakeDialogRoute
import com.kmptemplate.libraries.navigation.dialog
import com.kmptemplate.libraries.navigation.toRouteOrNull
import com.kmptemplate.libraries.ui.components.dialog.ShakeDialog
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, multibinding = true)
@Inject
class ShakeDialogEntryPoint : FeatureEntryPoint {

    override fun NavGraphBuilder.buildNavGraph(router: Router) {
        dialog<ShakeDialogRoute> { backStackEntry, dialogState ->
            val route = backStackEntry.toRouteOrNull<ShakeDialogRoute>()
            
            ShakeDialog(
                state = dialogState,
                headline = route?.headline ?: "I felt that.",
                subtext = route?.subtext,
                onDismiss = { router.goBack() },
                onReportBug = {
                    router.goBack()
                    router.navigate(
                        BugReportRoute(contextMessage = "Triggered via shake")
                    )
                },
            )
        }
    }
}

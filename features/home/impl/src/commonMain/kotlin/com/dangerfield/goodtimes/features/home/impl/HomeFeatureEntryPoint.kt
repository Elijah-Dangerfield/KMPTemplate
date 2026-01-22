package com.dangerfield.goodtimes.features.home.impl

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.toRoute
import com.dangerfield.goodtimes.features.home.AboutMeRoute
import com.dangerfield.goodtimes.features.home.AboutYouRoute
import com.dangerfield.goodtimes.features.home.FakeSkipDialogRoute
import com.dangerfield.goodtimes.features.home.FeedbackRoute
import com.dangerfield.goodtimes.features.home.FreshStartDialogRoute
import com.dangerfield.goodtimes.features.home.HomeRoute
import com.dangerfield.goodtimes.features.home.MoodRoute
import com.dangerfield.goodtimes.features.home.PersistenceUnlockedDialogRoute
import com.dangerfield.goodtimes.features.home.QAMenuRoute
import com.dangerfield.goodtimes.features.home.SecretOptionDialogRoute
import com.dangerfield.goodtimes.features.home.SettingsRoute
import com.dangerfield.goodtimes.features.home.TaskPreviewDetailRoute
import com.dangerfield.goodtimes.features.home.TaskPreviewListRoute
import com.dangerfield.goodtimes.features.home.UselessButtonDialogRoute
import com.dangerfield.goodtimes.features.home.impl.bugreport.BugReportScreen
import com.dangerfield.goodtimes.features.home.impl.bugreport.BugReportViewModel
import com.dangerfield.goodtimes.features.home.impl.feedback.FeedbackScreen
import com.dangerfield.goodtimes.features.home.impl.feedback.FeedbackViewModel
import com.dangerfield.goodtimes.features.home.impl.qa.QAMenuScreen
import com.dangerfield.goodtimes.features.home.impl.qa.TaskPreviewDetailScreen
import com.dangerfield.goodtimes.features.home.impl.qa.TaskPreviewListScreen
import com.dangerfield.goodtimes.features.home.impl.qa.TaskPreviewListState
import com.dangerfield.goodtimes.features.profile.BugReportRoute
import com.dangerfield.goodtimes.libraries.core.BuildInfo
import com.dangerfield.goodtimes.libraries.goodtimes.AppCache
import com.dangerfield.goodtimes.libraries.goodtimes.TaskRepository
import com.dangerfield.goodtimes.libraries.navigation.FeatureEntryPoint
import com.dangerfield.goodtimes.libraries.navigation.Router
import com.dangerfield.goodtimes.libraries.navigation.bottomSheet
import com.dangerfield.goodtimes.libraries.navigation.dialog
import com.dangerfield.goodtimes.libraries.navigation.screen
import com.dangerfield.goodtimes.libraries.navigation.toRouteOrNull
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn
import com.dangerfield.goodtimes.features.tasks.impl.TaskViewModelFactory
import kotlinx.coroutines.withContext

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, multibinding = true)
@Inject
class HomeFeatureEntryPoint(
    private val homeViewModelFactory: () -> HomeViewModel,
    private val settingsViewModelFactory: () -> SettingsViewModel,
    private val moodViewModelFactory: (dismissCount: Int, sessionNumber: Int, isFirstSession: Boolean, fromSettings: Boolean) -> MoodViewModel,
    private val feedbackViewModelFactory: () -> FeedbackViewModel,
    private val bugReportViewModelFactory: (logId: String?, errorCode: Int?, contextMessage: String?) -> BugReportViewModel,
    private val aboutYouViewModelFactory: () -> AboutYouViewModel,
    private val aboutMeViewModelFactory: () -> AboutMeViewModel,
    private val freshStartViewModelFactory: () -> FreshStartViewModel,
    private val appCache: AppCache,
    private val taskViewModelFactory: TaskViewModelFactory,
    private val taskRepository: TaskRepository,
) : FeatureEntryPoint {

    override fun NavGraphBuilder.buildNavGraph(router: Router) {
        screen<HomeRoute> {
            val viewModel: HomeViewModel = viewModel { homeViewModelFactory() }
            val state by viewModel.stateFlow.collectAsStateWithLifecycle()
            val scope = rememberCoroutineScope()

            LaunchedEffect(viewModel) {
                viewModel.eventFlow.collectLatest { event ->
                    when (event) {
                        is HomeEvent.ShowMoodBottomSheet -> {
                            router.navigate(MoodRoute(
                                dismissCount = event.dismissCount,
                                sessionNumber = event.sessionNumber,
                                isFirstEverMoodPrompt = event.isFirstEverMoodPrompt
                            ))
                        }
                        is HomeEvent.NavigateToUselessButtonDialog -> {
                            router.navigate(UselessButtonDialogRoute(clickCount = event.clickCount))
                        }
                        is HomeEvent.NavigateToFakeSkipDialog -> {
                            router.navigate(FakeSkipDialogRoute(
                                taskId = event.taskId,
                                taskCategories = event.taskCategories
                            ))
                        }
                    }
                }
            }

            HomeScreen(
                state = state,
                viewModelFactory = taskViewModelFactory,
                onSettingsClicked = { 
                    scope.launch {
                        val visitCount = appCache.get().getVisitCount("settingsVisits") + 1
                        router.navigate(SettingsRoute(visitCount = visitCount)) 
                    }
                },
                onAction = viewModel::takeAction
            )
        }

        bottomSheet<MoodRoute> { backStackEntry, sheetState ->
            val route: MoodRoute = backStackEntry.toRoute()
            val viewModel: MoodViewModel = viewModel {
                moodViewModelFactory(route.dismissCount, route.sessionNumber, route.isFirstEverMoodPrompt, route.fromSettings)
            }
            val state by viewModel.stateFlow.collectAsStateWithLifecycle()

            LaunchedEffect(viewModel) {
                viewModel.eventFlow.collectLatest { event ->
                    when (event) {
                        is MoodEvent.Close -> router.goBack()
                    }
                }
            }

            MoodScreen(
                state = state,
                sheetState = sheetState,
                onAction = viewModel::takeAction,
                onDismissRequest = { viewModel.takeAction(MoodAction.Dismiss) }
            )
        }

        screen<SettingsRoute> { backStackEntry ->
            val route: SettingsRoute = backStackEntry.toRoute()
            val viewModel: SettingsViewModel = viewModel { settingsViewModelFactory() }
            val state by viewModel.stateFlow.collectAsStateWithLifecycle()

            LaunchedEffect(viewModel) {
                viewModel.eventFlow.collectLatest { event ->
                    when (event) {
                        is SettingsEvent.NavigateToOnboarding -> {

                        }
                        is SettingsEvent.NavigateToAboutYou -> {
                            router.navigate(AboutYouRoute())
                        }
                        is SettingsEvent.NavigateToAboutMe -> {
                            router.navigate(AboutMeRoute())
                        }
                        is SettingsEvent.NavigateToFreshStartDialog -> {
                            router.navigate(FreshStartDialogRoute())
                        }
                        is SettingsEvent.NavigateToBugReport -> {
                            router.navigate(BugReportRoute())
                        }
                        is SettingsEvent.NavigateToFeedback -> {
                            router.navigate(FeedbackRoute())
                        }
                        is SettingsEvent.NavigateToUselessButtonDialog -> {
                            router.navigate(UselessButtonDialogRoute(clickCount = event.clickCount))
                        }
                        is SettingsEvent.NavigateToMoodPrompt -> {
                            router.navigate(MoodRoute(
                                dismissCount = 0,
                                sessionNumber = 0, // Not used when fromSettings is true
                                isFirstEverMoodPrompt = event.isFirstEverMoodPrompt,
                                fromSettings = true
                            ))
                        }
                        is SettingsEvent.NavigateToSecretOption -> {
                            router.navigate(SecretOptionDialogRoute())
                        }
                        is SettingsEvent.NavigateToPersistenceUnlocked -> {
                            router.navigate(PersistenceUnlockedDialogRoute(visitCount = event.visitCount))
                        }
                        is SettingsEvent.NavigateToQAMenu -> {
                            router.navigate(QAMenuRoute())
                        }
                    }
                }
            }

            SettingsScreen(
                visitCount = route.visitCount,
                state = state,
                isDebug = BuildInfo.isDebug,
                onAction = viewModel::takeAction,
                onBackClicked = { router.goBack() }
            )
        }

        screen<AboutYouRoute> {
            val viewModel = viewModel { aboutYouViewModelFactory() }
            val state by viewModel.stateFlow.collectAsStateWithLifecycle()

            AboutYouScreen(
                state = state,
                onBackClicked = { router.goBack() },
                onAction = viewModel::takeAction
            )
        }

        screen<AboutMeRoute> {
            val viewModel = viewModel { aboutMeViewModelFactory() }
            val state by viewModel.stateFlow.collectAsStateWithLifecycle()

            AboutMeScreen(
                state = state,
                onBackClicked = { router.goBack() }
            )
        }

        dialog<FreshStartDialogRoute> { backStackEntry, dialogState ->
            val viewModel = viewModel { freshStartViewModelFactory() }
            val state by viewModel.stateFlow.collectAsStateWithLifecycle()

            LaunchedEffect(viewModel) {
                viewModel.eventFlow.collectLatest { event ->
                    when (event) {
                        is FreshStartEvent.PerformFreshStart -> {
                            /*
                            TODO:
                            -  delete all cache
                            - drop all tables
                            - navigate to onboarding popping back stack
                             */
                            router.goBack()
                        }
                    }
                }
            }

            FreshStartDialog(
                state = state,
                dialogState = dialogState,
                onConfirm = { viewModel.takeAction(FreshStartAction.Confirm) },
                onDismiss = { router.goBack() }
            )
        }

        dialog<UselessButtonDialogRoute> { backStackEntry, dialogState ->

            val route: UselessButtonDialogRoute = backStackEntry.toRoute()

            UselessButtonDialog(
                dialogState = dialogState,
                clickCount = route.clickCount,
                onDismiss = { router.goBack() }
            )
        }

        dialog<SecretOptionDialogRoute> { backStackEntry, dialogState ->
            SecretOptionDialog(
                dialogState = dialogState,
                onDismiss = { router.goBack() }
            )
        }

        dialog<PersistenceUnlockedDialogRoute> { backStackEntry, dialogState ->
            val route: PersistenceUnlockedDialogRoute = backStackEntry.toRoute()
            PersistenceUnlockedDialog(
                visitCount = route.visitCount,
                dialogState = dialogState,
                onDismiss = { router.goBack() }
            )
        }

        dialog<FakeSkipDialogRoute> { backStackEntry, dialogState ->
            FakeSkipDialog(
                dialogState = dialogState,
                onDismiss = { router.goBack() }
            )
        }

        screen<FeedbackRoute> {
            val viewModel = viewModel { feedbackViewModelFactory() }
            val state by viewModel.stateFlow.collectAsStateWithLifecycle()

            FeedbackScreen(
                state = state,
                onAction = viewModel::takeAction
            )
        }

        screen<BugReportRoute> { backStackEntry ->
            val route = backStackEntry.toRouteOrNull<BugReportRoute>() ?: BugReportRoute()
            val viewModel = viewModel {
                bugReportViewModelFactory(
                    route.logId,
                    route.errorCode,
                    route.contextMessage
                )
            }
            val state by viewModel.stateFlow.collectAsStateWithLifecycle()

            BugReportScreen(
                state = state,
                onAction = viewModel::takeAction
            )
        }

        screen<QAMenuRoute> {
            QAMenuScreen(
                onBackClicked = { router.goBack() },
                onTaskPreviewClicked = { router.navigate(TaskPreviewListRoute()) }
            )
        }

        screen<TaskPreviewListRoute> {
            val state by produceState<TaskPreviewListState>(TaskPreviewListState.Loading) {
                val tasks = taskRepository.getAllTasks()
                value = TaskPreviewListState.Loaded(
                    totalCount = tasks.size,
                    tasksByType = tasks.groupBy { it.type },
                )
            }

            TaskPreviewListScreen(
                state = state,
                onBackClicked = { router.goBack() },
                onTaskClicked = { task ->
                    router.navigate(TaskPreviewDetailRoute(taskId = task.id))
                }
            )
        }

        screen<TaskPreviewDetailRoute> { backStackEntry ->
            val route: TaskPreviewDetailRoute = backStackEntry.toRoute()
            var task by remember { mutableStateOf<com.dangerfield.goodtimes.libraries.goodtimes.Task?>(null) }

            LaunchedEffect(route.taskId) {
                task = taskRepository.getTask(route.taskId)
            }

            TaskPreviewDetailScreen(
                task = task,
                taskViewModelFactory = taskViewModelFactory,
                onBackClicked = { router.goBack() }
            )
        }
    }
}

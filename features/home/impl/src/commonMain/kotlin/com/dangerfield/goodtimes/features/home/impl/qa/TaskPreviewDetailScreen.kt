package com.dangerfield.goodtimes.features.home.impl.qa

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.dangerfield.goodtimes.features.tasks.impl.TaskHost
import com.dangerfield.goodtimes.features.tasks.impl.TaskViewModelFactory
import com.dangerfield.goodtimes.libraries.goodtimes.Task
import com.dangerfield.libraries.ui.components.Screen
import com.dangerfield.libraries.ui.components.header.TopBar

@Composable
fun TaskPreviewDetailScreen(
    task: Task?,
    taskViewModelFactory: TaskViewModelFactory,
    onBackClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Screen(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopBar(
                title = task?.id ?: "Loading...",
                onNavigateBack = onBackClicked
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            TaskHost(
                task = task,
                onTaskCompleted = { },
                viewModelFactory = taskViewModelFactory,
                showFakeSkipButton = false,
                onFakeSkipClicked = {},
            )
        }
    }
}

package com.dangerfield.goodtimes.features.tasks.impl

import com.dangerfield.goodtimes.features.tasks.impl.templates.drawing.DrawingViewModel
import com.dangerfield.goodtimes.features.tasks.impl.templates.instruction.InstructionViewModel
import com.dangerfield.goodtimes.features.tasks.impl.templates.prompt.PromptViewModel
import com.dangerfield.goodtimes.libraries.goodtimes.AppCache
import com.dangerfield.goodtimes.libraries.goodtimes.Task
import com.dangerfield.libraries.ui.DrawingSaver
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class TaskViewModelFactoryImpl(
    private val drawingSaver: DrawingSaver,
    private val appCache: AppCache,
) : TaskViewModelFactory {
    
    override fun createPromptViewModel(task: Task): PromptViewModel {
        return PromptViewModel(task)
    }
    
    override fun createInstructionViewModel(task: Task): InstructionViewModel {
        return InstructionViewModel(task)
    }
    
    override fun createDrawingViewModel(task: Task): DrawingViewModel {
        return DrawingViewModel(task, drawingSaver, appCache)
    }
}

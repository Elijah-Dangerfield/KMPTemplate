package com.dangerfield.goodtimes.features.tasks.impl

import com.dangerfield.goodtimes.features.tasks.impl.templates.instruction.InstructionViewModel
import com.dangerfield.goodtimes.features.tasks.impl.templates.prompt.PromptViewModel
import com.dangerfield.goodtimes.libraries.goodtimes.Task
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class TaskViewModelFactoryImpl : TaskViewModelFactory {
    
    override fun createPromptViewModel(task: Task): PromptViewModel {
        return PromptViewModel(task)
    }
    
    override fun createInstructionViewModel(task: Task): InstructionViewModel {
        return InstructionViewModel(task)
    }
}

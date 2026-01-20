package com.dangerfield.goodtimes.libraries.storage.impl.db

import com.dangerfield.goodtimes.libraries.goodtimes.storage.db.SessionDao
import com.dangerfield.goodtimes.libraries.goodtimes.storage.db.TaskProgressDao
import com.dangerfield.goodtimes.libraries.goodtimes.storage.db.TaskResultsDao
import com.dangerfield.goodtimes.libraries.goodtimes.storage.db.TasksDao
import com.dangerfield.goodtimes.libraries.goodtimes.storage.db.UserDao
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, boundType = TasksDao::class)
class ProvideTasksDao @Inject constructor(
    provider: AppDatabaseProvider
) : TasksDao by provider.database.tasksDao()

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, boundType = TaskProgressDao::class)
class ProvideTaskProgressDao @Inject constructor(
    provider: AppDatabaseProvider
) : TaskProgressDao by provider.database.taskProgressDao()

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, boundType = TaskResultsDao::class)
class ProvideTaskResultsDao @Inject constructor(
    provider: AppDatabaseProvider
) : TaskResultsDao by provider.database.taskResultDao()

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, boundType = UserDao::class)
class ProvideUserDao @Inject constructor(
    provider: AppDatabaseProvider
) : UserDao by provider.database.userDao()

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, boundType = SessionDao::class)
class ProvideSessionDao @Inject constructor(
    provider: AppDatabaseProvider
) : SessionDao by provider.database.sessionDao()

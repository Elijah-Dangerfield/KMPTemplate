package com.dangerfield.goodtimes.libraries.storage.impl.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.TypeConverters
import com.dangerfield.goodtimes.libraries.goodtimes.storage.db.SessionDao
import com.dangerfield.goodtimes.libraries.goodtimes.storage.db.SessionEntity
import com.dangerfield.goodtimes.libraries.goodtimes.storage.db.TaskEntity
import com.dangerfield.goodtimes.libraries.goodtimes.storage.db.TaskProgressDao
import com.dangerfield.goodtimes.libraries.goodtimes.storage.db.TaskProgressEntity
import com.dangerfield.goodtimes.libraries.goodtimes.storage.db.TaskResultEntity
import com.dangerfield.goodtimes.libraries.goodtimes.storage.db.TaskResultsDao
import com.dangerfield.goodtimes.libraries.goodtimes.storage.db.TasksDao
import com.dangerfield.goodtimes.libraries.goodtimes.storage.db.UserDao
import com.dangerfield.goodtimes.libraries.goodtimes.storage.db.UserEntity

@Database(
    entities = [
        TaskEntity::class,
        TaskProgressEntity::class,
        TaskResultEntity::class,
        UserEntity::class,
        SessionEntity::class,
    ],
    version = 2,
    exportSchema = true
)
@TypeConverters(CoreTypeConverters::class)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tasksDao(): TasksDao
    abstract fun taskProgressDao(): TaskProgressDao
    abstract fun taskResultDao(): TaskResultsDao
    abstract fun userDao(): UserDao
    abstract fun sessionDao(): SessionDao
}

@Suppress("KotlinNoActualForExpect")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}

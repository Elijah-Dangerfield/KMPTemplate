package com.kmptemplate.libraries.storage.impl.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.TypeConverters
import com.kmptemplate.libraries.kmptemplate.storage.db.SessionDao
import com.kmptemplate.libraries.kmptemplate.storage.db.SessionEntity
import com.kmptemplate.libraries.kmptemplate.storage.db.UserDao
import com.kmptemplate.libraries.kmptemplate.storage.db.UserEntity

@Database(
    entities = [
        UserEntity::class,
        SessionEntity::class,
    ],
    version = 4, // Bumped version for schema change
    exportSchema = true
)
@TypeConverters(CoreTypeConverters::class)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun sessionDao(): SessionDao
}

@Suppress("KotlinNoActualForExpect")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}

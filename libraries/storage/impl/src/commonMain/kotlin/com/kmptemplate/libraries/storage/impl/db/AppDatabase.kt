package com.kmptemplate.libraries.storage.impl.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.TypeConverters
import com.kmptemplate.libraries.kmptemplate.storage.db.ExampleUserDataDao
import com.kmptemplate.libraries.kmptemplate.storage.db.ExampleUserDataEntity

@Database(
    entities = [
        ExampleUserDataEntity::class,
    ],
    version = 5, // Bumped: demo User/Session tables replaced by the example table
    exportSchema = true
)
@TypeConverters(CoreTypeConverters::class)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun exampleUserDataDao(): ExampleUserDataDao
}

@Suppress("KotlinNoActualForExpect")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}

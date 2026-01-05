package com.dangerfield.goodtimes.libraries.storage.impl.db

//import android.content.Context
//import androidx.room.Room
//import androidx.room.RoomDatabase
//import me.tatarka.inject.annotations.Inject
//import software.amazon.lastmile.kotlin.inject.anvil.AppScope
//import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
//import software.amazon.lastmile.kotlin.inject.anvil.SingleIn
//import java.io.File

//@SingleIn(AppScope::class)
//@ContributesBinding(AppScope::class)
//class AndroidAppDatabaseBuilderFactory @Inject constructor(
//    private val context: Context
//) : AppDatabaseBuilderFactory {
//
//    override fun create(): RoomDatabase.Builder<AppDatabase> {
//        val root = System.getProperty("user.home")?.let(::File) ?: File("/data/local/tmp")
//        val dbDir = File(root, "goodtimese/databases").apply { mkdirs() }
//        val dbFile = File(dbDir, "goodtimes.db")
//        return Room.databaseBuilder<AppDatabase>(
//            name = dbFile.absolutePath,
//            context = context,
//        )
//    }
//}

package pt.rikmartins.clubemg.mobile.cache

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import pt.rikmartins.clubemg.mobile.data.cache.AppDatabase

fun getDatabaseBuilder(context: Context): RoomDatabase.Builder<AppDatabase> {
    val appContext = context.applicationContext
    val dbFile = appContext.getDatabasePath(DATABASE_FILE_NAME)
    return Room.databaseBuilder<AppDatabase>(
        context = appContext,
        name = dbFile.absolutePath
    )
}

private const val DATABASE_FILE_NAME = "cmg.db"
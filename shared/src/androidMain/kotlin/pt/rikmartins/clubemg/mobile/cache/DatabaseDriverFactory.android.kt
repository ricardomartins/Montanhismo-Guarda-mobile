package pt.rikmartins.clubemg.mobile.cache

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

actual class DatabaseDriverFactory(private val context: Context) {

    actual fun createDriver(): SqlDriver = AndroidSqliteDriver(
        schema = AppDatabase.Schema,
        context = context.applicationContext,
        name = DATABASE_NAME,
        callback = object : AndroidSqliteDriver.Callback(AppDatabase.Schema) {
            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                db.setForeignKeyConstraintsEnabled(true)
            }
        }
    )

    private companion object {
        const val DATABASE_NAME = "clubemg.db"
    }
}
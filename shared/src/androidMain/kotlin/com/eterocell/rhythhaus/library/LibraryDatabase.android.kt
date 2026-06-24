package com.eterocell.rhythhaus.library

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

object LibraryDatabaseContext {
    lateinit var applicationContext: Context
}

actual class LibraryDatabase(private val context: Context) {
    private val androidDriver: AndroidSqliteDriver by lazy {
        AndroidSqliteDriver(
            schema = RhythHausDatabase.Schema,
            context = context,
            name = "rhythhaus.db",
        )
    }

    actual val driver: SqlDriver get() = androidDriver

    actual val database: RhythHausDatabase by lazy { RhythHausDatabase(driver) }
}

actual fun createLibraryDatabase(): LibraryDatabase = LibraryDatabase(LibraryDatabaseContext.applicationContext)

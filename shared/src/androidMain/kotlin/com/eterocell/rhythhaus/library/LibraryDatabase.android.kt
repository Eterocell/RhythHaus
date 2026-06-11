package com.eterocell.rhythhaus.library

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

actual class LibraryDatabaseDriverFactory(private val context: Context) {
    actual fun createDriver(): SqlDriver = AndroidSqliteDriver(
        schema = RhythHausDatabase.Schema,
        context = context,
        name = "rhythhaus.db",
    )
}

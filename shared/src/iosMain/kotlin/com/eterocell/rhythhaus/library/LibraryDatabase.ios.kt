package com.eterocell.rhythhaus.library

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver

actual class LibraryDatabaseDriverFactory {
    actual fun createDriver(): SqlDriver = NativeSqliteDriver(
        schema = RhythHausDatabase.Schema,
        name = "rhythhaus.db",
    )
}

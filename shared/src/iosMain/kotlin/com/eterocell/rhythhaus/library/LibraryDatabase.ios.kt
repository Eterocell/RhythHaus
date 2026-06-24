package com.eterocell.rhythhaus.library

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver

actual class LibraryDatabase {
    private val nativeDriver: NativeSqliteDriver by lazy {
        NativeSqliteDriver(
            schema = RhythHausDatabase.Schema,
            name = "rhythhaus.db",
        )
    }

    actual val driver: SqlDriver get() = nativeDriver

    actual val database: RhythHausDatabase by lazy { RhythHausDatabase(driver) }
}

actual fun createLibraryDatabase(): LibraryDatabase = LibraryDatabase()

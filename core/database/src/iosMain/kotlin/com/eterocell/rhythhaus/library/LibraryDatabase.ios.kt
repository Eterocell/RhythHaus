package com.eterocell.rhythhaus.library

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import co.touchlab.sqliter.DatabaseConfiguration

/** iOS SQLDelight database backed by the platform-native SQLite driver. */
public actual class LibraryDatabase {
    private val nativeDriver: NativeSqliteDriver by lazy {
        NativeSqliteDriver(
            schema = RhythHausDatabase.Schema,
            name = libraryDatabaseFileName,
            onConfiguration = { configuration ->
                configuration.copy(
                    extendedConfig =
                        DatabaseConfiguration.Extended(
                            foreignKeyConstraints = true),
                )
            },
        )
    }

    /** Native SQLite driver for this library database. */
    public actual val driver: SqlDriver
        get() = nativeDriver

    /** Generated query facade backed by the native SQLite driver. */
    public actual val database: RhythHausDatabase by lazy {
        RhythHausDatabase(driver)
    }
}

/** Creates the iOS database using its platform-default storage location. */
public actual fun createLibraryDatabase(): LibraryDatabase = LibraryDatabase()

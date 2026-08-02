package com.eterocell.rhythhaus.library

import app.cash.sqldelight.db.SqlDriver

/** Provides the platform SQLDelight driver and generated database facade. */
public expect class LibraryDatabase {
    /** SQLDelight driver used to access the local library database. */
    public val driver: SqlDriver

    /** Generated SQLDelight query facade for the local library database. */
    public val database: RhythHausDatabase
}

/** Creates the platform-default library database. */
public expect fun createLibraryDatabase(): LibraryDatabase

internal const val libraryDatabaseFileName: String = "rhythhaus.db"

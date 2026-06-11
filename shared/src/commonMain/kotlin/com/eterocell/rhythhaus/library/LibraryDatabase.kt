package com.eterocell.rhythhaus.library

import app.cash.sqldelight.db.SqlDriver

expect class LibraryDatabaseDriverFactory {
    fun createDriver(): SqlDriver
}

fun createLibraryDatabase(factory: LibraryDatabaseDriverFactory): RhythHausDatabase = RhythHausDatabase(factory.createDriver())

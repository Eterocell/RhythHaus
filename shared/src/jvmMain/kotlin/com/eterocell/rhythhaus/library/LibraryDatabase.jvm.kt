package com.eterocell.rhythhaus.library

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.File
import java.util.Properties

actual class LibraryDatabase(private val databaseFile: File = defaultDatabaseFile()) {
    private val jdbcDriver: JdbcSqliteDriver by lazy {
        databaseFile.parentFile?.mkdirs()
        JdbcSqliteDriver(
            url = "jdbc:sqlite:${databaseFile.absolutePath}",
            properties = Properties().apply { put("foreign_keys", "true") },
            schema = RhythHausDatabase.Schema,
        )
    }

    actual val driver: SqlDriver get() = jdbcDriver

    actual val database: RhythHausDatabase by lazy { RhythHausDatabase(driver) }
}

private fun defaultDatabaseFile(): File = File(
    System.getProperty("user.home"),
    "Library/Application Support/RhythHaus/rhythhaus.db",
)

actual fun createLibraryDatabase(): LibraryDatabase = LibraryDatabase()

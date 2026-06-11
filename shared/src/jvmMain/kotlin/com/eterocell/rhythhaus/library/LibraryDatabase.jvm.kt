package com.eterocell.rhythhaus.library

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.File

actual class LibraryDatabaseDriverFactory(private val databaseFile: File = defaultDatabaseFile()) {
    actual fun createDriver(): SqlDriver {
        databaseFile.parentFile?.mkdirs()
        return JdbcSqliteDriver("jdbc:sqlite:${databaseFile.absolutePath}").also { driver ->
            RhythHausDatabase.Schema.create(driver)
        }
    }
}

private fun defaultDatabaseFile(): File = File(
    System.getProperty("user.home"),
    "Library/Application Support/RhythHaus/rhythhaus.db",
)

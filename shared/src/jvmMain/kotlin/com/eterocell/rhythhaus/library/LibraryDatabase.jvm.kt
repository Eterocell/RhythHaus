package com.eterocell.rhythhaus.library

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.File

actual class LibraryDatabaseDriverFactory(private val databaseFile: File = defaultDatabaseFile()) {
    actual fun createDriver(): SqlDriver {
        databaseFile.parentFile?.mkdirs()
        val shouldCreateSchema = !databaseFile.exists() || databaseFile.length() == 0L
        return JdbcSqliteDriver("jdbc:sqlite:${databaseFile.absolutePath}").also { driver ->
            if (shouldCreateSchema) {
                RhythHausDatabase.Schema.create(driver)
            }
        }
    }
}

private fun defaultDatabaseFile(): File = File(
    System.getProperty("user.home"),
    "Library/Application Support/RhythHaus/rhythhaus.db",
)

package com.eterocell.rhythhaus.library

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.File

actual class LibraryDatabase(private val databaseFile: File = defaultDatabaseFile()) {
    private val jdbcDriver: JdbcSqliteDriver by lazy {
        databaseFile.parentFile?.mkdirs()
        val shouldCreateSchema = !databaseFile.exists() || databaseFile.length() == 0L
        JdbcSqliteDriver("jdbc:sqlite:${databaseFile.absolutePath}").also { driver ->
            if (shouldCreateSchema) {
                RhythHausDatabase.Schema.create(driver)
            }
        }
    }

    actual val driver: SqlDriver get() = jdbcDriver

    actual val database: RhythHausDatabase by lazy { RhythHausDatabase(driver) }
}

private fun defaultDatabaseFile(): File = File(
    System.getProperty("user.home"),
    "Library/Application Support/RhythHaus/rhythhaus.db",
)

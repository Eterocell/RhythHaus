package com.eterocell.rhythhaus.library

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.File
import java.util.Properties

actual class LibraryDatabase(
    private val databaseFile: File = defaultDatabaseFile()
) {
    private val jdbcDriver: JdbcSqliteDriver by lazy {
        databaseFile.parentFile?.mkdirs()
        val url = "jdbc:sqlite:${databaseFile.absolutePath}"
        val properties = foreignKeyProperties()
        bootstrapLegacyVersionZeroDatabase(url, properties)
        JdbcSqliteDriver(
            url = url,
            properties = properties,
            schema = RhythHausDatabase.Schema,
        )
    }

    actual val driver: SqlDriver
        get() = jdbcDriver

    actual val database: RhythHausDatabase by lazy { RhythHausDatabase(driver) }
}

private val libraryTables =
    setOf("library_source", "library_track", "scan_session", "scan_error")
private val playlistTables = setOf("playlist", "playlist_entry")

private fun foreignKeyProperties(): Properties =
    Properties().apply { put("foreign_keys", "true") }

private fun bootstrapLegacyVersionZeroDatabase(
    url: String,
    properties: Properties
) {
    val driver = JdbcSqliteDriver(url, properties)
    try {
        if (driver.userVersion() != 0L) return
        val tables = driver.userTables()
        val legacyVersion =
            when (tables) {
                libraryTables -> 1L
                libraryTables + playlistTables ->
                    RhythHausDatabase.Schema.version
                else -> return
            }
        driver.execute(null, "PRAGMA user_version = $legacyVersion", 0).value
    } finally {
        driver.close()
    }
}

private fun JdbcSqliteDriver.userVersion(): Long =
    executeQuery(
            identifier = null,
            sql = "PRAGMA user_version",
            mapper = { cursor ->
                QueryResult.Value(
                    if (cursor.next().value) cursor.getLong(0) ?: 0L else 0L)
            },
            parameters = 0,
        )
        .value

private fun JdbcSqliteDriver.userTables(): Set<String> =
    executeQuery(
            identifier = null,
            sql =
                "SELECT name FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'sqlite_%'",
            mapper = { cursor ->
                val tables = mutableSetOf<String>()
                while (cursor.next().value) cursor
                    .getString(0)
                    ?.let(tables::add)
                QueryResult.Value(tables)
            },
            parameters = 0,
        )
        .value

private fun defaultDatabaseFile(): File =
    File(
        System.getProperty("user.home"),
        "Library/Application Support/RhythHaus/rhythhaus.db",
    )

actual fun createLibraryDatabase(): LibraryDatabase = LibraryDatabase()

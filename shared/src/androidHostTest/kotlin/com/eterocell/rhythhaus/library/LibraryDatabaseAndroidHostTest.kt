package com.eterocell.rhythhaus.library

import androidx.sqlite.db.SupportSQLiteDatabase
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.lang.reflect.Proxy
import kotlin.test.Test
import kotlin.test.assertFails

class LibraryDatabaseAndroidHostTest {
    @Test
    fun productionAndroidCallbackRejectsInvalidPlaylistEntryForeignKeys() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        RhythHausDatabase.Schema.create(driver)
        val supportDatabase = Proxy.newProxyInstance(
            SupportSQLiteDatabase::class.java.classLoader,
            arrayOf(SupportSQLiteDatabase::class.java),
        ) { _, method, arguments ->
            if (method.name == "setForeignKeyConstraintsEnabled") {
                val enabled = arguments?.single() as Boolean
                driver.execute(null, "PRAGMA foreign_keys = ${if (enabled) "ON" else "OFF"}", 0)
            }
            defaultValue(method.returnType)
        } as SupportSQLiteDatabase

        try {
            libraryDatabaseCallback().onOpen(supportDatabase)
            val database = RhythHausDatabase(driver)
            assertFails {
                database.playlistQueries.insertEntry(
                    id = "invalid-entry",
                    playlistId = "missing-playlist",
                    trackId = "missing-track",
                    position = 0,
                    createdAtEpochMillis = 1,
                )
            }
        } finally {
            driver.close()
        }
    }
}

private fun defaultValue(type: Class<*>): Any? = when (type) {
    Boolean::class.javaPrimitiveType -> false
    Byte::class.javaPrimitiveType -> 0.toByte()
    Short::class.javaPrimitiveType -> 0.toShort()
    Int::class.javaPrimitiveType -> 0
    Long::class.javaPrimitiveType -> 0L
    Float::class.javaPrimitiveType -> 0f
    Double::class.javaPrimitiveType -> 0.0
    Char::class.javaPrimitiveType -> '\u0000'
    else -> null
}

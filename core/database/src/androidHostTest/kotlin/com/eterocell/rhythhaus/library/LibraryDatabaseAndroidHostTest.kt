package com.eterocell.rhythhaus.library

import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import androidx.sqlite.db.SupportSQLiteDatabase
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.lang.reflect.Proxy
import kotlin.test.Test
import kotlin.test.assertFails
import kotlin.test.assertSame

class LibraryDatabaseAndroidHostTest {
    @Test
    fun directSetterNormalizesApplicationContext() {
        val applicationContext = Application()
        val suppliedContext = ApplicationContextWrapper(applicationContext)

        setLibraryDatabaseAndroidContext(suppliedContext)

        assertSame(
            applicationContext, LibraryDatabaseContext.applicationContext)
    }

    @Test
    fun contextSetterNormalizesAndDatabaseFactoryReadsTheHolder() {
        val applicationContext = Application()
        val suppliedContext = ApplicationContextWrapper(applicationContext)

        LibraryDatabaseContext.applicationContext = suppliedContext

        assertSame(
            applicationContext, LibraryDatabaseContext.applicationContext)
        val database = createLibraryDatabase()
        val contextField =
            LibraryDatabase::class.java.getDeclaredField("context")
        contextField.isAccessible = true
        assertSame(applicationContext, contextField.get(database))
    }

    @Test
    fun productionAndroidCallbackRejectsInvalidPlaylistEntryForeignKeys() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        RhythHausDatabase.Schema.create(driver)
        val supportDatabase =
            Proxy.newProxyInstance(
                SupportSQLiteDatabase::class.java.classLoader,
                arrayOf(SupportSQLiteDatabase::class.java),
            ) { _, method, arguments ->
                if (method.name == "setForeignKeyConstraintsEnabled") {
                    val enabled = arguments?.single() as Boolean
                    driver.execute(
                        null,
                        "PRAGMA foreign_keys = ${if (enabled) "ON" else "OFF"}",
                        0)
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

private class ApplicationContextWrapper(
    private val application: Context,
) : ContextWrapper(application) {
    override fun getApplicationContext(): Context = application
}

private fun defaultValue(type: Class<*>): Any? =
    when (type) {
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

// Library extraction

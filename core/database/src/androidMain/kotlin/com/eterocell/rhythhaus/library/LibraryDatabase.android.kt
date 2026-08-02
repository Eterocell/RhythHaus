package com.eterocell.rhythhaus.library

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

private object LibraryDatabaseAndroidContextHolder {
    lateinit var applicationContext: Context
}

/**
 * Sets the Android application context used by the default database factory.
 */
public fun setLibraryDatabaseAndroidContext(context: Context) {
    LibraryDatabaseAndroidContextHolder.applicationContext =
        context.applicationContext
}

/** Android SQLDelight database backed by the supplied application context. */
public actual class LibraryDatabase(private val context: Context) {
    private val androidDriver: AndroidSqliteDriver by lazy {
        AndroidSqliteDriver(
            schema = RhythHausDatabase.Schema,
            context = context,
            name = libraryDatabaseFileName,
            callback = libraryDatabaseCallback(),
        )
    }

    /** Android SQLite driver for this library database. */
    public actual val driver: SqlDriver
        get() = androidDriver

    /** Generated query facade backed by the Android SQLite driver. */
    public actual val database: RhythHausDatabase by lazy {
        RhythHausDatabase(driver)
    }
}

/** Creates the Android database using the initialized application context. */
public actual fun createLibraryDatabase(): LibraryDatabase =
    LibraryDatabase(LibraryDatabaseAndroidContextHolder.applicationContext)

internal fun libraryDatabaseCallback() =
    object : AndroidSqliteDriver.Callback(RhythHausDatabase.Schema) {
        override fun onOpen(db: SupportSQLiteDatabase) {
            db.setForeignKeyConstraintsEnabled(true)
        }
    }

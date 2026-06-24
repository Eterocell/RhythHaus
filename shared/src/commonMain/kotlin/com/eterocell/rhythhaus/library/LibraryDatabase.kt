package com.eterocell.rhythhaus.library

import app.cash.sqldelight.db.SqlDriver

expect class LibraryDatabase {
    val driver: SqlDriver
    val database: RhythHausDatabase
}

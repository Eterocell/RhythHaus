package com.eterocell.rhythhaus.library

import android.content.Context

/**
 * Retains the process-wide Android context used by shared platform services.
 */
public object LibraryDatabaseContext {
    private lateinit var storedApplicationContext: Context

    public var applicationContext: Context
        get() = storedApplicationContext
        set(value) {
            storedApplicationContext = value.applicationContext
            setLibraryDatabaseAndroidContext(storedApplicationContext)
        }
}

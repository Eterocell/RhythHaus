package com.eterocell.rhythhaus

import android.app.Application
import com.eterocell.rhythhaus.library.LibraryDatabaseContext

class RhythHausApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        LibraryDatabaseContext.applicationContext = this
    }
}

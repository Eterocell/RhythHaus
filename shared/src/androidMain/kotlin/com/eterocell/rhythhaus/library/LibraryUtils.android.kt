package com.eterocell.rhythhaus.library

actual fun currentTimeMillis(): Long = System.currentTimeMillis()
actual fun uuid4(): String = java.util.UUID.randomUUID().toString()

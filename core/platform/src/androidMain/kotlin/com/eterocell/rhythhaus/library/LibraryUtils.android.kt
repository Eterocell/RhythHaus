package com.eterocell.rhythhaus.library

/** Returns the current Unix epoch timestamp in milliseconds. */
public actual fun currentTimeMillis(): Long = System.currentTimeMillis()

/** Returns a newly generated RFC 4122 version 4 UUID string. */
public actual fun uuid4(): String = java.util.UUID.randomUUID().toString()

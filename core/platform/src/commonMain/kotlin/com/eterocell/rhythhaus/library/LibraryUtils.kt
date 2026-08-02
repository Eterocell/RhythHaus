package com.eterocell.rhythhaus.library

/** Returns the current Unix epoch timestamp in milliseconds. */
public expect fun currentTimeMillis(): Long

/** Returns a newly generated RFC 4122 version 4 UUID string. */
public expect fun uuid4(): String

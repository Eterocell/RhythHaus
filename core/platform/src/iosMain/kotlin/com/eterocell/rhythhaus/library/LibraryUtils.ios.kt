package com.eterocell.rhythhaus.library

import platform.Foundation.NSDate
import platform.Foundation.NSUUID
import platform.Foundation.timeIntervalSince1970

/** Returns the current Unix epoch timestamp in milliseconds. */
public actual fun currentTimeMillis(): Long =
    (NSDate().timeIntervalSince1970 * 1000).toLong()

/** Returns a newly generated RFC 4122 version 4 UUID string. */
public actual fun uuid4(): String = NSUUID().UUIDString()

package com.eterocell.rhythhaus

import platform.Foundation.NSLock

/**
 * Apple actual: a Foundation [NSLock] provides the blocking ownership lock on
 * iOS targets, where the common stdlib has no portable `synchronized`.
 */
internal actual class OwnershipLock actual constructor() {
    private val lock = NSLock()

    actual fun <T> withLock(block: () -> T): T {
        lock.lock()
        try {
            return block()
        } finally {
            lock.unlock()
        }
    }
}

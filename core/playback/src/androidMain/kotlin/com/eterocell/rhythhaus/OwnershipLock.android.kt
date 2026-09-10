package com.eterocell.rhythhaus

/** Android actual: a plain monitor provides the blocking ownership lock. */
internal actual class OwnershipLock actual constructor() {
    private val monitor = Any()

    actual fun <T> withLock(block: () -> T): T =
        synchronized(monitor) { block() }
}

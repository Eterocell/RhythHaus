package com.eterocell.rhythhaus

actual fun platformLog(tag: String, message: String) {
    println("[$tag] $message")
}

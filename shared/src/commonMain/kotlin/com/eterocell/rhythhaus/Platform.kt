package com.eterocell.rhythhaus

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
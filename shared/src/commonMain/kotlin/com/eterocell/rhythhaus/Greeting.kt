package com.eterocell.rhythhaus

class Greeting {
    private val platform = getPlatform()

    fun greet(): String = sayHello(platform.name)
}

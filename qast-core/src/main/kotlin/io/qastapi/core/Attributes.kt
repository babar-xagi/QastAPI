package io.qastapi.core

import java.util.concurrent.ConcurrentHashMap

class AttributeKey<T : Any>(val name: String) {
    override fun toString(): String = "AttributeKey($name)"
}

class Attributes {
    private val map = ConcurrentHashMap<AttributeKey<*>, Any>()

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> get(key: AttributeKey<T>): T? = map[key] as? T

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getOrPut(key: AttributeKey<T>, block: () -> T): T =
        map.computeIfAbsent(key) { block() } as T

    fun <T : Any> put(key: AttributeKey<T>, value: T) {
        map[key] = value
    }

    fun <T : Any> remove(key: AttributeKey<T>) {
        map.remove(key)
    }

    fun contains(key: AttributeKey<*>): Boolean = map.containsKey(key)
}

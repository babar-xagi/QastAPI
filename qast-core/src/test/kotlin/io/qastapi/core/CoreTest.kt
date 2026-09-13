package io.qastapi.core

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CoreTest {

    @Test
    fun testLifecycle() = runTest {
        val lifecycle = ApplicationLifecycle()
        var started = false
        var stopped = false

        lifecycle.onStart { started = true }
        lifecycle.onStop { stopped = true }

        assertEquals(LifecycleState.UNINITIALIZED, lifecycle.state)

        lifecycle.start { }
        assertEquals(LifecycleState.RUNNING, lifecycle.state)
        assertTrue(started)

        lifecycle.stop { }
        assertEquals(LifecycleState.STOPPED, lifecycle.state)
        assertTrue(stopped)
    }

    @Test
    fun testEnvironment() {
        val dev = Environment.fromString("development")
        assertTrue(dev.isDev())

        val prod = Environment.fromString("prod")
        assertTrue(prod.isProd())
    }

    @Test
    fun testExceptions() {
        val ex = NotFoundException("Item not found", mapOf("id" to 123))
        assertEquals(404, ex.statusCode)
        assertEquals("Item not found", ex.message)
    }

    @Test
    fun testAttributes() {
        val attrs = Attributes()
        val key = AttributeKey<String>("testKey")
        attrs.put(key, "hello")
        assertEquals("hello", attrs.get(key))
    }
}

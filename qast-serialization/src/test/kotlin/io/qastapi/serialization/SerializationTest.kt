package io.qastapi.serialization

import io.qastapi.http.HttpMethod
import io.qastapi.http.HttpStatus
import io.qastapi.http.QastRequest
import io.qastapi.routing.QastContext
import io.qastapi.routing.Router
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@Serializable
data class TestUser(val id: Long, val name: String)

class SerializationTest {

    @Test
    fun testMapSerialization() {
        val map = mapOf("message" to "Hello QastAPI", "count" to 42, "active" to true)
        val json = QastJson.serialize(map)
        assertTrue(json.contains("\"message\":\"Hello QastAPI\""))
        assertTrue(json.contains("\"count\":42"))
        assertTrue(json.contains("\"active\":true"))
    }

    @Test
    fun testSerializableClassSerialization() {
        val user = TestUser(101L, "Alice")
        val json = QastJson.serialize(user)
        assertTrue(json.contains("\"id\":101"))
        assertTrue(json.contains("\"name\":\"Alice\""))
    }

    @Test
    fun testSerializableClassDeserialization() {
        val json = """{"id":202,"name":"Bob"}"""
        val user = QastJson.decodeFromString<TestUser>(json)
        assertEquals(202L, user.id)
        assertEquals("Bob", user.name)
    }

    @Test
    fun testRouterWithSerialization() = runTest {
        val router = Router()
        installSerialization(router)

        router.get("/users") {
            listOf(TestUser(1L, "Charlie"), TestUser(2L, "Dave"))
        }

        val req = QastRequest(HttpMethod.GET, "/users", "/users")
        val resp = router.handle(req)

        assertEquals(HttpStatus.OK, resp.status)
        assertEquals("application/json; charset=utf-8", resp.headers.contentType)
        val body = resp.bodyText()
        assertTrue(body.contains("\"name\":\"Charlie\""))
        assertTrue(body.contains("\"name\":\"Dave\""))
    }

    @Test
    fun testRequestBodyDeserialization() = runTest {
        val router = Router()
        installSerialization(router)

        router.post("/users") {
            val user = body<TestUser>()
            mapOf("received" to user.name, "id" to user.id)
        }

        val payload = """{"id":7,"name":"Eve"}""".toByteArray()
        val req = QastRequest(HttpMethod.POST, "/users", "/users", body = payload)
        val resp = router.handle(req)

        assertEquals(HttpStatus.OK, resp.status)
        assertTrue(resp.bodyText().contains("\"received\":\"Eve\""))
        assertTrue(resp.bodyText().contains("\"id\":7"))
    }
}

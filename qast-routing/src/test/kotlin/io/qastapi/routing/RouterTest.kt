package io.qastapi.routing

import io.qastapi.http.HttpHeaders
import io.qastapi.http.HttpMethod
import io.qastapi.http.HttpStatus
import io.qastapi.http.QastRequest
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RouterTest {

    @Test
    fun testBasicRouteMatching() = runTest {
        val router = Router()
        router.get("/hello") {
            "Hello World"
        }

        val req = QastRequest(
            method = HttpMethod.GET,
            path = "/hello",
            uri = "/hello"
        )
        val resp = router.handle(req)
        assertEquals(HttpStatus.OK, resp.status)
        assertEquals("Hello World", resp.bodyText())
    }

    @Test
    fun testPathParameterExtraction() = runTest {
        val router = Router()
        router.get("/users/{id}") {
            val id = path("id")
            "User: $id"
        }

        val req = QastRequest(
            method = HttpMethod.GET,
            path = "/users/42",
            uri = "/users/42"
        )
        val resp = router.handle(req)
        assertEquals(HttpStatus.OK, resp.status)
        assertEquals("User: 42", resp.bodyText())
    }

    @Test
    fun testRouteGrouping() = runTest {
        val router = Router()
        router.group("/api/v1") {
            get("/status") {
                "OK"
            }
            get("/items/{id}") {
                "Item ${path("id")}"
            }
        }

        val statusReq = QastRequest(HttpMethod.GET, "/api/v1/status", "/api/v1/status")
        val statusResp = router.handle(statusReq)
        assertEquals("OK", statusResp.bodyText())

        val itemReq = QastRequest(HttpMethod.GET, "/api/v1/items/99", "/api/v1/items/99")
        val itemResp = router.handle(itemReq)
        assertEquals("Item 99", itemResp.bodyText())
    }

    @Test
    fun testMethodNotAllowed() = runTest {
        val router = Router()
        router.post("/items") {
            "created"
        }

        val req = QastRequest(HttpMethod.GET, "/items", "/items")
        val resp = router.handle(req)
        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, resp.status)
        assertTrue(resp.headers.asMap()[HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS]?.contains("POST") == true)
    }

    @Test
    fun testNotFound() = runTest {
        val router = Router()
        val req = QastRequest(HttpMethod.GET, "/unknown", "/unknown")
        val resp = router.handle(req)
        assertEquals(HttpStatus.NOT_FOUND, resp.status)
    }

    @Test
    fun testMiddlewarePipeline() = runTest {
        val router = Router()
        var middlewareExecuted = false

        router.use { ctx, next ->
            middlewareExecuted = true
            ctx.response.header("X-Custom", "test")
            next()
        }

        router.get("/test") {
            "done"
        }

        val req = QastRequest(HttpMethod.GET, "/test", "/test")
        val resp = router.handle(req)
        assertTrue(middlewareExecuted)
        assertEquals("test", resp.headers["X-Custom"])
    }
}

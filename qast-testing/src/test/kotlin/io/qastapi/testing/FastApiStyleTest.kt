package io.qastapi.testing

import io.qastapi.Delete
import io.qastapi.Get
import io.qastapi.Post
import io.qastapi.QastAPI
import io.qastapi.QastContext
import io.qastapi.qastapi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HomeController {
    @Get("/")
    fun home(): String {
        return "hello world"
    }

    @Get("/users/{id}")
    fun getUser(ctx: QastContext): Map<String, String> {
        val id = ctx.path("id")
        return mapOf("id" to id, "name" to "User $id")
    }

    @Post("/items")
    fun createItem(ctx: QastContext): String {
        return "created: ${ctx.bodyText()}"
    }

    @Delete("/items/{id}")
    fun deleteItem(ctx: QastContext): String {
        return "deleted: ${ctx.path("id")}"
    }
}

class FastApiStyleTest {

    @Test
    fun testFastApiStyleAppInstanceWithDirectRoutes() {
        // 1. FastAPI-style: var app = qastapi()
        val app = qastapi()
        assertFalse(app.isRunning)

        // 2. Direct route definitions on app
        app.get("/") {
            "hello world"
        }

        app.get("/greet/{name}") {
            val name = path("name")
            "hello $name"
        }

        app.post("/echo") {
            bodyText()
        }

        // 3. Test using in-memory test runner
        qastTest(app) {
            client.get("/")
                .expectStatus(200)
                .expectBody("hello world")

            client.get("/greet/antigravity")
                .expectStatus(200)
                .expectBody("hello antigravity")

            client.post("/echo", body = "ping-pong")
                .expectStatus(200)
                .expectBody("ping-pong")
        }
    }

    @Test
    fun testFastApiStyleWithAnnotationController() {
        val app = QastAPI()

        // Register annotated controller
        app.register(HomeController())

        qastTest(app) {
            client.get("/")
                .expectStatus(200)
                .expectBody("hello world")

            val userRes = client.get("/users/42")
                .expectStatus(200)
            assertTrue(userRes.bodyText.contains("\"id\":\"42\""))
            assertTrue(userRes.bodyText.contains("\"name\":\"User 42\""))

            client.post("/items", body = "gadget")
                .expectStatus(200)
                .expectBody("created: gadget")

            client.delete("/items/101")
                .expectStatus(200)
                .expectBody("deleted: 101")
        }
    }

    @Test
    fun testBackwardCompatibilityLambdaDsl() {
        var initialized = false
        val app = QastAPI(autoStart = false) {
            initialized = true
            get("/legacy") {
                "legacy works"
            }
        }

        assertTrue(initialized)

        qastTest(app) {
            client.get("/legacy")
                .expectStatus(200)
                .expectBody("legacy works")
        }
    }
}

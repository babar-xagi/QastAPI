package io.qastapi.testing

import io.qastapi.QastAPI
import io.qastapi.core.NotFoundException
import io.qastapi.http.HttpStatus
import io.qastapi.http.SetCookie
import io.qastapi.serialization.body
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@Serializable
data class Item(val id: String, val name: String, val price: Double)

@Serializable
data class CreateItemRequest(val name: String, val price: Double)

class FullFrameworkTest {

    @Test
    fun testComprehensivePhase1Features() {
        val app = QastAPI(autoStart = false) {
            get("/") {
                "Hello QastAPI!"
            }

            get("/json") {
                mapOf("framework" to "QastAPI", "version" to "0.1.0")
            }

            get("/items/{id}") {
                val itemId = path("id")
                Item(id = itemId, name = "Widget $itemId", price = 19.99)
            }

            get("/search") {
                val q = query("q") ?: "all"
                val limit = query("limit")?.toIntOrNull() ?: 10
                mapOf("query" to q, "limit" to limit)
            }

            post("/items") {
                val req = body<CreateItemRequest>()
                status(HttpStatus.CREATED)
                Item(id = "new-100", name = req.name, price = req.price)
            }

            delete("/items/{id}") {
                val itemId = path("id")
                if (itemId == "missing") {
                    throw NotFoundException("Item $itemId was not found")
                }
                status(HttpStatus.NO_CONTENT)
                ""
            }

            get("/cookie-demo") {
                cookie(SetCookie(name = "sessionId", value = "sess_xyz"))
                "cookie set"
            }
        }

        qastTest(app) {
            // 1. Plain text GET
            client.get("/")
                .expectStatus(200)
                .expectBody("Hello QastAPI!")

            // 2. JSON Map GET
            val jsonRes = client.get("/json")
                .expectStatus(200)
                .expectHeader("Content-Type", "application/json; charset=utf-8")
            assertTrue(jsonRes.bodyText.contains("\"framework\":\"QastAPI\""))

            // 3. Path parameter GET with Serializable object
            val itemRes = client.get("/items/42")
                .expectStatus(200)
            val item = itemRes.json<Item>()
            assertEquals("42", item.id)
            assertEquals("Widget 42", item.name)

            // 4. Query parameter
            val searchRes = client.get("/search?q=kotlin&limit=5")
                .expectStatus(200)
            assertTrue(searchRes.bodyText.contains("\"query\":\"kotlin\""))
            assertTrue(searchRes.bodyText.contains("\"limit\":5"))

            // 5. POST with JSON body
            val createRes = client.post(
                "/items",
                body = """{"name":"Super Gizmo","price":49.95}"""
            )
                .expectStatus(201)
            val created = createRes.json<Item>()
            assertEquals("new-100", created.id)
            assertEquals("Super Gizmo", created.name)
            assertEquals(49.95, created.price)

            // 6. DELETE 204
            client.delete("/items/42")
                .expectStatus(204)

            // 7. Exception handling 404
            val errorRes = client.delete("/items/missing")
                .expectStatus(404)
            assertTrue(errorRes.bodyText.contains("not found"))

            // 8. 405 Method Not Allowed
            client.post("/json")
                .expectStatus(405)

            // 9. Route Not Found 404
            client.get("/non-existent-route")
                .expectStatus(404)
        }
    }
}

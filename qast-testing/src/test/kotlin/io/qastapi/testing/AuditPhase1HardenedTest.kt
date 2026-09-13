package io.qastapi.testing

import io.qastapi.QastAPI
import io.qastapi.core.Environment
import io.qastapi.http.HttpHeaders
import io.qastapi.http.HttpStatus
import io.qastapi.serialization.body
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@Serializable
data class AuditPayload(val name: String, val age: Int)

class AuditPhase1HardenedTest {

    @Test
    fun testRouteConflictsAndPrecedence() {
        val app = QastAPI(autoStart = false) {
            // Register parameterized route FIRST
            get("/users/{id}") {
                "PARAM: ${path("id")}"
            }
            // Register exact route SECOND - exact route must take precedence over param!
            get("/users/me") {
                "EXACT_ME"
            }
            // Wildcard route
            get("/users/*") {
                "WILDCARD"
            }
            // Trailing slash route
            get("/trailing") {
                "TRAILING_OK"
            }
            // Grouped routes
            group("/v1") {
                get("/profile") {
                    "V1_PROFILE"
                }
            }
        }

        qastTest(app) {
            // Exact route must beat parameterized route even if registered second
            client.get("/users/me")
                .expectStatus(200)
                .expectBody("EXACT_ME")

            // Parameterized route matches other IDs
            client.get("/users/42")
                .expectStatus(200)
                .expectBody("PARAM: 42")

            // Wildcard route matches subpaths
            client.get("/users/settings/privacy")
                .expectStatus(200)
                .expectBody("WILDCARD")

            // Trailing slash normalization
            client.get("/trailing")
                .expectStatus(200)
                .expectBody("TRAILING_OK")

            client.get("/trailing/")
                .expectStatus(200)
                .expectBody("TRAILING_OK")

            // Grouped route
            client.get("/v1/profile")
                .expectStatus(200)
                .expectBody("V1_PROFILE")
        }
    }

    @Test
    fun testPathParameterUrlDecoding() {
        val app = QastAPI(autoStart = false) {
            get("/users/{name}") {
                "NAME: ${path("name")}"
            }
            get("/tags/{tag}") {
                "TAG: ${path("tag")}"
            }
        }

        qastTest(app) {
            // Space in path: %20 -> space
            client.get("/users/john%20doe")
                .expectStatus(200)
                .expectBody("NAME: john doe")

            // + in path: %2B -> +
            client.get("/tags/c%2B%2B")
                .expectStatus(200)
                .expectBody("TAG: c++")
        }
    }

    @Test
    fun testQueryStringParsing() {
        val app = QastAPI(autoStart = false) {
            get("/search") {
                val q = query("q") ?: ""
                val tags = queryAll("tag")
                mapOf("q" to q, "tags" to tags)
            }
        }

        qastTest(app) {
            // Space and plus in query string
            val res1 = client.get("/search?q=hello%20world")
                .expectStatus(200)
            assertTrue(res1.bodyText.contains("\"q\":\"hello world\""))

            val res2 = client.get("/search?q=hello+world")
                .expectStatus(200)
            assertTrue(res2.bodyText.contains("\"q\":\"hello world\""))

            // Percent sign in query string: 100%
            val res3 = client.get("/search?q=100%25")
                .expectStatus(200)
            assertTrue(res3.bodyText.contains("\"q\":\"100%\""))

            // Unicode query string
            val res4 = client.get("/search?q=%E4%BD%A0%E5%A5%BD")
                .expectStatus(200)
            assertTrue(res4.bodyText.contains("\"q\":\"你好\""))

            // Repeated query parameters: tag=kotlin&tag=ai
            val res5 = client.get("/search?tag=kotlin&tag=ai")
                .expectStatus(200)
            assertTrue(res5.bodyText.contains("\"tags\":[\"kotlin\",\"ai\"]"))
        }
    }

    @Test
    fun testHeadRequestAndNoContent204() {
        val app = QastAPI(autoStart = false) {
            get("/data") {
                "Some response content"
            }
            delete("/resource") {
                status(HttpStatus.NO_CONTENT)
                ""
            }
        }

        qastTest(app) {
            // HEAD request should invoke GET route, preserve headers, but have empty body
            val headRes = client.head("/data")
                .expectStatus(200)
            assertEquals(0, headRes.bodyBytes.size)

            // 204 No Content should have empty body and no Content-Length header
            val deleteRes = client.delete("/resource")
                .expectStatus(204)
            assertEquals(0, deleteRes.bodyBytes.size)
            assertNull(deleteRes.headers[HttpHeaders.CONTENT_LENGTH])
        }
    }

    @Test
    fun testMalformedJsonReturns400Never500() {
        val app = QastAPI(autoStart = false) {
            post("/parse") {
                val payload = body<AuditPayload>()
                mapOf("received" to payload.name)
            }
        }

        qastTest(app) {
            // Malformed JSON (unclosed bracket)
            val res1 = client.post("/parse", body = "{\"name\": \"Alice\", \"age\": ")
                .expectStatus(400)
            assertTrue(res1.bodyText.contains("Invalid JSON payload"))

            // Non-JSON garbage string
            val res2 = client.post("/parse", body = "THIS IS NOT JSON")
                .expectStatus(400)
            assertTrue(res2.bodyText.contains("Invalid JSON payload"))

            // Empty body
            val res3 = client.post("/parse", body = "")
                .expectStatus(400)
            assertTrue(res3.bodyText.contains("Request body cannot be blank"))
        }
    }

    @Test
    fun testUnhandledExceptionSafeInProduction() {
        val app = QastAPI(autoStart = false) {
            get("/crash") {
                throw RuntimeException("Secret internal db password: supersecret123")
            }
        }

        // Set production environment temporarily
        System.setProperty("qast.env", "production")
        try {
            qastTest(app) {
                val res = client.get("/crash")
                    .expectStatus(500)
                // In production mode, secret message must NOT leak to client
                assertTrue(!res.bodyText.contains("supersecret123"))
                assertTrue(res.bodyText.contains("An unexpected error occurred"))
            }
        } finally {
            System.clearProperty("qast.env")
        }
    }
}

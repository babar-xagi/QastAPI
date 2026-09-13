package io.qastapi.testing

import io.qastapi.QastAPI
import io.qastapi.config.ConfigLoader
import io.qastapi.core.Environment
import io.qastapi.core.LogFormat
import io.qastapi.http.HttpHeaders
import io.qastapi.http.HttpStatus
import io.qastapi.routing.Middlewares
import io.qastapi.routing.requestId
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class Phase2DeveloperExperienceTest {

    @BeforeTest
    fun setup() {
        Environment.setProfile("development")
    }

    @AfterTest
    fun tearDown() {
        Environment.setProfile("development")
    }

    @Test
    fun testProfileCascadingAndOverriding() {
        val tempDir = File.createTempFile("qast-profile-test", "")
        tempDir.delete()
        tempDir.mkdirs()

        try {
            val baseFile = File(tempDir, "qast.toml")
            baseFile.writeText(
                """
                [project]
                name = "cascade-test"
                version = "0.2.0"

                [server]
                port = 8000
                devMode = true

                [logging]
                format = "pretty"
                """.trimIndent()
            )

            val devFile = File(tempDir, "qast.dev.toml")
            devFile.writeText(
                """
                [server]
                port = 8000
                devMode = true

                [logging]
                format = "pretty"
                """.trimIndent()
            )

            val prodFile = File(tempDir, "qast.prod.toml")
            prodFile.writeText(
                """
                [server]
                port = 8080
                devMode = false

                [logging]
                format = "json"

                [environment]
                mode = "production"
                """.trimIndent()
            )

            // Test dev profile
            val devConfig = ConfigLoader.load(baseFile, profile = "dev")
            assertEquals(8000, devConfig.server.port)
            assertTrue(devConfig.server.devMode)
            assertEquals(LogFormat.PRETTY, devConfig.logging.format)
            assertEquals(Environment.DEVELOPMENT, devConfig.environment)

            // Test prod profile
            val prodConfig = ConfigLoader.load(baseFile, profile = "prod")
            assertEquals(8080, prodConfig.server.port)
            assertFalse(prodConfig.server.devMode)
            assertEquals(LogFormat.JSON, prodConfig.logging.format)
            assertEquals(Environment.PRODUCTION, prodConfig.environment)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun testRequestIdGenerationAndPropagation() {
        var recordedRequestId = ""
        val app = QastAPI(autoStart = false) {
            use(Middlewares.structuredLogging(LogFormat.PRETTY))

            get("/test-id") {
                recordedRequestId = requestId
                mapOf("requestId" to requestId)
            }
        }

        qastTest(app) {
            // Case 1: Auto-generated request ID
            val res1 = client.get("/test-id")
                .expectStatus(200)
            val headerId = res1.headers[HttpHeaders.X_REQUEST_ID]
            assertTrue(!headerId.isNullOrBlank(), "X-Request-ID should be generated and returned in headers")
            assertEquals(headerId, recordedRequestId)

            // Case 2: Custom passed request ID
            val res2 = client.get("/test-id", headers = mapOf(HttpHeaders.X_REQUEST_ID to "req-custom-999"))
                .expectStatus(200)
                .expectHeader(HttpHeaders.X_REQUEST_ID, "req-custom-999")
            assertEquals("req-custom-999", recordedRequestId)
        }
    }

    @Test
    fun testRichJsonErrorInDevelopment() {
        Environment.setProfile("development")
        val app = QastAPI(autoStart = false) {
            use(Middlewares.errorHandling())

            get("/crash") {
                throw IllegalStateException("Simulated database failure")
            }
        }

        qastTest(app) {
            val res = client.get("/crash", headers = mapOf(HttpHeaders.ACCEPT to "application/json"))
                .expectStatus(500)

            val body = res.bodyText
            assertTrue(body.contains("IllegalStateException"), "Rich dev error should contain exceptionClass")
            assertTrue(body.contains("Simulated database failure"), "Rich dev error should contain message")
            assertTrue(body.contains("stackTrace"), "Rich dev error should contain stackTrace")
            assertTrue(body.contains("requestId"), "Rich dev error should contain requestId")
        }
    }

    @Test
    fun testHtmlDeveloperErrorPageInDevelopment() {
        Environment.setProfile("development")
        val app = QastAPI(autoStart = false) {
            use(Middlewares.errorHandling())

            get("/crash-browser") {
                throw NullPointerException("User profile not found")
            }
        }

        qastTest(app) {
            val res = client.get("/crash-browser", headers = mapOf(HttpHeaders.ACCEPT to "text/html,application/xhtml+xml"))
                .expectStatus(500)
                .expectHeader(HttpHeaders.CONTENT_TYPE, "text/html; charset=utf-8")

            val body = res.bodyText
            assertTrue(body.contains("<!DOCTYPE html>"), "Should render HTML DOCTYPE")
            assertTrue(body.contains("QastAPI Developer Error"), "Should contain QastAPI dev header")
            assertTrue(body.contains("User profile not found"), "Should display exception message")
            assertTrue(body.contains("NullPointerException"), "Should display exception class")
            assertTrue(body.contains("Request Details"), "Should display request details card")
        }
    }

    @Test
    fun testSanitizedErrorInProduction() {
        Environment.setProfile("production")
        val app = QastAPI(autoStart = false) {
            use(Middlewares.errorHandling())

            get("/crash-prod") {
                throw SecurityException("Database secret leak!")
            }
        }

        qastTest(app) {
            val res = client.get("/crash-prod")
                .expectStatus(500)

            val body = res.bodyText
            assertTrue(body.contains("An unexpected error occurred"), "Prod should sanitize message")
            assertFalse(body.contains("Database secret leak!"), "Prod must NEVER leak exception message")
            assertFalse(body.contains("SecurityException"), "Prod must NEVER leak exception class")
            assertFalse(body.contains("stackTrace"), "Prod must NEVER leak stack trace")
            assertTrue(body.contains("requestId"), "Prod should provide correlation requestId")
        }
    }

    @Test
    fun testSmartRouteSuggestionsOnNotFound() {
        Environment.setProfile("development")
        val app = QastAPI(autoStart = false) {
            use(Middlewares.errorHandling())

            get("/users/{id}") {
                mapOf("user" to path("id"))
            }

            get("/products/search") {
                mapOf("items" to emptyList<String>())
            }
        }

        qastTest(app) {
            // Typo /user/123 -> suggests /users/{id}
            val res = client.get("/user/123")
                .expectStatus(404)

            val body = res.bodyText
            assertTrue(body.contains("Did you mean"), "404 in dev should contain 'Did you mean' suggestion")
            assertTrue(body.contains("/users/{id}"), "Suggestion should point to /users/{id}")
        }
    }
}

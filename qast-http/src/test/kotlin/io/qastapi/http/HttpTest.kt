package io.qastapi.http

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HttpTest {

    @Test
    fun testHeadersCaseInsensitive() {
        val headers = MutableHttpHeaders()
        headers.set("Content-Type", "application/json")
        assertEquals("application/json", headers["content-type"])
        assertEquals("application/json", headers["CONTENT-TYPE"])
        assertEquals("application/json", headers.contentType)
    }

    @Test
    fun testHttpStatus() {
        val status = HttpStatus.fromCode(200)
        assertEquals(HttpStatus.OK, status)
        assertTrue(status.isSuccess)

        val notFound = HttpStatus.fromCode(404)
        assertEquals(HttpStatus.NOT_FOUND, notFound)
        assertTrue(notFound.isClientError)
    }

    @Test
    fun testCookieParsing() {
        val cookieHeader = "sessionId=abc123; theme=dark; token=xyz"
        val cookies = Cookie.parse(cookieHeader)
        assertEquals("abc123", cookies["sessionId"])
        assertEquals("dark", cookies["theme"])
        assertEquals("xyz", cookies["token"])
    }

    @Test
    fun testSetCookieHeader() {
        val setCookie = SetCookie(
            name = "auth",
            value = "secret",
            maxAge = 3600,
            path = "/",
            secure = true,
            httpOnly = true,
            sameSite = SameSite.LAX
        )
        val header = setCookie.toHeaderValue()
        assertTrue(header.contains("auth=secret"))
        assertTrue(header.contains("Max-Age=3600"))
        assertTrue(header.contains("Secure"))
        assertTrue(header.contains("HttpOnly"))
        assertTrue(header.contains("SameSite=Lax"))
    }

    @Test
    fun testQastResponseBuilders() {
        val jsonResp = QastResponse.json("""{"hello":"world"}""", HttpStatus.CREATED)
        assertEquals(HttpStatus.CREATED, jsonResp.status)
        assertEquals("application/json; charset=utf-8", jsonResp.headers.contentType)
        assertEquals("""{"hello":"world"}""", jsonResp.bodyText())

        val redirectResp = QastResponse.redirect("/home")
        assertEquals(HttpStatus.FOUND, redirectResp.status)
        assertEquals("/home", redirectResp.headers.location)
    }
}

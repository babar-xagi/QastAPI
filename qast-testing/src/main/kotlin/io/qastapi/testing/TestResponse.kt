package io.qastapi.testing

import io.qastapi.http.HttpHeaders
import io.qastapi.http.HttpStatus
import io.qastapi.http.QastResponse
import io.qastapi.serialization.QastJson
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestResponse(
    val status: HttpStatus,
    val headers: HttpHeaders,
    val bodyBytes: ByteArray
) {
    val bodyText: String get() = String(bodyBytes, Charsets.UTF_8)

    fun expectStatus(expectedCode: Int): TestResponse {
        assertEquals(expectedCode, status.code, "Expected HTTP status $expectedCode but got ${status.code}")
        return this
    }

    fun expectStatus(expected: HttpStatus): TestResponse {
        assertEquals(expected, status, "Expected HTTP status $expected but got $status")
        return this
    }

    fun expectBody(expected: String): TestResponse {
        assertEquals(expected, bodyText, "Expected body to be '$expected' but was '$bodyText'")
        return this
    }

    fun expectBodyContains(substring: String): TestResponse {
        assertTrue(bodyText.contains(substring), "Expected body to contain '$substring' but got '$bodyText'")
        return this
    }

    fun expectHeader(name: String, expectedValue: String): TestResponse {
        val actual = headers[name]
        assertEquals(expectedValue, actual, "Expected header '$name' to be '$expectedValue' but was '$actual'")
        return this
    }

    inline fun <reified T> json(): T {
        return QastJson.decodeFromString(bodyText)
    }

    companion object {
        fun from(response: QastResponse): TestResponse {
            return TestResponse(
                status = response.status,
                headers = response.headers,
                bodyBytes = response.body
            )
        }
    }
}

package io.qastapi.testing

import io.qastapi.http.Cookie
import io.qastapi.http.HttpHeaders
import io.qastapi.http.HttpMethod
import io.qastapi.http.MutableHttpHeaders
import io.qastapi.http.QastRequest
import io.qastapi.routing.QastApp
import kotlinx.coroutines.runBlocking
import java.net.URI
import java.net.URLDecoder

class TestClient(private val app: QastApp) {

    suspend fun get(path: String, headers: Map<String, String> = emptyMap()): TestResponse =
        request(HttpMethod.GET, path, null, headers)

    suspend fun post(path: String, body: String? = null, headers: Map<String, String> = emptyMap()): TestResponse =
        request(HttpMethod.POST, path, body, headers)

    suspend fun put(path: String, body: String? = null, headers: Map<String, String> = emptyMap()): TestResponse =
        request(HttpMethod.PUT, path, body, headers)

    suspend fun patch(path: String, body: String? = null, headers: Map<String, String> = emptyMap()): TestResponse =
        request(HttpMethod.PATCH, path, body, headers)

    suspend fun delete(path: String, headers: Map<String, String> = emptyMap()): TestResponse =
        request(HttpMethod.DELETE, path, null, headers)

    suspend fun head(path: String, headers: Map<String, String> = emptyMap()): TestResponse =
        request(HttpMethod.HEAD, path, null, headers)

    suspend fun options(path: String, headers: Map<String, String> = emptyMap()): TestResponse =
        request(HttpMethod.OPTIONS, path, null, headers)

    suspend fun request(
        method: HttpMethod,
        pathAndQuery: String,
        body: String? = null,
        headers: Map<String, String> = emptyMap()
    ): TestResponse {
        val uri = URI.create(pathAndQuery)
        val path = uri.path
        val queryParams = io.qastapi.http.QueryParser.parse(uri.rawQuery)

        val reqHeaders = MutableHttpHeaders()
        headers.forEach { (k, v) -> reqHeaders.set(k, v) }

        if (body != null && reqHeaders.contentType == null) {
            reqHeaders.set(HttpHeaders.CONTENT_TYPE, "application/json")
        }

        val cookies = Cookie.parse(reqHeaders[HttpHeaders.COOKIE])
        val bodyBytes = body?.toByteArray(Charsets.UTF_8) ?: ByteArray(0)

        val request = QastRequest(
            method = method,
            path = path,
            uri = pathAndQuery,
            queryParams = queryParams,
            headers = reqHeaders,
            cookies = cookies,
            body = bodyBytes
        )

        val response = app.router.handle(request)
        return TestResponse.from(response)
    }
}

class TestContext(val app: QastApp) {
    val client = TestClient(app)
}

fun qastTest(app: QastApp, block: suspend TestContext.() -> Unit) {
    runBlocking {
        val context = TestContext(app)
        context.block()
    }
}

fun qastTest(block: suspend TestContext.() -> Unit) {
    val app = QastApp()
    qastTest(app, block)
}

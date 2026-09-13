package io.qastapi.routing

import io.qastapi.core.Attributes
import io.qastapi.http.HttpHeaders
import io.qastapi.http.HttpMethod
import io.qastapi.http.HttpStatus
import io.qastapi.http.QastRequest
import io.qastapi.http.QastResponse
import io.qastapi.http.SetCookie
import java.nio.charset.Charset

class QastContext(
    val request: QastRequest,
    val response: QastResponse = QastResponse()
) {
    val method: HttpMethod get() = request.method
    val path: String get() = request.path
    val uri: String get() = request.uri
    val pathParams: Map<String, String> get() = request.pathParams
    val queryParams: Map<String, List<String>> get() = request.queryParams
    val headers: HttpHeaders get() = request.headers
    val cookies: Map<String, String> get() = request.cookies
    val attributes: Attributes get() = request.attributes

    fun path(name: String): String = request.path(name)
    fun pathOrNull(name: String): String? = request.pathOrNull(name)
    fun query(name: String): String? = request.query(name)
    fun queryAll(name: String): List<String> = request.queryAll(name)
    fun header(name: String): String? = request.header(name)
    fun cookie(name: String): String? = request.cookie(name)
    fun bodyText(charset: Charset = Charsets.UTF_8): String = request.bodyText(charset)

    fun status(status: HttpStatus): QastContext {
        response.status(status)
        return this
    }

    fun status(code: Int): QastContext {
        response.status(HttpStatus.fromCode(code))
        return this
    }

    fun header(name: String, value: String): QastContext {
        response.header(name, value)
        return this
    }

    fun cookie(cookie: SetCookie): QastContext {
        response.cookie(cookie)
        return this
    }

    fun text(text: String, status: HttpStatus = HttpStatus.OK): QastResponse {
        response.status = status
        response.contentType("text/plain; charset=utf-8")
        response.body(text)
        return response
    }

    fun json(jsonString: String, status: HttpStatus = HttpStatus.OK): QastResponse {
        response.status = status
        response.contentType("application/json; charset=utf-8")
        response.body(jsonString)
        return response
    }

    fun html(htmlString: String, status: HttpStatus = HttpStatus.OK): QastResponse {
        response.status = status
        response.contentType("text/html; charset=utf-8")
        response.body(htmlString)
        return response
    }

    fun redirect(location: String, permanent: Boolean = false): QastResponse {
        val st = if (permanent) HttpStatus.MOVED_PERMANENTLY else HttpStatus.FOUND
        response.status = st
        response.header(HttpHeaders.LOCATION, location)
        return response
    }
}

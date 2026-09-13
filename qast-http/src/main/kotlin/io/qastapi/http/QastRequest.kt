package io.qastapi.http

import io.qastapi.core.Attributes
import java.nio.charset.Charset

class QastRequest(
    val method: HttpMethod,
    val path: String,
    val uri: String,
    val queryParams: Map<String, List<String>> = emptyMap(),
    val pathParams: Map<String, String> = emptyMap(),
    val headers: HttpHeaders = HttpHeaders(),
    val cookies: Map<String, String> = emptyMap(),
    val body: ByteArray = ByteArray(0),
    val remoteAddress: String? = null,
    val attributes: Attributes = Attributes()
) {
    fun query(name: String): String? = queryParams[name]?.firstOrNull()

    fun queryAll(name: String): List<String> = queryParams[name] ?: emptyList()

    fun path(name: String): String = pathParams[name]
        ?: throw IllegalArgumentException("Path parameter '$name' not found in path params: ${pathParams.keys}")

    fun pathOrNull(name: String): String? = pathParams[name]

    fun header(name: String): String? = headers[name]

    fun headerAll(name: String): List<String> = headers.getAll(name)

    fun cookie(name: String): String? = cookies[name]

    fun bodyText(charset: Charset = Charsets.UTF_8): String = String(body, charset)

    val contentType: String? get() = headers.contentType

    val contentLength: Long? get() = headers.contentLength

    fun copyWith(
        pathParams: Map<String, String> = this.pathParams,
        queryParams: Map<String, List<String>> = this.queryParams
    ): QastRequest {
        return QastRequest(
            method = this.method,
            path = this.path,
            uri = this.uri,
            queryParams = queryParams,
            pathParams = pathParams,
            headers = this.headers,
            cookies = this.cookies,
            body = this.body,
            remoteAddress = this.remoteAddress,
            attributes = this.attributes
        )
    }
}

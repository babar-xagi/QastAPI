package io.qastapi.http

import java.nio.charset.Charset

class QastResponse(
    var status: HttpStatus = HttpStatus.OK,
    val headers: MutableHttpHeaders = MutableHttpHeaders(),
    val cookies: MutableList<SetCookie> = mutableListOf(),
    var body: ByteArray = ByteArray(0)
) {
    fun header(name: String, value: String): QastResponse {
        headers.set(name, value)
        return this
    }

    fun addHeader(name: String, value: String): QastResponse {
        headers.add(name, value)
        return this
    }

    fun cookie(cookie: SetCookie): QastResponse {
        cookies.add(cookie)
        return this
    }

    fun status(status: HttpStatus): QastResponse {
        this.status = status
        return this
    }

    fun contentType(type: String): QastResponse {
        headers.set(HttpHeaders.CONTENT_TYPE, type)
        return this
    }

    fun body(bytes: ByteArray): QastResponse {
        this.body = bytes
        return this
    }

    fun body(text: String, charset: Charset = Charsets.UTF_8): QastResponse {
        this.body = text.toByteArray(charset)
        return this
    }

    fun bodyText(charset: Charset = Charsets.UTF_8): String = String(body, charset)

    companion object {
        fun ok(body: String = ""): QastResponse =
            text(body, HttpStatus.OK)

        fun text(text: String, status: HttpStatus = HttpStatus.OK): QastResponse {
            val resp = QastResponse(status = status)
            resp.contentType("text/plain; charset=utf-8")
            resp.body(text)
            return resp
        }

        fun json(jsonString: String, status: HttpStatus = HttpStatus.OK): QastResponse {
            val resp = QastResponse(status = status)
            resp.contentType("application/json; charset=utf-8")
            resp.body(jsonString)
            return resp
        }

        fun html(htmlString: String, status: HttpStatus = HttpStatus.OK): QastResponse {
            val resp = QastResponse(status = status)
            resp.contentType("text/html; charset=utf-8")
            resp.body(htmlString)
            return resp
        }

        fun bytes(
            bytes: ByteArray,
            contentType: String = "application/octet-stream",
            status: HttpStatus = HttpStatus.OK
        ): QastResponse {
            val resp = QastResponse(status = status)
            resp.contentType(contentType)
            resp.body(bytes)
            return resp
        }

        fun status(status: HttpStatus): QastResponse =
            QastResponse(status = status)

        fun redirect(location: String, permanent: Boolean = false): QastResponse {
            val status = if (permanent) HttpStatus.MOVED_PERMANENTLY else HttpStatus.FOUND
            return QastResponse(status = status).header(HttpHeaders.LOCATION, location)
        }
    }
}

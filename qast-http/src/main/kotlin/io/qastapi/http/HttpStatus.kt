package io.qastapi.http

import io.qastapi.core.HttpException

data class HttpStatus(val code: Int, val reasonPhrase: String) {
    val isInformational: Boolean get() = code in 100..199
    val isSuccess: Boolean get() = code in 200..299
    val isRedirection: Boolean get() = code in 300..399
    val isClientError: Boolean get() = code in 400..499
    val isServerError: Boolean get() = code in 500..599

    override fun toString(): String = "$code $reasonPhrase"

    companion object {
        // 1xx Informational
        val CONTINUE = HttpStatus(100, "Continue")
        val SWITCHING_PROTOCOLS = HttpStatus(101, "Switching Protocols")

        // 2xx Success
        val OK = HttpStatus(200, "OK")
        val CREATED = HttpStatus(201, "Created")
        val ACCEPTED = HttpStatus(202, "Accepted")
        val NO_CONTENT = HttpStatus(204, "No Content")
        val RESET_CONTENT = HttpStatus(205, "Reset Content")
        val PARTIAL_CONTENT = HttpStatus(206, "Partial Content")

        // 3xx Redirection
        val MULTIPLE_CHOICES = HttpStatus(300, "Multiple Choices")
        val MOVED_PERMANENTLY = HttpStatus(301, "Moved Permanently")
        val FOUND = HttpStatus(302, "Found")
        val SEE_OTHER = HttpStatus(303, "See Other")
        val NOT_MODIFIED = HttpStatus(304, "Not Modified")
        val TEMPORARY_REDIRECT = HttpStatus(307, "Temporary Redirect")
        val PERMANENT_REDIRECT = HttpStatus(308, "Permanent Redirect")

        // 4xx Client Errors
        val BAD_REQUEST = HttpStatus(400, "Bad Request")
        val UNAUTHORIZED = HttpStatus(401, "Unauthorized")
        val FORBIDDEN = HttpStatus(403, "Forbidden")
        val NOT_FOUND = HttpStatus(404, "Not Found")
        val METHOD_NOT_ALLOWED = HttpStatus(405, "Method Not Allowed")
        val NOT_ACCEPTABLE = HttpStatus(406, "Not Acceptable")
        val REQUEST_TIMEOUT = HttpStatus(408, "Request Timeout")
        val CONFLICT = HttpStatus(409, "Conflict")
        val GONE = HttpStatus(410, "Gone")
        val LENGTH_REQUIRED = HttpStatus(411, "Length Required")
        val PAYLOAD_TOO_LARGE = HttpStatus(413, "Payload Too Large")
        val URI_TOO_LONG = HttpStatus(414, "URI Too Long")
        val UNSUPPORTED_MEDIA_TYPE = HttpStatus(415, "Unsupported Media Type")
        val RANGE_NOT_SATISFIABLE = HttpStatus(416, "Range Not Satisfiable")
        val EXPECTATION_FAILED = HttpStatus(417, "Expectation Failed")
        val UNPROCESSABLE_ENTITY = HttpStatus(422, "Unprocessable Entity")
        val TOO_MANY_REQUESTS = HttpStatus(429, "Too Many Requests")

        // 5xx Server Errors
        val INTERNAL_SERVER_ERROR = HttpStatus(500, "Internal Server Error")
        val NOT_IMPLEMENTED = HttpStatus(501, "Not Implemented")
        val BAD_GATEWAY = HttpStatus(502, "Bad Gateway")
        val SERVICE_UNAVAILABLE = HttpStatus(503, "Service Unavailable")
        val GATEWAY_TIMEOUT = HttpStatus(504, "Gateway Timeout")
        val HTTP_VERSION_NOT_SUPPORTED = HttpStatus(505, "HTTP Version Not Supported")

        private val allStatuses = listOf(
            CONTINUE, SWITCHING_PROTOCOLS,
            OK, CREATED, ACCEPTED, NO_CONTENT, RESET_CONTENT, PARTIAL_CONTENT,
            MULTIPLE_CHOICES, MOVED_PERMANENTLY, FOUND, SEE_OTHER, NOT_MODIFIED, TEMPORARY_REDIRECT, PERMANENT_REDIRECT,
            BAD_REQUEST, UNAUTHORIZED, FORBIDDEN, NOT_FOUND, METHOD_NOT_ALLOWED, NOT_ACCEPTABLE,
            REQUEST_TIMEOUT, CONFLICT, GONE, LENGTH_REQUIRED, PAYLOAD_TOO_LARGE, URI_TOO_LONG,
            UNSUPPORTED_MEDIA_TYPE, RANGE_NOT_SATISFIABLE, EXPECTATION_FAILED, UNPROCESSABLE_ENTITY, TOO_MANY_REQUESTS,
            INTERNAL_SERVER_ERROR, NOT_IMPLEMENTED, BAD_GATEWAY, SERVICE_UNAVAILABLE, GATEWAY_TIMEOUT, HTTP_VERSION_NOT_SUPPORTED
        ).associateBy { it.code }

        fun fromCode(code: Int): HttpStatus {
            return allStatuses[code] ?: HttpStatus(code, "Unknown Status ($code)")
        }
    }
}

val HttpException.status: HttpStatus
    get() = HttpStatus.fromCode(statusCode)

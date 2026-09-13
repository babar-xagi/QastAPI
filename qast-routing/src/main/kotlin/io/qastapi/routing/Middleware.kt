package io.qastapi.routing

import io.qastapi.core.Environment
import io.qastapi.core.HttpException
import io.qastapi.core.LogFormat
import io.qastapi.http.HttpHeaders
import io.qastapi.http.HttpMethod
import io.qastapi.http.HttpStatus
import io.qastapi.http.QastResponse
import java.time.Instant

typealias Middleware = suspend (ctx: QastContext, next: suspend () -> Unit) -> Unit

object Middlewares {

    /**
     * Standard request logging middleware (delegates to [structuredLogging] with [LogFormat.PRETTY]).
     */
    fun logging(): Middleware = structuredLogging(LogFormat.PRETTY)

    /**
     * Structured request and response logging middleware supporting both pretty-printed terminal format
     * and single-line JSON format.
     */
    fun structuredLogging(format: LogFormat = LogFormat.PRETTY): Middleware = { ctx, next ->
        val existingId = ctx.header(HttpHeaders.X_REQUEST_ID)
        val reqId = if (!existingId.isNullOrBlank()) existingId else generateRequestId()
        ctx.attributes.put(REQUEST_ID_KEY, reqId)
        ctx.response.header(HttpHeaders.X_REQUEST_ID, reqId)

        val start = System.currentTimeMillis()
        try {
            next()
        } finally {
            val duration = System.currentTimeMillis() - start
            val status = ctx.response.status
            StructuredLogger.log(
                format = format,
                method = ctx.method,
                path = ctx.path,
                status = status,
                durationMs = duration,
                requestId = reqId,
                clientIp = "127.0.0.1",
                userAgent = ctx.header(HttpHeaders.USER_AGENT)
            )
        }
    }

    /**
     * Full Cross-Origin Resource Sharing (CORS) middleware.
     */
    fun cors(
        allowedOrigins: List<String> = listOf("*"),
        allowedMethods: List<HttpMethod> = HttpMethod.entries,
        allowedHeaders: List<String> = listOf("*"),
        allowCredentials: Boolean = false,
        maxAgeSeconds: Long = 86400
    ): Middleware = { ctx, next ->
        val origin = ctx.header(HttpHeaders.ORIGIN) ?: "*"
        val matchedOrigin = if (allowedOrigins.contains("*") || allowedOrigins.contains(origin)) origin else null

        if (matchedOrigin != null) {
            ctx.response.header(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, matchedOrigin)
            if (allowCredentials) {
                ctx.response.header(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true")
            }
        }

        if (ctx.method == HttpMethod.OPTIONS) {
            ctx.response.status(HttpStatus.NO_CONTENT)
            ctx.response.header(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, allowedMethods.joinToString(", ") { it.value })
            ctx.response.header(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, allowedHeaders.joinToString(", "))
            ctx.response.header(HttpHeaders.ACCESS_CONTROL_MAX_AGE, maxAgeSeconds.toString())
        } else {
            next()
        }
    }

    /**
     * Exception handling middleware. In development, provides rich error payloads or an interactive
     * HTML developer error page when accessed by web browsers. In production, sanitizes messages to prevent
     * information leakage while providing correlation request IDs.
     */
    fun errorHandling(
        exceptionHandlers: Map<Class<out Throwable>, suspend (QastContext, Throwable) -> Any?> = emptyMap(),
        formatJson: (Map<String, Any?>) -> String = { map -> formatSimpleJson(map) }
    ): Middleware = { ctx, next ->
        try {
            next()
        } catch (t: Throwable) {
            val reqId = ctx.requestId.ifEmpty {
                val newId = generateRequestId()
                ctx.attributes.put(REQUEST_ID_KEY, newId)
                newId
            }
            ctx.response.header(HttpHeaders.X_REQUEST_ID, reqId)

            val customHandler = exceptionHandlers.entries.firstOrNull { it.key.isAssignableFrom(t.javaClass) }?.value
            if (customHandler != null) {
                val res = customHandler(ctx, t)
                if (res is QastResponse) {
                    // response already set
                } else if (res != null) {
                    ctx.text(res.toString())
                }
            } else if (t is HttpException) {
                val status = HttpStatus.fromCode(t.statusCode)
                val acceptsHtml = ctx.header(HttpHeaders.ACCEPT)?.contains("text/html") == true &&
                        !Environment.current().isProd()

                if (acceptsHtml) {
                    @Suppress("UNCHECKED_CAST")
                    val suggestions = ((t.details as? Map<*, *>)?.get("suggestions") as? List<String>) ?: emptyList()
                    val html = DeveloperErrorPage.render(status, t, ctx, suggestions)
                    ctx.html(html, status)
                } else {
                    val bodyMap = mutableMapOf<String, Any?>(
                        "error" to status.reasonPhrase,
                        "status" to status.code,
                        "message" to t.message,
                        "requestId" to reqId
                    )
                    if (t.details != null) {
                        bodyMap["details"] = t.details
                    }
                    ctx.json(formatJson(bodyMap), status)
                }
            } else {
                val status = HttpStatus.INTERNAL_SERVER_ERROR
                val acceptsHtml = ctx.header(HttpHeaders.ACCEPT)?.contains("text/html") == true &&
                        !Environment.current().isProd()

                if (acceptsHtml) {
                    val html = DeveloperErrorPage.render(status, t, ctx)
                    ctx.html(html, status)
                } else if (Environment.current().isProd()) {
                    val bodyMap = mapOf(
                        "error" to "Internal Server Error",
                        "status" to 500,
                        "message" to "An unexpected error occurred",
                        "requestId" to reqId
                    )
                    ctx.json(formatJson(bodyMap), status)
                } else {
                    // Rich developer JSON error
                    val frames = t.stackTrace.take(15).map { frame ->
                        "${frame.className}.${frame.methodName}(${frame.fileName ?: "Unknown"}:${frame.lineNumber})"
                    }
                    val bodyMap = mapOf(
                        "error" to "Internal Server Error",
                        "status" to 500,
                        "message" to (t.message ?: "An unexpected error occurred"),
                        "exceptionClass" to t.javaClass.name,
                        "requestId" to reqId,
                        "timestamp" to Instant.now().toString(),
                        "path" to ctx.path,
                        "method" to ctx.method.value,
                        "stackTrace" to frames
                    )
                    ctx.json(formatJson(bodyMap), status)
                }
            }
        }
    }

    private fun formatSimpleJson(map: Map<String, Any?>): String {
        val entries = map.entries.joinToString(",") { (k, v) ->
            val vStr = when (v) {
                null -> "null"
                is Number, is Boolean -> v.toString()
                is List<*> -> "[" + v.joinToString(",") { item ->
                    if (item is Number || item is Boolean) item.toString()
                    else "\"${item.toString().replace("\"", "\\\"")}\""
                } + "]"
                is Map<*, *> -> formatSimpleJson(v.entries.associate { it.key.toString() to it.value })
                else -> "\"${v.toString().replace("\"", "\\\"")}\""
            }
            "\"$k\":$vStr"
        }
        return "{$entries}"
    }
}

package io.qastapi.routing

import io.qastapi.core.HttpException
import io.qastapi.http.HttpHeaders
import io.qastapi.http.HttpMethod
import io.qastapi.http.HttpStatus
import io.qastapi.http.QastResponse

typealias Middleware = suspend (ctx: QastContext, next: suspend () -> Unit) -> Unit

object Middlewares {

    fun logging(): Middleware = { ctx, next ->
        val start = System.currentTimeMillis()
        try {
            next()
        } finally {
            val duration = System.currentTimeMillis() - start
            val status = ctx.response.status
            println("[QastAPI] ${ctx.method.value} ${ctx.path} -> ${status.code} (${duration}ms)")
        }
    }

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

    fun errorHandling(
        exceptionHandlers: Map<Class<out Throwable>, suspend (QastContext, Throwable) -> Any?> = emptyMap(),
        formatJson: (Map<String, Any?>) -> String = { map ->
            // fallback simple JSON serializer
            val entries = map.entries.joinToString(",") { (k, v) ->
                val vStr = when (v) {
                    null -> "null"
                    is Number, is Boolean -> v.toString()
                    else -> "\"${v.toString().replace("\"", "\\\"")}\""
                }
                "\"$k\":$vStr"
            }
            "{$entries}"
        }
    ): Middleware = { ctx, next ->
        try {
            next()
        } catch (t: Throwable) {
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
                val bodyMap = mutableMapOf<String, Any?>(
                    "error" to status.reasonPhrase,
                    "status" to status.code,
                    "message" to t.message
                )
                if (t.details != null) {
                    bodyMap["details"] = t.details
                }
                ctx.json(formatJson(bodyMap), status)
            } else {
                val status = HttpStatus.INTERNAL_SERVER_ERROR
                val message = if (io.qastapi.core.Environment.current().isProd()) {
                    "An unexpected error occurred"
                } else {
                    t.message ?: "An unexpected error occurred"
                }
                val bodyMap = mapOf(
                    "error" to "Internal Server Error",
                    "status" to 500,
                    "message" to message
                )
                ctx.json(formatJson(bodyMap), status)
            }
        }
    }
}

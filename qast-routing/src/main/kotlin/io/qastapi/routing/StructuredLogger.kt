package io.qastapi.routing

import io.qastapi.core.LogFormat
import io.qastapi.http.HttpMethod
import io.qastapi.http.HttpStatus
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Structured logger for HTTP requests and responses supporting both ANSI-colorized pretty format
 * and machine-readable JSON format.
 */
object StructuredLogger {

    private val timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        .withZone(ZoneId.systemDefault())

    // ANSI Colors
    private const val RESET = "\u001B[0m"
    private const val RED = "\u001B[31m"
    private const val GREEN = "\u001B[32m"
    private const val YELLOW = "\u001B[33m"
    private const val BLUE = "\u001B[34m"
    private const val MAGENTA = "\u001B[35m"
    private const val CYAN = "\u001B[36m"
    private const val GRAY = "\u001B[90m"
    private const val BOLD = "\u001B[1m"

    fun log(
        format: LogFormat,
        method: HttpMethod,
        path: String,
        status: HttpStatus,
        durationMs: Long,
        requestId: String,
        clientIp: String = "127.0.0.1",
        userAgent: String? = null
    ) {
        val now = Instant.now()
        val line = when (format) {
            LogFormat.PRETTY -> formatPretty(now, method, path, status, durationMs, requestId, clientIp)
            LogFormat.JSON -> formatJson(now, method, path, status, durationMs, requestId, clientIp, userAgent)
        }
        println(line)
    }

    fun formatPretty(
        time: Instant,
        method: HttpMethod,
        path: String,
        status: HttpStatus,
        durationMs: Long,
        requestId: String,
        clientIp: String
    ): String {
        val timeStr = timeFormatter.format(time)
        val methodColor = when (method) {
            HttpMethod.GET -> GREEN
            HttpMethod.POST -> BLUE
            HttpMethod.PUT -> YELLOW
            HttpMethod.PATCH -> MAGENTA
            HttpMethod.DELETE -> RED
            else -> CYAN
        }
        val statusColor = when (status.code) {
            in 200..299 -> GREEN
            in 300..399 -> CYAN
            in 400..499 -> YELLOW
            else -> RED
        }
        val durationColor = when {
            durationMs < 100 -> GREEN
            durationMs < 500 -> YELLOW
            else -> RED
        }

        val idSegment = if (requestId.isNotEmpty()) "$GRAY[$requestId]$RESET " else ""
        val coloredMethod = "$methodColor$BOLD${method.value.padEnd(6)}$RESET"
        val coloredStatus = "$statusColor$BOLD${status.code} ${status.reasonPhrase}$RESET"
        val coloredDuration = "$durationColor${durationMs}ms$RESET"

        return "$GRAY$timeStr$RESET $idSegment$coloredMethod $path -> $coloredStatus ($coloredDuration) $GRAY$clientIp$RESET"
    }

    fun formatJson(
        time: Instant,
        method: HttpMethod,
        path: String,
        status: HttpStatus,
        durationMs: Long,
        requestId: String,
        clientIp: String,
        userAgent: String?
    ): String {
        val isoTime = time.toString()
        val uaEscaped = userAgent?.replace("\"", "\\\"") ?: ""
        val pathEscaped = path.replace("\"", "\\\"")

        val uaField = if (userAgent != null) ""","userAgent":"$uaEscaped"""" else ""
        val idField = if (requestId.isNotEmpty()) ""","requestId":"$requestId"""" else ""

        return """{"timestamp":"$isoTime","level":"INFO"$idField,"method":"${method.value}","path":"$pathEscaped","status":${status.code},"durationMs":$durationMs,"clientIp":"$clientIp"$uaField}"""
    }
}

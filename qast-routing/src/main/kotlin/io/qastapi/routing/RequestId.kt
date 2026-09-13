package io.qastapi.routing

import io.qastapi.core.AttributeKey
import io.qastapi.http.HttpHeaders
import java.util.UUID

/**
 * Attribute key for storing the correlation/request ID in request attributes.
 */
val REQUEST_ID_KEY = AttributeKey<String>("qast.request_id")

/**
 * Returns the correlation/request ID for the current request.
 * Prioritizes the ID generated/stored in [attributes], falls back to [HttpHeaders.X_REQUEST_ID],
 * or empty string if not set.
 */
val QastContext.requestId: String
    get() = attributes.getOrNull(REQUEST_ID_KEY)
        ?: header(HttpHeaders.X_REQUEST_ID)
        ?: ""

/**
 * Helper to generate a compact, clean request ID.
 */
fun generateRequestId(): String {
    val raw = UUID.randomUUID().toString().replace("-", "")
    return "req-" + raw.substring(0, 12)
}

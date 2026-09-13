package io.qastapi.http

enum class HttpMethod(val value: String) {
    GET("GET"),
    POST("POST"),
    PUT("PUT"),
    PATCH("PATCH"),
    DELETE("DELETE"),
    HEAD("HEAD"),
    OPTIONS("OPTIONS");

    companion object {
        fun fromString(value: String): HttpMethod {
            return entries.firstOrNull { it.value.equals(value.trim(), ignoreCase = true) }
                ?: throw IllegalArgumentException("Unsupported HTTP method: $value")
        }
    }
}

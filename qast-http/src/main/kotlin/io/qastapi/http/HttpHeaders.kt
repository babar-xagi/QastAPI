package io.qastapi.http

import java.util.TreeMap

open class HttpHeaders(
    headersMap: Map<String, List<String>> = emptyMap()
) {
    protected val map = TreeMap<String, MutableList<String>>(String.CASE_INSENSITIVE_ORDER).apply {
        headersMap.forEach { (k, v) ->
            put(k, v.toMutableList())
        }
    }

    operator fun get(name: String): String? = map[name]?.firstOrNull()

    fun getAll(name: String): List<String> = map[name] ?: emptyList()

    fun contains(name: String): Boolean = map.containsKey(name)

    fun names(): Set<String> = map.keys

    fun asMap(): Map<String, List<String>> = map.toMap()

    val contentType: String? get() = get(CONTENT_TYPE)
    val contentLength: Long? get() = get(CONTENT_LENGTH)?.toLongOrNull()
    val authorization: String? get() = get(AUTHORIZATION)
    val accept: String? get() = get(ACCEPT)
    val userAgent: String? get() = get(USER_AGENT)
    val host: String? get() = get(HOST)
    val location: String? get() = get(LOCATION)

    companion object {
        const val ACCEPT = "Accept"
        const val ACCEPT_CHARSET = "Accept-Charset"
        const val ACCEPT_ENCODING = "Accept-Encoding"
        const val ACCEPT_LANGUAGE = "Accept-Language"
        const val AUTHORIZATION = "Authorization"
        const val CACHE_CONTROL = "Cache-Control"
        const val CONTENT_DISPOSITION = "Content-Disposition"
        const val CONTENT_ENCODING = "Content-Encoding"
        const val CONTENT_LENGTH = "Content-Length"
        const val CONTENT_TYPE = "Content-Type"
        const val COOKIE = "Cookie"
        const val HOST = "Host"
        const val LOCATION = "Location"
        const val ORIGIN = "Origin"
        const val SET_COOKIE = "Set-Cookie"
        const val USER_AGENT = "User-Agent"
        const val ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin"
        const val ACCESS_CONTROL_ALLOW_METHODS = "Access-Control-Allow-Methods"
        const val ACCESS_CONTROL_ALLOW_HEADERS = "Access-Control-Allow-Headers"
        const val ACCESS_CONTROL_ALLOW_CREDENTIALS = "Access-Control-Allow-Credentials"
        const val ACCESS_CONTROL_MAX_AGE = "Access-Control-Max-Age"
    }
}

class MutableHttpHeaders(
    headersMap: Map<String, List<String>> = emptyMap()
) : HttpHeaders(headersMap) {

    fun set(name: String, value: String): MutableHttpHeaders {
        map[name] = mutableListOf(value)
        return this
    }

    fun add(name: String, value: String): MutableHttpHeaders {
        map.computeIfAbsent(name) { mutableListOf() }.add(value)
        return this
    }

    fun remove(name: String): MutableHttpHeaders {
        map.remove(name)
        return this
    }

    fun clear(): MutableHttpHeaders {
        map.clear()
        return this
    }
}

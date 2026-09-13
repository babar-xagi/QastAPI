package io.qastapi.http

data class Cookie(
    val name: String,
    val value: String
) {
    companion object {
        fun parse(header: String?): Map<String, String> = SetCookie.parse(header)
    }
}

enum class SameSite(val value: String) {
    STRICT("Strict"),
    LAX("Lax"),
    NONE("None")
}

data class SetCookie(
    val name: String,
    val value: String,
    val maxAge: Long? = null,
    val domain: String? = null,
    val path: String? = "/",
    val secure: Boolean = false,
    val httpOnly: Boolean = false,
    val sameSite: SameSite? = null
) {
    fun toHeaderValue(): String {
        val sb = StringBuilder()
        sb.append(name).append('=').append(value)
        if (maxAge != null) sb.append("; Max-Age=").append(maxAge)
        if (domain != null) sb.append("; Domain=").append(domain)
        if (path != null) sb.append("; Path=").append(path)
        if (secure) sb.append("; Secure")
        if (httpOnly) sb.append("; HttpOnly")
        if (sameSite != null) sb.append("; SameSite=").append(sameSite.value)
        return sb.toString()
    }

    companion object {
        fun parse(header: String?): Map<String, String> {
            if (header.isNullOrBlank()) return emptyMap()
            val result = mutableMapOf<String, String>()
            header.split(";").forEach { part ->
                val trimmed = part.trim()
                val eqIdx = trimmed.indexOf('=')
                if (eqIdx > 0) {
                    val k = trimmed.substring(0, eqIdx).trim()
                    val v = trimmed.substring(eqIdx + 1).trim()
                    result[k] = v
                }
            }
            return result
        }
    }
}

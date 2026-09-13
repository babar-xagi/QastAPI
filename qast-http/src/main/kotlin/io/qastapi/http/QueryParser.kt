package io.qastapi.http

import java.net.URLDecoder

object QueryParser {

    fun parse(rawQuery: String?): Map<String, List<String>> {
        if (rawQuery.isNullOrBlank()) return emptyMap()

        val cleanQuery = if (rawQuery.startsWith("?")) rawQuery.substring(1) else rawQuery
        val result = mutableMapOf<String, MutableList<String>>()

        cleanQuery.split("&").forEach { param ->
            if (param.isNotEmpty()) {
                val eqIdx = param.indexOf('=')
                val rawName = if (eqIdx >= 0) param.substring(0, eqIdx) else param
                val rawValue = if (eqIdx >= 0) param.substring(eqIdx + 1) else ""

                val name = safeDecode(rawName)
                val value = safeDecode(rawValue)

                result.computeIfAbsent(name) { mutableListOf() }.add(value)
            }
        }

        return result
    }

    private fun safeDecode(value: String): String {
        return try {
            URLDecoder.decode(value, Charsets.UTF_8)
        } catch (_: Exception) {
            value
        }
    }
}

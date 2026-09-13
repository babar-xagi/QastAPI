package io.qastapi.routing

import java.net.URLDecoder
import java.util.regex.Pattern

enum class PatternType {
    EXACT,
    PARAMETERIZED,
    WILDCARD
}

class PathPattern private constructor(
    val rawPattern: String,
    private val regex: Pattern,
    private val paramNames: List<String>,
    val isWildcard: Boolean,
    val type: PatternType,
    val specificityScore: Int
) : Comparable<PathPattern> {

    fun match(path: String): Map<String, String>? {
        val normalizedPath = normalize(path)
        val matcher = regex.matcher(normalizedPath)
        if (!matcher.matches()) {
            return null
        }

        if (paramNames.isEmpty()) {
            return emptyMap()
        }

        val params = mutableMapOf<String, String>()
        for (i in paramNames.indices) {
            val value = matcher.group(i + 1)
            if (value != null) {
                val decoded = decodePathSegment(value)
                params[paramNames[i]] = decoded
            }
        }
        return params
    }

    override fun compareTo(other: PathPattern): Int {
        // Higher score should come first in priority
        if (this.type != other.type) {
            return this.type.ordinal.compareTo(other.type.ordinal)
        }
        return other.specificityScore.compareTo(this.specificityScore)
    }

    override fun toString(): String = rawPattern

    companion object {
        private val PARAM_REGEX = Pattern.compile("\\{([a-zA-Z_][a-zA-Z0-9_]*)\\}")

        fun parse(pattern: String): PathPattern {
            val normalized = normalize(pattern)
            val paramNames = mutableListOf<String>()
            val sb = StringBuilder("^")
            var isWildcard = false

            val matcher = PARAM_REGEX.matcher(normalized)
            var lastIndex = 0
            var literalLength = 0

            while (matcher.find()) {
                val literal = normalized.substring(lastIndex, matcher.start())
                sb.append(Pattern.quote(literal))
                literalLength += literal.length

                val paramName = matcher.group(1)
                paramNames.add(paramName)
                sb.append("([^/]+)")

                lastIndex = matcher.end()
            }

            val remaining = normalized.substring(lastIndex)
            if (remaining.endsWith("/*")) {
                val prefix = remaining.substring(0, remaining.length - 2)
                sb.append(Pattern.quote(prefix))
                sb.append("/?(.*)")
                paramNames.add("*")
                isWildcard = true
                literalLength += prefix.length
            } else if (remaining.endsWith("/**")) {
                val prefix = remaining.substring(0, remaining.length - 3)
                sb.append(Pattern.quote(prefix))
                sb.append("/?(.*)")
                paramNames.add("*")
                isWildcard = true
                literalLength += prefix.length
            } else {
                sb.append(Pattern.quote(remaining))
                literalLength += remaining.length
            }

            sb.append("/?$") // allow optional trailing slash

            val type = when {
                isWildcard -> PatternType.WILDCARD
                paramNames.isNotEmpty() -> PatternType.PARAMETERIZED
                else -> PatternType.EXACT
            }

            val specificityScore = (literalLength * 10) - (paramNames.size * 5)

            return PathPattern(
                rawPattern = normalized,
                regex = Pattern.compile(sb.toString()),
                paramNames = paramNames,
                isWildcard = isWildcard,
                type = type,
                specificityScore = specificityScore
            )
        }

        fun normalize(path: String): String {
            if (path.isEmpty()) return "/"
            var p = path.trim()
            if (!p.startsWith("/")) p = "/$p"
            p = p.replace(Regex("/+"), "/")
            if (p.length > 1 && p.endsWith("/")) {
                p = p.substring(0, p.length - 1)
            }
            return p
        }

        fun decodePathSegment(value: String): String {
            // In RFC 3986 URI paths, '+' is a literal plus sign, not a space.
            // Replace '+' with '%2B' before URLDecoder to prevent '+' from turning into space.
            return try {
                val safe = value.replace("+", "%2B")
                URLDecoder.decode(safe, Charsets.UTF_8)
            } catch (_: Exception) {
                value
            }
        }
    }
}

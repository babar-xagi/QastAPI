package io.qastapi.config

/**
 * Minimal zero-dependency TOML parser designed specifically for QastAPI configuration.
 *
 * Supported TOML Subset:
 * - Table headers: `[table]` and `[table.subtable]`
 * - Key-value pairs: `key = "quoted string"`, `key = 'single-quoted'`
 * - Numerical literals: Integers, Longs, and Floating-point Doubles
 * - Booleans: `true` and `false` (case-insensitive)
 * - Single-line comments starting with `#`
 * - Environment variable interpolation:
 *   - `${VAR}` (substituted from env or system properties)
 *   - `${VAR:-default}` or `${VAR:default}` (falls back to default value if unset)
 *
 * Non-supported full TOML features (to be handled in future phases if needed):
 * - Multi-line strings ("""...""")
 * - Array of tables (`[[table]]`)
 * - Inline tables (`key = { a = 1, b = 2 }`)
 * - Date/time literals (parsed as strings)
 */
object TomlParser {

    fun parse(content: String): Map<String, Map<String, Any>> {
        val result = mutableMapOf<String, MutableMap<String, Any>>()
        var currentSection = "default"

        content.lines().forEach { rawLine ->
            val line = rawLine.trim()
            if (line.isEmpty() || line.startsWith("#")) {
                return@forEach
            }

            if (line.startsWith("[") && line.endsWith("]")) {
                currentSection = line.substring(1, line.length - 1).trim()
                result.computeIfAbsent(currentSection) { mutableMapOf() }
            } else if (line.contains("=")) {
                val eqIdx = line.indexOf('=')
                val key = line.substring(0, eqIdx).trim()
                val rawValue = line.substring(eqIdx + 1).trim()
                val resolvedValue = resolveEnvVars(rawValue)
                val parsedValue = parseValue(resolvedValue)

                result.computeIfAbsent(currentSection) { mutableMapOf() }[key] = parsedValue
            }
        }

        return result
    }

    fun resolveEnvVars(value: String): String {
        val pattern = Regex("\\$\\{([a-zA-Z_][a-zA-Z0-9_]*)(?::-(.*?)|:(.*?))?\\}")
        return pattern.replace(value) { match ->
            val varName = match.groupValues[1]
            val defaultVal = match.groupValues[2].ifEmpty { match.groupValues[3] }
            val envVal = System.getenv(varName) ?: System.getProperty(varName)
            envVal ?: defaultVal
        }
    }

    private fun parseValue(raw: String): Any {
        val trimmed = raw.trim()
        // String
        if ((trimmed.startsWith("\"") && trimmed.endsWith("\"")) ||
            (trimmed.startsWith("'") && trimmed.endsWith("'"))
        ) {
            return trimmed.substring(1, trimmed.length - 1)
        }
        // Boolean
        if (trimmed.equals("true", ignoreCase = true)) return true
        if (trimmed.equals("false", ignoreCase = true)) return false

        // Int / Long
        trimmed.toLongOrNull()?.let { return it }
        // Double
        trimmed.toDoubleOrNull()?.let { return it }

        // Unquoted string fallback
        return trimmed
    }
}

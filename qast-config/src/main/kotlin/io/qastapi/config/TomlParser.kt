package io.qastapi.config

/**
 * Minimal zero-dependency TOML parser designed specifically for QastAPI configuration.
 *
 * Supported TOML Subset:
 * - Table headers: `[table]` and `[table.subtable]`
 * - Key-value pairs: `key = "quoted string"`, `key = 'single-quoted'`
 * - Numerical literals: Integers, Longs, and Floating-point Doubles
 * - Booleans: `true` and `false` (case-insensitive)
 * - Arrays: `["item1", "item2"]`, `[1, 2, 3]`
 * - Comments: Full-line comments starting with `#` and inline comments (`key = "val" # comment`)
 * - Environment variable interpolation:
 *   - `${VAR}` (substituted from env or system properties)
 *   - `${VAR:-default}` or `${VAR:default}` (falls back to default value if unset)
 */
object TomlParser {

    fun parse(content: String): Map<String, Map<String, Any>> {
        val result = mutableMapOf<String, MutableMap<String, Any>>()
        var currentSection = "default"

        content.lines().forEach { rawLine ->
            val line = stripComments(rawLine)
            if (line.isEmpty()) {
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

    fun stripComments(line: String): String {
        var inQuotes = false
        var quoteChar = ' '
        val sb = StringBuilder()
        for (i in line.indices) {
            val c = line[i]
            if ((c == '"' || c == '\'') && (i == 0 || line[i - 1] != '\\')) {
                if (inQuotes && c == quoteChar) {
                    inQuotes = false
                } else if (!inQuotes) {
                    inQuotes = true
                    quoteChar = c
                }
            }
            if (c == '#' && !inQuotes) {
                break
            }
            sb.append(c)
        }
        return sb.toString().trim()
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

    fun parseValue(raw: String): Any {
        val trimmed = raw.trim()

        // Array: ["a", "b", "c"]
        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            val inner = trimmed.substring(1, trimmed.length - 1).trim()
            if (inner.isEmpty()) return emptyList<Any>()

            val elements = mutableListOf<String>()
            val cur = StringBuilder()
            var inQuotes = false
            var quoteChar = ' '
            for (i in inner.indices) {
                val c = inner[i]
                if ((c == '"' || c == '\'') && (i == 0 || inner[i - 1] != '\\')) {
                    if (inQuotes && c == quoteChar) {
                        inQuotes = false
                    } else if (!inQuotes) {
                        inQuotes = true
                        quoteChar = c
                    }
                }
                if (c == ',' && !inQuotes) {
                    elements.add(cur.toString().trim())
                    cur.clear()
                } else {
                    cur.append(c)
                }
            }
            if (cur.isNotEmpty()) {
                elements.add(cur.toString().trim())
            }
            return elements.filter { it.isNotEmpty() }.map { parseValue(it) }
        }

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

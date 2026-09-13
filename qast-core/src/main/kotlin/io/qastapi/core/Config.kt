package io.qastapi.core

/**
 * Supported structured log output formats.
 */
enum class LogFormat {
    PRETTY,
    JSON;

    companion object {
        fun fromString(str: String): LogFormat = when (str.trim().lowercase()) {
            "json" -> JSON
            else -> PRETTY
        }
    }
}

/**
 * Configuration options for the built-in structured logging system.
 */
data class LoggingConfig(
    val level: String = "INFO",
    val format: LogFormat = LogFormat.PRETTY,
    val includeRequestId: Boolean = true,
    val logHeaders: Boolean = false
)

data class ServerConfig(
    val host: String = "0.0.0.0",
    val port: Int = 8000,
    val backlog: Int = 1024,
    val readTimeoutMs: Long = 30_000,
    val writeTimeoutMs: Long = 30_000,
    val devMode: Boolean = true,
    val engine: String = "netty"
)

data class ProjectConfig(
    val name: String = "qast-app",
    val version: String = "0.1.0"
)

data class QastConfig(
    val project: ProjectConfig = ProjectConfig(),
    val server: ServerConfig = ServerConfig(),
    val environment: Environment = Environment.current(),
    val logging: LoggingConfig = LoggingConfig(),
    val plugins: List<String> = emptyList(),
    val rawSections: Map<String, Map<String, Any>> = emptyMap()
)

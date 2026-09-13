package io.qastapi.core

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
    val environment: Environment = Environment.current()
)

package io.qastapi

import io.qastapi.config.ConfigLoader
import io.qastapi.core.ServerConfig
import io.qastapi.http.HttpEngineFactory
import io.qastapi.routing.QastApp

// Re-export routing types and annotations for clean `import io.qastapi.*`
typealias Get = io.qastapi.routing.Get
typealias Post = io.qastapi.routing.Post
typealias Put = io.qastapi.routing.Put
typealias Patch = io.qastapi.routing.Patch
typealias Delete = io.qastapi.routing.Delete
typealias Head = io.qastapi.routing.Head
typealias Options = io.qastapi.routing.Options
typealias HttpRoute = io.qastapi.routing.HttpRoute
typealias Route = io.qastapi.routing.Route
typealias QastContext = io.qastapi.routing.QastContext

/**
 * Creates and optionally starts a QastAPI application.
 *
 * Supports both FastAPI-style and Kotlin DSL-style configuration:
 *
 * 1. FastAPI-style:
 * ```kotlin
 * val app = qastapi()
 *
 * app.get("/") {
 *     "hello world"
 * }
 *
 * fun main() {
 *     app.run()
 * }
 * ```
 *
 * 2. Kotlin DSL style:
 * ```kotlin
 * fun main() {
 *     QastAPI {
 *         get("/") {
 *             "hello world"
 *         }
 *     }
 * }
 * ```
 */
fun QastAPI(
    port: Int? = null,
    host: String? = null,
    engine: String? = null,
    autoStart: Boolean? = null,
    wait: Boolean? = null,
    block: (QastApp.() -> Unit)? = null
): QastApp {
    val baseConfig = ConfigLoader.load()
    val serverConfig = ServerConfig(
        host = host ?: baseConfig.server.host,
        port = port ?: baseConfig.server.port,
        backlog = baseConfig.server.backlog,
        devMode = baseConfig.server.devMode,
        engine = engine ?: baseConfig.server.engine
    )
    val config = baseConfig.copy(server = serverConfig)
    val httpEngine = HttpEngineFactory.create(serverConfig)

    val app = QastApp(config, httpEngine)
    block?.invoke(app)

    val shouldAutoStart = autoStart ?: (block != null)
    val shouldWait = wait ?: shouldAutoStart

    if (shouldAutoStart) {
        app.start(wait = shouldWait)
    }

    return app
}

/**
 * Lowercase alias for [QastAPI], matching Python's `fastapi()` naming style.
 */
fun qastapi(
    port: Int? = null,
    host: String? = null,
    engine: String? = null,
    autoStart: Boolean? = null,
    wait: Boolean? = null,
    block: (QastApp.() -> Unit)? = null
): QastApp = QastAPI(
    port = port,
    host = host,
    engine = engine,
    autoStart = autoStart,
    wait = wait,
    block = block
)

package io.qastapi

import io.qastapi.config.ConfigLoader
import io.qastapi.core.QastConfig
import io.qastapi.core.ServerConfig
import io.qastapi.http.HttpEngineFactory
import io.qastapi.routing.QastApp

fun QastAPI(
    port: Int? = null,
    host: String? = null,
    engine: String? = null,
    autoStart: Boolean = true,
    wait: Boolean = autoStart,
    block: QastApp.() -> Unit
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
    app.block()

    if (autoStart) {
        app.start(wait = wait)
    }

    return app
}

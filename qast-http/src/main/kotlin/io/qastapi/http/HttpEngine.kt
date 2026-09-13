package io.qastapi.http

import io.qastapi.core.ServerConfig

typealias HttpHandler = suspend (QastRequest) -> QastResponse

interface RunningServer {
    val host: String
    val port: Int
    val isRunning: Boolean
    suspend fun stop()
}

interface HttpEngine {
    val name: String
    fun start(config: ServerConfig, handler: HttpHandler): RunningServer
}

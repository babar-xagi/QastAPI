package io.qastapi.http

import io.qastapi.core.ServerConfig

object HttpEngineFactory {
    fun create(config: ServerConfig): HttpEngine {
        return when (config.engine.lowercase()) {
            "sun", "jdk" -> SunHttpEngine()
            else -> NettyHttpEngine()
        }
    }
}

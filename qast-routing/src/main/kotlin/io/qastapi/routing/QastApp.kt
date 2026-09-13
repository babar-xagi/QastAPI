package io.qastapi.routing

import io.qastapi.config.ConfigLoader
import io.qastapi.core.HttpException
import io.qastapi.core.QastApplication
import io.qastapi.core.QastConfig
import io.qastapi.core.ServerConfig
import io.qastapi.http.HttpEngine
import io.qastapi.http.HttpEngineFactory
import io.qastapi.http.HttpMethod
import io.qastapi.http.HttpStatus
import io.qastapi.http.RunningServer
import kotlinx.coroutines.runBlocking
import java.util.concurrent.CountDownLatch

class QastApp(
    config: QastConfig = ConfigLoader.load(),
    var engine: HttpEngine = HttpEngineFactory.create(config.server)
) : QastApplication(config) {

    val router = Router()
    private var runningServer: RunningServer? = null
    private val stopLatch = CountDownLatch(1)

    val port: Int get() = runningServer?.port ?: config.server.port
    val host: String get() = runningServer?.host ?: config.server.host
    val isRunning: Boolean get() = runningServer?.isRunning ?: false

    init {
        tryAutoInstallSerialization()
    }

    private fun tryAutoInstallSerialization() {
        try {
            val clazz = Class.forName("io.qastapi.serialization.SerializationExtensionsKt")
            val method = clazz.getMethod("installSerialization", Router::class.java)
            method.invoke(null, router)
        } catch (_: Throwable) {
            // Serialization module not on classpath, continue with basic serialization
        }
    }

    fun use(middleware: Middleware): QastApp {
        router.use(middleware)
        return this
    }

    inline fun <reified E : Throwable> exception(noinline handler: suspend (QastContext, E) -> Any?): QastApp {
        router.exception(handler)
        return this
    }

    fun route(method: HttpMethod, pattern: String, handler: RouteHandler): QastApp {
        router.route(method, pattern, handler)
        return this
    }

    fun get(pattern: String, handler: RouteHandler): QastApp {
        router.get(pattern, handler)
        return this
    }

    fun post(pattern: String, handler: RouteHandler): QastApp {
        router.post(pattern, handler)
        return this
    }

    fun put(pattern: String, handler: RouteHandler): QastApp {
        router.put(pattern, handler)
        return this
    }

    fun patch(pattern: String, handler: RouteHandler): QastApp {
        router.patch(pattern, handler)
        return this
    }

    fun delete(pattern: String, handler: RouteHandler): QastApp {
        router.delete(pattern, handler)
        return this
    }

    fun head(pattern: String, handler: RouteHandler): QastApp {
        router.head(pattern, handler)
        return this
    }

    fun options(pattern: String, handler: RouteHandler): QastApp {
        router.options(pattern, handler)
        return this
    }

    fun group(prefix: String, block: Router.() -> Unit): QastApp {
        router.group(prefix, block)
        return this
    }

    fun start(wait: Boolean = false): RunningServer {
        var server: RunningServer? = null

        runBlocking {
            lifecycle.start {
                server = engine.start(config.server) { request ->
                    router.handle(request)
                }
                runningServer = server
            }
        }

        val boundServer = server ?: throw IllegalStateException("Server failed to start")

        if (config.server.devMode) {
            printBanner(boundServer.port)
        }

        Runtime.getRuntime().addShutdownHook(Thread {
            stop()
        })

        if (wait) {
            try {
                stopLatch.await()
            } catch (_: InterruptedException) {
                // Thread interrupted
            }
        }

        return boundServer
    }

    fun stop() {
        val server = runningServer ?: return
        runBlocking {
            lifecycle.stop {
                server.stop()
            }
        }
        runningServer = null
        stopLatch.countDown()
    }

    private fun printBanner(boundPort: Int) {
        val totalRoutes = router.allRoutes().size
        val banner = """
            
   ____             _      _    ____ ___ 
  / __ \____ ______/ /_   / \  / __ \_ _|
 / / / / __ `/ ___/ __/  / _ \/ /_/ /| | 
/ /_/ / /_/ (__  ) /_   / ___/ ____/ | | 
\___\_\__,_/____/\__/  /_/  /_/    |___| 
 QastAPI v0.1.0 (Kotlin / JVM)

 ✓ Config loaded (${config.project.name} v${config.project.version})
 ✓ Engine: ${engine.name}
 ✓ $totalRoutes routes registered

 ➜ Local:   http://localhost:$boundPort
 ➜ Network: http://${config.server.host}:$boundPort
 
        """.trimIndent()
        println(banner)
    }
}

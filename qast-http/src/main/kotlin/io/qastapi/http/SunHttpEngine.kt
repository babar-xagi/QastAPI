package io.qastapi.http

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import io.qastapi.core.ServerConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.net.InetSocketAddress
import java.net.URI
import java.net.URLDecoder
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class SunHttpEngine : HttpEngine {
    override val name: String = "sun"

    override fun start(config: ServerConfig, handler: HttpHandler): RunningServer {
        val address = if (config.host == "0.0.0.0") {
            InetSocketAddress(config.port)
        } else {
            InetSocketAddress(config.host, config.port)
        }

        val server = HttpServer.create(address, config.backlog)
        val executor = Executors.newCachedThreadPool { runnable ->
            Thread(runnable).apply { isDaemon = true }
        }
        server.executor = executor

        server.createContext("/") { exchange ->
            handleExchange(exchange, handler)
        }

        server.start()

        val boundPort = server.address.port
        val boundHost = server.address.hostString

        return object : RunningServer {
            private val running = AtomicBoolean(true)

            override val host: String = boundHost
            override val port: Int = boundPort
            override val isRunning: Boolean get() = running.get()

            override suspend fun stop() {
                if (running.compareAndSet(true, false)) {
                    server.stop(0)
                    executor.shutdown()
                }
            }
        }
    }

    private fun handleExchange(exchange: HttpExchange, handler: HttpHandler) {
        try {
            val uri = exchange.requestURI
            val path = uri.path
            val queryParams = QueryParser.parse(uri.rawQuery)

            val method = try {
                HttpMethod.fromString(exchange.requestMethod)
            } catch (e: Exception) {
                exchange.sendResponseHeaders(405, -1)
                exchange.close()
                return
            }

            val headersMap = mutableMapOf<String, MutableList<String>>()
            exchange.requestHeaders.forEach { (k, v) ->
                headersMap[k] = v.toMutableList()
            }
            val headers = HttpHeaders(headersMap)
            val cookies = Cookie.parse(headers[HttpHeaders.COOKIE])
            val bodyBytes = exchange.requestBody.readBytes()
            val remoteAddr = exchange.remoteAddress?.address?.hostAddress

            val request = QastRequest(
                method = method,
                path = path,
                uri = uri.toString(),
                queryParams = queryParams,
                headers = headers,
                cookies = cookies,
                body = bodyBytes,
                remoteAddress = remoteAddr
            )

            val response = runBlocking(Dispatchers.IO) {
                try {
                    handler(request)
                } catch (t: Throwable) {
                    QastResponse.json(
                        """{"error":"Internal Server Error","message":"${t.message?.replace("\"", "\\\"") ?: "Unknown error"}"}""",
                        HttpStatus.INTERNAL_SERVER_ERROR
                    )
                }
            }

            response.headers.asMap().forEach { (k, list) ->
                list.forEach { v ->
                    exchange.responseHeaders.add(k, v)
                }
            }

            response.cookies.forEach { cookie ->
                exchange.responseHeaders.add(HttpHeaders.SET_COOKIE, cookie.toHeaderValue())
            }

            val bytes = response.body
            if (response.status == HttpStatus.NO_CONTENT || bytes.isEmpty()) {
                exchange.sendResponseHeaders(response.status.code, -1)
            } else {
                exchange.sendResponseHeaders(response.status.code, bytes.size.toLong())
                exchange.responseBody.write(bytes)
            }
            exchange.responseBody.flush()
        } finally {
            exchange.close()
        }
    }
}

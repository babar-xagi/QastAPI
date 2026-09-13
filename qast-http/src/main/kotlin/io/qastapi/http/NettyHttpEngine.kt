package io.qastapi.http

import io.netty.bootstrap.ServerBootstrap
import io.netty.buffer.Unpooled
import io.netty.channel.Channel
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.EventLoopGroup
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.http.DefaultFullHttpResponse
import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpHeaderValues
import io.netty.handler.codec.http.HttpObjectAggregator
import io.netty.handler.codec.http.HttpResponseStatus
import io.netty.handler.codec.http.HttpServerCodec
import io.netty.handler.codec.http.HttpUtil
import io.netty.handler.codec.http.HttpVersion
import io.netty.handler.codec.http.QueryStringDecoder
import io.qastapi.core.ServerConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicBoolean

class NettyHttpEngine : HttpEngine {
    override val name: String = "netty"

    override fun start(config: ServerConfig, handler: HttpHandler): RunningServer {
        val bossGroup: EventLoopGroup = NioEventLoopGroup(1)
        val workerGroup: EventLoopGroup = NioEventLoopGroup()
        val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

        val bootstrap = ServerBootstrap()
            .group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel::class.java)
            .option(ChannelOption.SO_BACKLOG, config.backlog)
            .childOption(ChannelOption.SO_KEEPALIVE, true)
            .childOption(ChannelOption.TCP_NODELAY, true)
            .childHandler(object : ChannelInitializer<SocketChannel>() {
                override fun initChannel(ch: SocketChannel) {
                    val p = ch.pipeline()
                    p.addLast("codec", HttpServerCodec())
                    p.addLast("aggregator", HttpObjectAggregator(10 * 1024 * 1024)) // 10MB max body
                    p.addLast("handler", NettyServerHandler(handler, scope))
                }
            })

        val channelFuture = if (config.host == "0.0.0.0") {
            bootstrap.bind(config.port).sync()
        } else {
            bootstrap.bind(config.host, config.port).sync()
        }

        val channel = channelFuture.channel()
        val boundAddress = channel.localAddress() as InetSocketAddress

        return object : RunningServer {
            private val running = AtomicBoolean(true)

            override val host: String = boundAddress.hostString
            override val port: Int = boundAddress.port
            override val isRunning: Boolean get() = running.get() && channel.isActive

            override suspend fun stop() {
                if (running.compareAndSet(true, false)) {
                    scope.cancel()
                    channel.close().sync()
                    bossGroup.shutdownGracefully().sync()
                    workerGroup.shutdownGracefully().sync()
                }
            }
        }
    }

    private class NettyServerHandler(
        private val handler: HttpHandler,
        private val scope: CoroutineScope
    ) : SimpleChannelInboundHandler<FullHttpRequest>() {

        override fun channelRead0(ctx: ChannelHandlerContext, msg: FullHttpRequest) {
            val decoder = QueryStringDecoder(msg.uri())
            val path = decoder.path()
            val queryParams = decoder.parameters()

            val method = try {
                HttpMethod.fromString(msg.method().name())
            } catch (e: Exception) {
                val errorResp = DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1,
                    HttpResponseStatus.METHOD_NOT_ALLOWED,
                    Unpooled.wrappedBuffer("Method Not Allowed".toByteArray())
                )
                ctx.writeAndFlush(errorResp).addListener(ChannelFutureListener.CLOSE)
                return
            }

            // Headers
            val headersMap = mutableMapOf<String, MutableList<String>>()
            msg.headers().forEach { entry ->
                headersMap.computeIfAbsent(entry.key) { mutableListOf() }.add(entry.value)
            }
            val headers = HttpHeaders(headersMap)

            // Cookies
            val cookies = Cookie.parse(headers[HttpHeaders.COOKIE])

            // Body
            val contentBuf = msg.content()
            val bodyBytes = ByteArray(contentBuf.readableBytes())
            contentBuf.getBytes(contentBuf.readerIndex(), bodyBytes)

            val remoteAddr = (ctx.channel().remoteAddress() as? InetSocketAddress)?.address?.hostAddress

            val request = QastRequest(
                method = method,
                path = path,
                uri = msg.uri(),
                queryParams = queryParams,
                headers = headers,
                cookies = cookies,
                body = bodyBytes,
                remoteAddress = remoteAddr
            )

            val keepAlive = HttpUtil.isKeepAlive(msg)

            scope.launch {
                val response = try {
                    handler(request)
                } catch (t: Throwable) {
                    QastResponse.json(
                        """{"error":"Internal Server Error","message":"${t.message?.replace("\"", "\\\"") ?: "Unknown error"}"}""",
                        HttpStatus.INTERNAL_SERVER_ERROR
                    )
                }

                val nettyStatus = HttpResponseStatus.valueOf(response.status.code, response.status.reasonPhrase)
                val nettyResponse = DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1,
                    nettyStatus,
                    Unpooled.wrappedBuffer(response.body)
                )

                response.headers.asMap().forEach { (k, list) ->
                    list.forEach { v ->
                        nettyResponse.headers().add(k, v)
                    }
                }

                response.cookies.forEach { cookie ->
                    nettyResponse.headers().add(HttpHeaderNames.SET_COOKIE, cookie.toHeaderValue())
                }

                val hasNoBody = response.status.code == 204 || response.status.code == 304 || response.status.code in 100..199
                if (!hasNoBody) {
                    nettyResponse.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.body.size)
                }

                if (keepAlive) {
                    nettyResponse.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE)
                    ctx.writeAndFlush(nettyResponse)
                } else {
                    nettyResponse.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE)
                    ctx.writeAndFlush(nettyResponse).addListener(ChannelFutureListener.CLOSE)
                }
            }
        }

        @Deprecated("Deprecated in Java")
        override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
            ctx.close()
        }
    }
}

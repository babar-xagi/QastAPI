package io.qastapi.routing

import io.qastapi.core.NotFoundException
import io.qastapi.http.HttpHeaders
import io.qastapi.http.HttpMethod
import io.qastapi.http.HttpStatus
import io.qastapi.http.QastRequest
import io.qastapi.http.QastResponse
import java.util.concurrent.CopyOnWriteArrayList

sealed interface RouteMatchResult {
    data class Match(val route: Route, val pathParams: Map<String, String>) : RouteMatchResult
    data class MethodNotAllowed(val allowedMethods: Set<HttpMethod>) : RouteMatchResult
    data object NotFound : RouteMatchResult
}

class Router {
    private val routes = CopyOnWriteArrayList<Route>()
    private val middlewares = CopyOnWriteArrayList<Middleware>()
    @PublishedApi internal val exceptionHandlers = mutableMapOf<Class<out Throwable>, suspend (QastContext, Throwable) -> Any?>()
    var responseSerializer: (suspend (Any?, QastContext) -> Unit)? = null

    init {
        // Default error handling middleware
        middlewares.add(Middlewares.errorHandling(exceptionHandlers))
    }

    fun use(middleware: Middleware): Router {
        // Insert user middlewares before the final handler
        middlewares.add(middleware)
        return this
    }

    inline fun <reified E : Throwable> exception(noinline handler: suspend (QastContext, E) -> Any?): Router {
        @Suppress("UNCHECKED_CAST")
        exceptionHandlers[E::class.java] = handler as suspend (QastContext, Throwable) -> Any?
        return this
    }

    fun route(method: HttpMethod, pattern: String, handler: RouteHandler): Router {
        val parsedPattern = PathPattern.parse(pattern)
        routes.add(Route(method, parsedPattern, handler))
        return this
    }

    fun get(pattern: String, handler: RouteHandler): Router = route(HttpMethod.GET, pattern, handler)
    fun post(pattern: String, handler: RouteHandler): Router = route(HttpMethod.POST, pattern, handler)
    fun put(pattern: String, handler: RouteHandler): Router = route(HttpMethod.PUT, pattern, handler)
    fun patch(pattern: String, handler: RouteHandler): Router = route(HttpMethod.PATCH, pattern, handler)
    fun delete(pattern: String, handler: RouteHandler): Router = route(HttpMethod.DELETE, pattern, handler)
    fun head(pattern: String, handler: RouteHandler): Router = route(HttpMethod.HEAD, pattern, handler)
    fun options(pattern: String, handler: RouteHandler): Router = route(HttpMethod.OPTIONS, pattern, handler)

    fun group(prefix: String, block: Router.() -> Unit): Router {
        val subRouter = Router()
        subRouter.block()
        val normalizedPrefix = PathPattern.normalize(prefix)
        for (r in subRouter.allRoutes()) {
            val combinedPattern = if (r.pattern.rawPattern == "/") {
                normalizedPrefix
            } else {
                PathPattern.normalize("$normalizedPrefix/${r.pattern.rawPattern}")
            }
            route(r.method, combinedPattern, r.handler)
        }
        return this
    }

    fun allRoutes(): List<Route> = routes.toList()

    fun match(method: HttpMethod, path: String): RouteMatchResult {
        val matchedMethodsForPath = mutableSetOf<HttpMethod>()
        val exactMatches = mutableListOf<Pair<Route, Map<String, String>>>()
        val headFallbackMatches = mutableListOf<Pair<Route, Map<String, String>>>()

        for (route in routes) {
            val params = route.pattern.match(path)
            if (params != null) {
                matchedMethodsForPath.add(route.method)
                if (route.method == method) {
                    exactMatches.add(route to params)
                } else if (method == HttpMethod.HEAD && route.method == HttpMethod.GET) {
                    headFallbackMatches.add(route to params)
                }
            }
        }

        val bestMatch = exactMatches.minByOrNull { it.first.pattern }
            ?: headFallbackMatches.minByOrNull { it.first.pattern }

        return when {
            bestMatch != null -> RouteMatchResult.Match(bestMatch.first, bestMatch.second)
            matchedMethodsForPath.isNotEmpty() -> {
                if (matchedMethodsForPath.contains(HttpMethod.GET)) {
                    matchedMethodsForPath.add(HttpMethod.HEAD)
                }
                RouteMatchResult.MethodNotAllowed(matchedMethodsForPath)
            }
            else -> RouteMatchResult.NotFound
        }
    }

    suspend fun handle(request: QastRequest): QastResponse {
        val matchResult = match(request.method, request.path)

        return when (matchResult) {
            is RouteMatchResult.NotFound -> {
                val ctx = QastContext(request)
                executePipeline(ctx) {
                    throw NotFoundException("Route '${request.method.value} ${request.path}' not found")
                }
                ctx.response
            }
            is RouteMatchResult.MethodNotAllowed -> {
                val ctx = QastContext(request)
                executePipeline(ctx) {
                    ctx.response.status(HttpStatus.METHOD_NOT_ALLOWED)
                    ctx.response.header(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, matchResult.allowedMethods.joinToString(", ") { it.value })
                    ctx.json(
                        """{"error":"Method Not Allowed","message":"Allowed methods: ${matchResult.allowedMethods.joinToString(", ") { it.value }}"}""",
                        HttpStatus.METHOD_NOT_ALLOWED
                    )
                }
                ctx.response
            }
            is RouteMatchResult.Match -> {
                val enrichedRequest = request.copyWith(pathParams = matchResult.pathParams)
                val ctx = QastContext(enrichedRequest)

                executePipeline(ctx) {
                    val result = matchResult.route.handler(ctx)
                    processHandlerResult(result, ctx)
                }

                // If request was HEAD, clear body bytes but retain all headers
                if (request.method == HttpMethod.HEAD) {
                    ctx.response.body = ByteArray(0)
                }

                // For 204 No Content, body must be empty and Content-Length removed
                if (ctx.response.status == HttpStatus.NO_CONTENT || ctx.response.status.code == 204) {
                    ctx.response.body = ByteArray(0)
                    ctx.response.headers.remove(HttpHeaders.CONTENT_LENGTH)
                    ctx.response.headers.remove(HttpHeaders.CONTENT_TYPE)
                }

                ctx.response
            }
        }
    }

    private suspend fun executePipeline(ctx: QastContext, target: suspend () -> Unit) {
        var index = 0
        val allMiddlewares = middlewares.toList()

        suspend fun next() {
            if (index < allMiddlewares.size) {
                val middleware = allMiddlewares[index++]
                middleware(ctx) { next() }
            } else {
                target()
            }
        }

        next()
    }

    private suspend fun processHandlerResult(result: Any?, ctx: QastContext) {
        when {
            result is QastResponse -> {
                // handler directly returned a QastResponse, merge it into ctx.response
                ctx.response.status = result.status
                result.headers.asMap().forEach { (k, v) ->
                    v.forEach { ctx.response.header(k, it) }
                }
                result.cookies.forEach { ctx.response.cookie(it) }
                ctx.response.body = result.body
            }
            result is String -> {
                if (ctx.response.headers.contentType == null) {
                    val isHtml = result.trimStart().startsWith("<html", ignoreCase = true) ||
                            result.trimStart().startsWith("<!DOCTYPE", ignoreCase = true)
                    if (isHtml) {
                        ctx.html(result, ctx.response.status)
                    } else {
                        ctx.text(result, ctx.response.status)
                    }
                } else {
                    ctx.response.body(result)
                }
            }
            result is ByteArray -> {
                if (ctx.response.headers.contentType == null) {
                    ctx.response.contentType("application/octet-stream")
                }
                ctx.response.body(result)
            }
            result != null -> {
                val serializer = responseSerializer
                if (serializer != null) {
                    serializer(result, ctx)
                } else {
                    // Fallback to simple representation if serializer not installed yet
                    ctx.json(result.toString(), ctx.response.status)
                }
            }
        }
    }
}

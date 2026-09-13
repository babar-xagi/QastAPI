package io.qastapi.routing

import io.qastapi.http.HttpMethod

typealias RouteHandler = suspend QastContext.() -> Any?

class Route(
    val method: HttpMethod,
    val pattern: PathPattern,
    val handler: RouteHandler
) {
    override fun toString(): String = "${method.value} ${pattern.rawPattern}"
}

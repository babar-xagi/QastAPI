package io.qastapi.serialization

import io.qastapi.core.BadRequestException
import io.qastapi.core.QastApplication
import io.qastapi.core.QastPlugin
import io.qastapi.http.HttpStatus
import io.qastapi.routing.QastContext
import io.qastapi.routing.Router
import kotlinx.serialization.SerializationException

inline fun <reified T> QastContext.body(): T {
    val text = bodyText()
    if (text.isBlank()) {
        throw BadRequestException("Request body cannot be blank")
    }
    return try {
        QastJson.decodeFromString<T>(text)
    } catch (e: SerializationException) {
        throw BadRequestException("Invalid JSON payload: ${e.message}", cause = e)
    }
}

inline fun <reified T> QastContext.json(): T = body<T>()

fun installSerialization(router: Router) {
    router.responseSerializer = { result, ctx ->
        val jsonString = QastJson.serialize(result)
        ctx.json(jsonString, ctx.response.status)
    }
}

class SerializationPlugin : QastPlugin {
    override val name: String = "qast-serialization"

    override fun install(app: QastApplication) {
        // Will be wired when Router is attached to application
    }
}

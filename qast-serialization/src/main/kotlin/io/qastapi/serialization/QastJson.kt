package io.qastapi.serialization

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.serializer
import kotlin.reflect.KClass

object QastJson {

    val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = false
        isLenient = true
        coerceInputValues = true
    }

    @OptIn(InternalSerializationApi::class, ExperimentalSerializationApi::class)
    fun serialize(value: Any?): String {
        if (value == null) return "null"

        // Check for direct JSON element
        if (value is JsonElement) {
            return json.encodeToString(JsonElement.serializer(), value)
        }

        // Fast path for maps
        if (value is Map<*, *>) {
            return encodeMapToJson(value)
        }

        // Fast path for collections/arrays
        if (value is Collection<*>) {
            return encodeCollectionToJson(value)
        }

        if (value is Array<*>) {
            return encodeCollectionToJson(value.toList())
        }

        // Fast path for primitives
        if (value is Number || value is Boolean) {
            return value.toString()
        }

        if (value is String) {
            return "\"${escapeJson(value)}\""
        }

        // Try kotlinx.serialization for @Serializable classes
        return try {
            @Suppress("UNCHECKED_CAST")
            val serializer = value::class.serializer() as KSerializer<Any>
            json.encodeToString(serializer, value)
        } catch (e: Exception) {
            // Fallback for custom objects: map toString
            "\"${escapeJson(value.toString())}\""
        }
    }

    inline fun <reified T> decodeFromString(string: String): T {
        return json.decodeFromString(string)
    }

    @OptIn(InternalSerializationApi::class)
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> decodeFromString(string: String, kClass: KClass<T>): T {
        val serializer = kClass.serializer() as KSerializer<T>
        return json.decodeFromString(serializer, string)
    }

    private fun encodeMapToJson(map: Map<*, *>): String {
        val sb = StringBuilder("{")
        var first = true
        for ((k, v) in map) {
            if (!first) sb.append(",")
            first = false
            sb.append("\"").append(escapeJson(k.toString())).append("\":")
            sb.append(serialize(v))
        }
        sb.append("}")
        return sb.toString()
    }

    private fun encodeCollectionToJson(collection: Collection<*>): String {
        val sb = StringBuilder("[")
        var first = true
        for (item in collection) {
            if (!first) sb.append(",")
            first = false
            sb.append(serialize(item))
        }
        sb.append("]")
        return sb.toString()
    }

    private fun escapeJson(s: String): String {
        val sb = StringBuilder()
        for (c in s) {
            when (c) {
                '\"' -> sb.append("\\\"")
                '\\' -> sb.append("\\\\")
                '\b' -> sb.append("\\b")
                '\u000C' -> sb.append("\\f")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                else -> {
                    if (c.code in 0x00..0x1F) {
                        sb.append(String.format("\\u%04x", c.code))
                    } else {
                        sb.append(c)
                    }
                }
            }
        }
        return sb.toString()
    }
}

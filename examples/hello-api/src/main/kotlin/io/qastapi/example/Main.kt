package io.qastapi.example

import io.qastapi.QastAPI
import io.qastapi.core.NotFoundException
import io.qastapi.http.HttpStatus
import io.qastapi.http.SetCookie
import io.qastapi.serialization.body
import kotlinx.serialization.Serializable

@Serializable
data class User(val id: Long, val name: String, val email: String)

@Serializable
data class CreateUserDto(val name: String, val email: String)

fun main() {
    QastAPI {
    // 1. Simple greeting (JSON)
    get("/") {
        mapOf(
            "message" to "Hello QastAPI!",
            "framework" to "QastAPI",
            "version" to "0.1.0",
            "status" to "running"
        )
    }

    // 2. Path parameter extraction
    get("/users/{id}") {
        val id = path("id").toLongOrNull()
            ?: throw NotFoundException("Invalid user ID")
        User(id = id, name = "User $id", email = "user$id@example.com")
    }

    // 3. Query parameter parsing
    get("/search") {
        val query = query("q") ?: ""
        val page = query("page")?.toIntOrNull() ?: 1
        mapOf("query" to query, "page" to page, "results" to emptyList<String>())
    }

    // 4. POST with typed JSON body
    post("/users") {
        val dto = body<CreateUserDto>()
        status(HttpStatus.CREATED)
        User(id = 42L, name = dto.name, email = dto.email)
    }

    // 5. DELETE with 204 No Content
    delete("/users/{id}") {
        val id = path("id")
        println("Deleted user $id")
        status(HttpStatus.NO_CONTENT)
        ""
    }

    // 6. Headers & Cookies demo
    get("/cookie-demo") {
        cookie(SetCookie(name = "qast_session", value = "secret-token-12345"))
        header("X-Framework", "QastAPI")
        mapOf("cookie" to "set", "status" to "ok")
    }
}
}

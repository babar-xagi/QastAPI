# QastAPI

> **QastAPI** is a Kotlin-first, AI-native, modular web and application framework designed to combine the simplicity of FastAPI, the productivity of Django, the type-safety and coroutine model of Kotlin, modern enterprise architecture, and one-command deployment.

---

## ⚡ Quick Start

A minimal QastAPI server takes just a few lines:

```kotlin
package com.example

import io.qastapi.QastAPI

fun main() {
    QastAPI {
        get("/") {
            mapOf("message" to "Hello QastAPI!", "status" to "healthy")
        }

        get("/users/{id}") {
            val id = path("id")
            mapOf("id" to id, "name" to "User $id")
        }
    }
}
```

Start the application:

```bash
./gradlew run
# or using the Qast CLI
./qast dev
```

Visit [http://localhost:8000](http://localhost:8000) in your browser or make a request:

```bash
curl http://localhost:8000/
# {"message":"Hello QastAPI!","status":"healthy"}
```

---

## 🚀 Key Features (Phase 1: Core HTTP Framework)

- **Coroutine-First & Non-Blocking**: Built from the ground up for asynchronous Kotlin Coroutines.
- **Production-Grade Engine**: High-performance Netty HTTP engine with pluggable engine architecture (including JDK SunHttpEngine fallback).
- **FastAPI Simplicity**: Elegant DSL for `get`, `post`, `put`, `patch`, `delete`, `options`, `head`.
- **Flexible Routing**:
  - Exact paths (`/items`)
  - Path parameters (`/users/{id}`)
  - Wildcards (`/files/*`)
  - Route groups (`group("/api/v1") { ... }`)
- **Automatic Content Negotiation & JSON Serialization**:
  - Returns `Map`, `List`, or `@Serializable` Kotlin classes directly as `application/json; charset=utf-8`.
  - Type-safe request body parsing: `val dto = body<CreateUserDto>()`.
- **Middleware Pipeline**:
  - Request logging (`Middlewares.logging()`)
  - Full CORS management (`Middlewares.cors()`)
  - Exception mapping to clean JSON error responses (`Middlewares.errorHandling()`)
- **HTTP Abstractions**:
  - Type-safe `HttpStatus` with helper methods (`isSuccess`, `isClientError`, `isServerError`).
  - Case-insensitive `HttpHeaders`.
  - `Cookie` and `SetCookie` parsing and headers.
- **Testing Framework**:
  - `qastTest` in-memory test runner with fluent assertions (`expectStatus`, `expectBody`, `expectJson`).
- **Developer CLI (`qast`)**:
  - `qast init [name]` — Scaffold a new project.
  - `qast dev` — Run the development server with live banner.
  - `qast routes` — Discover and display all registered routes in an ASCII table.
  - `qast doctor` — Diagnose environment, Java runtime, and port status.

---

## 🛠 Project Structure

QastAPI is organized as a clean modular monorepo:

```text
qastapi/
├── qast-core/             # Application lifecycle, configuration, exceptions, plugins
├── qast-http/             # HTTP abstractions, status codes, Netty & Sun HTTP engines
├── qast-routing/          # Routing DSL, path matchers, middleware pipeline
├── qast-serialization/    # kotlinx.serialization JSON integration
├── qast-config/           # TOML parser and environment variable interpolation
├── qast-testing/          # Testing harness and fluent assertions (qastTest)
├── qast-cli/              # Command-line interface tool
└── examples/
    └── hello-api/         # Runnable Phase 1 sample application
```

---

## 🖥 Using the CLI

Run the CLI using `./qast` (Unix) or `.\qast.bat` (Windows):

### System Health Check (`qast doctor`)
```bash
./qast doctor
```

Output:
```text
QastAPI Doctor — System Diagnostics

 [✓] Java: version 21.0.12.1 (Eclipse Adoptium) - Compatible (17+ required)
 [✓] OS: Windows 10 (amd64)
 [✓] Default Port (8000): Available
 [✓] Project Config: Found qast.toml in current directory

Doctor check completed.
```

### Inspect Routes (`qast routes`)
```bash
./qast routes
```

Output:
```text
Registered Routes (discovered in Main.kt):
+--------+------------------------------------+
| METHOD | PATH                               |
+--------+------------------------------------+
| GET    | /                                  |
| GET    | /users/{id}                        |
| GET    | /search                            |
| POST   | /users                             |
| DELETE | /users/{id}                        |
| GET    | /cookie-demo                       |
+--------+------------------------------------+
Total routes: 6
```

### Initialize a New Project (`qast init`)
```bash
./qast init my-service
cd my-service
```

---

## 🧪 Testing

Run the full automated test suite:

```bash
./gradlew test
```

### Writing Tests with `qastTest`

```kotlin
import io.qastapi.QastAPI
import io.qastapi.testing.qastTest
import kotlin.test.Test

class ApiTest {
    @Test
    fun testEndpoints() {
        val app = QastAPI(autoStart = false) {
            get("/ping") { "pong" }
        }

        qastTest(app) {
            client.get("/ping")
                .expectStatus(200)
                .expectBody("pong")
        }
    }
}
```

---

## 📖 Documentation & Roadmap

- [VISION.md](VISION.md) — Framework philosophy and core vision.
- [ARCHITECTURE.md](ARCHITECTURE.md) — Architecture, module layout, and engine internals.
- [ROADMAP.md](ROADMAP.md) — The 15-phase roadmap from core HTTP to AI agents and enterprise deployment.
- [CONTRIBUTING.md](CONTRIBUTING.md) — Guidelines for contributing to QastAPI.

---

## 📜 License

Licensed under the Apache License, Version 2.0. See [LICENSE](LICENSE) for details.

# QastAPI

> **QastAPI** is a Kotlin-first, AI-native, modular web and application framework designed to combine the simplicity of FastAPI, the productivity of Django, the type-safety and coroutine model of Kotlin, modern enterprise architecture, and one-command deployment.

---

## ⚡ Quick Start

A minimal QastAPI server takes just a few lines:

```kotlin
package com.example

import io.qastapi.*

val app = qastapi()

fun main() {
    app.get("/") {
        mapOf("message" to "Hello QastAPI!", "status" to "healthy")
    }

    app.get("/users/{id}") {
        val id = path("id")
        mapOf("id" to id, "name" to "User $id")
    }

    app.run()
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

## 🚀 Key Features (Phase 1 & Phase 2)

- **Coroutine-First & Non-Blocking**: Built from the ground up for asynchronous Kotlin Coroutines.
- **Production-Grade Engines**: High-performance Netty HTTP engine with pluggable engine architecture (including JDK SunHttpEngine fallback).
- **FastAPI Simplicity**: Elegant DSL for `get`, `post`, `put`, `patch`, `delete`, `options`, `head`.
- **Flexible Routing & Route Priority**:
  - Deterministic route specificity (`EXACT` > `PARAMETERIZED` > `WILDCARD`)
  - Named path parameters (`/users/{id}`) and wildcards (`/files/*`)
  - Route groups (`group("/api/v1") { ... }`)
- **Developer Experience (DX)**:
  - **Hot Reload**: Automatic file watching (`DevWatcher`) and fast restarts in `qast dev`.
  - **Interactive Developer Error Page**: Dark-mode HTML error page rendered for browser requests in development.
  - **Smart 404 Route Suggestions**: Provides "Did you mean?" suggestions on route typos.
  - **Structured Logging**: Console pretty colorized logging or machine-readable JSON format.
  - **Request Correlation**: End-to-end `X-Request-ID` generation, propagation, and handler access (`ctx.requestId`).
  - **Cascading Configuration Profiles**: Multi-environment profile overrides (`qast.toml`, `qast.dev.toml`, `qast.prod.toml`).
  - **Modular App Scaffolding**: `qast app <name>` generates Django-style app modules.
  - **Plugin Management**: `qast add <feature>` and `qast remove <feature>`.
- **Automatic Content Negotiation & JSON Serialization**:
  - Returns `Map`, `List`, or `@Serializable` Kotlin classes directly as `application/json; charset=utf-8`.
  - Type-safe request body parsing: `val dto = body<CreateUserDto>()` with 400 Bad Request on malformed inputs.
- **Middleware Pipeline**:
  - Structured request logging (`Middlewares.structuredLogging()`)
  - Full CORS management (`Middlewares.cors()`)
  - Production-safe exception mapping (`Middlewares.errorHandling()`)
- **HTTP Abstractions**:
  - Type-safe `HttpStatus` with RFC-compliant 204/304 header rules.
  - Case-insensitive `HttpHeaders`.
  - `Cookie` and `SetCookie` parsing and headers.
- **Testing Framework**:
  - `qastTest` in-memory test runner with fluent assertions (`expectStatus`, `expectBody`, `expectHeader`, `expectBodyContains`).
- **Developer CLI (`qast`)**:
  - `qast init [name] [--template minimal|modular]` — Scaffold a new project.
  - `qast app [name]` — Scaffold a new application submodule.
  - `qast add [feature]` / `qast remove [feature]` — Manage plugins and features.
  - `qast dev [--profile <name>]` — Run development server with hot reload.
  - `qast routes` — Discover and display all registered routes in an ASCII table.
  - `qast doctor` — Comprehensive diagnostic check of JVM, memory, ports, and configuration.

---

## 🛠 Project Structure

QastAPI is organized as a clean modular monorepo:

```text
qastapi/
├── qast-core/             # Application lifecycle, configuration, profiles, exceptions, plugins
├── qast-http/             # HTTP abstractions, status codes, Netty & Sun HTTP engines
├── qast-routing/          # Routing DSL, path matchers, structured logging, dev error page, middleware
├── qast-serialization/    # kotlinx.serialization JSON integration
├── qast-config/           # TOML parser, environment interpolation, profile cascading
├── qast-testing/          # Testing harness and fluent assertions (qastTest)
├── qast-cli/              # Command-line interface tool (init, dev, app, add, remove, doctor, routes)
└── examples/
    └── hello-api/         # Runnable sample application
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
QastAPI Doctor — System & Project Diagnostics

 [✓] Java: version 21.0.12.1 (Eclipse Adoptium) - Compatible (17+ required)
     Location: C:\Program Files\Eclipse Adoptium\jdk-21.0.12.1-hotspot
 [✓] System: Windows 10 (amd64), 16 CPU cores available
 [✓] JVM Memory: Max Heap 4048MB (Allocated: 256MB)
 [✓] Configuration: Valid qast.toml found (Project: my-app v0.1.0)
     Found dev profile: qast.dev.toml
     Found prod profile: qast.prod.toml
 [✓] Server Port (8000): Available
 [✓] Build Tool: Found Gradle wrapper (gradlew.bat)
 [✓] Source Entry: Found src/main/kotlin/Main.kt

Diagnostic Summary: 7 Passed | 0 Warnings | 0 Errors
Doctor check completed.
```

### Start Development Server with Hot Reload (`qast dev`)
```bash
./qast dev --profile development
```

### Scaffold a New Application Submodule (`qast app <name>`)
```bash
./qast app users
# Creates: users/module.kt, users/routes.kt, users/models.kt
```

### Add Features and Plugins (`qast add <feature>`)
```bash
./qast add cors
./qast add orm
./qast add auth
```

### Inspect Routes (`qast routes`)
```bash
./qast routes
```

Output:
```text
METHOD    PATH
GET       /
GET       /users/{id}
GET       /search
POST      /users
DELETE    /users/{id}
GET       /cookie-demo
```

### Initialize a New Project (`qast init`)
```bash
./qast init my-service --template minimal
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

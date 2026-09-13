# QastAPI Architecture

## 1. High-Level Architecture

```text
                         QastAPI
                            │
        ┌───────────────────┼───────────────────┐
        │                   │                   │
     Web/API           Enterprise            AI
        │                   │                   │
    Routing              Identity            Models
    Validation           Policies            Agents
    OpenAPI              Tenancy             RAG
    WebSockets           Audit               Memory
    Serialization        Events              Tools
    Coroutines           Workflows           Python
                         Jobs                Native AI
                         Cache               Human Approval
                         Observability
                             │
                        Qast Runtime
                             │
          ┌──────────────────┼──────────────────┐
          │                  │                  │
         JVM             Containers           Cloud
```

---

## 2. Module Boundaries (Phase 1 Implemented)

### `qast-core`
- **Application Lifecycle**: State transitions (`UNINITIALIZED` -> `INITIALIZING` -> `RUNNING` -> `STOPPING` -> `STOPPED`), with asynchronous lifecycle hooks (`onStart`, `onStop`).
- **Configuration Models**: `ServerConfig`, `ProjectConfig`, `QastConfig`.
- **Exceptions**: `QastException`, `HttpException` hierarchy (`BadRequestException`, `NotFoundException`, `UnauthorizedException`, etc.).
- **Plugin System**: `QastPlugin`, `PluginRegistry`.
- **Attribute Storage**: Type-safe attribute map (`AttributeKey<T>`).

### `qast-http`
- **HTTP Abstractions**: `HttpMethod`, `HttpStatus`, `HttpHeaders`, `Cookie`, `SetCookie`.
- **Request / Response**: `QastRequest`, `QastResponse`.
- **Engine Interface**: `HttpEngine` and `RunningServer`.
- **Engine Implementations**:
  - `NettyHttpEngine`: Production-grade non-blocking engine using Netty 4.1, `NioEventLoopGroup`, and coroutine scope dispatching (`Dispatchers.Default`).
  - `SunHttpEngine`: Zero-dependency JDK `com.sun.net.httpserver.HttpServer` adapter.

### `qast-routing`
- **Pattern Matching**: `PathPattern` compiles regexes, extracts named parameters (`/users/{id}`) and wildcards (`/files/*`).
- **Router**: Tree-like structure supporting `get`, `post`, `put`, `patch`, `delete`, `options`, `head`.
- **Route Groups**: Prefix inheritance via `group("/prefix") { ... }`.
- **Middleware Pipeline**: Onion-model async middlewares (`Middlewares.logging()`, `Middlewares.cors()`, `Middlewares.errorHandling()`).
- **Context**: `QastContext` providing concise helpers (`path("id")`, `query("q")`, `bodyText()`, `status(201)`).

### `qast-serialization`
- **Kotlinx Serialization**: Integrated `Json` configured for lenient, unknown-key-ignoring parsing.
- **Auto Serialization**: Maps, Lists, Collections, and `@Serializable` classes returned from handlers are automatically encoded to JSON with `application/json; charset=utf-8`.
- **Body Binding**: `ctx.body<T>()` and `ctx.json<T>()` automatically deserializes request payloads.

### `qast-config`
- **TOML Parser**: Zero-dependency parser for `qast.toml`.
- **Environment Interpolation**: `${PORT:-8000}`, `${DATABASE_URL}`.

### `qast-testing`
- **Harness**: `qastTest` creates an in-memory client (`TestClient`) executing requests through the full routing and middleware pipeline.
- **Fluent Assertions**: `.expectStatus()`, `.expectBody()`, `.expectHeader()`, `.expectJson()`.

### `qast-cli`
- **CLI Commands**: `qast init`, `qast dev`, `qast routes`, `qast doctor`.

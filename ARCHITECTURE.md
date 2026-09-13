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

## 2. Module Boundaries (Phase 1 & 2 Implemented)

### `qast-core`
- **Application Lifecycle**: State transitions (`UNINITIALIZED` -> `INITIALIZING` -> `RUNNING` -> `STOPPING` -> `STOPPED`), with asynchronous lifecycle hooks (`onStart`, `onStop`).
- **Configuration Models**: `ServerConfig`, `ProjectConfig`, `QastConfig`, `LoggingConfig`, `LogFormat` (`PRETTY`, `JSON`).
- **Environment Profiles**: `Environment` (`DEVELOPMENT`, `TEST`, `STAGING`, `PRODUCTION`), programmatic override and profile detection.
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
- **Pattern Matching**: `PathPattern` with priority ranking (`EXACT` > `PARAMETERIZED` > `WILDCARD`) and RFC 3986 path segment decoding.
- **Router**: Tree-like structure supporting `get`, `post`, `put`, `patch`, `delete`, `options`, `head`.
- **Smart 404 Route Suggestions**: `RouteSuggester` calculates edit distance and path similarities to suggest closest matches on 404 in development.
- **Developer Error Page**: `DeveloperErrorPage` renders interactive dark-mode HTML error page for browsers in development.
- **Middleware Pipeline**:
  - `Middlewares.structuredLogging()`: Request ID extraction/generation, ANSI-colorized pretty logging or machine-readable JSON logging.
  - `Middlewares.cors()`: Full CORS headers and OPTIONS preflight handling.
  - `Middlewares.errorHandling()`: Safe production 500 responses without leaks; rich JSON or HTML in development.
- **Context**: `QastContext` providing concise helpers (`path("id")`, `query("q")`, `bodyText()`, `status(201)`, `requestId`).

### `qast-serialization`
- **Kotlinx Serialization**: Integrated `Json` configured for lenient, unknown-key-ignoring parsing.
- **Auto Serialization**: Maps, Lists, Collections, and `@Serializable` classes returned from handlers are automatically encoded to JSON with `application/json; charset=utf-8`.
- **Body Binding**: `ctx.body<T>()` and `ctx.json<T>()` automatically deserializes request payloads with 400 Bad Request on malformed inputs.

### `qast-config`
- **TOML Parser**: Zero-dependency parser supporting tables, key-values, inline string/number arrays, and inline comments.
- **Environment Interpolation**: `${PORT:-8000}`, `${DATABASE_URL}`.
- **Profile Cascading**: `ConfigLoader` loads `qast.toml` and overlays `qast.<profile>.toml` (e.g. `qast.dev.toml`, `qast.prod.toml`).

### `qast-testing`
- **Harness**: `qastTest` creates an in-memory client (`TestClient`) executing requests through the full routing and middleware pipeline without port binding.
- **Fluent Assertions**: `.expectStatus()`, `.expectBody()`, `.expectHeader()`, `.expectBodyContains()`, `.json<T>()`.

### `qast-cli`
- **Scaffolding & Templates**: `qast init <name> [--template minimal|modular]` generates multi-profile projects.
- **Module Generator**: `qast app <name>` generates Django-style application submodules (`module.kt`, `routes.kt`, `models.kt`).
- **Feature/Plugin Management**: `qast add <feature>` and `qast remove <feature>` for `cors`, `logging`, `orm`, `auth`, `admin`, `ai`, etc.
- **Development Server**: `qast dev [--profile <profile>] [--no-watch]` with cross-platform file change detection (`DevWatcher`) and hot reload.
- **System Diagnostics**: `qast doctor` validates Java runtime, OS, memory, port availability, configuration syntax, and wrappers.
- **Route Inspection**: `qast routes [file]` lists all registered routes in a clean ASCII table.


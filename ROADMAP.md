# QastAPI Roadmap

This roadmap tracks the development progression of QastAPI. Full vision details are maintained in [QastAPI_Project_Vision_Roadmap.md](QastAPI_Project_Vision_Roadmap.md).

---

## 🎯 Development Phases

- [x] **Phase 0 — Vision & Architecture**: Core principles, repository setup, Gradle build logic, package structures.
- [x] **Phase 1 — Core HTTP Framework**: Smallest usable production-grade HTTP framework with Netty & Sun engines, coroutines, full routing DSL, path/query params, JSON serialization, exceptions, testing framework, CLI (`init`, `dev`, `routes`, `doctor`), and examples.
- [x] **Phase 2 — Developer Experience**: Minimal & modular project generator, hot reload (`DevWatcher`), dark-mode HTML developer error page & rich JSON errors, smart 404 route suggestions ("Did you mean?"), structured request logging (`PRETTY` & `JSON`), request correlation (`X-Request-ID`), enhanced `qast doctor` diagnostics, `qast app` submodule scaffolding, `qast add`/`remove` plugin system, cascading environment profiles (`qast.toml`, `qast.dev.toml`, `qast.prod.toml`).
- [ ] **Phase 3 — Validation & OpenAPI**: KSP / compile-time validation, automatic Swagger UI, `/openapi.json`.
- [ ] **Phase 4 — Database & ORM**: PostgreSQL connection pooling, migrations, type-safe query DSL, SQLite.
- [ ] **Phase 5 — Auth & Admin**: JWT, sessions, roles/permissions, auto-generated admin dashboard.
- [ ] **Phase 6 — Production Fundamentals**: Health checks, graceful shutdown, rate limiting, security headers.
- [ ] **Phase 7 — AI Foundation**: AI provider abstraction (OpenAI, Anthropic, Gemini, local models), streaming, tool calling.
- [ ] **Phase 8 — Agents + RAG**: Multi-agent loops, memory, vector database embeddings, retrieval pipeline.
- [ ] **Phase 9 — Enterprise Architecture**: Multi-tenancy, RBAC/ABAC policy engine, audit logging.
- [ ] **Phase 10 — Durable Workflows**: Persisted execution state, long-running processes, human approval gates.
- [ ] **Phase 11 — Observability & Operations**: OpenTelemetry distributed tracing, metrics, logs.
- [ ] **Phase 12 — Deployment Platform**: One-command deployment (`qast deploy`) to Docker, Kubernetes, Cloud.
- [ ] **Phase 13 — Contracts & SDK Generation**: Multi-language SDK generation (Android Kotlin, TypeScript, Python).
- [ ] **Phase 14 — Distributed Systems**: Event brokers (Kafka, RabbitMQ), service discovery, gRPC.
- [ ] **Phase 15 — QastAPI 1.0**: Stable API guarantees, production benchmarks, LTS release.

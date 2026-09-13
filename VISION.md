# QastAPI — Vision & Philosophy

> **Start tiny. Add only what you need. Scale without changing frameworks.**

## 1. The Core Vision

Kotlin is one of the most expressive, type-safe, and enjoyable programming languages in existence. However, the backend landscape in Kotlin is often polarized:
- **Heavy enterprise frameworks**: Spring Boot is powerful but comes with heavy runtime reflection, slow cold starts, and complex configuration abstractions.
- **Micro-frameworks**: Libraries like Ktor and Javalin are clean but require assembling dozens of third-party libraries for ORM, migrations, authentication, admin, validation, and AI capabilities.
- **Python's AI Dominance**: While Python has FastAPI and extensive AI tooling, it lacks Kotlin's multiplatform capabilities, static typing, and coroutine performance at scale.

**QastAPI** fills this space by unifying:

```text
FastAPI simplicity
        +
Django productivity
        +
Kotlin type safety
        +
Coroutines
        +
Compile-time generation
        +
AI-native architecture
        +
Enterprise capabilities
        +
One-command deployment
        =
QastAPI
```

---

## 2. Core Tenets

### 1. Minimal by Default
A new project starts with 4 files:
```text
myapp/
├── main.kt
├── settings.kt
├── qast.toml
└── build.gradle.kts
```

### 2. Structure on Demand
When you need an ORM, an admin dashboard, or AI agents, add them on demand:
```bash
qast add orm
qast add auth
qast add admin
qast add agent
```

### 3. Batteries Optional
Every feature is modular. If you only need a high-performance REST API, `qast-core`, `qast-http`, and `qast-routing` provide a sub-megabyte footprint with ultra-fast startup.

### 4. AI-Native
AI application features (agents, tools, memory, RAG, and Python workers) are treated as first-class framework citizens, not external wrappers.

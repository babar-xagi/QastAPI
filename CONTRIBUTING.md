# Contributing to QastAPI

Thank you for your interest in contributing to QastAPI!

## Development Setup

### Prerequisites
- **JDK 21** or higher (OpenJDK / Eclipse Adoptium recommended)
- **Git**

### Build and Test
Clone the repository and run all tests via Gradle:

```bash
git clone https://github.com/babar-xagi/QastAPI.git
cd QastAPI
./gradlew test
```

### Running the Example Application
```bash
./gradlew :examples:hello-api:run
```

### Running the CLI
```bash
# Unix
./qast doctor
./qast routes

# Windows
.\qast.bat doctor
.\qast.bat routes
```

---

## Architectural Guidelines

1. **Keep Core Minimal**: `qast-core` must remain lightweight without external heavy runtime dependencies.
2. **Coroutine-First**: All I/O and handler pipelines must be suspending (`suspend`).
3. **Batteries Optional**: Any substantial capability (database, auth, admin, AI) belongs in its dedicated optional module.
4. **No Heavy Reflection**: Prefer compile-time generation (KSP) and kotlinx.serialization over runtime reflection.

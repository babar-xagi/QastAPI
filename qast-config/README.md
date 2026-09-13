# qast-config

`qast-config` provides application configuration loading and TOML parsing for QastAPI.

## Supported TOML Subset

`qast-config` includes a lightweight, zero-dependency TOML parser (`TomlParser`) focused on server and environment configuration.

### Features Supported:
- **Tables**: `[project]`, `[server]`, `[environment]`
- **Strings**: Double-quoted `"value"` and single-quoted `'value'`
- **Numbers**: Integers (`port = 8000`) and Floats
- **Booleans**: `true` / `false`
- **Comments**: Lines starting with `#`
- **Environment Interpolation**:
  - `${PORT}` (fetches from environment)
  - `${PORT:-8000}` (with fallback default)
  - `${DATABASE_URL}`

### Example `qast.toml`:
```toml
[project]
name = "my-service"
version = "0.1.0"

[server]
host = "0.0.0.0"
port = ${PORT:-8000}
engine = "netty"

[environment]
mode = "${ENV:-development}"
```

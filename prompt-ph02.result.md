READ CAREFULLY:

@QastAPI_Project_Vision_Roadmap.md
@README.md
@ARCHITECTURE.md
@ROADMAP.md

Phase 2 has been implemented.

DO NOT start Phase 3 yet.

Perform a strict Phase 2 Developer Experience audit.

============================================================
1. VERIFY QAST INIT
   ============================================================

Create a fresh project outside the QastAPI repository:

    qast init phase2-audit

Verify generated structure is minimal.

It must not generate unnecessary architecture such as:

    controllers/
    services/
    repositories/
    database/
    admin/

Run:

    ./gradlew clean build

Then run the application and verify GET / works.

Also test invalid names:

    ../../evil
    ..\..\evil
    hello/world
    hello\world

They must be rejected.

Verify existing non-empty directories are never overwritten silently.

============================================================
2. AUDIT qast app
   ============================================================

Run:

    qast app school

Review generated files.

QastAPI philosophy is:

    Start tiny.
    Add structure on demand.

ORM is not implemented yet.

Therefore do NOT create ORM-style models unless genuinely required.

If models.kt currently only contains DTOs, either:

A. rename it to a clearer name such as dto.kt/types.kt

or preferably:

B. generate only:

    school/
    ├── module.kt
    └── routes.kt

until additional files are required.

Verify:

    qast app school

run twice does not overwrite existing user code.

Test invalid/path traversal module names.

============================================================
3. AUDIT qast add
   ============================================================

This is critical.

Features such as:

    orm
    auth
    admin
    ai
    agent
    rag
    tenancy

are NOT implemented yet.

Running:

    qast add orm

must NOT make it appear that ORM is installed.

Preferred behavior:

    Feature `orm` is planned for Phase 4 and is not available yet.

    Available features:
      cors
      logging

Only genuinely implemented features may be installable.

Separate feature states:

    AVAILABLE
    PLANNED
    INSTALLED

Example:

    qast features

should ideally show:

    FEATURE      STATUS
    logging      available
    cors         available
    orm          planned
    auth         planned
    ai           planned

Do not generate fake config for unimplemented features.

============================================================
4. AUDIT qast remove
   ============================================================

Verify:

    qast remove <feature>

does not delete or modify user-written code.

It must:

- safely remove framework-owned registration/config only
- be idempotent
- report feature not installed cleanly
- preserve comments and unrelated qast.toml configuration

Add round-trip test:

    original config
        ↓
    qast add logging
        ↓
    qast remove logging
        ↓
    unrelated config remains unchanged

============================================================
5. HOT RELOAD STRESS TEST
   ============================================================

Run:

    qast dev

Then modify a watched Kotlin file at least 10 times.

Verify:

- each valid edit triggers only one restart
- debounce works
- no duplicate server exists
- old JVM exits
- port is released
- memory/process count does not continually grow
- invalid Kotlin compilation does not kill the watcher
- fixing the compilation error recovers automatically
- Ctrl+C stops all child processes

Test Windows PowerShell specifically.

============================================================
6. CONFIG PROFILE PRECEDENCE
   ============================================================

Verify exact precedence and document it.

Test:

    qast.toml
    qast.dev.toml
    environment variables
    CLI flags

Create conflicting values for the same setting.

Ensure final value follows documented precedence.

Test:

    QAST_PROFILE
    QAST_ENV
    ENV
    system property
    programmatic override

If multiple environment-selection mechanisms exist unnecessarily,
simplify them.

Avoid ambiguous precedence.

============================================================
7. STRUCTURED LOGGING
   ============================================================

Verify PRETTY logging.

Verify JSON logging output is valid JSON per line.

Test values containing:

    quotes
    backslashes
    unicode
    newline characters

Never construct JSON logs using unsafe string concatenation.

Verify request ID exists in:

- request context
- response X-Request-ID header
- request log
- error log

Verify incoming valid X-Request-ID handling.

Reject/sanitize unreasonable or malicious request IDs if required.

============================================================
8. DEVELOPMENT ERROR PAGE SECURITY
   ============================================================

Development HTML errors contain:

- stack traces
- request headers
- environment information

Verify this behavior is strictly disabled in production.

Production responses must never expose:

- Authorization headers
- cookies
- filesystem paths
- environment secrets
- stack traces
- database credentials
- API keys

Also sanitize sensitive fields in development display where sensible:

    Authorization
    Cookie
    Set-Cookie
    X-API-Key

Test this explicitly.

============================================================
9. ROUTE SUGGESTIONS
   ============================================================

Audit RouteSuggester.

Test:

    /user
    /users
    /usr/123
    /totally-unrelated

Suggestions should:

- be useful
- be deterministic
- not expose hidden/internal routes
- have a threshold
- avoid suggesting nonsense

Benchmark with large route counts.

Do not allow expensive typo matching to become a DoS vector.

============================================================
10. qast doctor
    ============================================================

Verify:

    qast doctor
    qast doctor --plain

Check:

- Java supported/unsupported versions
- missing Gradle wrapper
- malformed qast.toml
- occupied port
- missing main source
- nested working directory
- healthy project

Verify exit codes:

    0 = healthy
    non-zero = blocking error

Warnings alone should have documented behavior.

============================================================
11. CLI PROJECT ROOT DISCOVERY
    ============================================================

From:

    project/
      src/main/kotlin/school/

run:

    qast doctor
    qast routes
    qast app test

Verify they resolve the correct nearest QastAPI root.

Do not walk indefinitely or modify unrelated parent directories.

============================================================
12. ROUTES OUTPUT
    ============================================================

Verify:

    qast routes

and:

    qast routes --json

JSON output must be valid machine-readable JSON.

No ANSI escape codes in JSON.

Test:

- exact routes
- parameterized routes
- wildcard routes
- grouped routes
- HEAD fallback representation

============================================================
13. UTF-8 / WINDOWS
    ============================================================

Test output logic for:

- PowerShell
- CMD
- Windows Terminal
- plain mode

Ensure formatting remains aligned without relying on emoji width.

No mojibake.

============================================================
14. FILE SAFETY
    ============================================================

Audit every CLI filesystem mutation.

Requirements:

- no path traversal
- no arbitrary overwrite
- atomic writes where reasonable
- duplicate-safe edits
- no silent user-code deletion
- deterministic generated files

============================================================
15. TEST COVERAGE
    ============================================================

Add missing tests discovered above.

Include tests for:

- fresh init
- standalone build
- duplicate init
- app generation
- duplicate app
- malicious paths
- add/remove round trip
- planned feature rejection
- profile precedence
- JSON logging validity
- sensitive error redaction
- route suggestions
- doctor errors
- nested project discovery
- routes JSON
- hot reload lifecycle

============================================================
16. FINAL VERIFICATION
    ============================================================

Run:

    ./gradlew clean test
    ./gradlew build

Then manually verify:

    qast --help
    qast --version
    qast doctor
    qast doctor --plain
    qast routes
    qast routes --json
    qast app school
    qast add logging
    qast remove logging
    qast add orm

The final command must clearly state ORM is not implemented yet.

Also run qast dev and verify reload + Ctrl+C shutdown.

============================================================
17. FINAL REPORT
    ============================================================

Provide:

1. bugs found
2. bugs fixed
3. design changes
4. files changed
5. tests added
6. exact test count
7. clean test result
8. build result
9. hot reload stress-test result
10. standalone init result
11. feature add/remove result
12. known limitations
13. git commit
14. git status
15. whether Phase 2 can genuinely be marked complete

Only after this audit passes should be 
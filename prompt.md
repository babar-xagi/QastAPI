Carefully audit the current QastAPI implementation after Phase 1.

DO NOT implement Phase 2 or ORM yet.

The goal is to harden Phase 1 and verify that every claim in the previous
implementation report is actually correct.

1. Run:
   ./gradlew clean test
   ./gradlew build

2. Inspect every Phase 1 module:
    - qast-core
    - qast-http
    - qast-routing
    - qast-serialization
    - qast-config
    - qast-testing
    - qast-cli
    - examples/hello-api

3. Verify module boundaries.
   qast-core must not depend on HTTP engines, routing implementations,
   CLI, serialization implementations, or examples.

4. Verify HttpEngine is a clean abstraction.
   Netty-specific types must not leak into public QastAPI APIs.

5. Review coroutine handling for:
    - blocking calls
    - GlobalScope usage
    - improper runBlocking usage
    - leaked jobs
    - dispatcher misuse
    - graceful shutdown

6. Verify HTTP semantics:
    - GET
    - POST
    - PUT
    - PATCH
    - DELETE
    - HEAD
    - OPTIONS
    - 404
    - 405
    - Content-Type
    - Content-Length
    - empty 204 responses
    - query decoding
    - URL decoding
    - duplicate headers
    - cookies

7. Add tests for route conflicts:
   /users/me
   /users/{id}
   /users/*
   grouped routes
   trailing slash behavior

8. Verify path parameter decoding.

9. Verify query strings containing:
   spaces
    +
   %
   unicode
   repeated query parameters

10. Verify malformed JSON returns a proper 400 response and never a 500.

11. Verify unhandled exceptions return a safe 500 response without leaking
    stack traces in production mode.

12. Fix qast routes.
    It must not display 200 OK for every endpoint because response status
    can be dynamic.

    Preferred output:

    METHOD    PATH
    GET       /
    GET       /users/{id}
    POST      /users
    DELETE    /users/{id}

13. Review the custom TOML parser.
    If it attempts to implement full TOML, replace it with a mature TOML
    implementation behind the qast-config abstraction.
    If intentionally minimal, explicitly document the supported subset.

14. Verify qast init produces a project that independently builds.

    Create a temporary project using:

    qast init phase1-test

    Then run its build and tests.

15. Verify Windows and Unix wrapper scripts:
    qast.bat
    qast

16. Start hello-api and perform real network tests for:
    GET /
    GET /users/123
    POST /users
    DELETE /users/42
    404
    405
    malformed JSON

17. Add missing tests discovered during the audit.

18. Run the entire build again:

    ./gradlew clean test
    ./gradlew build

19. Do not hide warnings or failing tests.

20. At the end provide:

    - exact files changed
    - bugs found
    - bugs fixed
    - tests added
    - final Gradle test result
    - final build result
    - known limitations
    - whether Phase 1 can genuinely be marked complete

Do not begin Phase 2 until Phase 1 passes this audit.
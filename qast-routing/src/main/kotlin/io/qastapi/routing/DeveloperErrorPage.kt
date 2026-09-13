package io.qastapi.routing

import io.qastapi.core.Environment
import io.qastapi.http.HttpStatus
import java.time.Instant

/**
 * Generates an interactive, styled dark-mode Developer Error Page for browser requests in development.
 */
object DeveloperErrorPage {

    fun render(
        status: HttpStatus,
        exception: Throwable?,
        ctx: QastContext,
        suggestions: List<String> = emptyList()
    ): String {
        val title = "${status.code} ${status.reasonPhrase}"
        val exceptionClass = exception?.javaClass?.name ?: status.reasonPhrase
        val message = exception?.message ?: status.reasonPhrase
        val requestId = ctx.requestId
        val timestamp = Instant.now().toString()
        val profile = Environment.currentProfile()

        val stackTraceHtml = exception?.stackTrace?.take(30)?.joinToString("\n") { frame ->
            val isAppCode = !frame.className.startsWith("java.") &&
                    !frame.className.startsWith("kotlin.") &&
                    !frame.className.startsWith("kotlinx.") &&
                    !frame.className.startsWith("io.netty.")
            val cssClass = if (isAppCode) "frame-app" else "frame-lib"
            "<div class=\"frame $cssClass\"><span class=\"method\">${escapeHtml(frame.className)}.${escapeHtml(frame.methodName)}</span><span class=\"loc\">(${escapeHtml(frame.fileName ?: "Unknown")}:${frame.lineNumber})</span></div>"
        } ?: "<div class=\"frame frame-lib\">No stack trace available.</div>"

        val suggestionsHtml = if (suggestions.isNotEmpty()) {
            """
            <div class="card suggestions-card">
                <h3>💡 Did you mean?</h3>
                <ul class="suggestions-list">
                    ${suggestions.joinToString("\n") { "<li><code>${escapeHtml(it)}</code></li>" }}
                </ul>
            </div>
            """.trimIndent()
        } else ""

        val headersHtml = ctx.headers.asMap().entries.joinToString("\n") { (k, v) ->
            "<tr><td><code>${escapeHtml(k)}</code></td><td><code>${escapeHtml(v.joinToString(", "))}</code></td></tr>"
        }

        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>$title — QastAPI Dev Error</title>
                <style>
                    :root {
                        --bg: #0d1117;
                        --card-bg: #161b22;
                        --border: #30363d;
                        --text: #c9d1d9;
                        --text-muted: #8b949e;
                        --accent-red: #f85149;
                        --accent-yellow: #d29922;
                        --accent-blue: #58a6ff;
                        --accent-green: #3fb950;
                        --code-bg: #21262d;
                    }
                    * { box-sizing: border-box; margin: 0; padding: 0; }
                    body {
                        background: var(--bg);
                        color: var(--text);
                        font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
                        padding: 2rem;
                        line-height: 1.5;
                    }
                    .container { max-width: 1000px; margin: 0 auto; }
                    .header {
                        border-bottom: 1px solid var(--border);
                        padding-bottom: 1.5rem;
                        margin-bottom: 1.5rem;
                        display: flex;
                        justify-content: space-between;
                        align-items: center;
                    }
                    .badge-red {
                        background: rgba(248, 81, 73, 0.15);
                        color: var(--accent-red);
                        border: 1px solid var(--accent-red);
                        padding: 0.25rem 0.75rem;
                        border-radius: 6px;
                        font-size: 0.875rem;
                        font-weight: 600;
                    }
                    .badge-yellow {
                        background: rgba(210, 153, 34, 0.15);
                        color: var(--accent-yellow);
                        border: 1px solid var(--accent-yellow);
                        padding: 0.25rem 0.75rem;
                        border-radius: 6px;
                        font-size: 0.875rem;
                        font-weight: 600;
                    }
                    .badge-profile {
                        background: rgba(88, 166, 255, 0.15);
                        color: var(--accent-blue);
                        border: 1px solid var(--accent-blue);
                        padding: 0.25rem 0.75rem;
                        border-radius: 6px;
                        font-size: 0.875rem;
                        font-weight: 600;
                    }
                    h1 { font-size: 1.75rem; color: #fff; margin-bottom: 0.5rem; }
                    h2 { font-size: 1.25rem; color: var(--accent-red); margin-bottom: 0.5rem; font-family: monospace; }
                    .msg { font-size: 1.1rem; color: var(--text); margin-bottom: 1rem; }
                    .card {
                        background: var(--card-bg);
                        border: 1px solid var(--border);
                        border-radius: 8px;
                        padding: 1.25rem;
                        margin-bottom: 1.5rem;
                    }
                    .card h3 { font-size: 1rem; color: #fff; margin-bottom: 0.75rem; text-transform: uppercase; letter-spacing: 0.05em; }
                    .suggestions-card { border-left: 4px solid var(--accent-yellow); }
                    .suggestions-list { list-style: none; padding-left: 0; }
                    .suggestions-list li { margin: 0.4rem 0; }
                    .suggestions-list code { background: var(--code-bg); padding: 0.2rem 0.5rem; border-radius: 4px; color: var(--accent-blue); }
                    .stack-trace {
                        background: var(--code-bg);
                        border-radius: 6px;
                        padding: 1rem;
                        font-family: ui-monospace, SFMono-Regular, Consolas, monospace;
                        font-size: 0.875rem;
                        overflow-x: auto;
                        max-height: 400px;
                        overflow-y: auto;
                    }
                    .frame { padding: 0.2rem 0; }
                    .frame-app { color: var(--accent-blue); font-weight: 600; }
                    .frame-lib { color: var(--text-muted); }
                    .frame .loc { color: var(--text-muted); margin-left: 0.5rem; font-weight: normal; }
                    table { width: 100%; border-collapse: collapse; text-align: left; font-size: 0.875rem; }
                    th, td { padding: 0.5rem; border-bottom: 1px solid var(--border); }
                    th { color: var(--text-muted); width: 35%; }
                    code { font-family: ui-monospace, SFMono-Regular, Consolas, monospace; }
                    .footer { text-align: center; color: var(--text-muted); font-size: 0.8rem; margin-top: 2rem; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <div>
                            <h1>QastAPI Developer Error</h1>
                            <div class="msg"><code>${ctx.method.value} ${escapeHtml(ctx.uri)}</code></div>
                        </div>
                        <div>
                            <span class="${if (status.isServerError) "badge-red" else "badge-yellow"}">$title</span>
                            <span class="badge-profile">Profile: $profile</span>
                        </div>
                    </div>

                    $suggestionsHtml

                    <div class="card">
                        <h3>Exception Summary</h3>
                        <h2>${escapeHtml(exceptionClass)}</h2>
                        <div class="msg">${escapeHtml(message)}</div>
                    </div>

                    <div class="card">
                        <h3>Stack Trace</h3>
                        <div class="stack-trace">
                            $stackTraceHtml
                        </div>
                    </div>

                    <div class="card">
                        <h3>Request Details</h3>
                        <table>
                            <tr><th>Method</th><td><code>${ctx.method.value}</code></td></tr>
                            <tr><th>Path</th><td><code>${escapeHtml(ctx.path)}</code></td></tr>
                            <tr><th>URI</th><td><code>${escapeHtml(ctx.uri)}</code></td></tr>
                            <tr><th>Request ID</th><td><code>${escapeHtml(requestId)}</code></td></tr>
                            <tr><th>Timestamp</th><td><code>$timestamp</code></td></tr>
                        </table>
                    </div>

                    <div class="card">
                        <h3>Request Headers</h3>
                        <table>
                            $headersHtml
                        </table>
                    </div>

                    <div class="footer">
                        QastAPI v0.1.0 • Running on Java ${System.getProperty("java.version")} (${System.getProperty("os.name")})
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent()
    }

    private fun escapeHtml(str: String): String {
        return str.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#x27;")
    }
}

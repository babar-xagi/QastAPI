package io.qastapi.cli

import io.qastapi.config.TomlParser
import java.io.File
import java.net.ServerSocket

object CliCommands {

    fun init(projectName: String, template: String = "minimal") {
        val targetDir = File(projectName)
        if (targetDir.exists() && targetDir.listFiles()?.isNotEmpty() == true) {
            println("❌ Error: Directory '$projectName' already exists and is not empty.")
            return
        }

        targetDir.mkdirs()

        // 1. main.kt
        val mainContent = if (template.lowercase() == "modular") {
            """
            package com.example

            import io.qastapi.QastAPI
            import com.example.users.registerUsersApp

            fun main() {
                QastAPI {
                    get("/") {
                        mapOf("message" to "Hello QastAPI!", "status" to "healthy")
                    }

                    // Register modular apps
                    registerUsersApp()
                }
            }
            """.trimIndent()
        } else {
            """
            package com.example

            import io.qastapi.QastAPI

            fun main() {
                QastAPI {
                    get("/") {
                        mapOf("message" to "Hello QastAPI!", "status" to "healthy")
                    }

                    get("/users/{id}") {
                        val id = path("id")
                        mapOf("id" to id, "name" to "User ${'$'}id")
                    }

                    post("/echo") {
                        val body = bodyText()
                        mapOf("echo" to body)
                    }
                }
            }
            """.trimIndent()
        }
        File(targetDir, "main.kt").writeText(mainContent)

        // 2. settings.kt
        File(targetDir, "settings.kt").writeText(
            """
            package com.example

            object AppSettings {
                const val APP_NAME = "$projectName"
            }
            """.trimIndent()
        )

        // 3. Base qast.toml
        File(targetDir, "qast.toml").writeText(
            """
            [project]
            name = "$projectName"
            version = "0.1.0"

            [server]
            host = "0.0.0.0"
            port = 8000

            [environment]
            mode = "development"

            [logging]
            level = "INFO"
            format = "pretty"
            """.trimIndent()
        )

        // 4. Development Profile: qast.dev.toml
        File(targetDir, "qast.dev.toml").writeText(
            """
            [server]
            port = 8000
            devMode = true

            [logging]
            format = "pretty"
            """.trimIndent()
        )

        // 5. Production Profile: qast.prod.toml
        File(targetDir, "qast.prod.toml").writeText(
            """
            [server]
            port = 8080
            devMode = false

            [logging]
            format = "json"

            [environment]
            mode = "production"
            """.trimIndent()
        )

        // 6. build.gradle.kts
        File(targetDir, "build.gradle.kts").writeText(
            """
            plugins {
                kotlin("jvm") version "2.1.0"
                kotlin("plugin.serialization") version "2.1.0"
                application
            }

            application {
                mainClass.set("com.example.MainKt")
            }

            kotlin {
                jvmToolchain(21)
                sourceSets["main"].kotlin.srcDirs(".", "src/main/kotlin")
            }

            repositories {
                mavenCentral()
                mavenLocal()
            }

            dependencies {
                implementation("io.qastapi:qast-core:0.1.0")
                implementation("io.qastapi:qast-http:0.1.0")
                implementation("io.qastapi:qast-routing:0.1.0")
                implementation("io.qastapi:qast-serialization:0.1.0")
                implementation("io.qastapi:qast-config:0.1.0")
            }
            """.trimIndent()
        )

        // 7. settings.gradle.kts
        File(targetDir, "settings.gradle.kts").writeText(
            """
            rootProject.name = "$projectName"

            // Link local QastAPI development workspace if present
            val localQast = file("..")
            if (localQast.resolve("qast-core").exists()) {
                includeBuild(localQast)
            }
            """.trimIndent()
        )

        // If modular template, create initial users app
        if (template.lowercase() == "modular") {
            scaffoldAppInDir(File(targetDir, "users"), "users")
        }

        println(
            """
            ✓ Successfully initialized QastAPI project in '$projectName/'
            
            Structure created:
            $projectName/
            ├── main.kt
            ├── settings.kt
            ├── qast.toml          (base configuration)
            ├── qast.dev.toml      (development profile)
            ├── qast.prod.toml     (production profile)
            ├── build.gradle.kts
            └── settings.gradle.kts

            Get started:
              cd $projectName
              qast dev
            """.trimIndent()
        )
    }

    fun app(appName: String) {
        val targetDir = File(appName.trim())
        val moduleName = targetDir.name.trim().lowercase()
        if (moduleName.isEmpty()) {
            println("❌ Error: App name cannot be empty. Example: qast app users")
            return
        }

        if (targetDir.exists() && targetDir.listFiles()?.isNotEmpty() == true) {
            println("❌ Error: Directory '${targetDir.path}' already exists and is not empty.")
            return
        }

        scaffoldAppInDir(targetDir, moduleName)

        val capName = moduleName.replaceFirstChar { it.uppercase() }
        println(
            """
            ✓ Successfully generated application module in '${targetDir.path}/'
            
            Files created:
            ${targetDir.path}/
            ├── module.kt  (registers routes on QastApp)
            ├── routes.kt  (REST endpoints for $moduleName)
            └── models.kt  (@Serializable DTOs)

            To use this module in your application, add the following to your main() in main.kt:
                QastAPI {
                    register${capName}App()
                }
            """.trimIndent()
        )
    }

    private fun scaffoldAppInDir(dir: File, appName: String) {
        dir.mkdirs()
        val capName = appName.replaceFirstChar { it.uppercase() }

        // models.kt
        File(dir, "models.kt").writeText(
            """
            package com.example.$appName

            import kotlinx.serialization.Serializable

            @Serializable
            data class ${capName}Item(
                val id: Long,
                val name: String,
                val description: String = ""
            )

            @Serializable
            data class Create${capName}Request(
                val name: String,
                val description: String = ""
            )
            """.trimIndent()
        )

        // routes.kt
        File(dir, "routes.kt").writeText(
            """
            package com.example.$appName

            import io.qastapi.routing.Router
            import io.qastapi.serialization.body

            fun Router.${appName}Routes() {
                group("/$appName") {
                    get("/") {
                        listOf(
                            ${capName}Item(1L, "Sample $capName 1", "Default item"),
                            ${capName}Item(2L, "Sample $capName 2", "Another item")
                        )
                    }

                    get("/{id}") {
                        val id = path("id").toLongOrNull() ?: 0L
                        ${capName}Item(id, "Sample $capName ${'$'}id")
                    }

                    post("/") {
                        val req = body<Create${capName}Request>()
                        ${capName}Item(System.currentTimeMillis(), req.name, req.description)
                    }
                }
            }
            """.trimIndent()
        )

        // module.kt
        File(dir, "module.kt").writeText(
            """
            package com.example.$appName

            import io.qastapi.routing.QastApp

            fun QastApp.register${capName}App() {
                router.${appName}Routes()
            }
            """.trimIndent()
        )
    }

    fun add(feature: String) {
        val feat = feature.trim().lowercase()
        val qastToml = File("qast.toml")

        if (!qastToml.exists()) {
            println("❌ Error: No 'qast.toml' found in current directory. Run 'qast init' first or cd into a Qast project.")
            return
        }

        val snippet = getFeatureSnippet(feat)
        if (snippet == null) {
            println("❌ Unknown feature '$feature'. Available features: cors, logging, orm, auth, admin, ai, agent, rag, cache, events, jobs, observe, tenancy.")
            return
        }

        val currentContent = qastToml.readText()
        val sectionHeader = "[${snippet.first}]"

        if (currentContent.contains(sectionHeader)) {
            println("ℹ Feature '$feature' is already configured in qast.toml.")
            return
        }

        // Append feature section
        val updatedContent = buildString {
            append(currentContent.trimEnd())
            append("\n\n")
            append(snippet.second.trimIndent())
            append("\n")
        }

        qastToml.writeText(updatedContent)
        println("✓ Added feature '$feature' to qast.toml")
        println(snippet.third)
    }

    fun remove(feature: String) {
        val feat = feature.trim().lowercase()
        val qastToml = File("qast.toml")

        if (!qastToml.exists()) {
            println("❌ Error: No 'qast.toml' found in current directory.")
            return
        }

        val targetSection = when (feat) {
            "database" -> "database"
            "orm" -> "database"
            else -> feat
        }

        val content = qastToml.readText()
        if (!content.contains("[$targetSection]")) {
            println("ℹ Feature '$feature' is not configured in qast.toml.")
            return
        }

        // Comment out the section in qast.toml
        val lines = content.lines()
        val newLines = mutableListOf<String>()
        var insideTargetSection = false

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                val sectionName = trimmed.substring(1, trimmed.length - 1).trim()
                insideTargetSection = (sectionName.equals(targetSection, ignoreCase = true))
            }
            if (insideTargetSection) {
                newLines.add("# $line")
            } else {
                newLines.add(line)
            }
        }

        qastToml.writeText(newLines.joinToString("\n"))
        println("✓ Disabled feature '$feature' in qast.toml.")
    }

    private fun getFeatureSnippet(feature: String): Triple<String, String, String>? {
        return when (feature) {
            "cors" -> Triple(
                "cors",
                """
                [cors]
                allowed_origins = ["*"]
                allowed_methods = ["GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"]
                allowed_headers = ["*"]
                """.trimIndent(),
                "Configured CORS settings in qast.toml."
            )
            "logging" -> Triple(
                "logging",
                """
                [logging]
                level = "INFO"
                format = "pretty" # use "json" for structured production logs
                include_request_id = true
                """.trimIndent(),
                "Configured structured logging in qast.toml."
            )
            "orm", "database" -> Triple(
                "database",
                """
                [database]
                provider = "postgresql"
                url = "${'$'}{DATABASE_URL:-postgresql://localhost:5432/app}"
                pool_size = 10
                """.trimIndent(),
                "Configured database connection in qast.toml."
            )
            "auth" -> Triple(
                "auth",
                """
                [auth]
                provider = "jwt"
                secret = "${'$'}{JWT_SECRET:-secret-key-change-in-prod}"
                expiration_hours = 24
                """.trimIndent(),
                "Configured JWT authentication in qast.toml."
            )
            "admin" -> Triple(
                "admin",
                """
                [admin]
                enabled = true
                path = "/admin"
                title = "QastAPI Admin Portal"
                """.trimIndent(),
                "Configured admin dashboard in qast.toml."
            )
            "ai" -> Triple(
                "ai",
                """
                [ai]
                provider = "openai"
                model = "gpt-4o"
                api_key = "${'$'}{OPENAI_API_KEY}"
                """.trimIndent(),
                "Configured AI provider in qast.toml."
            )
            "agent" -> Triple(
                "agent",
                """
                [agent]
                enabled = true
                max_steps = 15
                timeout_seconds = 60
                """.trimIndent(),
                "Configured AI agent runtime in qast.toml."
            )
            "rag" -> Triple(
                "rag",
                """
                [rag]
                provider = "pgvector"
                dimension = 1536
                top_k = 5
                """.trimIndent(),
                "Configured RAG vector database in qast.toml."
            )
            "cache" -> Triple(
                "cache",
                """
                [cache]
                provider = "redis"
                url = "${'$'}{REDIS_URL:-redis://localhost:6379}"
                default_ttl_seconds = 3600
                """.trimIndent(),
                "Configured Redis cache in qast.toml."
            )
            "events" -> Triple(
                "events",
                """
                [events]
                provider = "kafka"
                bootstrap_servers = "${'$'}{KAFKA_SERVERS:-localhost:9092}"
                """.trimIndent(),
                "Configured Kafka event bus in qast.toml."
            )
            "jobs" -> Triple(
                "jobs",
                """
                [jobs]
                enabled = true
                concurrency = 4
                retry_limit = 3
                """.trimIndent(),
                "Configured background jobs in qast.toml."
            )
            "observe" -> Triple(
                "observe",
                """
                [observe]
                provider = "opentelemetry"
                endpoint = "${'$'}{OTEL_EXPORTER_OTLP_ENDPOINT:-http://localhost:4317}"
                tracing_enabled = true
                metrics_enabled = true
                """.trimIndent(),
                "Configured OpenTelemetry observability in qast.toml."
            )
            "tenancy" -> Triple(
                "tenancy",
                """
                [tenancy]
                strategy = "schema"
                header = "X-Tenant-ID"
                """.trimIndent(),
                "Configured multi-tenancy in qast.toml."
            )
            else -> null
        }
    }

    fun doctor() {
        println("QastAPI Doctor — System & Project Diagnostics\n")
        var passed = 0
        var warnings = 0
        var errors = 0

        // 1. Java check
        val javaVersion = System.getProperty("java.version")
        val javaVendor = System.getProperty("java.vendor")
        val javaHome = System.getProperty("java.home")
        val majorVersion = javaVersion.split(".").firstOrNull()?.toIntOrNull()
            ?: javaVersion.split("-").firstOrNull()?.split(".")?.firstOrNull()?.toIntOrNull()
            ?: 0

        if (majorVersion >= 17) {
            println(" [✓] Java: version $javaVersion ($javaVendor) - Compatible (17+ required)")
            println("     Location: $javaHome")
            passed++
        } else {
            println(" [!] Java: version $javaVersion - QastAPI requires Java 17 or higher")
            warnings++
        }

        // 2. OS & Hardware
        val osName = System.getProperty("os.name")
        val osArch = System.getProperty("os.arch")
        val cores = Runtime.getRuntime().availableProcessors()
        println(" [✓] System: $osName ($osArch), $cores CPU cores available")
        passed++

        // 3. Memory
        val maxMemoryMb = Runtime.getRuntime().maxMemory() / (1024 * 1024)
        val totalMemoryMb = Runtime.getRuntime().totalMemory() / (1024 * 1024)
        println(" [✓] JVM Memory: Max Heap ${maxMemoryMb}MB (Allocated: ${totalMemoryMb}MB)")
        passed++

        // 4. Project Configuration & Parsing Check
        val qastToml = File("qast.toml")
        var configuredPort = 8000
        if (qastToml.exists()) {
            try {
                val parsed = TomlParser.parse(qastToml.readText())
                val pName = parsed["project"]?.get("name") ?: "unnamed"
                val pVer = parsed["project"]?.get("version") ?: "0.1.0"
                configuredPort = (parsed["server"]?.get("port") as? Number)?.toInt() ?: 8000
                println(" [✓] Configuration: Valid qast.toml found (Project: $pName v$pVer)")
                passed++

                // Check profile files
                val devToml = File("qast.dev.toml")
                val prodToml = File("qast.prod.toml")
                if (devToml.exists()) println("     Found dev profile: qast.dev.toml")
                if (prodToml.exists()) println("     Found prod profile: qast.prod.toml")
            } catch (e: Exception) {
                println(" [✗] Configuration: qast.toml has syntax errors: ${e.message}")
                errors++
            }
        } else {
            println(" [i] Project Config: No qast.toml found in current directory (Run 'qast init <name>')")
        }

        // 5. Port Check
        val portAvailable = try {
            ServerSocket(configuredPort).use { true }
        } catch (_: Exception) {
            false
        }
        if (portAvailable) {
            println(" [✓] Server Port ($configuredPort): Available")
            passed++
        } else {
            println(" [!] Server Port ($configuredPort): Currently in use by another process")
            warnings++
        }

        // 6. Build tool wrapper check
        val gradlewName = if (System.getProperty("os.name").lowercase().contains("windows")) "gradlew.bat" else "gradlew"
        val gradlewFile = File(gradlewName)
        if (gradlewFile.exists()) {
            println(" [✓] Build Tool: Found Gradle wrapper ($gradlewName)")
            passed++
        } else {
            println(" [i] Build Tool: No local Gradle wrapper found in current directory")
        }

        // 7. Source files check
        val mainFile = findMainKtFile()
        if (mainFile != null && mainFile.exists()) {
            println(" [✓] Source Entry: Found ${mainFile.path}")
            passed++
        } else {
            println(" [i] Source Entry: No 'main.kt' detected in current directory")
        }

        println("\nDiagnostic Summary: $passed Passed | $warnings Warnings | $errors Errors")
        println("Doctor check completed.")
    }

    fun routes(targetFile: File? = null) {
        val file = targetFile?.takeIf { it.exists() } ?: findMainKtFile()
        if (file == null || !file.exists()) {
            println("No 'main.kt' found in current directory. Pass a file path or run inside a QastAPI project.")
            return
        }

        println("METHOD    PATH")
        val content = file.readText()
        val routeRegex = Regex("""(get|post|put|patch|delete|options|head)\s*\(\s*["']([^"']+)["']""")
        val matches = routeRegex.findAll(content).toList()

        if (matches.isEmpty()) {
            println("(No routes detected in ${file.name})")
        } else {
            matches.forEach { match ->
                val method = match.groupValues[1].uppercase().padEnd(9)
                val path = match.groupValues[2]
                println("$method $path")
            }
        }
    }

    fun dev(watch: Boolean = true, profile: String = "development") {
        println(
            """
   ____             _      _    ____ ___ 
  / __ \____ ______/ /_   / \  / __ \_ _|
 / / / / __ `/ ___/ __/  / _ \/ /_/ /| | 
/ /_/ / /_/ (__  ) /_   / ___/ ____/ | | 
\___\_\__,_/____/\__/  /_/  /_/    |___| 
 QastAPI Development Server

 Active Profile: $profile
 Hot Reload:     ${if (watch) "Enabled (watching .kt, .kts, .toml)" else "Disabled"}
            """.trimIndent()
        )

        val gradlew = if (System.getProperty("os.name").lowercase().contains("windows")) "gradlew.bat" else "./gradlew"
        val gradleFile = File(gradlew)

        if (!gradleFile.exists()) {
            println("Note: To run in dev mode, ensure Gradle wrapper ($gradlew) is available in your project directory.")
            return
        }

        if (!watch) {
            println("Starting server process...")
            val process = ProcessBuilder(gradleFile.absolutePath, "run", "-Pqast.profile=$profile")
                .inheritIO()
                .start()
            process.waitFor()
            return
        }

        var currentProcess: Process? = null

        fun startProcess() {
            currentProcess?.let { p ->
                if (p.isAlive) {
                    p.destroy()
                    p.waitFor()
                }
            }
            currentProcess = ProcessBuilder(gradleFile.absolutePath, "run", "-Pqast.profile=$profile")
                .inheritIO()
                .start()
        }

        startProcess()

        val watcher = DevWatcher(listOf(File("."))) { changedFile ->
            println("\n[Qast Dev] Detected change in ${changedFile.name}... Reloading application...")
            startProcess()
            println("[Qast Dev] Reload triggered successfully.")
        }

        watcher.start()

        Runtime.getRuntime().addShutdownHook(Thread {
            watcher.stop()
            currentProcess?.destroy()
        })

        try {
            currentProcess?.waitFor()
        } catch (_: InterruptedException) {
            watcher.stop()
            currentProcess?.destroy()
        }
    }

    private fun findMainKtFile(): File? {
        val candidates = listOf(
            File("main.kt"),
            File("src/main/kotlin/Main.kt"),
            File("examples/hello-api/src/main/kotlin/io/qastapi/example/Main.kt"),
            File("../examples/hello-api/src/main/kotlin/io/qastapi/example/Main.kt"),
            File("../main.kt")
        )
        for (f in candidates) {
            if (f.exists()) return f
        }
        val root = if (File("..").exists() && File("../settings.gradle.kts").exists()) File("..") else File(".")
        return root.walkTopDown().maxDepth(6).firstOrNull { it.isFile && it.name.equals("main.kt", ignoreCase = true) }
    }
}

package io.qastapi.cli

import java.io.File
import java.net.ServerSocket

object CliCommands {

    fun init(projectName: String) {
        val targetDir = File(projectName)
        if (targetDir.exists() && targetDir.listFiles()?.isNotEmpty() == true) {
            println("❌ Error: Directory '$projectName' already exists and is not empty.")
            return
        }

        targetDir.mkdirs()

        // 1. main.kt
        File(targetDir, "main.kt").writeText(
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
        )

        // 2. settings.kt
        File(targetDir, "settings.kt").writeText(
            """
            package com.example

            object AppSettings {
                const val APP_NAME = "$projectName"
            }
            """.trimIndent()
        )

        // 3. qast.toml
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
            """.trimIndent()
        )

        // 4. build.gradle.kts
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

        // 5. settings.gradle.kts
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

        println(
            """
            ✓ Successfully initialized QastAPI project in '$projectName/'
            
            Structure created:
            $projectName/
            ├── main.kt
            ├── settings.kt
            ├── qast.toml
            ├── build.gradle.kts
            └── settings.gradle.kts

            Get started:
              cd $projectName
              qast dev
            """.trimIndent()
        )
    }

    fun doctor() {
        println("QastAPI Doctor — System Diagnostics\n")

        // 1. Java check
        val javaVersion = System.getProperty("java.version")
        val javaVendor = System.getProperty("java.vendor")
        val majorVersion = javaVersion.split(".").firstOrNull()?.toIntOrNull()
            ?: javaVersion.split("-").firstOrNull()?.split(".")?.firstOrNull()?.toIntOrNull()
            ?: 0

        if (majorVersion >= 17) {
            println(" [✓] Java: version $javaVersion ($javaVendor) - Compatible (17+ required)")
        } else {
            println(" [!] Java: version $javaVersion - QastAPI recommends Java 17 or higher")
        }

        // 2. OS check
        val osName = System.getProperty("os.name")
        val osArch = System.getProperty("os.arch")
        println(" [✓] OS: $osName ($osArch)")

        // 3. Port 8000 check
        val portAvailable = try {
            ServerSocket(8000).use { true }
        } catch (_: Exception) {
            false
        }
        if (portAvailable) {
            println(" [✓] Default Port (8000): Available")
        } else {
            println(" [!] Default Port (8000): In use by another process")
        }

        // 4. Project check
        val qastToml = File("qast.toml")
        if (qastToml.exists()) {
            println(" [✓] Project Config: Found qast.toml in current directory")
        } else {
            println(" [i] Project Config: No qast.toml found in current directory")
        }

        println("\nDoctor check completed.")
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

    fun dev() {
        println(
            """
   ____             _      _    ____ ___ 
  / __ \____ ______/ /_   / \  / __ \_ _|
 / / / / __ `/ ___/ __/  / _ \/ /_/ /| | 
/ /_/ / /_/ (__  ) /_   / ___/ ____/ | | 
\___\_\__,_/____/\__/  /_/  /_/    |___| 
 QastAPI Development Server

 Starting development server...
            """.trimIndent()
        )

        val gradlew = if (System.getProperty("os.name").lowercase().contains("windows")) "gradlew.bat" else "./gradlew"
        val gradleFile = File(gradlew)

        if (gradleFile.exists()) {
            println("Running via Gradle...")
            val process = ProcessBuilder(gradleFile.absolutePath, "run")
                .inheritIO()
                .start()
            process.waitFor()
        } else {
            println("Note: To run in dev mode, ensure Gradle wrapper is available in your project directory.")
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

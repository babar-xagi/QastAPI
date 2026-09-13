package io.qastapi.cli

import java.io.File

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        printHelp()
        return
    }

    when (val cmd = args[0].lowercase()) {
        "init" -> {
            var name = "my-qast-app"
            var template = "minimal"
            var i = 1
            while (i < args.size) {
                when (args[i]) {
                    "--template", "-t" -> {
                        if (i + 1 < args.size) {
                            template = args[i + 1]
                            i++
                        }
                    }
                    else -> {
                        if (!args[i].startsWith("-")) {
                            name = args[i]
                        }
                    }
                }
                i++
            }
            CliCommands.init(name, template)
        }
        "app" -> {
            if (args.size < 2) {
                println("❌ Usage: qast app <name> (e.g. qast app users)")
                return
            }
            CliCommands.app(args[1])
        }
        "add" -> {
            if (args.size < 2) {
                println("❌ Usage: qast add <feature> (e.g. qast add cors, qast add orm, qast add auth)")
                return
            }
            CliCommands.add(args[1])
        }
        "remove" -> {
            if (args.size < 2) {
                println("❌ Usage: qast remove <feature> (e.g. qast remove cors)")
                return
            }
            CliCommands.remove(args[1])
        }
        "doctor" -> {
            CliCommands.doctor()
        }
        "routes" -> {
            val file = if (args.size > 1) File(args[1]) else null
            CliCommands.routes(file)
        }
        "dev" -> {
            var watch = true
            var profile = "development"
            var i = 1
            while (i < args.size) {
                when (args[i]) {
                    "--no-watch" -> watch = false
                    "--watch" -> watch = true
                    "--profile", "-p" -> {
                        if (i + 1 < args.size) {
                            profile = args[i + 1]
                            i++
                        }
                    }
                }
                i++
            }
            CliCommands.dev(watch = watch, profile = profile)
        }
        "help", "--help", "-h" -> {
            printHelp()
        }
        else -> {
            println("Unknown command: $cmd")
            printHelp()
        }
    }
}

private fun printHelp() {
    println(
        """
        QastAPI CLI — Kotlin-first, AI-native Application Framework

        Usage:
          qast <command> [arguments]

        Commands:
          init <name>             Initialize a new QastAPI project (--template minimal|modular)
          app <name>              Scaffold a new application submodule (module.kt, routes.kt, models.kt)
          add <feature>           Add and configure a feature (cors, logging, orm, auth, ai, etc.)
          remove <feature>        Disable or remove a configured feature
          dev                     Start the development server with hot reload (--profile, --no-watch)
          routes [file]           List all registered routes
          doctor                  Run system environment, memory, port, and dependency diagnostics
          help                    Show this help message
        """.trimIndent()
    )
}

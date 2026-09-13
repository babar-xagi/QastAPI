package io.qastapi.cli

import java.io.File

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        printHelp()
        return
    }

    when (val cmd = args[0].lowercase()) {
        "init" -> {
            val name = if (args.size > 1) args[1] else "my-qast-app"
            CliCommands.init(name)
        }
        "doctor" -> {
            CliCommands.doctor()
        }
        "routes" -> {
            val file = if (args.size > 1) File(args[1]) else null
            CliCommands.routes(file)
        }
        "dev" -> {
            CliCommands.dev()
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
          init <name>     Initialize a new minimal QastAPI project
          dev             Start the development server
          routes [file]   List all registered routes
          doctor          Run system environment and dependency diagnostics
          help            Show this help message
        """.trimIndent()
    )
}

package io.qastapi.config

import io.qastapi.core.Environment
import io.qastapi.core.ProjectConfig
import io.qastapi.core.QastConfig
import io.qastapi.core.ServerConfig
import java.io.File

object ConfigLoader {

    fun load(configFile: File? = null): QastConfig {
        val file = configFile ?: findDefaultConfigFile()
        if (file == null || !file.exists()) {
            return QastConfig()
        }

        val content = file.readText()
        val parsed = TomlParser.parse(content)

        val projectSection = parsed["project"] ?: emptyMap()
        val serverSection = parsed["server"] ?: emptyMap()
        val envSection = parsed["environment"] ?: emptyMap()

        val projectName = projectSection["name"]?.toString() ?: "qast-app"
        val projectVersion = projectSection["version"]?.toString() ?: "0.1.0"

        val serverHost = serverSection["host"]?.toString() ?: "0.0.0.0"
        val serverPort = (serverSection["port"] as? Number)?.toInt() ?: 8000
        val serverBacklog = (serverSection["backlog"] as? Number)?.toInt() ?: 1024
        val serverEngine = serverSection["engine"]?.toString() ?: "netty"
        val devMode = (serverSection["devMode"] as? Boolean) ?: true

        val envMode = envSection["mode"]?.toString() ?: "development"

        return QastConfig(
            project = ProjectConfig(
                name = projectName,
                version = projectVersion
            ),
            server = ServerConfig(
                host = serverHost,
                port = serverPort,
                backlog = serverBacklog,
                devMode = devMode,
                engine = serverEngine
            ),
            environment = Environment.fromString(envMode)
        )
    }

    private fun findDefaultConfigFile(): File? {
        val cwdFile = File("qast.toml")
        if (cwdFile.exists()) return cwdFile

        val appDir = System.getProperty("user.dir")
        val appFile = File(appDir, "qast.toml")
        if (appFile.exists()) return appFile

        return null
    }
}

package io.qastapi.config

import io.qastapi.core.Environment
import io.qastapi.core.LogFormat
import io.qastapi.core.LoggingConfig
import io.qastapi.core.ProjectConfig
import io.qastapi.core.QastConfig
import io.qastapi.core.ServerConfig
import java.io.File

object ConfigLoader {

    /**
     * Loads the base configuration file (default `qast.toml`), detects or accepts the active [profile],
     * loads profile-specific overrides (e.g., `qast.<profile>.toml`), and merges them.
     */
    fun load(configFile: File? = null, profile: String? = null): QastConfig {
        val baseFile = configFile ?: findDefaultConfigFile("qast.toml")

        val baseMap: MutableMap<String, MutableMap<String, Any>> = if (baseFile != null && baseFile.exists()) {
            TomlParser.parse(baseFile.readText()).mapValues { it.value.toMutableMap() }.toMutableMap()
        } else {
            mutableMapOf()
        }

        // Determine active profile
        val activeProfile = profile
            ?: (baseMap["environment"]?.get("mode") as? String)
            ?: Environment.currentProfile()

        // Look for profile override file: e.g. qast.dev.toml, qast.prod.toml, qast.test.toml
        val profileFile = findProfileConfigFile(baseFile, activeProfile)
        if (profileFile != null && profileFile.exists()) {
            val profileMap = TomlParser.parse(profileFile.readText())
            mergeConfigs(baseMap, profileMap)
        }

        return buildConfig(baseMap, activeProfile)
    }

    private fun mergeConfigs(
        base: MutableMap<String, MutableMap<String, Any>>,
        override: Map<String, Map<String, Any>>
    ) {
        for ((section, values) in override) {
            val baseSection = base.computeIfAbsent(section) { mutableMapOf() }
            for ((key, value) in values) {
                baseSection[key] = value
            }
        }
    }

    private fun buildConfig(
        map: Map<String, Map<String, Any>>,
        activeProfile: String
    ): QastConfig {
        val projectSection = map["project"] ?: emptyMap()
        val serverSection = map["server"] ?: emptyMap()
        val envSection = map["environment"] ?: emptyMap()
        val loggingSection = map["logging"] ?: emptyMap()
        val pluginsSection = map["plugins"] ?: emptyMap()

        val projectName = projectSection["name"]?.toString() ?: "qast-app"
        val projectVersion = projectSection["version"]?.toString() ?: "0.1.0"

        val serverHost = serverSection["host"]?.toString() ?: "0.0.0.0"
        val serverPort = (serverSection["port"] as? Number)?.toInt() ?: 8000
        val serverBacklog = (serverSection["backlog"] as? Number)?.toInt() ?: 1024
        val readTimeoutMs = (serverSection["readTimeoutMs"] as? Number)?.toLong() ?: 30_000L
        val writeTimeoutMs = (serverSection["writeTimeoutMs"] as? Number)?.toLong() ?: 30_000L
        val serverEngine = serverSection["engine"]?.toString() ?: "netty"
        val resolvedEnv = Environment.fromString(envSection["mode"]?.toString() ?: activeProfile)
        val devMode = (serverSection["devMode"] as? Boolean) ?: (!resolvedEnv.isProd())

        // Logging
        val logLevel = loggingSection["level"]?.toString() ?: "INFO"
        val logFormat = LogFormat.fromString(loggingSection["format"]?.toString() ?: if (resolvedEnv.isProd()) "json" else "pretty")
        val includeRequestId = (loggingSection["include_request_id"] as? Boolean)
            ?: (loggingSection["includeRequestId"] as? Boolean)
            ?: true
        val logHeaders = (loggingSection["log_headers"] as? Boolean)
            ?: (loggingSection["logHeaders"] as? Boolean)
            ?: false

        // Plugins
        val pluginsList = when (val p = pluginsSection["active"] ?: map["default"]?.get("plugins")) {
            is List<*> -> p.mapNotNull { it?.toString() }
            is String -> listOf(p)
            else -> emptyList()
        }

        return QastConfig(
            project = ProjectConfig(
                name = projectName,
                version = projectVersion
            ),
            server = ServerConfig(
                host = serverHost,
                port = serverPort,
                backlog = serverBacklog,
                readTimeoutMs = readTimeoutMs,
                writeTimeoutMs = writeTimeoutMs,
                devMode = devMode,
                engine = serverEngine
            ),
            environment = resolvedEnv,
            logging = LoggingConfig(
                level = logLevel,
                format = logFormat,
                includeRequestId = includeRequestId,
                logHeaders = logHeaders
            ),
            plugins = pluginsList,
            rawSections = map
        )
    }

    private fun findDefaultConfigFile(name: String): File? {
        val cwdFile = File(name)
        if (cwdFile.exists()) return cwdFile

        val appDir = System.getProperty("user.dir")
        val appFile = File(appDir, name)
        if (appFile.exists()) return appFile

        return null
    }

    private fun findProfileConfigFile(baseFile: File?, profile: String): File? {
        val shortProfile = when (profile.lowercase()) {
            "development" -> "dev"
            "production" -> "prod"
            "testing" -> "test"
            else -> profile.lowercase()
        }

        val candidates = listOf(
            "qast.$shortProfile.toml",
            "qast.$profile.toml"
        )

        val parentDir = baseFile?.parentFile ?: File(".")
        for (candidate in candidates) {
            val fileInParent = File(parentDir, candidate)
            if (fileInParent.exists()) return fileInParent

            val fileInCwd = File(candidate)
            if (fileInCwd.exists()) return fileInCwd
        }

        return null
    }
}

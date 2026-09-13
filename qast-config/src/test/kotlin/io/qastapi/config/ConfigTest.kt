package io.qastapi.config

import io.qastapi.core.Environment
import io.qastapi.core.LogFormat
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ConfigTest {

    @Test
    fun testParseTomlBasic() {
        val toml = """
            [project]
            name = "school-ai"
            version = "0.2.0"

            [server]
            host = "127.0.0.1"
            port = 9000

            [environment]
            mode = "production"
        """.trimIndent()

        val parsed = TomlParser.parse(toml)
        assertEquals("school-ai", parsed["project"]?.get("name"))
        assertEquals("0.2.0", parsed["project"]?.get("version"))
        assertEquals("127.0.0.1", parsed["server"]?.get("host"))
        assertEquals(9000L, parsed["server"]?.get("port"))
        assertEquals("production", parsed["environment"]?.get("mode"))
    }

    @Test
    fun testParseTomlArraysAndComments() {
        val toml = """
            # Global config
            [plugins]
            active = ["cors", "logging", "auth"] # registered plugins

            [server]
            port = 8080 # http port
            devMode = true
        """.trimIndent()

        val parsed = TomlParser.parse(toml)
        assertEquals(listOf("cors", "logging", "auth"), parsed["plugins"]?.get("active"))
        assertEquals(8080L, parsed["server"]?.get("port"))
        assertEquals(true, parsed["server"]?.get("devMode"))
    }

    @Test
    fun testEnvVarInterpolationWithDefault() {
        val resolved = TomlParser.resolveEnvVars("\${NON_EXISTENT_PORT:-8080}")
        assertEquals("8080", resolved)
    }

    @Test
    fun testLoadConfigFile() {
        val tempFile = File.createTempFile("qast", ".toml")
        try {
            tempFile.writeText(
                """
                [project]
                name = "my-test-app"

                [server]
                port = 8888
                """.trimIndent()
            )
            val config = ConfigLoader.load(tempFile)
            assertEquals("my-test-app", config.project.name)
            assertEquals(8888, config.server.port)
        } finally {
            tempFile.delete()
        }
    }

    @Test
    fun testProfileCascadingOverrides() {
        val dir = File.createTempFile("qast-test-dir", "")
        dir.delete()
        dir.mkdirs()

        try {
            val baseFile = File(dir, "qast.toml")
            baseFile.writeText(
                """
                [project]
                name = "cascade-app"
                version = "1.0.0"

                [server]
                port = 8000
                devMode = true

                [logging]
                format = "pretty"
                """.trimIndent()
            )

            val prodFile = File(dir, "qast.prod.toml")
            prodFile.writeText(
                """
                [server]
                port = 443
                devMode = false

                [logging]
                format = "json"

                [environment]
                mode = "production"
                """.trimIndent()
            )

            // Test loading development (base without prod override)
            val devConfig = ConfigLoader.load(baseFile, profile = "development")
            assertEquals(8000, devConfig.server.port)
            assertTrue(devConfig.server.devMode)
            assertEquals(LogFormat.PRETTY, devConfig.logging.format)
            assertEquals(Environment.DEVELOPMENT, devConfig.environment)

            // Test loading production with cascading override
            val prodConfig = ConfigLoader.load(baseFile, profile = "prod")
            assertEquals(443, prodConfig.server.port)
            assertFalse(prodConfig.server.devMode)
            assertEquals(LogFormat.JSON, prodConfig.logging.format)
            assertEquals(Environment.PRODUCTION, prodConfig.environment)
            assertEquals("cascade-app", prodConfig.project.name) // inherited from base
        } finally {
            dir.deleteRecursively()
        }
    }
}

package io.qastapi.config

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
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
}

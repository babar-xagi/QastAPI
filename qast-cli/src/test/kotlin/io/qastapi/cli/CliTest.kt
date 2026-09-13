package io.qastapi.cli

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class CliTest {

    @Test
    fun testInitProjectScaffold() {
        val testDir = File("build/tmp/test-init-app")
        if (testDir.exists()) testDir.deleteRecursively()

        CliCommands.init(testDir.path)

        assertTrue(File(testDir, "main.kt").exists())
        assertTrue(File(testDir, "settings.kt").exists())
        assertTrue(File(testDir, "qast.toml").exists())
        assertTrue(File(testDir, "build.gradle.kts").exists())

        // Cleanup
        testDir.deleteRecursively()
    }
}

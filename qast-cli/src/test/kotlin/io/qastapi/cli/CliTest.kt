package io.qastapi.cli

import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CliTest {

    @Test
    fun testInitProjectScaffoldWithProfiles() {
        val testDir = File("build/tmp/test-init-profiles")
        if (testDir.exists()) testDir.deleteRecursively()

        CliCommands.init(testDir.path, template = "minimal")

        assertTrue(File(testDir, "main.kt").exists())
        assertTrue(File(testDir, "settings.kt").exists())
        assertTrue(File(testDir, "qast.toml").exists())
        assertTrue(File(testDir, "qast.dev.toml").exists())
        assertTrue(File(testDir, "qast.prod.toml").exists())
        assertTrue(File(testDir, "build.gradle.kts").exists())
        assertTrue(File(testDir, "settings.gradle.kts").exists())

        val tomlContent = File(testDir, "qast.toml").readText()
        assertTrue(tomlContent.contains("[project]"))
        assertTrue(tomlContent.contains("[server]"))
        assertTrue(tomlContent.contains("[logging]"))

        // Cleanup
        testDir.deleteRecursively()
    }

    @Test
    fun testInitModularTemplate() {
        val testDir = File("build/tmp/test-modular-app")
        if (testDir.exists()) testDir.deleteRecursively()

        CliCommands.init(testDir.path, template = "modular")

        assertTrue(File(testDir, "main.kt").exists())
        assertTrue(File(testDir, "users/module.kt").exists())
        assertTrue(File(testDir, "users/routes.kt").exists())
        assertTrue(File(testDir, "users/models.kt").exists())

        val mainText = File(testDir, "main.kt").readText()
        assertTrue(mainText.contains("registerUsersApp()"))

        // Cleanup
        testDir.deleteRecursively()
    }

    @Test
    fun testAppScaffoldCommand() {
        val appDir = File("build/tmp/test-apps")
        appDir.mkdirs()
        val origUserDir = System.getProperty("user.dir")

        try {
            val target = File(appDir, "school")
            CliCommands.app(target.path)

            assertTrue(File(target, "module.kt").exists())
            assertTrue(File(target, "routes.kt").exists())
            assertTrue(File(target, "models.kt").exists())

            val moduleContent = File(target, "module.kt").readText()
            assertTrue(moduleContent.contains("registerSchoolApp"))

            val routesContent = File(target, "routes.kt").readText()
            assertTrue(routesContent.contains("/school"))

            val modelsContent = File(target, "models.kt").readText()
            assertTrue(modelsContent.contains("SchoolItem"))
        } finally {
            appDir.deleteRecursively()
        }
    }

    @Test
    fun testAddAndRemoveFeature() {
        val tempDir = File("build/tmp/test-add-feature")
        tempDir.mkdirs()
        val qastToml = File(tempDir, "qast.toml")
        qastToml.writeText(
            """
            [project]
            name = "test-add"
            version = "0.1.0"
            """.trimIndent()
        )

        val origCwd = File(".").canonicalPath
        // Change working directory simulation
        val backupToml = File("qast.toml")
        val hadToml = backupToml.exists()
        val backupContent = if (hadToml) backupToml.readText() else ""

        try {
            backupToml.writeText(qastToml.readText())

            // Test add cors
            CliCommands.add("cors")
            val withCors = backupToml.readText()
            assertTrue(withCors.contains("[cors]"))
            assertTrue(withCors.contains("allowed_origins"))

            // Test add auth
            CliCommands.add("auth")
            val withAuth = backupToml.readText()
            assertTrue(withAuth.contains("[auth]"))
            assertTrue(withAuth.contains("provider = \"jwt\""))

            // Test remove cors
            CliCommands.remove("cors")
            val removedCors = backupToml.readText()
            assertTrue(removedCors.contains("# [cors]"))
        } finally {
            if (hadToml) {
                backupToml.writeText(backupContent)
            } else {
                backupToml.delete()
            }
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun testDoctorExecutesWithoutError() {
        // Runs doctor diagnostics
        CliCommands.doctor()
    }

    @Test
    fun testDevWatcherChangeDetection() {
        val watchDir = File(System.getProperty("java.io.tmpdir"), "qast-watch-dir-" + System.currentTimeMillis())
        watchDir.mkdirs()

        val sampleFile = File(watchDir, "Sample.kt")
        sampleFile.writeText("val x = 1")

        val latch = CountDownLatch(1)
        var detectedFile: File? = null

        val watcher = DevWatcher(
            watchDirs = listOf(watchDir),
            pollIntervalMs = 100L,
            debounceMs = 50L
        ) { changed ->
            detectedFile = changed
            latch.countDown()
        }

        watcher.start()

        try {
            Thread.sleep(150)
            sampleFile.writeText("val x = 2 // modified")
            sampleFile.setLastModified(System.currentTimeMillis() + 2000)

            val detected = latch.await(2, TimeUnit.SECONDS)
            assertTrue(detected, "Watcher should have detected file modification")
            assertEquals("Sample.kt", detectedFile?.name)
        } finally {
            watcher.stop()
            watchDir.deleteRecursively()
        }
    }
}

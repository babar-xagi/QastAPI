package io.qastapi.cli

import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Cross-platform file watcher with change debouncing for QastAPI hot reload in development.
 */
class DevWatcher(
    private val watchDirs: List<File>,
    private val extensions: Set<String> = setOf("kt", "kts", "toml"),
    private val pollIntervalMs: Long = 500L,
    private val debounceMs: Long = 350L,
    private val onChange: (File) -> Unit
) {
    private val running = AtomicBoolean(false)
    private var thread: Thread? = null
    private val fileSnapshots = mutableMapOf<String, Long>()

    init {
        snapshotFiles()
    }

    private fun snapshotFiles() {
        fileSnapshots.clear()
        for (dir in watchDirs) {
            if (!dir.exists()) continue
            dir.walkTopDown()
                .filter { isWatchable(it) }
                .forEach { fileSnapshots[it.absolutePath] = it.lastModified() }
        }
    }

    private fun isWatchable(file: File): Boolean {
        if (!file.isFile) return false
        val path = file.absolutePath.replace("\\", "/")
        if (path.contains("/build/") || path.contains("/.gradle/") || path.contains("/.idea/") || path.contains("/.git/")) {
            return false
        }
        val ext = file.extension.lowercase()
        return extensions.contains(ext)
    }

    fun start() {
        if (running.getAndSet(true)) return
        snapshotFiles()

        thread = Thread {
            var lastTriggerTime = 0L
            while (running.get()) {
                try {
                    Thread.sleep(pollIntervalMs)
                    val changedFile = checkForChanges()
                    if (changedFile != null) {
                        val now = System.currentTimeMillis()
                        if (now - lastTriggerTime > debounceMs) {
                            lastTriggerTime = now
                            snapshotFiles()
                            onChange(changedFile)
                        }
                    }
                } catch (_: InterruptedException) {
                    break
                }
            }
        }.apply {
            isDaemon = true
            name = "qast-dev-watcher"
            start()
        }
    }

    fun stop() {
        running.set(false)
        thread?.interrupt()
        thread = null
    }

    private fun checkForChanges(): File? {
        val currentFiles = mutableSetOf<String>()
        for (dir in watchDirs) {
            if (!dir.exists()) continue
            for (file in dir.walkTopDown().filter { isWatchable(it) }) {
                currentFiles.add(file.absolutePath)
                val lastModified = file.lastModified()
                val prevModified = fileSnapshots[file.absolutePath]
                if (prevModified == null || prevModified != lastModified) {
                    return file
                }
            }
        }

        // Check for deleted files
        for (prevPath in fileSnapshots.keys) {
            if (!currentFiles.contains(prevPath)) {
                return File(prevPath)
            }
        }
        return null
    }
}

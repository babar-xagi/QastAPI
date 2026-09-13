package io.qastapi.core

open class QastApplication(
    val config: QastConfig = QastConfig()
) {
    val lifecycle = ApplicationLifecycle()
    val plugins = PluginRegistry()
    val attributes = Attributes()

    fun install(plugin: QastPlugin): QastApplication {
        plugins.register(plugin)
        plugin.install(this)
        return this
    }

    fun onStart(hook: suspend () -> Unit): QastApplication {
        lifecycle.onStart(hook)
        return this
    }

    fun onStop(hook: suspend () -> Unit): QastApplication {
        lifecycle.onStop(hook)
        return this
    }
}

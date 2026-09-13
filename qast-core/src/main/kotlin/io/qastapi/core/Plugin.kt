package io.qastapi.core

import java.util.concurrent.ConcurrentHashMap

interface QastPlugin {
    val name: String
    fun install(app: QastApplication)
}

class PluginRegistry {
    private val plugins = ConcurrentHashMap<String, QastPlugin>()

    fun register(plugin: QastPlugin) {
        plugins[plugin.name] = plugin
    }

    fun get(name: String): QastPlugin? = plugins[name]

    fun all(): List<QastPlugin> = plugins.values.toList()
}

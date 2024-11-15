package com.lebrislo.bluetooth.mesh.utils

import com.getcapacitor.JSObject
import com.lebrislo.bluetooth.mesh.NrfMeshPlugin

class NotificationManager() {

    private lateinit var plugin: NrfMeshPlugin

    companion object {
        @Volatile
        private var instance: NotificationManager? = null

        fun getInstance() =
            instance ?: synchronized(this) {
                instance ?: NotificationManager().also { instance = it }
            }
    }

    private fun assertPlugin(): Boolean {
        if (!::plugin.isInitialized) {
            throw IllegalStateException("Plugin not set")
        }
        return true
    }

    /**
     * Set the plugin, must be called before any other method.
     *
     * @param plugin Plugin.
     */
    fun setPlugin(plugin: NrfMeshPlugin) {
        this.plugin = plugin
    }

    fun sendNotification(event: String, data: JSObject) {
        if (assertPlugin()) {
            plugin.sendNotification(event, data)
        }
    }
}
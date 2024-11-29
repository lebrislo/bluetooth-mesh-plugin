package com.lebrislo.bluetooth.mesh.utils

import com.getcapacitor.JSObject
import com.lebrislo.bluetooth.mesh.BluetoothMeshPlugin

class NotificationManager private constructor() {

    private lateinit var plugin: BluetoothMeshPlugin

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
    fun setPlugin(plugin: BluetoothMeshPlugin) {
        this.plugin = plugin
    }

    fun sendNotification(event: String, data: JSObject) {
        if (assertPlugin()) {
            plugin.sendNotification(event, data)
        }
    }
}
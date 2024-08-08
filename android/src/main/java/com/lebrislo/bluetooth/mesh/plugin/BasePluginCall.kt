package com.lebrislo.bluetooth.mesh.plugin

import com.getcapacitor.JSObject
import com.getcapacitor.PluginCall
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * BasePluginCall is a class that provides a base implementation for plugin calls.
 * It is used to handle the resolution and rejection of plugin calls.
 *
 * @param call The plugin call to be resolved or rejected.
 * @param timeout The time in milliseconds before the call is rejected.
 */
abstract class BasePluginCall(val call: PluginCall, val timeout: Int = 10000) {

    private var isResolved: Boolean = false

    /**
     * Initializes the BasePluginCall with a timeout.
     *
     * If the plugin call is not resolved within the timeout, it will be rejected.
     */
    init {
        CoroutineScope(Dispatchers.Default).launch {
            delay(timeout.toLong())
            if (!isResolved) {
                val rejectObject = JSObject()
                rejectObject.put("methodName", call.methodName)
                call.data.keys().forEach { key ->
                    rejectObject.put(key, call.data.get(key))
                }
                reject("Operation timed out", rejectObject)
            }
        }
    }

    /**
     * Resolves the plugin call with the given result.
     *
     * @param result The result to resolve the plugin call with.
     */
    fun resolve(result: JSObject) {
        isResolved = true
        call.resolve(result)
    }

    /**
     * Rejects the plugin call with the given message and data.
     *
     * @param message The message to reject the plugin call with.
     * @param data The data to reject the plugin call with.
     */
    private fun reject(message: String, data: JSObject? = null) {
        call.reject(message, data)
    }
}

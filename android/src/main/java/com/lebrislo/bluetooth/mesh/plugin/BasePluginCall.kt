package com.lebrislo.bluetooth.mesh.plugin

import com.getcapacitor.JSObject
import com.getcapacitor.PluginCall
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

abstract class BasePluginCall(val call: PluginCall, val timeout: Int = 10000) {

    private var isResolved: Boolean = false

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

    fun resolve(result: JSObject) {
        isResolved = true
        call.resolve(result)
    }

    private fun reject(message: String, data: JSObject? = null) {
        call.reject(message, data)
    }
}

package com.lebrislo.bluetooth.mesh.plugin

import com.getcapacitor.JSObject
import com.getcapacitor.PluginCall
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

open abstract class BasePluginCall(val call: PluginCall, val timeout: Int = 10000) {

    private var isResolved: Boolean = false

    init {
        CoroutineScope(Dispatchers.Default).launch {
            delay(timeout.toLong())
            if (!isResolved) {
                reject("Operation timed out")
            }
        }
    }

    fun resolve(result: JSObject) {
        isResolved = true
        call.resolve(result)
    }

    fun reject(message: String) {
        call.reject(message)
    }
}
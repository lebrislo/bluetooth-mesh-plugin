package com.lebrislo.bluetooth.mesh.plugin

import com.getcapacitor.PluginCall


class PluginCallManager private constructor() {
    private val tag: String = PluginCallManager::class.java.simpleName

    private val pluginCalls: MutableList<BasePluginCall> = mutableListOf()

    companion object {

        @Volatile
        private var instance: PluginCallManager? = null

        fun getInstance() =
            instance ?: synchronized(this) {
                instance ?: PluginCallManager().also { instance = it }
            }
    }

    fun addSigPluginCall(call: PluginCall, meshOperation: Int, meshAddress: Int) {
        pluginCalls.add(SigPluginCall(call, meshOperation, meshAddress))
    }
}
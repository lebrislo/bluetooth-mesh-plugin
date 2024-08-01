package com.lebrislo.bluetooth.mesh.plugin

import com.getcapacitor.PluginCall
import com.lebrislo.bluetooth.mesh.plugin.SigOperationPair.Companion.getSigOperationPair
import com.lebrislo.bluetooth.mesh.plugin.SigPluginCall.Companion.generateSigPluginCallResponse
import no.nordicsemi.android.mesh.transport.MeshMessage


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

    fun addSigPluginCall(meshOperation: Int, meshAddress: Int, call: PluginCall) {
        val operationPair = getSigOperationPair(meshOperation)
        pluginCalls.add(SigPluginCall(operationPair, meshAddress, call))
    }

    fun resolveSigPluginCall(meshMessage: MeshMessage) {
        val pluginCall =
            pluginCalls.find { it is SigPluginCall && it.meshOperationCallback == meshMessage.opCode && it.meshAddress == meshMessage.src }

        pluginCall as SigPluginCall

        val callResponse = generateSigPluginCallResponse(meshMessage)
        pluginCall.resolve(callResponse)
        pluginCalls.remove(pluginCall)
    }
}
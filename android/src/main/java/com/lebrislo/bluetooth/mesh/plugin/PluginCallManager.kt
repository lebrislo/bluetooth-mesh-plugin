package com.lebrislo.bluetooth.mesh.plugin

import com.getcapacitor.PluginCall
import com.lebrislo.bluetooth.mesh.NrfMeshPlugin
import com.lebrislo.bluetooth.mesh.plugin.ConfigOperationPair.Companion.getConfigOperationPair
import com.lebrislo.bluetooth.mesh.plugin.ConfigPluginCall.Companion.generateConfigPluginCallResponse
import com.lebrislo.bluetooth.mesh.plugin.SigOperationPair.Companion.getSigOperationPair
import com.lebrislo.bluetooth.mesh.plugin.SigPluginCall.Companion.generateSigPluginCallResponse
import no.nordicsemi.android.mesh.transport.MeshMessage


class PluginCallManager private constructor() {
    private val tag: String = PluginCallManager::class.java.simpleName
    private val meshEventString: String = "meshEvent"

    private lateinit var plugin: NrfMeshPlugin
    private val pluginCalls: MutableList<BasePluginCall> = mutableListOf()

    companion object {

        @Volatile
        private var instance: PluginCallManager? = null

        fun getInstance() =
            instance ?: synchronized(this) {
                instance ?: PluginCallManager().also { instance = it }
            }
    }

    fun setPlugin(plugin: NrfMeshPlugin) {
        this.plugin = plugin
    }

    fun addSigPluginCall(meshOperation: Int, meshAddress: Int, call: PluginCall) {
        val operationPair = getSigOperationPair(meshOperation)
        pluginCalls.add(SigPluginCall(operationPair, meshAddress, call))
    }

    fun resolveSigPluginCall(meshMessage: MeshMessage) {
        val callResponse = generateSigPluginCallResponse(meshMessage)

        val pluginCall =
            pluginCalls.find { it is SigPluginCall && it.meshOperationCallback == meshMessage.opCode && it.meshAddress == meshMessage.src }

        if (pluginCall == null) {
            plugin.sendNotification(meshEventString, callResponse)
        } else {
            pluginCall as SigPluginCall
            pluginCall.resolve(callResponse)
            pluginCalls.remove(pluginCall)
        }
    }

    fun addConfigPluginCall(meshOperation: Int, meshAddress: Int, call: PluginCall) {
        val operationPair = getConfigOperationPair(meshOperation)
        pluginCalls.add(ConfigPluginCall(operationPair, meshAddress, call))
    }

    fun resolveConfigPluginCall(meshMessage: MeshMessage) {
        val callResponse = generateConfigPluginCallResponse(meshMessage)

        val pluginCall =
            pluginCalls.find { it is ConfigPluginCall && it.meshOperationCallback == meshMessage.opCode && it.meshAddress == meshMessage.src }

        if (pluginCall == null) {
            plugin.sendNotification(meshEventString, callResponse)
        } else {
            pluginCall as ConfigPluginCall
            pluginCall.resolve(callResponse)
            pluginCalls.remove(pluginCall)
        }
    }

    fun addVendorPluginCall(modelId: Int, companyIdentifier: Int, opCode: Int, meshAddress: Int, call: PluginCall) {
    }
}
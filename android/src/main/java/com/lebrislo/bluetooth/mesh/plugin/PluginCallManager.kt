package com.lebrislo.bluetooth.mesh.plugin

import com.getcapacitor.PluginCall
import com.lebrislo.bluetooth.mesh.NrfMeshPlugin
import com.lebrislo.bluetooth.mesh.plugin.ConfigOperationPair.Companion.getConfigOperationPair
import com.lebrislo.bluetooth.mesh.plugin.ConfigPluginCall.Companion.generateConfigPluginCallResponse
import com.lebrislo.bluetooth.mesh.plugin.SigOperationPair.Companion.getSigOperationPair
import com.lebrislo.bluetooth.mesh.plugin.SigPluginCall.Companion.generateSigPluginCallResponse
import com.lebrislo.bluetooth.mesh.plugin.VendorPluginCall.Companion.generateVendorPluginCallResponse
import no.nordicsemi.android.mesh.transport.MeshMessage

/**
 * This class is used to manage plugin calls.
 */
class PluginCallManager private constructor() {
    private val tag: String = PluginCallManager::class.java.simpleName
    private val MESH_EVENT_STRING: String = "meshEvent"

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

    /**
     * Set the plugin, must be called before any other method.
     *
     * @param plugin Plugin.
     */
    fun setPlugin(plugin: NrfMeshPlugin) {
        this.plugin = plugin
    }

    /**
     * Add a SIG plugin call to the list of plugin calls to watch for a response.
     *
     * @param meshOperation Mesh operation.
     * @param meshAddress Mesh address.
     * @param call Plugin call.
     */
    fun addSigPluginCall(meshOperation: Int, meshAddress: Int, call: PluginCall) {
        val operationPair = getSigOperationPair(meshOperation)
        pluginCalls.add(SigPluginCall(operationPair, meshAddress, call))
    }

    /**
     * Resolve a SIG plugin call.
     * If the call is not found in the list of plugin calls, a notification is sent to the listeners.
     *
     * @param meshMessage Mesh message.
     */
    fun resolveSigPluginCall(meshMessage: MeshMessage) {
        val callResponse = generateSigPluginCallResponse(meshMessage)

        val pluginCall =
            pluginCalls.find { it is SigPluginCall && it.meshOperationCallback == meshMessage.opCode && it.meshAddress == meshMessage.src }

        if (pluginCall != null) {
            pluginCall as SigPluginCall
            pluginCall.resolve(callResponse)
            pluginCalls.remove(pluginCall)
        }
        plugin.sendNotification(MESH_EVENT_STRING, callResponse)
    }

    /**
     * Add a Config plugin call to the list of plugin calls to watch for a response.
     *
     * @param meshOperation Mesh operation.
     * @param meshAddress Mesh address.
     * @param call Plugin call.
     */
    fun addConfigPluginCall(meshOperation: Int, meshAddress: Int, call: PluginCall) {
        val operationPair = getConfigOperationPair(meshOperation)
        pluginCalls.add(ConfigPluginCall(operationPair, meshAddress, call))
    }

    /**
     * Resolve a Config plugin call.
     * If the call is not found in the list of plugin calls, a notification is sent to the listeners.
     *
     * @param meshMessage Mesh message.
     */
    fun resolveConfigPluginCall(meshMessage: MeshMessage) {
        val callResponse = generateConfigPluginCallResponse(meshMessage)

        val pluginCall =
            pluginCalls.find { it is ConfigPluginCall && it.meshOperationCallback == meshMessage.opCode && it.meshAddress == meshMessage.src }

        if (pluginCall == null) {
            plugin.sendNotification(MESH_EVENT_STRING, callResponse)
        } else {
            pluginCall as ConfigPluginCall
            pluginCall.resolve(callResponse)
            pluginCalls.remove(pluginCall)
        }
    }

    /**
     * Add a Vendor plugin call to the list of plugin calls to watch for a response.
     *
     * @param modelId Model ID.
     * @param opCode Operation code sent.
     * @param opPairCode Operation code to watch for.
     * @param meshAddress Mesh address.
     * @param call Plugin call.
     */
    fun addVendorPluginCall(modelId: Int, opCode: Int, opPairCode: Int, meshAddress: Int, call: PluginCall) {
        pluginCalls.add(VendorPluginCall(modelId, opCode, opPairCode, meshAddress, call))
    }

    /**
     * Resolve a Vendor plugin call.
     * If the call is not found in the list of plugin calls, a notification is sent to the listeners.
     *
     * @param meshMessage Mesh message.
     */
    fun resolveVendorPluginCall(meshMessage: MeshMessage) {
        val callResponse = generateVendorPluginCallResponse(meshMessage)

        val pluginCall =
            pluginCalls.find { it is VendorPluginCall && it.meshOperationCallback == meshMessage.opCode && it.meshAddress == meshMessage.src }

        if (pluginCall == null) {
            plugin.sendNotification(MESH_EVENT_STRING, callResponse)
        } else {
            pluginCall as VendorPluginCall
            pluginCall.resolve(callResponse)
            pluginCalls.remove(pluginCall)
        }
    }
}
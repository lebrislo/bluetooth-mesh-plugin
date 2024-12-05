package com.lebrislo.bluetooth.mesh.plugin

import android.util.Log
import com.getcapacitor.PluginCall
import com.lebrislo.bluetooth.mesh.BluetoothMeshPlugin.Companion.MESH_MODEL_MESSAGE_EVENT_STRING
import com.lebrislo.bluetooth.mesh.plugin.ConfigOperationPair.Companion.getConfigOperationPair
import com.lebrislo.bluetooth.mesh.plugin.ConfigPluginCall.Companion.generateConfigPluginCallResponse
import com.lebrislo.bluetooth.mesh.plugin.SigOperationPair.Companion.getSigOperationPair
import com.lebrislo.bluetooth.mesh.plugin.SigPluginCall.Companion.generateSigPluginCallResponse
import com.lebrislo.bluetooth.mesh.plugin.VendorPluginCall.Companion.generateVendorPluginCallResponse
import com.lebrislo.bluetooth.mesh.utils.NotificationManager
import no.nordicsemi.android.mesh.transport.MeshMessage
import no.nordicsemi.android.mesh.utils.MeshAddress

/**
 * This class is used to manage plugin calls.
 */
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

    /**
     * Remove a plugin call from the list of plugin calls.
     *
     * @param call Plugin call.
     */
    fun removePluginCall(call: BasePluginCall) {
        pluginCalls.remove(call)
        Log.d(tag, "Remove call due to timeout ${call.call.methodName}")
    }

    /**
     * Add a SIG plugin call to the list of plugin calls to watch for a response.
     *
     * @param meshOperation Mesh operation.
     * @param meshAddress Mesh address.
     * @param call Plugin call.
     */
    fun addSigPluginCall(meshOperation: Int, meshAddress: Int, call: PluginCall) {
        if (!MeshAddress.isValidUnicastAddress(meshAddress)) {
            return call.resolve()
        }
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
        Log.d(tag, "resolveSigPluginCall ${meshMessage.opCode} from ${meshMessage.src}")
        val callResponse = generateSigPluginCallResponse(meshMessage)

        val pluginCall =
            pluginCalls.find { it is SigPluginCall && it.meshOperationCallback == meshMessage.opCode && it.meshAddress == meshMessage.src }

        Log.d(tag, "resolveSigPluginCall: registered call: ${pluginCall != null}")

        if (pluginCall != null) {
            pluginCall as SigPluginCall
            pluginCall.resolve(callResponse)
            pluginCalls.remove(pluginCall)
        }
        NotificationManager.getInstance().sendNotification(MESH_MODEL_MESSAGE_EVENT_STRING, callResponse)
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
        Log.d(tag, "resolveConfigPluginCall ${meshMessage.opCode} from ${meshMessage.src}")
        val callResponse = generateConfigPluginCallResponse(meshMessage)

        val pluginCall =
            pluginCalls.find { it is ConfigPluginCall && it.meshOperationCallback == meshMessage.opCode && it.meshAddress == meshMessage.src }

        Log.d(tag, "resolveConfigPluginCall: registered call: ${pluginCall != null}")

        if (pluginCall != null) {
            pluginCall as ConfigPluginCall
            pluginCall.resolve(callResponse)
            pluginCalls.remove(pluginCall)
        }
        NotificationManager.getInstance().sendNotification(MESH_MODEL_MESSAGE_EVENT_STRING, callResponse)
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
        if (!MeshAddress.isValidUnicastAddress(meshAddress)) {
            return call.resolve()
        }
        pluginCalls.add(VendorPluginCall(modelId, opCode, opPairCode, meshAddress, call))
    }

    /**
     * Resolve a Vendor plugin call.
     * If the call is not found in the list of plugin calls, a notification is sent to the listeners.
     *
     * @param meshMessage Mesh message.
     */
    fun resolveVendorPluginCall(meshMessage: MeshMessage) {
        Log.d(tag, "resolveVendorPluginCall ${meshMessage.opCode} from ${meshMessage.src}")
        val callResponse = generateVendorPluginCallResponse(meshMessage)

        val pluginCall =
            pluginCalls.find { it is VendorPluginCall && it.meshOperationCallback == meshMessage.opCode && it.meshAddress == meshMessage.src }

        Log.d(tag, "resolveVendorPluginCall: registered call: ${pluginCall != null}")

        if (pluginCall != null) {
            pluginCall as VendorPluginCall
            pluginCall.resolve(callResponse)
            pluginCalls.remove(pluginCall)
        }
        NotificationManager.getInstance().sendNotification(MESH_MODEL_MESSAGE_EVENT_STRING, callResponse)
    }
}
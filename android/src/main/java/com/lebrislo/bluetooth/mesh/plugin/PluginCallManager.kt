package com.lebrislo.bluetooth.mesh.plugin

import android.util.Log
import com.getcapacitor.PluginCall
import com.lebrislo.bluetooth.mesh.BluetoothMeshPlugin.Companion.MESH_MODEL_MESSAGE_EVENT_STRING
import com.lebrislo.bluetooth.mesh.plugin.FoundationPluginCall.Companion.generateFoundationPluginCallResponse
import com.lebrislo.bluetooth.mesh.plugin.OperationPair.Companion.getOperationPair
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
        val operationPair = getOperationPair(meshOperation)
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
     * Add a Foundation plugin call to the list of plugin calls to watch for a response.
     *
     * @param meshOperation Mesh operation.
     * @param meshAddress Mesh address.
     * @param call Plugin call.
     */
    fun addFoundationPluginCall(meshOperation: Int, meshAddress: Int, call: PluginCall) {
        val operationPair = getOperationPair(meshOperation)
        pluginCalls.add(FoundationPluginCall(operationPair, meshAddress, call))
    }

    /**
     * Resolve a Foundation plugin call.
     * If the call is not found in the list of plugin calls, a notification is sent to the listeners.
     *
     * @param meshMessage Mesh message.
     */
    fun resolveFoundationPluginCall(meshMessage: MeshMessage) {
        Log.d(tag, "resolveFoundationPluginCall ${meshMessage.opCode} from ${meshMessage.src}")
        val callResponse = generateFoundationPluginCallResponse(meshMessage)

        val pluginCall =
            pluginCalls.find { it is FoundationPluginCall && it.meshOperationCallback == meshMessage.opCode && it.meshAddress == meshMessage.src }

        Log.d(tag, "resolveFoundationPluginCall: registered call: ${pluginCall != null}")

        if (pluginCall != null) {
            pluginCall as FoundationPluginCall
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
            pluginCalls.find { it is VendorPluginCall && it.meshOperationCallback == meshMessage.opCode && (it.meshAddress == meshMessage.src || it.meshAddress == 0xFFFF) }

        Log.d(tag, "resolveVendorPluginCall: registered call: ${pluginCall != null}")

        if (pluginCall != null) {
            pluginCall as VendorPluginCall
            pluginCall.resolve(callResponse)
            pluginCalls.remove(pluginCall)
        }
        NotificationManager.getInstance().sendNotification(MESH_MODEL_MESSAGE_EVENT_STRING, callResponse)
    }
}
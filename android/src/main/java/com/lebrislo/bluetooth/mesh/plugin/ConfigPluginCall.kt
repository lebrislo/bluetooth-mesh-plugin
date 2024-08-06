package com.lebrislo.bluetooth.mesh.plugin

import com.getcapacitor.JSObject
import com.getcapacitor.PluginCall
import no.nordicsemi.android.mesh.transport.ConfigAppKeyStatus
import no.nordicsemi.android.mesh.transport.ConfigModelAppStatus
import no.nordicsemi.android.mesh.transport.ConfigNodeResetStatus
import no.nordicsemi.android.mesh.transport.MeshMessage

class ConfigPluginCall(val meshOperationCallback: Int, val meshAddress: Int, call: PluginCall) : BasePluginCall(call) {
    companion object {
        @JvmStatic
        fun generateConfigPluginCallResponse(meshMessage: MeshMessage): JSObject {
            return when (meshMessage) {
                is ConfigAppKeyStatus -> configAppKeyStatusResponse(meshMessage)
                is ConfigNodeResetStatus -> configNodeResetStatusResponse(meshMessage)
                is ConfigModelAppStatus -> configModelAppStatusResponse(meshMessage)
                else -> JSObject()
            }
        }

        private fun configAppKeyStatusResponse(meshMessage: ConfigAppKeyStatus): JSObject {
            val result = JSObject()
            result.put("src", meshMessage.src)
            result.put("dst", meshMessage.dst)
            result.put("opcode", meshMessage.opCode)
            result.put("status", meshMessage.statusCode)
            result.put("netKeyIndex", meshMessage.netKeyIndex)
            result.put("appKeyIndex", meshMessage.appKeyIndex)
            return result
        }

        private fun configNodeResetStatusResponse(meshMessage: ConfigNodeResetStatus): JSObject {
            val result = JSObject()
            result.put("src", meshMessage.src)
            result.put("dst", meshMessage.dst)
            result.put("opcode", meshMessage.opCode)
            result.put("status", meshMessage.statusCode)
            return result
        }

        private fun configModelAppStatusResponse(meshMessage: ConfigModelAppStatus): JSObject {
            val result = JSObject()
            result.put("src", meshMessage.src)
            result.put("dst", meshMessage.dst)
            result.put("opcode", meshMessage.opCode)
            result.put("status", meshMessage.statusCode)
            result.put("elementAddress", meshMessage.elementAddress)
            result.put("modelId", meshMessage.modelIdentifier)
            result.put("appKeyIndex", meshMessage.appKeyIndex)
            return result
        }
    }
}

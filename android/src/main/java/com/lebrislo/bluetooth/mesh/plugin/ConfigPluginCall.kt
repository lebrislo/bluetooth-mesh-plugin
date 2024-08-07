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
            val result = JSObject()
            result.put("src", meshMessage.src)
            result.put("dst", meshMessage.dst)
            result.put("opcode", meshMessage.opCode)
            result.put(
                "data", when (meshMessage) {
                    is ConfigAppKeyStatus -> configAppKeyStatusResponse(meshMessage)
                    is ConfigNodeResetStatus -> configNodeResetStatusResponse(meshMessage)
                    is ConfigModelAppStatus -> configModelAppStatusResponse(meshMessage)
                    else -> JSObject()
                }
            )
            return result
        }

        private fun configAppKeyStatusResponse(meshMessage: ConfigAppKeyStatus): JSObject {
            val data = JSObject()
            data.put("status", meshMessage.statusCode)
            data.put("netKeyIndex", meshMessage.netKeyIndex)
            data.put("appKeyIndex", meshMessage.appKeyIndex)
            return data
        }

        private fun configNodeResetStatusResponse(meshMessage: ConfigNodeResetStatus): JSObject {
            val data = JSObject()
            data.put("status", meshMessage.statusCode)
            return data
        }

        private fun configModelAppStatusResponse(meshMessage: ConfigModelAppStatus): JSObject {
            val data = JSObject()
            data.put("status", meshMessage.statusCode)
            data.put("elementAddress", meshMessage.elementAddress)
            data.put("modelId", meshMessage.modelIdentifier)
            data.put("appKeyIndex", meshMessage.appKeyIndex)
            return data
        }
    }
}

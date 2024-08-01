package com.lebrislo.bluetooth.mesh.plugin

import com.getcapacitor.JSObject
import com.getcapacitor.PluginCall
import no.nordicsemi.android.mesh.transport.GenericOnOffStatus
import no.nordicsemi.android.mesh.transport.LightHslStatus
import no.nordicsemi.android.mesh.transport.MeshMessage

class SigPluginCall(val meshOperationCallback: Int, val meshAddress: Int, call: PluginCall) : BasePluginCall(call) {
    companion object {
        @JvmStatic
        fun generateSigPluginCallResponse(meshMessage: MeshMessage): JSObject {
            return when (meshMessage) {
                is GenericOnOffStatus -> genericOnOffStatusResponse(meshMessage)
                is LightHslStatus -> lightHslStatusResponse(meshMessage)
                else -> JSObject()
            }
        }

        private fun genericOnOffStatusResponse(meshMessage: GenericOnOffStatus): JSObject {
            val result = JSObject()
            result.put("src", meshMessage.src)
            result.put("dst", meshMessage.dst)
            result.put("opcode", meshMessage.opCode)
            result.put("onOff", meshMessage.parameters[0].toInt() == 1)
            return result
        }

        private fun lightHslStatusResponse(meshMessage: LightHslStatus): JSObject {
            val result = JSObject()
            result.put("src", meshMessage.src)
            result.put("dst", meshMessage.dst)
            result.put("opcode", meshMessage.opCode)
            result.put("hue", meshMessage.presentHue)
            result.put("saturation", meshMessage.presentSaturation)
            result.put("lightness", meshMessage.presentLightness)
            return result
        }
    }
}

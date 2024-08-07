package com.lebrislo.bluetooth.mesh.plugin

import com.getcapacitor.JSObject
import com.getcapacitor.PluginCall
import no.nordicsemi.android.mesh.transport.GenericOnOffStatus
import no.nordicsemi.android.mesh.transport.GenericPowerLevelStatus
import no.nordicsemi.android.mesh.transport.LightHslStatus
import no.nordicsemi.android.mesh.transport.MeshMessage

class SigPluginCall(val meshOperationCallback: Int, val meshAddress: Int, call: PluginCall) : BasePluginCall(call) {
    companion object {
        @JvmStatic
        fun generateSigPluginCallResponse(meshMessage: MeshMessage): JSObject {
            val result = JSObject()
            result.put("src", meshMessage.src)
            result.put("dst", meshMessage.dst)
            result.put("opcode", meshMessage.opCode)
            result.put(
                "data", when (meshMessage) {
                    is GenericOnOffStatus -> genericOnOffStatusResponse(meshMessage)
                    is GenericPowerLevelStatus -> genericPowerLevelStatusResponse(meshMessage)
                    is LightHslStatus -> lightHslStatusResponse(meshMessage)
                    else -> JSObject()
                }
            )
            return result
        }

        private fun genericOnOffStatusResponse(meshMessage: GenericOnOffStatus): JSObject {
            val data = JSObject()
            data.put("onOff", meshMessage.parameters[0].toInt() == 1)
            return data
        }

        private fun genericPowerLevelStatusResponse(meshMessage: GenericPowerLevelStatus): JSObject {
            val data = JSObject()
            data.put("powerLevel", meshMessage.presentLevel.toUShort().toInt())
            return data
        }

        private fun lightHslStatusResponse(meshMessage: LightHslStatus): JSObject {
            val data = JSObject()
            data.put("hue", meshMessage.presentHue.toUShort().toInt())
            data.put("saturation", meshMessage.presentSaturation.toUShort().toInt())
            data.put("lightness", meshMessage.presentLightness.toUShort().toInt())
            return data
        }
    }
}

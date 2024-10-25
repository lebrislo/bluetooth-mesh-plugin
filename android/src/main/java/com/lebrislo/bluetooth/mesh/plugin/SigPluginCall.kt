package com.lebrislo.bluetooth.mesh.plugin

import android.util.Log
import com.getcapacitor.JSObject
import com.getcapacitor.PluginCall
import no.nordicsemi.android.mesh.transport.GenericLevelStatus
import no.nordicsemi.android.mesh.transport.GenericOnOffStatus
import no.nordicsemi.android.mesh.transport.GenericPowerLevelStatus
import no.nordicsemi.android.mesh.transport.LightCtlStatus
import no.nordicsemi.android.mesh.transport.LightCtlTemperatureRangeStatus
import no.nordicsemi.android.mesh.transport.LightHslStatus
import no.nordicsemi.android.mesh.transport.MeshMessage

/**
 * This class is used to generate a response for a SIG plugin call.
 */
class SigPluginCall(val meshOperationCallback: Int, val meshAddress: Int, call: PluginCall) : BasePluginCall(call) {
    companion object {
        private val tag: String = SigPluginCall::class.java.simpleName

        /**
         * Generates a response for a SIG plugin call.
         *
         * @param meshMessage Mesh message.
         */
        @JvmStatic
        fun generateSigPluginCallResponse(meshMessage: MeshMessage): JSObject {
            val result = JSObject()
            result.put("src", meshMessage.src)
            result.put("dst", meshMessage.dst)
            result.put("opcode", meshMessage.opCode)
            result.put(
                "data", when (meshMessage) {
                    is GenericOnOffStatus -> genericOnOffStatusResponse(meshMessage)
                    is GenericLevelStatus -> genericLevelStatusResponse(meshMessage)
                    is GenericPowerLevelStatus -> genericPowerLevelStatusResponse(meshMessage)
                    is LightHslStatus -> lightHslStatusResponse(meshMessage)
                    is LightCtlStatus -> lightCtlStatusResponse(meshMessage)
                    is LightCtlTemperatureRangeStatus -> lightCtlTemperatureRangeStatusResponse(meshMessage)
                    else -> JSObject()
                }
            )
            Log.d(tag, "generateSigPluginCallResponse: $result")
            return result
        }

        private fun genericOnOffStatusResponse(meshMessage: GenericOnOffStatus): JSObject {
            val data = JSObject()
            data.put("onOff", meshMessage.parameters[0].toInt() == 1)
            return data
        }

        private fun genericLevelStatusResponse(meshMessage: GenericLevelStatus): JSObject {
            val data = JSObject()
            data.put("level", meshMessage.presentLevel)
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

        private fun lightCtlStatusResponse(meshMessage: LightCtlStatus): JSObject {
            val data = JSObject()
            data.put("lightness", meshMessage.presentLightness.toUShort().toInt())
            data.put("temperature", meshMessage.presentTemperature.toUShort().toInt())
            return data
        }

        private fun lightCtlTemperatureRangeStatusResponse(meshMessage: LightCtlTemperatureRangeStatus): JSObject {
            val data = JSObject()
            data.put("status", meshMessage.statusCode.toUShort().toInt())
            data.put("min", meshMessage.rangeMin.toUShort().toInt())
            data.put("max", meshMessage.rangeMax.toUShort().toInt())
            return data
        }
    }
}

package com.lebrislo.bluetooth.mesh.plugin

import com.getcapacitor.JSArray
import com.getcapacitor.JSObject
import com.getcapacitor.PluginCall
import no.nordicsemi.android.mesh.transport.ConfigAppKeyStatus
import no.nordicsemi.android.mesh.transport.ConfigCompositionDataStatus
import no.nordicsemi.android.mesh.transport.ConfigModelAppStatus
import no.nordicsemi.android.mesh.transport.ConfigNodeResetStatus
import no.nordicsemi.android.mesh.transport.MeshMessage

/**
 * This class is used to generate a response for a Config plugin call.
 */
class ConfigPluginCall(val meshOperationCallback: Int, val meshAddress: Int, call: PluginCall) : BasePluginCall(call) {
    companion object {
        /**
         * Generates a response for a Config plugin call.
         *
         * @param meshMessage Mesh message.
         */
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
                    is ConfigCompositionDataStatus -> configCompositionDataStatusResponse(meshMessage)
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

        private fun configCompositionDataStatusResponse(meshMessage: ConfigCompositionDataStatus): JSObject {
            val data = JSObject()
            data.put("status", meshMessage.statusCode)
            data.put("companyIdentifier", meshMessage.companyIdentifier)
            data.put("productIdentifier", meshMessage.productIdentifier)
            data.put("versionIdentifier", meshMessage.versionIdentifier)
            data.put("crpl", meshMessage.crpl)
            data.put("features", meshMessage.features)
            data.put("relayFeatureSupported", meshMessage.isRelayFeatureSupported)
            data.put("proxyFeatureSupported", meshMessage.isProxyFeatureSupported)
            data.put("friendFeatureSupported", meshMessage.isFriendFeatureSupported)
            data.put("lowPowerFeatureSupported", meshMessage.isLowPowerFeatureSupported)

            val elements = JSArray()
            meshMessage.elements.forEach { element ->
                val elementData = JSObject()
                elementData.put("locationDescriptor", element.value.locationDescriptor)
                elementData.put("elementAddress", element.value.elementAddress)

                val models = JSArray()
                element.value.meshModels.forEach { model ->
                    val modelData = JSObject()
                    modelData.put("modelId", model.value.modelId)
                    modelData.put("modelName", model.value.modelName)
                    models.put(modelData)
                }
                elementData.put("models", models)

                elements.put(elementData)
            }

            data.put("elements", elements)

            return data
        }
    }
}

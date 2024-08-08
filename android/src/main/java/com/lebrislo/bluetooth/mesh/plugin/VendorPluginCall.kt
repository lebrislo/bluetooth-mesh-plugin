package com.lebrislo.bluetooth.mesh.plugin

import com.getcapacitor.JSArray
import com.getcapacitor.JSObject
import com.getcapacitor.PluginCall
import no.nordicsemi.android.mesh.transport.MeshMessage
import no.nordicsemi.android.mesh.transport.VendorModelMessageStatus

class VendorPluginCall(
    val modelId: Int,
    val meshOperation: Int,
    val meshOperationCallback: Int,
    val meshAddress: Int,
    call: PluginCall
) :
    BasePluginCall(call) {
    companion object {
        @JvmStatic
        fun generateVendorPluginCallResponse(meshMessage: MeshMessage): JSObject {
            meshMessage as VendorModelMessageStatus
            val result = JSObject()
            result.put("src", meshMessage.src)
            result.put("dst", meshMessage.dst)
            result.put("opcode", meshMessage.opCode)
            result.put("vendorModelId", meshMessage.modelIdentifier)
            val data = JSArray()
            meshMessage.parameters.forEach {
                data.put(it.toInt())
            }
            result.put("data", data)
            return result
        }
    }
}
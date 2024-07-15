package com.lebrislo.bluetooth.mesh

import com.getcapacitor.JSObject
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.CapacitorPlugin

@CapacitorPlugin(name = "NrfMesh")
class NrfMeshPlugin : Plugin() {
    private val implementation = NrfMesh()

    @PluginMethod
    fun echo(call: PluginCall) {
        val value = call.getString("value")

        val ret = JSObject()
        ret.put("value", implementation.echo(value!!))
        call.resolve(ret)
    }
}

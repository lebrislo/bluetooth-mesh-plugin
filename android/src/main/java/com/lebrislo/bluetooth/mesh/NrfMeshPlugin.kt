package com.lebrislo.bluetooth.mesh

import com.getcapacitor.JSObject
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.CapacitorPlugin
import com.lebrislo.bluetooth.mesh.models.UnprovisionedDevice
import com.lebrislo.bluetooth.mesh.scanner.ScanCallback

@CapacitorPlugin(name = "NrfMesh")
class NrfMeshPlugin : Plugin() {
    private val tag: String = NrfMeshPlugin::class.java.simpleName

    private lateinit var implementation: NrfMeshManager

    override fun load() {
        this.implementation = NrfMeshManager(this.context)
    }

    @PluginMethod
    fun echo(call: PluginCall) {
        val value = call.getString("value")

        implementation.loadMeshNetwork()
        val ret = JSObject()
        ret.put("value", implementation.echo(value!!))
        call.resolve(ret)
    }

    @PluginMethod
    fun scanUnprovisionedDevices(call: PluginCall) {
        implementation.scanUnprovisionedDevices(object : ScanCallback {
            override fun onScanCompleted(unprovisionedDevices: List<UnprovisionedDevice>) {
                val devicesArray = JSObject()
                val devicesJson = unprovisionedDevices.map { device ->
                    JSObject().apply {
                        put("rssi", device.rssi)
                        put("macAddress", device.macAddress)
                        put("name", device.name)
                        put("advData", device.advData.joinToString(","))
                    }
                }
                devicesArray.put("devices", devicesJson)
                call.resolve(devicesArray)
            }

            override fun onScanFailed(error: String) {
                call.reject(error)
            }
        })
    }
}

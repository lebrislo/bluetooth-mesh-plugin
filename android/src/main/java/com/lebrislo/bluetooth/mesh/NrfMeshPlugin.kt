package com.lebrislo.bluetooth.mesh

import com.getcapacitor.JSArray
import com.getcapacitor.JSObject
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.CapacitorPlugin
import com.lebrislo.bluetooth.mesh.models.BleMeshDevice
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
        val timeout = call.getInt("timeout", 5000)

        implementation.scanUnprovisionedDevices(object : ScanCallback {
            override fun onScanCompleted(bleMeshDevices: List<BleMeshDevice>) {
                val devicesArray = JSArray()
                bleMeshDevices.map { device ->
                    val advDataArray = JSArray()
                    device.advData.forEach { byte ->
                        advDataArray.put(byte.toInt() and 0xFF)
                    }
                    val deviceJson = JSObject().apply {
                        put("rssi", device.rssi)
                        put("macAddress", device.macAddress)
                        put("name", device.name)
                        put("advData", advDataArray)
                    }
                    devicesArray.put(deviceJson)
                }
                val result = JSObject()
                result.put("devices", devicesArray)
                call.resolve(result)
            }

            override fun onScanFailed(error: String) {
                call.reject(error)
            }
        }, timeout!!)
    }

    @PluginMethod
    fun scanProvisionedDevices(call: PluginCall) {
        val timeout = call.getInt("timeout", 5000)

        implementation.scanProvisionedDevices(object : ScanCallback {
            override fun onScanCompleted(bleMeshDevices: List<BleMeshDevice>) {
                val devicesArray = JSArray()
                bleMeshDevices.map { device ->
                    val advDataArray = JSArray()
                    device.advData.forEach { byte ->
                        advDataArray.put(byte.toInt() and 0xFF)
                    }
                    val deviceJson = JSObject().apply {
                        put("rssi", device.rssi)
                        put("macAddress", device.macAddress)
                        put("name", device.name)
                        put("advData", advDataArray)
                    }
                    devicesArray.put(deviceJson)
                }
                val result = JSObject()
                result.put("devices", devicesArray)
                call.resolve(result)
            }

            override fun onScanFailed(error: String) {
                call.reject(error)
            }
        }, timeout!!)
    }
}

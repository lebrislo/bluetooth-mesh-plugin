package com.lebrislo.bluetooth.mesh

import com.getcapacitor.JSArray
import com.getcapacitor.JSObject
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.CapacitorPlugin
import com.lebrislo.bluetooth.mesh.models.ExtendedBluetoothDevice
import com.lebrislo.bluetooth.mesh.scanner.ScanCallback
import com.lebrislo.bluetooth.mesh.utils.Utils
import no.nordicsemi.android.mesh.MeshManagerApi
import java.util.UUID

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
        val ret = JSObject()
        ret.put("value", implementation.echo(value!!))
        call.resolve(ret)
    }

    @PluginMethod
    fun scanUnprovisionedDevices(call: PluginCall) {
        val timeout = call.getInt("timeout", 5000)

        implementation.scanUnprovisionedDevices(object : ScanCallback {
            override fun onScanCompleted(bleMeshDevices: List<ExtendedBluetoothDevice>?) {
                val devicesArray = JSArray()
                bleMeshDevices?.map { device ->
                    val serviceData = Utils.getServiceData(
                        device.scanResult!!,
                        MeshManagerApi.MESH_PROVISIONING_UUID
                    )
                    val uuid: UUID = implementation.meshManagerApi.getDeviceUuid(serviceData!!)
                    val deviceJson = JSObject().apply {
                        put("rssi", device.rssi)
                        put("macAddress", device.address)
                        put("name", device.name)
                        put("uuid", uuid)
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
            override fun onScanCompleted(bleMeshDevices: List<ExtendedBluetoothDevice>?) {
                val devicesArray = JSArray()
                bleMeshDevices?.map { device ->
                    val serviceData = Utils.getServiceData(
                        device.scanResult!!,
                        MeshManagerApi.MESH_PROVISIONING_UUID
                    )
                    val uuid: UUID = implementation.meshManagerApi.getDeviceUuid(serviceData!!)
                    val deviceJson = JSObject().apply {
                        put("rssi", device.rssi)
                        put("macAddress", device.address)
                        put("name", device.name)
                        put("uuid", uuid)
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
    fun provisionDevice(call: PluginCall) {
        val uuid = call.getString("uuid")

        if (uuid == null) {
            call.reject("UUID is required")
        }

        implementation.provisionDevice(UUID.fromString(uuid))
    }
}

package com.lebrislo.bluetooth.mesh

import com.getcapacitor.JSArray
import com.getcapacitor.JSObject
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.CapacitorPlugin
import com.lebrislo.bluetooth.mesh.models.ExtendedBluetoothDevice
import com.lebrislo.bluetooth.mesh.models.MeshDevice
import com.lebrislo.bluetooth.mesh.scanner.ScanCallback
import com.lebrislo.bluetooth.mesh.utils.Utils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
    fun getProvisioningCapabilities(call: PluginCall) {
        val uuidString = call.getString("uuid")
        if (uuidString == null) {
            call.reject("UUID is required")
            return
        }
        val uuid = UUID.fromString(uuidString)

        val deferred = implementation.getProvisioningCapabilities(uuid)

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val unprovisionedDevice = deferred.await()
                if (unprovisionedDevice != null) {

                    val result = JSObject().apply {
                        put("numberOfElements", unprovisionedDevice.provisioningCapabilities.numberOfElements)
                        val oobTypeArray = JSArray().apply {
                            unprovisionedDevice.provisioningCapabilities.availableOOBTypes.forEach {
                                put(it)
                            }
                        }
                        put("availableOOBTypes", oobTypeArray)
                        put("algorithms", unprovisionedDevice.provisioningCapabilities.rawAlgorithm)
                        put("publicKeyType", unprovisionedDevice.provisioningCapabilities.rawPublicKeyType)
                        put("staticOobTypes", unprovisionedDevice.provisioningCapabilities.rawStaticOOBType)
                        put("outputOobSize", unprovisionedDevice.provisioningCapabilities.outputOOBSize)
                        put("outputOobActions", unprovisionedDevice.provisioningCapabilities.rawOutputOOBAction)
                        put("inputOobSize", unprovisionedDevice.provisioningCapabilities.inputOOBSize)
                        put("inputOobActions", unprovisionedDevice.provisioningCapabilities.rawInputOOBAction)
                    }
                    call.resolve(result)
                } else {
                    call.reject("Failed to get provisioning capabilities")
                }
            } catch (e: Exception) {
                call.reject("Error: ${e.message}")
            }
        }
    }

    @PluginMethod
    fun provisionDevice(call: PluginCall) {
        val uuid = call.getString("uuid")

        if (uuid == null) {
            call.reject("UUID is required")
        }

        val deferred = implementation.provisionDevice(UUID.fromString(uuid))

        CoroutineScope(Dispatchers.Main).launch {
            try {
                if (deferred == null) {
                    call.reject("Failed to provision device")
                    return@launch
                }

                val meshDevice = deferred.await()
                if (meshDevice == null) {
                    call.reject("Failed to provision device")
                    return@launch
                }

                when (meshDevice) {
                    is MeshDevice.Provisioned -> {
                        val result = JSObject().apply {
                            put("provisioningComplete", true)
                            put("uuid", meshDevice.node.uuid)
                            put("unicastAddress", meshDevice.node.unicastAddress)
                        }
                        call.resolve(result)
                    }

                    is MeshDevice.Unprovisioned -> {
                        val result = JSObject().apply {
                            put("provisioningComplete", false)
                            put("uuid", meshDevice.node.deviceUuid)
                        }
                        call.resolve(result)
                    }
                }
            } catch (e: Exception) {
                call.reject("Error: ${e.message}")
            }
        }
    }
}

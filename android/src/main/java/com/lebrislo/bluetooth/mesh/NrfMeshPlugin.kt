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
            } finally {
                implementation.disconnectBle()
            }
        }
    }

    @PluginMethod
    fun unprovisionDevice(call: PluginCall) {
        val unicastAddress = call.getInt("unicastAddress")

        if (unicastAddress == null) {
            call.reject("unicastAddress is required")
        }

        val deferred = implementation.unprovisionDevice(unicastAddress!!)

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val result = deferred.await()
                if (result!!) {
                    call.resolve(JSObject().put("success", true))
                } else {
                    call.reject("Failed to unprovision device")
                }
            } catch (e: Exception) {
                call.reject("Error: ${e.message}")
            }
        }
    }

    @PluginMethod
    fun createApplicationKey(call: PluginCall) {
        val result = implementation.createApplicationKey()

        if (result) {
            call.resolve()
        } else {
            call.reject("Failed to add application key")
        }
    }

    @PluginMethod
    fun removeApplicationKey(call: PluginCall) {
        val appKeyIndex = call.getInt("appKeyIndex")

        if (appKeyIndex == null) {
            call.reject("appKeyIndex is required")
        }

        val result = implementation.removeApplicationKey(appKeyIndex!!)

        if (result) {
            call.resolve()
        } else {
            call.reject("Failed to remove application key")
        }
    }

    @PluginMethod
    fun addApplicationKeyToNode(call: PluginCall) {
        val unicastAddress = call.getInt("unicastAddress")
        val appKeyIndex = call.getInt("appKeyIndex")

        if (appKeyIndex == null || unicastAddress == null) {
            call.reject("appKeyIndex and unicastAddress are required")
        }

        val result = implementation.addApplicationKeyToNode(unicastAddress!!, appKeyIndex!!)

        if (result) {
            call.resolve()
        } else {
            call.reject("Failed to add application to node")
        }

//        implementation.disconnectBle()
    }

    @PluginMethod
    fun bindApplicationKeyToModel(call: PluginCall) {
        val elementAddress = call.getInt("elementAddress")
        val appKeyIndex = call.getInt("appKeyIndex")
        val modelId = call.getInt("modelId")

        if (elementAddress == null || appKeyIndex == null || modelId == null) {
            call.reject("elementAddress, appKeyIndex and modelId are required")
        }

        val result = implementation.bindApplicationKeyToModel(elementAddress!!, appKeyIndex!!, modelId!!)

        if (result) {
            call.resolve()
        } else {
            call.reject("Failed to bind application key")
        }

//        implementation.disconnectBle()
    }

    @PluginMethod
    fun compositionDataGet(call: PluginCall) {
        val unicastAddress = call.getInt("unicastAddress")

        if (unicastAddress == null) {
            call.reject("unicastAddress is required")
        }

        val deferred = implementation.compositionDataGet(unicastAddress!!)

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val result = deferred.await()
                if (result!!) {
                    val meshNetwork = implementation.exportMeshNetwork()
                    call.resolve(JSObject().put("meshNetwork", meshNetwork))
                } else {
                    call.reject("Failed to get composition data")
                }
            } catch (e: Exception) {
                call.reject("Error: ${e.message}")
            }
        }
    }

    @PluginMethod
    fun sendGenericOnOffSet(call: PluginCall) {
        val unicastAddress = call.getInt("unicastAddress")
        val appKeyIndex = call.getInt("appKeyIndex")
        val onOff = call.getBoolean("onOff")
        if (unicastAddress == null || appKeyIndex == null || onOff == null) {
            call.reject("unicastAddress, appKeyIndex, and onOff are required")
        }
        val result = implementation.sendGenericOnOffSet(
            unicastAddress!!,
            onOff!!,
            appKeyIndex!!,
            0
        )

        if (result) {
            call.resolve()
        } else {
            call.reject("Failed to send Generic OnOff Set")
        }
    }

    @PluginMethod
    fun exportMeshNetwork(call: PluginCall) {
        val result = implementation.exportMeshNetwork()

        if (result != null) {
            call.resolve(JSObject().put("meshNetwork", result))
        } else {
            call.reject("Failed to export mesh network")
        }
    }
}

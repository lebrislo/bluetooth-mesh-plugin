package com.lebrislo.bluetooth.mesh

import com.getcapacitor.JSArray
import com.getcapacitor.JSObject
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.CapacitorPlugin
import com.lebrislo.bluetooth.mesh.models.MeshDevice
import com.lebrislo.bluetooth.mesh.plugin.PluginCallManager
import com.lebrislo.bluetooth.mesh.utils.Permissions
import com.lebrislo.bluetooth.mesh.utils.Utils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import no.nordicsemi.android.mesh.MeshManagerApi
import no.nordicsemi.android.mesh.opcodes.ApplicationMessageOpCodes
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
        val scanDuration = call.getInt("timeout", 5000)

        if (!Permissions.isBleEnabled(context)) {
            call.reject("Bluetooth is disabled")
        }

        if (!Permissions.isLocationGranted(context)) {
            call.reject("Location permission is required")
        }

        CoroutineScope(Dispatchers.Main).launch {
            val devices = implementation.scanUnprovisionedDevices(scanDuration!!)

            val result = JSArray().apply {
                devices.forEach {
                    val serviceData = Utils.getServiceData(
                        it.scanResult!!,
                        MeshManagerApi.MESH_PROVISIONING_UUID
                    )
                    val uuid: UUID = implementation.meshManagerApi.getDeviceUuid(serviceData!!)

                    put(JSObject().apply {
                        put("uuid", uuid.toString())
                        put("macAddress", it.scanResult.device.address)
                        put("rssi", it.rssi)
                        put("name", it.name)
                    })
                }
            }
            call.resolve(JSObject().put("devices", result))
        }
    }

    @PluginMethod
    fun scanProvisionedDevices(call: PluginCall) {
        val scanDuration = call.getInt("timeout", 5000)

        if (!Permissions.isBleEnabled(context)) {
            call.reject("Bluetooth is disabled")
        }

        if (!Permissions.isLocationGranted(context)) {
            call.reject("Location permission is required")
        }

        CoroutineScope(Dispatchers.Main).launch {
            val devices = implementation.scanProvisionedDevices(scanDuration!!)

            val result = JSArray().apply {
                devices.forEach {
                    val serviceData = Utils.getServiceData(
                        it.scanResult!!,
                        MeshManagerApi.MESH_PROXY_UUID
                    )
                    val uuid: UUID = implementation.meshManagerApi.getDeviceUuid(serviceData!!)

                    put(JSObject().apply {
                        put("uuid", uuid.toString())
                        put("macAddress", it.scanResult.device.address)
                        put("rssi", it.rssi)
                        put("name", it.name)
                    })
                }
            }
            call.resolve(JSObject().put("devices", result))
        }
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
            return
        }

        PluginCallManager.getInstance()
            .addSigPluginCall(ApplicationMessageOpCodes.GENERIC_ON_OFF_SET, unicastAddress, call)

        val result = implementation.sendGenericOnOffSet(
            unicastAddress,
            onOff,
            appKeyIndex,
            0
        )

        if (!result) {
            call.reject("Failed to send Generic OnOff Set")
        }
    }

    @PluginMethod
    fun sendGenericPowerLevelSet(call: PluginCall) {
        val unicastAddress = call.getInt("unicastAddress")
        val appKeyIndex = call.getInt("appKeyIndex")
        val powerLevel = call.getInt("powerLevel")

        if (unicastAddress == null || appKeyIndex == null || powerLevel == null) {
            call.reject("unicastAddress, appKeyIndex, and powerLevel are required")
            return
        }

        PluginCallManager.getInstance()
            .addSigPluginCall(ApplicationMessageOpCodes.GENERIC_POWER_LEVEL_SET, unicastAddress, call)

        val result = implementation.sendGenericPowerLevelSet(
            unicastAddress,
            powerLevel,
            appKeyIndex,
            0
        )

        if (!result) {
            call.reject("Failed to send Generic Power Level Set")
        }
    }

    @PluginMethod
    fun sendLightHslSet(call: PluginCall) {
        val unicastAddress = call.getInt("unicastAddress")
        val appKeyIndex = call.getInt("appKeyIndex")
        val hue = call.getInt("hue")
        val saturation = call.getInt("saturation")
        val lightness = call.getInt("lightness")

        if (unicastAddress == null || appKeyIndex == null || hue == null || saturation == null || lightness == null) {
            call.reject("unicastAddress, appKeyIndex, hue, saturation, and lightness are required")
            return
        }

        PluginCallManager.getInstance()
            .addSigPluginCall(ApplicationMessageOpCodes.LIGHT_HSL_SET, unicastAddress, call)

        val result = implementation.sendLightHslSet(
            unicastAddress,
            hue,
            saturation,
            lightness,
            appKeyIndex,
            0
        )

        if (!result) {
            call.reject("Failed to send Light HSL Set")
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

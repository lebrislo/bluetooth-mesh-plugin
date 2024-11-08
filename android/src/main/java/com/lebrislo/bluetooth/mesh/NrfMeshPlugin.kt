package com.lebrislo.bluetooth.mesh

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothAdapter.ACTION_REQUEST_ENABLE
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.activity.result.ActivityResult
import com.getcapacitor.JSArray
import com.getcapacitor.JSObject
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.ActivityCallback
import com.getcapacitor.annotation.CapacitorPlugin
import com.lebrislo.bluetooth.mesh.models.BleMeshDevice
import com.lebrislo.bluetooth.mesh.plugin.PluginCallManager
import com.lebrislo.bluetooth.mesh.utils.BluetoothStateReceiver
import com.lebrislo.bluetooth.mesh.utils.PermissionsManager
import com.lebrislo.bluetooth.mesh.utils.Utils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import no.nordicsemi.android.mesh.MeshManagerApi
import no.nordicsemi.android.mesh.opcodes.ApplicationMessageOpCodes
import no.nordicsemi.android.mesh.opcodes.ConfigMessageOpCodes
import java.util.UUID


@CapacitorPlugin(
    name = "NrfMesh",
)
class NrfMeshPlugin : Plugin() {
    private val tag: String = NrfMeshPlugin::class.java.simpleName

    companion object {
        const val MESH_MODEL_MESSAGE_EVENT_STRING: String = "meshModelMessageEvent"
        const val BLUETOOTH_ADAPTER_EVENT_STRING: String = "bluetoothAdapterEvent"
        const val BLUETOOTH_CONNECTION_EVENT_STRING: String = "bluetoothConnectionEvent"
    }

    private lateinit var implementation: NrfMeshManager
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothStateReceiver: BroadcastReceiver

    override fun load() {
        this.implementation = NrfMeshManager(this.context)
        PluginCallManager.getInstance().setPlugin(this)
        PermissionsManager.getInstance().setActivity(activity)
        PermissionsManager.getInstance().setContext(context)

        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
    }

    override fun handleOnStart() {
        Log.d(tag, "handleOnStart")
        super.handleOnStart()
        // Register for Bluetooth state changes
        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)

        bluetoothStateReceiver = BluetoothStateReceiver(this)
        context.registerReceiver(bluetoothStateReceiver, filter)

        implementation.startScan()
    }

    override fun handleOnStop() {
        Log.d(tag, "handleOnStop")
        super.handleOnStop()
        try {
            context.unregisterReceiver(bluetoothStateReceiver)
        } catch (e: IllegalArgumentException) {
            Log.e(tag, "handleOnStop : Receiver not registered")
        }

        if (implementation.isBleConnected()) {
            CoroutineScope(Dispatchers.IO).launch {
                implementation.disconnectBle()
            }
        }

        implementation.stopScan()
    }

    override fun handleOnDestroy() {
        Log.d(tag, "handleOnDestroy")
        super.handleOnDestroy()
        try {
            context.unregisterReceiver(bluetoothStateReceiver)
        } catch (e: IllegalArgumentException) {
            Log.e(tag, "handleOnDestroy : Receiver not registered")
        }

        if (implementation.isBleConnected()) {
            CoroutineScope(Dispatchers.IO).launch {
                implementation.disconnectBle()
            }
        }
        implementation.stopScan()
    }

    private fun assertBluetoothAdapter(call: PluginCall): Boolean? {
        if (bluetoothAdapter == null) {
            call.reject("Bluetooth LE not initialized.")
            return null
        }
        return true
    }

    @PluginMethod
    fun isBluetoothEnabled(call: PluginCall) {
        assertBluetoothAdapter(call) ?: return
        return call.resolve(JSObject().put("enabled", bluetoothAdapter.isEnabled))
    }

    @PluginMethod
    fun requestBluetoothEnable(call: PluginCall) {
        assertBluetoothAdapter(call) ?: return
        val intent = Intent(ACTION_REQUEST_ENABLE)
        startActivityForResult(call, intent, "handleRequestEnableResult")
    }

    @ActivityCallback
    private fun handleRequestEnableResult(call: PluginCall, result: ActivityResult) {
        return call.resolve(JSObject().put("enabled", result.resultCode == Activity.RESULT_OK))
    }

    @PluginMethod
    fun isBluetoothConnected(call: PluginCall) {
        val connected = implementation.isBleConnected()
        implementation.connectedDevice()?.let {
            return call.resolve(JSObject().put("connected", connected).put("macAddress", it.address))
        } ?: return call.resolve(JSObject().put("connected", connected))
    }

    @PluginMethod
    fun disconnectBle(call: PluginCall) {
        if (!implementation.isBleConnected()) {
            return call.resolve()
        }
        CoroutineScope(Dispatchers.IO).launch {
            implementation.disconnectBle()
            return@launch call.resolve()
        }
    }

    @PluginMethod
    fun initMeshNetwork(call: PluginCall) {
        val networkName = call.getString("networkName") ?: return call.reject("networkName is required")

        implementation.initMeshNetwork(networkName)

        val network = implementation.exportMeshNetwork()

        return if (network != null) {
            call.resolve(JSObject().put("meshNetwork", network))
        } else {
            call.reject("Failed to initialize mesh network")
        }
    }

    @PluginMethod
    fun exportMeshNetwork(call: PluginCall) {
        val result = implementation.exportMeshNetwork()

        return if (result != null) {
            call.resolve(JSObject().put("meshNetwork", result))
        } else {
            call.reject("Failed to export mesh network")
        }
    }

    @PluginMethod
    fun importMeshNetwork(call: PluginCall) {
        val meshNetwork = call.getString("meshNetwork") ?: return call.reject("meshNetwork is required")

        implementation.importMeshNetwork(meshNetwork)

        return call.resolve()
    }

    @PluginMethod
    fun scanMeshDevices(call: PluginCall) {
        val scanDuration = call.getInt("timeout") ?: 5000

        CoroutineScope(Dispatchers.IO).launch {
            val devices = implementation.scanMeshDevices(scanDuration)

            // return a dict of devices, unprovisioned and provisioned
            val result = JSObject().apply {
                put("unprovisioned", JSArray().apply {
                    devices.forEach {
                        val serviceData = Utils.getServiceData(
                            it.scanResult!!,
                            MeshManagerApi.MESH_PROVISIONING_UUID
                        )

                        if (serviceData == null || serviceData.size < 18) return@forEach

                        val uuid: UUID = implementation.meshManagerApi.getDeviceUuid(serviceData)

                        put(JSObject().apply {
                            put("uuid", uuid.toString())
                            put("macAddress", it.scanResult.device.address)
                            put("rssi", it.rssi)
                            put("name", it.name)
                        })
                    }
                })
                put("provisioned", JSArray().apply {
                    devices.forEach {
                        val serviceData = Utils.getServiceData(
                            it.scanResult!!,
                            MeshManagerApi.MESH_PROXY_UUID
                        )

                        if (serviceData == null || serviceData.size < 18) return@forEach

                        val uuid: UUID = implementation.meshManagerApi.getDeviceUuid(serviceData)

                        put(JSObject().apply {
                            put("uuid", uuid.toString())
                            put("macAddress", it.scanResult.device.address)
                            put("rssi", it.rssi)
                            put("name", it.name)
                        })
                    }
                })
            }
            return@launch call.resolve(result)
        }
    }

    private fun connectedToUnprovisionedDestinations(destinationMacAddress: String): Boolean {
        return implementation.isBleConnected() && implementation.connectedDevice()?.address == destinationMacAddress
    }

    private suspend fun connectionToUnprovisionedDevice(
        destinationMacAddress: String,
        destinationUuid: String
    ): Boolean {
        return withContext(Dispatchers.IO) {

            if (!connectedToUnprovisionedDestinations(destinationMacAddress)) {
                if (implementation.isBleConnected()) {
                    withContext(Dispatchers.IO) {
                        implementation.disconnectBle()
                    }
                }

                val bluetoothDevice = withContext(Dispatchers.IO) {
                    implementation.searchUnprovisionedBluetoothDevice(destinationUuid)
                }

                if (bluetoothDevice == null) {
                    Log.d(tag, "connectionToUnprovisionedDevice : Failed to find unprovisioned device")
                    return@withContext false
                }

                withContext(Dispatchers.IO) {
                    implementation.connectBle(bluetoothDevice)
                }
            }
            return@withContext true
        }
    }

    private suspend fun connectionToProvisionedDevice(
    ): Boolean {
        return withContext(Dispatchers.IO) {
            var proxy: BluetoothDevice? = null
            val timeoutMillis = 5000L // 5 seconds timeout
            val startTime = System.currentTimeMillis()

            while (System.currentTimeMillis() - startTime < timeoutMillis) {
                // Attempt to find the proxy
                proxy = withContext(Dispatchers.IO) {
                    implementation.searchProxyMesh()
                }

                // If proxy is found, break out of the loop
                if (proxy != null) {
                    break
                }

                // Wait for 1 second before retrying
                delay(1000L)
            }

            if (proxy == null) {
                Log.d(tag, "connectionToProvisionedDevice : Failed to find proxy node")
                return@withContext false
            }

            withContext(Dispatchers.IO) {
                implementation.connectBle(proxy)
            }
            return@withContext true
        }
    }


    @PluginMethod
    fun getProvisioningCapabilities(call: PluginCall) {
        val macAddress = call.getString("macAddress") ?: return call.reject("macAddress is required")
        val uuid = call.getString("uuid") ?: return call.reject("uuid is required")

        CoroutineScope(Dispatchers.Main).launch {
            val connected = connectionToUnprovisionedDevice(macAddress, uuid)
            if (!connected) {
                return@launch call.reject("Failed to connect to device : $macAddress $uuid")
            }

            val deferred = implementation.getProvisioningCapabilities(UUID.fromString(uuid))

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
                return@launch call.resolve(result)
            } else {
                return@launch call.reject("Failed to get provisioning capabilities")
            }
        }
    }

    @PluginMethod
    fun provisionDevice(call: PluginCall) {
        val macAddress = call.getString("macAddress") ?: return call.reject("macAddress is required")
        val uuid = call.getString("uuid") ?: return call.reject("uuid is required")

        CoroutineScope(Dispatchers.Main).launch {
            val connected = connectionToUnprovisionedDevice(macAddress, uuid)
            if (!connected) {
                return@launch call.reject("Failed to connect to device : $macAddress $uuid")
            }

            val deferred = implementation.provisionDevice(UUID.fromString(uuid))

            val meshDevice = deferred.await() ?: return@launch call.reject("Failed to provision device")

            withContext(Dispatchers.IO) {
                implementation.disconnectBle()
            }

            when (meshDevice) {
                is BleMeshDevice.Provisioned -> {
                    val result = JSObject().apply {
                        put("provisioningComplete", true)
                        put("uuid", meshDevice.node.uuid)
                        put("unicastAddress", meshDevice.node.unicastAddress)
                    }
                    return@launch call.resolve(result)
                }

                is BleMeshDevice.Unprovisioned -> {
                    val result = JSObject().apply {
                        put("provisioningComplete", false)
                        put("uuid", meshDevice.node.deviceUuid)
                    }
                    return@launch call.resolve(result)
                }
            }
        }
    }

    @PluginMethod
    fun unprovisionDevice(call: PluginCall) {
        val unicastAddress = call.getInt("unicastAddress") ?: return call.reject("unicastAddress is required")

        CoroutineScope(Dispatchers.Main).launch {
            val connected = connectionToProvisionedDevice()
            if (!connected) {
                return@launch call.reject("Failed to connect to Mesh proxy")
            }

            PluginCallManager.getInstance()
                .addConfigPluginCall(ConfigMessageOpCodes.CONFIG_NODE_RESET, unicastAddress, call)

            implementation.unprovisionDevice(unicastAddress)
        }
    }

    @PluginMethod
    fun createApplicationKey(call: PluginCall) {
        val result = implementation.createApplicationKey()

        return if (result) {
            call.resolve()
        } else {
            call.reject("Failed to add application key")
        }
    }

    @PluginMethod
    fun removeApplicationKey(call: PluginCall) {
        val appKeyIndex = call.getInt("appKeyIndex") ?: return call.reject("appKeyIndex is required")

        val result = implementation.removeApplicationKey(appKeyIndex)

        return if (result) {
            call.resolve()
        } else {
            call.reject("Failed to remove application key")
        }
    }

    @PluginMethod
    fun addApplicationKeyToNode(call: PluginCall) {
        val unicastAddress = call.getInt("unicastAddress") ?: return call.reject("unicastAddress is required")
        val appKeyIndex = call.getInt("appKeyIndex") ?: return call.reject("appKeyIndex is required")
        val acknowledgement = call.getBoolean("acknowledgement") ?: false

        CoroutineScope(Dispatchers.Main).launch {
            val connected = connectionToProvisionedDevice()
            if (!connected) {
                return@launch call.reject("Failed to connect to Mesh proxy")
            }

            if (acknowledgement) {
                PluginCallManager.getInstance()
                    .addConfigPluginCall(ConfigMessageOpCodes.CONFIG_APPKEY_ADD, unicastAddress, call)
            }

            val result = implementation.addApplicationKeyToNode(unicastAddress, appKeyIndex)

            if (!result) return@launch call.reject("Failed to add application key to node")
            if (!acknowledgement) return@launch call.resolve()
        }
    }

    @PluginMethod
    fun bindApplicationKeyToModel(call: PluginCall) {
        val unicastAddress = call.getInt("unicastAddress") ?: return call.reject("unicastAddress is required")
        val appKeyIndex = call.getInt("appKeyIndex") ?: return call.reject("appKeyIndex is required")
        val modelId = call.getInt("modelId") ?: return call.reject("modelId is required")
        val acknowledgement = call.getBoolean("acknowledgement") ?: false

        CoroutineScope(Dispatchers.Main).launch {
            val connected = connectionToProvisionedDevice()
            if (!connected) {
                return@launch call.reject("Failed to connect to Mesh proxy")
            }

            if (acknowledgement) {
                PluginCallManager.getInstance()
                    .addConfigPluginCall(ConfigMessageOpCodes.CONFIG_MODEL_APP_BIND, unicastAddress, call)
            }

            val result = implementation.bindApplicationKeyToModel(unicastAddress, appKeyIndex, modelId)

            if (!result) return@launch call.reject("Failed to bind application key")
            if (!acknowledgement) return@launch call.resolve()
        }
    }

    @PluginMethod
    fun getCompositionData(call: PluginCall) {
        val unicastAddress = call.getInt("unicastAddress") ?: return call.reject("unicastAddress is required")

        CoroutineScope(Dispatchers.Main).launch {
            val connected = connectionToProvisionedDevice()
            if (!connected) {
                call.reject("Failed to connect to Mesh proxy")
                return@launch
            }

            PluginCallManager.getInstance()
                .addConfigPluginCall(ConfigMessageOpCodes.CONFIG_COMPOSITION_DATA_GET, unicastAddress, call)

            val result = implementation.compositionDataGet(unicastAddress)

            if (!result) call.reject("Failed to get composition data")
        }
    }

    @PluginMethod
    fun sendGenericOnOffSet(call: PluginCall) {
        val unicastAddress = call.getInt("unicastAddress") ?: return call.reject("unicastAddress is required")
        val appKeyIndex = call.getInt("appKeyIndex") ?: return call.reject("appKeyIndex is required")
        val onOff = call.getBoolean("onOff") ?: return call.reject("onOff is required")
        val acknowledgement = call.getBoolean("acknowledgement") ?: false

        CoroutineScope(Dispatchers.Main).launch {
            val connected = connectionToProvisionedDevice()
            if (!connected) {
                return@launch call.reject("Failed to connect to Mesh proxy")
            }

            if (acknowledgement) {
                PluginCallManager.getInstance()
                    .addSigPluginCall(ApplicationMessageOpCodes.GENERIC_ON_OFF_SET, unicastAddress, call)
            }

            val result = implementation.sendGenericOnOffSet(
                unicastAddress,
                appKeyIndex,
                onOff
            )

            if (!result) return@launch call.reject("Failed to send Generic OnOff Set")
            if (!acknowledgement) return@launch call.resolve()
        }
    }

    @PluginMethod
    fun sendGenericOnOffGet(call: PluginCall) {
        val unicastAddress = call.getInt("unicastAddress") ?: return call.reject("unicastAddress is required")
        val appKeyIndex = call.getInt("appKeyIndex") ?: return call.reject("appKeyIndex is required")

        CoroutineScope(Dispatchers.Main).launch {
            val connected = connectionToProvisionedDevice()
            if (!connected) {
                return@launch call.reject("Failed to connect to Mesh proxy")

            }

            PluginCallManager.getInstance()
                .addSigPluginCall(ApplicationMessageOpCodes.GENERIC_ON_OFF_GET, unicastAddress, call)

            val result = implementation.sendGenericOnOffGet(
                unicastAddress,
                appKeyIndex,
            )

            if (!result) return@launch call.reject("Failed to send Generic OnOff Get")
        }
    }

    @PluginMethod
    fun sendGenericPowerLevelSet(call: PluginCall) {
        val unicastAddress = call.getInt("unicastAddress") ?: return call.reject("unicastAddress is required")
        val appKeyIndex = call.getInt("appKeyIndex") ?: return call.reject("appKeyIndex is required")
        val powerLevel = call.getInt("powerLevel") ?: return call.reject("powerLevel is required")
        val acknowledgement = call.getBoolean("acknowledgement") ?: false

        CoroutineScope(Dispatchers.Main).launch {
            val connected = connectionToProvisionedDevice()
            if (!connected) {
                return@launch call.reject("Failed to connect to Mesh proxy")
            }

            if (acknowledgement) {
                PluginCallManager.getInstance()
                    .addSigPluginCall(ApplicationMessageOpCodes.GENERIC_POWER_LEVEL_SET, unicastAddress, call)
            }

            val result = implementation.sendGenericPowerLevelSet(
                unicastAddress,
                appKeyIndex,
                powerLevel
            )

            if (!result) return@launch call.reject("Failed to send Generic Power Level Set")
            if (!acknowledgement) return@launch call.resolve()
        }
    }

    @PluginMethod
    fun sendGenericPowerLevelGet(call: PluginCall) {
        val unicastAddress = call.getInt("unicastAddress") ?: return call.reject("unicastAddress is required")
        val appKeyIndex = call.getInt("appKeyIndex") ?: return call.reject("appKeyIndex is required")

        CoroutineScope(Dispatchers.Main).launch {
            val connected = connectionToProvisionedDevice()
            if (!connected) {
                return@launch call.reject("Failed to connect to Mesh proxy")
            }

            PluginCallManager.getInstance()
                .addSigPluginCall(ApplicationMessageOpCodes.GENERIC_POWER_LEVEL_GET, unicastAddress, call)

            val result = implementation.sendGenericPowerLevelGet(
                unicastAddress,
                appKeyIndex,
            )

            if (!result) return@launch call.reject("Failed to send Generic Power Level Get")
        }
    }

    @PluginMethod
    fun sendLightHslSet(call: PluginCall) {
        val unicastAddress = call.getInt("unicastAddress") ?: return call.reject("unicastAddress is required")
        val appKeyIndex = call.getInt("appKeyIndex") ?: return call.reject("appKeyIndex is required")
        val hue = call.getInt("hue") ?: return call.reject("hue is required")
        val saturation = call.getInt("saturation") ?: return call.reject("saturation is required")
        val lightness = call.getInt("lightness") ?: return call.reject("lightness is required")
        val acknowledgement = call.getBoolean("acknowledgement") ?: false

        CoroutineScope(Dispatchers.Main).launch {
            val connected = connectionToProvisionedDevice()
            if (!connected) {
                return@launch call.reject("Failed to connect to Mesh proxy")
            }

            if (acknowledgement) {
                PluginCallManager.getInstance()
                    .addSigPluginCall(ApplicationMessageOpCodes.LIGHT_HSL_SET, unicastAddress, call)
            }

            val result = implementation.sendLightHslSet(
                unicastAddress,
                appKeyIndex,
                hue,
                saturation,
                lightness
            )

            if (!result) return@launch call.reject("Failed to send Light HSL Set")
            if (!acknowledgement) return@launch call.resolve()
        }
    }

    @PluginMethod
    fun sendLightHslGet(call: PluginCall) {
        val unicastAddress = call.getInt("unicastAddress") ?: return call.reject("unicastAddress is required")
        val appKeyIndex = call.getInt("appKeyIndex") ?: return call.reject("appKeyIndex is required")

        CoroutineScope(Dispatchers.Main).launch {
            val connected = connectionToProvisionedDevice()
            if (!connected) {
                return@launch call.reject("Failed to connect to Mesh proxy")
            }

            PluginCallManager.getInstance()
                .addSigPluginCall(ApplicationMessageOpCodes.LIGHT_HSL_GET, unicastAddress, call)

            val result = implementation.sendLightHslGet(
                unicastAddress,
                appKeyIndex,
            )

            if (!result) return@launch call.reject("Failed to send Light HSL Get")
        }
    }

    @PluginMethod
    fun sendLightCtlSet(call: PluginCall) {
        val unicastAddress = call.getInt("unicastAddress") ?: return call.reject("unicastAddress is required")
        val appKeyIndex = call.getInt("appKeyIndex") ?: return call.reject("appKeyIndex is required")
        val lightness = call.getInt("lightness") ?: return call.reject("lightness is required")
        val temperature = call.getInt("temperature") ?: return call.reject("temperature is required")
        val deltaUv = call.getInt("deltaUv") ?: return call.reject("deltaUv is required")
        val acknowledgement = call.getBoolean("acknowledgement") ?: false

        CoroutineScope(Dispatchers.Main).launch {
            val connected = connectionToProvisionedDevice()
            if (!connected) {
                return@launch call.reject("Failed to connect to Mesh proxy")
            }

            if (acknowledgement) {
                PluginCallManager.getInstance()
                    .addSigPluginCall(ApplicationMessageOpCodes.LIGHT_CTL_SET, unicastAddress, call)
            }

            val result = implementation.sendLightCtlSet(
                unicastAddress,
                appKeyIndex,
                lightness,
                temperature,
                deltaUv
            )

            if (!result) return@launch call.reject("Failed to send Light CTL Set")
            if (!acknowledgement) return@launch call.resolve()
        }
    }

    @PluginMethod
    fun sendLightCtlGet(call: PluginCall) {
        val unicastAddress = call.getInt("unicastAddress") ?: return call.reject("unicastAddress is required")
        val appKeyIndex = call.getInt("appKeyIndex") ?: return call.reject("appKeyIndex is required")

        CoroutineScope(Dispatchers.Main).launch {
            val connected = connectionToProvisionedDevice()
            if (!connected) {
                return@launch call.reject("Failed to connect to Mesh proxy")
            }

            PluginCallManager.getInstance()
                .addSigPluginCall(ApplicationMessageOpCodes.LIGHT_CTL_GET, unicastAddress, call)

            val result = implementation.sendLightCtlGet(
                unicastAddress,
                appKeyIndex,
            )

            if (!result) return@launch call.reject("Failed to send Light CTL Get")
        }
    }

    @PluginMethod
    fun sendLightCtlTemperatureRangeSet(call: PluginCall) {
        val unicastAddress = call.getInt("unicastAddress") ?: return call.reject("unicastAddress is required")
        val appKeyIndex = call.getInt("appKeyIndex") ?: return call.reject("appKeyIndex is required")
        val rangeMin = call.getInt("rangeMin") ?: return call.reject("rangeMin is required")
        val rangeMax = call.getInt("rangeMax") ?: return call.reject("rangeMax is required")
        val acknowledgement = call.getBoolean("acknowledgement") ?: false

        CoroutineScope(Dispatchers.Main).launch {
            val connected = connectionToProvisionedDevice()
            if (!connected) {
                return@launch call.reject("Failed to connect to Mesh proxy")
            }

            if (acknowledgement) {
                PluginCallManager.getInstance()
                    .addSigPluginCall(ApplicationMessageOpCodes.LIGHT_CTL_TEMPERATURE_RANGE_SET, unicastAddress, call)
            }

            val result = implementation.sendLightCtlTemperatureRangeSet(
                unicastAddress,
                appKeyIndex,
                rangeMin,
                rangeMax
            )

            if (!result) return@launch call.reject("Failed to send Light CTL Temperature Range Set")
            if (!acknowledgement) return@launch call.resolve()
        }
    }

    @PluginMethod
    fun sendLightCtlTemperatureRangeGet(call: PluginCall) {
        val unicastAddress = call.getInt("unicastAddress") ?: return call.reject("unicastAddress is required")
        val appKeyIndex = call.getInt("appKeyIndex") ?: return call.reject("appKeyIndex is required")

        CoroutineScope(Dispatchers.Main).launch {
            val connected = connectionToProvisionedDevice()
            if (!connected) {
                return@launch call.reject("Failed to connect to Mesh proxy")
            }

            PluginCallManager.getInstance()
                .addSigPluginCall(ApplicationMessageOpCodes.LIGHT_CTL_TEMPERATURE_RANGE_GET, unicastAddress, call)

            val result = implementation.sendLightCtlTemperatureRangeGet(
                unicastAddress,
                appKeyIndex,
            )

            if (!result) return@launch call.reject("Failed to send Light CTL Get")
        }
    }

    @PluginMethod
    fun sendVendorModelMessage(call: PluginCall) {
        val unicastAddress = call.getInt("unicastAddress") ?: return call.reject("unicastAddress is required")
        val appKeyIndex = call.getInt("appKeyIndex") ?: return call.reject("appKeyIndex is required")
        val modelId = call.getInt("modelId") ?: return call.reject("modelId is required")
        val opcode = call.getInt("opcode") ?: return call.reject("opcode is required")
        val payload = call.getObject("payload") ?: JSObject()
        val opPairCode = call.getInt("opPairCode")
        val companyIdentifier = modelId.shr(16)

        var payloadData = byteArrayOf()
        // Convert the payload object into a ByteArray
        payloadData = payload.keys()
            .asSequence()
            .mapNotNull { key -> payload.getInt(key) } // Convert each value to an Int, ignoring nulls
            .map { it.toByte() } // Convert each Int to a Byte
            .toList()
            .toByteArray()


        if (opPairCode != null) {
            PluginCallManager.getInstance()
                .addVendorPluginCall(modelId, opcode, opPairCode, unicastAddress, call)
        }

        CoroutineScope(Dispatchers.Main).launch {
            val connected = connectionToProvisionedDevice()
            if (!connected) {
                return@launch call.reject("Failed to connect to Mesh proxy")
            }

            val result = implementation.sendVendorModelMessage(
                unicastAddress,
                appKeyIndex,
                modelId,
                companyIdentifier,
                opcode,
                payloadData,
            )

            if (!result) return@launch call.reject("Failed to send Vendor Model Message")
            if (opPairCode == null) return@launch call.resolve()
        }
    }

    fun sendNotification(eventName: String, data: JSObject) {
        if (!hasListeners(eventName)) {
            return
        }
        notifyListeners(eventName, data)
    }
}

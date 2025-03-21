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
import com.lebrislo.bluetooth.mesh.ble.BleMeshManager
import com.lebrislo.bluetooth.mesh.models.BleMeshDevice
import com.lebrislo.bluetooth.mesh.permissions.PermissionsManager
import com.lebrislo.bluetooth.mesh.plugin.PluginCallManager
import com.lebrislo.bluetooth.mesh.utils.BluetoothStateReceiver
import com.lebrislo.bluetooth.mesh.utils.NodesOnlineStateManager
import com.lebrislo.bluetooth.mesh.utils.NotificationManager
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
    name = "BluetoothMesh",
)
class BluetoothMeshPlugin : Plugin() {
    private val tag: String = BluetoothMeshPlugin::class.java.simpleName

    companion object {
        const val MESH_MODEL_MESSAGE_EVENT_STRING: String = "meshModelMessageEvent"
        const val BLUETOOTH_ADAPTER_EVENT_STRING: String = "bluetoothAdapterEvent"
        const val BLUETOOTH_CONNECTION_EVENT_STRING: String = "bluetoothConnectionEvent"
        const val MESH_DEVICE_SCAN_EVENT: String = "meshDeviceScanEvent"
        const val MESH_NODE_ONLINE_STATE_EVENT: String = "meshNodeOnlineStateEvent"
    }

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothStateReceiver: BroadcastReceiver

    private lateinit var bleController: BleController
    private lateinit var meshController: MeshController

    override fun load() {
        NotificationManager.getInstance().setPlugin(this)
        PermissionsManager.getInstance().setActivity(activity)
        PermissionsManager.getInstance().setContext(context)

        val meshApiManager = MeshManagerApi(context)
        val bleMeshManager = BleMeshManager(context)
        bleController = BleController(bleMeshManager, meshApiManager)
        meshController = MeshController(bleMeshManager, meshApiManager)

        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        Log.d(tag, "Bluetooth Adapter enabled : ${bluetoothAdapter.isEnabled}")
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

        this.restartScan()
        CoroutineScope(Dispatchers.Main).launch {
            if (!assertBluetoothEnabled(null)) return@launch
            connectionToProvisionedDevice()
        }
        NodesOnlineStateManager.getInstance().resetStatus()
        NodesOnlineStateManager.getInstance().startMonitoring()
    }

    override fun handleOnStop() {
        Log.d(tag, "handleOnStop")
        super.handleOnStop()

        try {
            context.unregisterReceiver(bluetoothStateReceiver)
        } catch (e: IllegalArgumentException) {
            Log.e(tag, "handleOnStop : Receiver not registered")
        }

        this.stopScan()
        NodesOnlineStateManager.getInstance().stopMonitoring()

        if (bleController.isBleConnected()) {
            CoroutineScope(Dispatchers.IO).launch {
                bleController.disconnectBle()
            }
        }
    }

    override fun handleOnDestroy() {
        Log.d(tag, "handleOnDestroy")
        super.handleOnDestroy()
        try {
            context.unregisterReceiver(bluetoothStateReceiver)
        } catch (e: IllegalArgumentException) {
            Log.e(tag, "handleOnDestroy : Receiver not registered")
        }

        if (bleController.isBleConnected()) {
            CoroutineScope(Dispatchers.IO).launch {
                bleController.disconnectBle()
            }
        }
        this.stopScan()
    }

    override fun startActivityForResult(call: PluginCall?, intent: Intent?, callbackName: String?) {
        super.startActivityForResult(call, intent, callbackName)
    }

    private fun assertBluetoothAdapter(call: PluginCall?): Boolean {
        if (bluetoothAdapter == null) {
            call?.reject("Bluetooth adapter is not available")
            return false
        }
        return true
    }

    private fun assertBluetoothEnabled(call: PluginCall?): Boolean {
        if (!assertBluetoothAdapter(call)) return false
        if (!bluetoothAdapter.isEnabled) {
            call?.reject("Bluetooth is not enabled")
            return false
        }
        return true
    }

    fun restartScan() {
        if (!assertBluetoothEnabled(null)) return
        bleController.restartMeshDevicesScan()
    }

    fun stopScan() {
        if (!assertBluetoothEnabled(null)) return
        bleController.stopScan()
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
        if (!assertBluetoothEnabled(null)) return
        val connected = bleController.isBleConnected()
        bleController.connectedDevice()?.let {
            return call.resolve(JSObject().put("connected", connected).put("macAddress", it.address))
        } ?: return call.resolve(JSObject().put("connected", connected))
    }

    @PluginMethod
    fun disconnectBle(call: PluginCall) {
        val autoReconnect = call.getBoolean("autoReconnect") ?: true
        if (!assertBluetoothEnabled(null)) return
        if (!bleController.isBleConnected()) {
            return call.resolve()
        }
        CoroutineScope(Dispatchers.IO).launch {
            bleController.disconnectBle(autoReconnect)
            return@launch call.resolve()
        }
    }

    @PluginMethod
    fun initMeshNetwork(call: PluginCall) {
        val networkName = call.getString("networkName") ?: return call.reject("networkName is required")

        meshController.initMeshNetwork(networkName)

        val network = meshController.exportMeshNetwork()

        return if (network != null) {
            call.resolve(JSObject().put("meshNetwork", network))
        } else {
            call.reject("Failed to initialize mesh network")
        }
    }

    @PluginMethod
    fun exportMeshNetwork(call: PluginCall) {
        val result = meshController.exportMeshNetwork()

        return if (result != null) {
            call.resolve(JSObject().put("meshNetwork", result))
        } else {
            call.reject("Failed to export mesh network")
        }
    }

    @PluginMethod
    fun importMeshNetwork(call: PluginCall) {
        val meshNetwork = call.getString("meshNetwork") ?: return call.reject("meshNetwork is required")

        meshController.importMeshNetwork(meshNetwork)

        bleController.restartMeshDevicesScan()

        return call.resolve()
    }

    @PluginMethod
    fun fetchMeshDevices(call: PluginCall) {

        val unprovisionedDevices = bleController.getUnprovisionedDevices()
        val provisionedDevices = bleController.getProvisionedDevices()
        // return a dict of devices, unprovisioned and provisioned
        val result = JSObject().apply {
            put("unprovisioned", JSArray().apply {
                unprovisionedDevices.forEach {
                    if (it.scanResult == null) return

                    put(JSObject().apply {
                        put("uuid", it.getDeviceUuid().toString())
                        put("macAddress", it.scanResult.device.address)
                        put("rssi", it.rssi)
                        put("name", it.name)
                    })
                }
            })
            put("provisioned", JSArray().apply {
                provisionedDevices.forEach {
                    if (it.scanResult == null) return

                    put(JSObject().apply {
                        put("uuid", it.getDeviceUuid().toString())
                        put("macAddress", it.scanResult.device.address)
                        put("rssi", it.rssi)
                        put("name", it.name)
                    })
                }
            })
        }
        return call.resolve(result)
    }

    @PluginMethod
    fun reloadScanMeshDevices(call: PluginCall) {
        if (!assertBluetoothEnabled(call)) return
        bleController.restartMeshDevicesScan()
        return call.resolve()
    }

    @PluginMethod
    fun getNodesOnlineStates(call: PluginCall) {
        val nodesOnlineStates = NodesOnlineStateManager.getInstance().getNodesOnlineStates()

        return call.resolve(nodesOnlineStates)
    }

    private fun connectedToUnprovisionedDestinations(destinationMacAddress: String): Boolean {
        return bleController.isBleConnected() && bleController.connectedDevice()?.address == destinationMacAddress
    }

    private suspend fun connectionToUnprovisionedDevice(
        destinationMacAddress: String,
        destinationUuid: String
    ): Boolean {
        return withContext(Dispatchers.IO) {

            if (!connectedToUnprovisionedDestinations(destinationMacAddress)) {
                if (bleController.isBleConnected()) {
                    withContext(Dispatchers.IO) {
                        bleController.disconnectBle(false)
                    }
                }

                val bluetoothDevice = withContext(Dispatchers.IO) {
                    bleController.searchUnprovisionedBluetoothDevice(destinationUuid)
                }

                if (bluetoothDevice == null) {
                    Log.d(tag, "connectionToUnprovisionedDevice : Failed to find unprovisioned device")
                    bleController.restartMeshDevicesScan()
                    return@withContext false
                }

                withContext(Dispatchers.IO) {
                    bleController.connectBle(bluetoothDevice, false)
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
                    bleController.searchProxyMesh()
                }

                // If proxy is found, break out of the loop
                if (proxy != null) {
                    break
                }

                // Wait for 1 second before retrying
                delay(1000L)
            }

            if (proxy == null) {
                Log.e(tag, "connectionToProvisionedDevice : Failed to find proxy node")
                bleController.restartMeshDevicesScan()
                return@withContext false
            }

            withContext(Dispatchers.IO) {
                bleController.connectBle(proxy)
            }
            return@withContext true
        }
    }


    @PluginMethod
    fun getProvisioningCapabilities(call: PluginCall) {
        val macAddress = call.getString("macAddress") ?: return call.reject("macAddress is required")
        val uuid = call.getString("uuid") ?: return call.reject("uuid is required")

        CoroutineScope(Dispatchers.Main).launch {
            if (!assertBluetoothEnabled(call)) return@launch
            val connected = connectionToUnprovisionedDevice(macAddress, uuid)
            if (!connected) {
                return@launch call.reject("Failed to connect to device : $macAddress $uuid")
            }

            val deferred = meshController.getProvisioningCapabilities(UUID.fromString(uuid))

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
            if (!assertBluetoothEnabled(call)) return@launch
            val connected = connectionToUnprovisionedDevice(macAddress, uuid)
            if (!connected) {
                return@launch call.reject("Failed to connect to device : $macAddress $uuid")
            }

            val deferred = meshController.provisionDevice(UUID.fromString(uuid))

            val meshDevice = deferred.await() ?: return@launch call.reject("Failed to provision device")

            withContext(Dispatchers.IO) {
                bleController.disconnectBle(false)
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
            if (!assertBluetoothEnabled(call)) return@launch
            val connected = connectionToProvisionedDevice()
            if (!connected) {
                return@launch call.reject("Failed to connect to Mesh proxy")
            }

            PluginCallManager.getInstance()
                .addConfigPluginCall(ConfigMessageOpCodes.CONFIG_NODE_RESET, unicastAddress, call)

            meshController.unprovisionDevice(unicastAddress)
        }
    }

    @PluginMethod
    fun createApplicationKey(call: PluginCall) {
        val result = meshController.createApplicationKey()

        return if (result) {
            call.resolve()
        } else {
            call.reject("Failed to add application key")
        }
    }

    @PluginMethod
    fun removeApplicationKey(call: PluginCall) {
        val appKeyIndex = call.getInt("appKeyIndex") ?: return call.reject("appKeyIndex is required")

        val result = meshController.removeApplicationKey(appKeyIndex)

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
            if (!assertBluetoothEnabled(call)) return@launch
            val connected = connectionToProvisionedDevice()
            if (!connected) {
                return@launch call.reject("Failed to connect to Mesh proxy")
            }

            if (acknowledgement) {
                PluginCallManager.getInstance()
                    .addConfigPluginCall(ConfigMessageOpCodes.CONFIG_APPKEY_ADD, unicastAddress, call)
            }

            val result = meshController.addApplicationKeyToNode(unicastAddress, appKeyIndex)

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
            if (!assertBluetoothEnabled(call)) return@launch
            val connected = connectionToProvisionedDevice()
            if (!connected) {
                return@launch call.reject("Failed to connect to Mesh proxy")
            }

            if (acknowledgement) {
                PluginCallManager.getInstance()
                    .addConfigPluginCall(ConfigMessageOpCodes.CONFIG_MODEL_APP_BIND, unicastAddress, call)
            }

            val result = meshController.bindApplicationKeyToModel(unicastAddress, appKeyIndex, modelId)

            if (!result) return@launch call.reject("Failed to bind application key")
            if (!acknowledgement) return@launch call.resolve()
        }
    }

    @PluginMethod
    fun getCompositionData(call: PluginCall) {
        val unicastAddress = call.getInt("unicastAddress") ?: return call.reject("unicastAddress is required")

        CoroutineScope(Dispatchers.Main).launch {
            if (!assertBluetoothEnabled(call)) return@launch
            val connected = connectionToProvisionedDevice()
            if (!connected) {
                call.reject("Failed to connect to Mesh proxy")
                return@launch
            }

            PluginCallManager.getInstance()
                .addConfigPluginCall(ConfigMessageOpCodes.CONFIG_COMPOSITION_DATA_GET, unicastAddress, call)

            val result = meshController.compositionDataGet(unicastAddress)

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
            if (!assertBluetoothEnabled(call)) return@launch
            val connected = connectionToProvisionedDevice()
            if (!connected) {
                return@launch call.reject("Failed to connect to Mesh proxy")
            }

            if (acknowledgement) {
                PluginCallManager.getInstance()
                    .addSigPluginCall(ApplicationMessageOpCodes.GENERIC_ON_OFF_SET, unicastAddress, call)
            }

            val result = meshController.sendGenericOnOffSet(
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
            if (!assertBluetoothEnabled(call)) return@launch
            val connected = connectionToProvisionedDevice()
            if (!connected) {
                return@launch call.reject("Failed to connect to Mesh proxy")

            }

            PluginCallManager.getInstance()
                .addSigPluginCall(ApplicationMessageOpCodes.GENERIC_ON_OFF_GET, unicastAddress, call)

            val result = meshController.sendGenericOnOffGet(
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
            if (!assertBluetoothEnabled(call)) return@launch
            val connected = connectionToProvisionedDevice()
            if (!connected) {
                return@launch call.reject("Failed to connect to Mesh proxy")
            }

            if (acknowledgement) {
                PluginCallManager.getInstance()
                    .addSigPluginCall(ApplicationMessageOpCodes.GENERIC_POWER_LEVEL_SET, unicastAddress, call)
            }

            val result = meshController.sendGenericPowerLevelSet(
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
            if (!assertBluetoothEnabled(call)) return@launch
            val connected = connectionToProvisionedDevice()
            if (!connected) {
                return@launch call.reject("Failed to connect to Mesh proxy")
            }

            PluginCallManager.getInstance()
                .addSigPluginCall(ApplicationMessageOpCodes.GENERIC_POWER_LEVEL_GET, unicastAddress, call)

            val result = meshController.sendGenericPowerLevelGet(
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
            if (!assertBluetoothEnabled(call)) return@launch
            val connected = connectionToProvisionedDevice()
            if (!connected) {
                return@launch call.reject("Failed to connect to Mesh proxy")
            }

            if (acknowledgement) {
                PluginCallManager.getInstance()
                    .addSigPluginCall(ApplicationMessageOpCodes.LIGHT_HSL_SET, unicastAddress, call)
            }

            val result = meshController.sendLightHslSet(
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
            if (!assertBluetoothEnabled(call)) return@launch
            val connected = connectionToProvisionedDevice()
            if (!connected) {
                return@launch call.reject("Failed to connect to Mesh proxy")
            }

            PluginCallManager.getInstance()
                .addSigPluginCall(ApplicationMessageOpCodes.LIGHT_HSL_GET, unicastAddress, call)

            val result = meshController.sendLightHslGet(
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
            if (!assertBluetoothEnabled(call)) return@launch
            val connected = connectionToProvisionedDevice()
            if (!connected) {
                return@launch call.reject("Failed to connect to Mesh proxy")
            }

            if (acknowledgement) {
                PluginCallManager.getInstance()
                    .addSigPluginCall(ApplicationMessageOpCodes.LIGHT_CTL_SET, unicastAddress, call)
            }

            val result = meshController.sendLightCtlSet(
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
            if (!assertBluetoothEnabled(call)) return@launch
            val connected = connectionToProvisionedDevice()
            if (!connected) {
                return@launch call.reject("Failed to connect to Mesh proxy")
            }

            PluginCallManager.getInstance()
                .addSigPluginCall(ApplicationMessageOpCodes.LIGHT_CTL_GET, unicastAddress, call)

            val result = meshController.sendLightCtlGet(
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
            if (!assertBluetoothEnabled(call)) return@launch
            val connected = connectionToProvisionedDevice()
            if (!connected) {
                return@launch call.reject("Failed to connect to Mesh proxy")
            }

            if (acknowledgement) {
                PluginCallManager.getInstance()
                    .addSigPluginCall(ApplicationMessageOpCodes.LIGHT_CTL_TEMPERATURE_RANGE_SET, unicastAddress, call)
            }

            val result = meshController.sendLightCtlTemperatureRangeSet(
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
            if (!assertBluetoothEnabled(call)) return@launch
            val connected = connectionToProvisionedDevice()
            if (!connected) {
                return@launch call.reject("Failed to connect to Mesh proxy")
            }

            PluginCallManager.getInstance()
                .addSigPluginCall(ApplicationMessageOpCodes.LIGHT_CTL_TEMPERATURE_RANGE_GET, unicastAddress, call)

            val result = meshController.sendLightCtlTemperatureRangeGet(
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

        // Convert the payload object into a ByteArray
        val payloadData: ByteArray = payload.keys()
            .asSequence()
            .mapNotNull { key -> payload.getInt(key) } // Convert each value to an Int, ignoring nulls
            .map { it.toByte() } // Convert each Int to a Byte
            .toList()
            .toByteArray()

        CoroutineScope(Dispatchers.Main).launch {
            if (!assertBluetoothEnabled(call)) return@launch
            val connected = connectionToProvisionedDevice()
            if (!connected) {
                return@launch call.reject("Failed to connect to Mesh proxy")
            }

            if (opPairCode != null) {
                PluginCallManager.getInstance()
                    .addVendorPluginCall(modelId, opcode, opPairCode, unicastAddress, call)
            }

            val result = meshController.sendVendorModelMessage(
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

    @PluginMethod
    fun sendConfigHeartbeatPublicationSet(call: PluginCall) {
        val unicastAddress = call.getInt("unicastAddress") ?: return call.reject("destination is required")
        val destinationAddress =
            call.getInt("destinationAddress") ?: return call.reject("destinationAddress is required")
        val count = call.getInt("count") ?: return call.reject("count is required")
        val period = call.getInt("period") ?: return call.reject("period is required")
        val ttl = call.getInt("ttl") ?: return call.reject("ttl is required")
        val netKeyIndex = call.getInt("netKeyIndex") ?: return call.reject("netKeyIndex is required")

        CoroutineScope(Dispatchers.Main).launch {
            if (!assertBluetoothEnabled(call)) return@launch
            val connected = connectionToProvisionedDevice()
            if (!connected) {
                return@launch call.reject("Failed to connect to Mesh proxy")
            }

            val result = meshController.sendConfigHeartbeatPublicationSet(
                unicastAddress,
                destinationAddress,
                count,
                period,
                ttl,
                netKeyIndex
            )

            if (!result) return@launch call.reject("Failed to send Heartbeat Publication")
        }
    }

    @PluginMethod
    fun sendHealthFaultGet(call: PluginCall) {
        val unicastAddress = call.getInt("unicastAddress") ?: return call.reject("unicastAddress is required")
        val appKeyIndex = call.getInt("appKeyIndex") ?: return call.reject("appKeyIndex is required")
        val companyId = call.getInt("companyId") ?: return call.reject("companyId is required")

        CoroutineScope(Dispatchers.Main).launch {
            if (!assertBluetoothEnabled(call)) return@launch
            val connected = connectionToProvisionedDevice()
            if (!connected) {
                return@launch call.reject("Failed to connect to Mesh proxy")
            }

            PluginCallManager.getInstance()
                .addSigPluginCall(ApplicationMessageOpCodes.HEALTH_FAULT_GET, unicastAddress, call)

            val result = meshController.sendHealthFaultGet(
                unicastAddress,
                appKeyIndex,
                companyId
            )

            if (!result) return@launch call.reject("Failed to send Health Fault Get")
        }
    }

    fun sendNotification(eventName: String, data: JSObject) {
        if (!hasListeners(eventName)) {
            return
        }
        notifyListeners(eventName, data)
    }
}

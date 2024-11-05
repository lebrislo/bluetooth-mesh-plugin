package com.lebrislo.bluetooth.mesh

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.util.Log
import com.lebrislo.bluetooth.mesh.ble.BleCallbacksManager
import com.lebrislo.bluetooth.mesh.ble.BleMeshManager
import com.lebrislo.bluetooth.mesh.models.BleMeshDevice
import com.lebrislo.bluetooth.mesh.models.ExtendedBluetoothDevice
import com.lebrislo.bluetooth.mesh.scanner.ScannerRepository
import com.lebrislo.bluetooth.mesh.utils.Utils
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import no.nordicsemi.android.mesh.MeshManagerApi
import no.nordicsemi.android.mesh.provisionerstates.UnprovisionedMeshNode
import no.nordicsemi.android.mesh.transport.ConfigAppKeyAdd
import no.nordicsemi.android.mesh.transport.ConfigCompositionDataGet
import no.nordicsemi.android.mesh.transport.ConfigModelAppBind
import no.nordicsemi.android.mesh.transport.ConfigNodeReset
import no.nordicsemi.android.mesh.transport.GenericLevelSet
import no.nordicsemi.android.mesh.transport.GenericLevelSetUnacknowledged
import no.nordicsemi.android.mesh.transport.GenericOnOffGet
import no.nordicsemi.android.mesh.transport.GenericOnOffSet
import no.nordicsemi.android.mesh.transport.GenericOnOffSetUnacknowledged
import no.nordicsemi.android.mesh.transport.GenericPowerLevelGet
import no.nordicsemi.android.mesh.transport.GenericPowerLevelSet
import no.nordicsemi.android.mesh.transport.GenericPowerLevelSetUnacknowledged
import no.nordicsemi.android.mesh.transport.LightCtlGet
import no.nordicsemi.android.mesh.transport.LightCtlSet
import no.nordicsemi.android.mesh.transport.LightCtlSetUnacknowledged
import no.nordicsemi.android.mesh.transport.LightCtlTemperatureRangeGet
import no.nordicsemi.android.mesh.transport.LightCtlTemperatureRangeSet
import no.nordicsemi.android.mesh.transport.LightCtlTemperatureRangeSetUnacknowledged
import no.nordicsemi.android.mesh.transport.LightHslGet
import no.nordicsemi.android.mesh.transport.LightHslSet
import no.nordicsemi.android.mesh.transport.LightHslSetUnacknowledged
import no.nordicsemi.android.mesh.transport.MeshMessage
import no.nordicsemi.android.mesh.transport.VendorModelMessageAcked
import no.nordicsemi.android.mesh.transport.VendorModelMessageUnacked
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class NrfMeshManager(private val context: Context) {
    private val tag: String = NrfMeshManager::class.java.simpleName

    private val meshCallbacksManager: MeshCallbacksManager
    private val meshProvisioningCallbacksManager: MeshProvisioningCallbacksManager
    private val meshStatusCallbacksManager: MeshStatusCallbacksManager
    private val bleCallbacksManager: BleCallbacksManager
    private val scannerRepository: ScannerRepository

    private val unprovisionedMeshNodes: ArrayList<UnprovisionedMeshNode> = ArrayList()

    private var bleMeshManager: BleMeshManager = BleMeshManager(context)
    var meshManagerApi: MeshManagerApi = MeshManagerApi(context)

    private val provisioningCapabilitiesMap = ConcurrentHashMap<UUID, CompletableDeferred<UnprovisionedMeshNode?>>()
    private val provisioningStatusMap = ConcurrentHashMap<String, CompletableDeferred<BleMeshDevice?>>()

    init {
        meshCallbacksManager = MeshCallbacksManager(bleMeshManager)
        meshProvisioningCallbacksManager =
            MeshProvisioningCallbacksManager(unprovisionedMeshNodes, this)
        meshStatusCallbacksManager = MeshStatusCallbacksManager()
        bleCallbacksManager = BleCallbacksManager(meshManagerApi)
        scannerRepository = ScannerRepository(context, meshManagerApi)

        meshManagerApi.setMeshManagerCallbacks(meshCallbacksManager)
        meshManagerApi.setProvisioningStatusCallbacks(meshProvisioningCallbacksManager)
        meshManagerApi.setMeshStatusCallbacks(meshStatusCallbacksManager)
        bleMeshManager.setGattCallbacks(bleCallbacksManager)

        meshManagerApi.loadMeshNetwork()
    }

    /**
     * Create a new mesh network
     *
     * @param networkName name of the mesh network
     *
     * @return Unit
     */
    fun initMeshNetwork(networkName: String) {
        meshManagerApi.resetMeshNetwork()
        meshManagerApi.meshNetwork!!.meshName = networkName
    }

    /**
     * Export the mesh network to a json string
     *
     * @return String
     */
    fun exportMeshNetwork(): String? {
        return meshManagerApi.exportMeshNetwork()
    }

    /**
     * Import a mesh network from a json string
     *
     * @param json json string of the mesh network
     */
    fun importMeshNetwork(json: String) {
        meshManagerApi.importMeshNetworkJson(json)
    }

    /**
     * Connect to a Bluetooth device
     *
     * @param bluetoothDevice BluetoothDevice to connect to
     *
     * @return Boolean whether the connection was successful
     */
    fun connectBle(bluetoothDevice: BluetoothDevice): Boolean {
        bleMeshManager.connect(bluetoothDevice).retry(3, 200).await()
        return bleMeshManager.isConnected
    }

    /**
     * Disconnect from a Bluetooth device
     */
    fun disconnectBle() {
        bleMeshManager.disconnect().await()
    }

    fun isBleConnected(): Boolean {
        return bleMeshManager.isConnected
    }

    fun connectedDevice(): BluetoothDevice? {
        return bleMeshManager.bluetoothDevice
    }

    fun startScan() {
        scannerRepository.startScanDevices()
    }

    fun stopScan() {
        scannerRepository.stopScanDevices()
    }

    /**
     * Search for a mesh proxy to connect to
     *
     * @return BluetoothDevice?
     */
    @SuppressLint("MissingPermission")
    suspend fun searchProxyMesh(): BluetoothDevice? {
        if (bleMeshManager.isConnected) {
            Log.d(tag, "searchProxyMesh : Connected to a bluetooth device")

            val isMeshProxy = scannerRepository.provisionedDevices.any() { device ->
                device.scanResult?.device?.address == bleMeshManager.bluetoothDevice?.address
            }

            Log.d(tag, "searchProxyMesh : Is mesh proxy: $isMeshProxy")

            if (isMeshProxy) {
                Log.i(tag, "searchProxyMesh : Connected to a mesh proxy ${bleMeshManager.bluetoothDevice?.address}")
                return bleMeshManager.bluetoothDevice
            } else {
                withContext(Dispatchers.IO) {
                    disconnectBle()
                }
            }
        }

        Log.d(tag, "searchProxyMesh : Provisioned devices: ${scannerRepository.provisionedDevices.size}")

        if (scannerRepository.provisionedDevices.isNotEmpty()) {
            synchronized(scannerRepository.provisionedDevices) {
                scannerRepository.provisionedDevices.sortBy { device -> device.scanResult?.rssi }
            }
            val device = scannerRepository.provisionedDevices.first().device
            Log.i(tag, "searchProxyMesh : Found a mesh proxy ${device!!.address}")
            return device
        }
        return null
    }

    /**
     * Search for an unprovisioned device to connect to
     *
     * @param uuid uuid of the device
     *
     * @return BluetoothDevice?
     */
    suspend fun searchUnprovisionedBluetoothDevice(uuid: String): BluetoothDevice? {
        if (bleMeshManager.isConnected) {
            val macAddress = bleMeshManager.bluetoothDevice!!.address
            if (scannerRepository.unprovisionedDevices.any { device -> device.scanResult?.device?.address == macAddress }) {
                return bleMeshManager.bluetoothDevice
            } else {
                withContext(Dispatchers.IO) {
                    disconnectBle()
                }
            }
        }

        return scannerRepository.unprovisionedDevices.firstOrNull { device ->
            device.scanResult?.let {
                val serviceData = Utils.getServiceData(it, MeshManagerApi.MESH_PROVISIONING_UUID)
                val deviceUuid = meshManagerApi.getDeviceUuid(serviceData!!)
                deviceUuid.toString() == uuid
            } ?: false
        }?.device
    }

    /**
     * Scan for mesh devices
     *
     * @param scanDurationMs duration of the scan in milliseconds
     *
     * @return List<ExtendedBluetoothDevice>
     */
    suspend fun scanMeshDevices(scanDurationMs: Int = 5000): List<ExtendedBluetoothDevice> {
        scannerRepository.unprovisionedDevices.clear()
        scannerRepository.provisionedDevices.clear()
        scannerRepository.stopScanDevices()
        scannerRepository.startScanDevices()
        delay(scanDurationMs.toLong())
        return scannerRepository.unprovisionedDevices + scannerRepository.provisionedDevices
    }

    /**
     * Get the provisioning capabilities of a device
     *
     * Note: The application must be connected to the concerned device before sending messages
     *
     * @param uuid uuid of the device
     *
     * @return CompletableDeferred<UnprovisionedMeshNode?>
     */
    fun getProvisioningCapabilities(uuid: UUID): CompletableDeferred<UnprovisionedMeshNode?> {
        val deferred = CompletableDeferred<UnprovisionedMeshNode?>()
        provisioningCapabilitiesMap[uuid] = deferred

        if (!bleMeshManager.isConnected) {
            Log.e(tag, "Failed to connect to unprovisioned device")
            deferred.cancel()
            return deferred
        } else {
            meshManagerApi.identifyNode(uuid)
        }

        return deferred
    }

    fun onProvisioningCapabilitiesReceived(meshNode: UnprovisionedMeshNode?) {
        val uuid = meshNode?.deviceUuid
        if (uuid != null) {
            provisioningCapabilitiesMap[uuid]?.complete(meshNode)
            provisioningCapabilitiesMap.remove(uuid)
        }
    }

    /**
     * Provision a device
     *
     * Note: The application must be connected to a mesh proxy before sending messages
     *
     * @param uuid uuid of the device
     *
     * @return CompletableDeferred<BleMeshDevice?>
     */
    fun provisionDevice(uuid: UUID): CompletableDeferred<BleMeshDevice?> {
        val deferred = CompletableDeferred<BleMeshDevice?>()
        provisioningStatusMap[uuid.toString()] = deferred

        val unprovisionedMeshNode = unprovisionedMeshNodes.firstOrNull { node ->
            node.deviceUuid == uuid
        }

        if (unprovisionedMeshNode == null) {
            Log.e(tag, "Unprovisioned Mesh Node not found, try identifying the node first")
            deferred.cancel()
            return deferred
        }

        val provisioner = meshManagerApi.meshNetwork?.selectedProvisioner
        val unicastAddress = meshManagerApi.meshNetwork?.nextAvailableUnicastAddress(
            unprovisionedMeshNode.numberOfElements,
            provisioner!!
        )
        meshManagerApi.meshNetwork?.assignUnicastAddress(unicastAddress!!)

        if (!bleMeshManager.isConnected) {
            Log.e(tag, "Failed to connect to unprovisioned device")
            deferred.cancel()
            return deferred
        } else {
            meshManagerApi.startProvisioning(unprovisionedMeshNode)
        }

        return deferred
    }

    fun onProvisioningFinish(bleMeshDevice: BleMeshDevice?) {
        when (bleMeshDevice) {
            is BleMeshDevice.Provisioned -> {
                val uuid = bleMeshDevice.node.uuid
                provisioningStatusMap[uuid]?.complete(bleMeshDevice)
                provisioningStatusMap.remove(uuid)
                unprovisionedMeshNodes.remove(unprovisionedMeshNodes.first { node -> node.deviceUuid.toString() == uuid })
            }

            is BleMeshDevice.Unprovisioned -> {
                val uuid = bleMeshDevice.node.deviceUuid
                provisioningStatusMap[uuid.toString()]?.complete(bleMeshDevice)
                provisioningStatusMap.remove(uuid.toString())
            }

            null -> {
                Log.e(tag, "Unknown provisioning state")
            }
        }
    }

    /**
     * Reset a provisioned device
     *
     * Note: The application must be connected to a mesh proxy before sending messages
     *
     * @param unicastAddress unicast address of the node
     *
     * @return Boolean whether the message was sent successfully
     */
    fun unprovisionDevice(unicastAddress: Int): Boolean {
        if (!bleMeshManager.isConnected) {
            Log.e(tag, "Failed to connect to provisioned device")
            return false
        } else {
            val configNodeReset = ConfigNodeReset()
            meshManagerApi.createMeshPdu(unicastAddress, configNodeReset)
        }

        CoroutineScope(Dispatchers.IO).launch {
            scanMeshDevices(1000)
        }

        return true
    }

    /**
     * Create an application key
     *
     * @return Boolean whether the application key was created successfully
     */
    fun createApplicationKey(): Boolean {
        val applicationKey = meshManagerApi.meshNetwork?.createAppKey()
        return meshManagerApi.meshNetwork?.addAppKey(applicationKey!!) ?: false
    }

    /**
     * Remove an application key from the mesh network
     *
     * @param appKeyIndex index of the application key
     *
     * @return Boolean whether the application key was removed successfully
     */
    fun removeApplicationKey(appKeyIndex: Int): Boolean {
        return meshManagerApi.meshNetwork?.getAppKey(appKeyIndex)?.let {
            meshManagerApi.meshNetwork?.removeAppKey(it)
        } ?: false
    }

    /**
     * Add an application key to a node
     *
     * Note: The application must be connected to a mesh proxy before sending messages
     *
     * @param elementAddress unicast address of the node's element
     * @param appKeyIndex index of the application key
     *
     * @return Boolean whether the message was sent successfully
     */
    fun addApplicationKeyToNode(elementAddress: Int, appKeyIndex: Int): Boolean {
        if (!bleMeshManager.isConnected) {
            Log.e(tag, "Not connected to a mesh proxy")
            return false
        }

        val netKey = meshManagerApi.meshNetwork?.primaryNetworkKey
        val appKey = meshManagerApi.meshNetwork?.getAppKey(appKeyIndex)

        val configModelAppBind = ConfigAppKeyAdd(netKey!!, appKey!!)
        meshManagerApi.createMeshPdu(elementAddress, configModelAppBind)

        return true
    }

    /**
     * Bind an application key to a model
     *
     * Note: The application must be connected to a mesh proxy before sending messages
     *
     * @param elementAddress unicast address of the node's element
     * @param appKeyIndex index of the application key
     * @param modelId model id
     *
     * @return Boolean whether the message was sent successfully
     */
    fun bindApplicationKeyToModel(elementAddress: Int, appKeyIndex: Int, modelId: Int): Boolean {
        if (!bleMeshManager.isConnected) {
            Log.e(tag, "Not connected to a mesh proxy")
            return false
        }

        val configModelAppBind = ConfigModelAppBind(elementAddress, modelId, appKeyIndex)
        meshManagerApi.createMeshPdu(elementAddress, configModelAppBind)

        return true
    }

    /**
     * Retrieve the composition data of a node
     *
     * Note: The application must be connected to a mesh proxy before sending messages
     *
     * @param unicastAddress unicast address of the node
     * @return Boolean
     */
    fun compositionDataGet(unicastAddress: Int): Boolean {
        if (!bleMeshManager.isConnected) {
            Log.e(tag, "Not connected to a mesh proxy")
            return false
        }

        val configCompositionDataGet = ConfigCompositionDataGet()
        meshManagerApi.createMeshPdu(unicastAddress, configCompositionDataGet)

        return true
    }

    /**
     * Send a Generic OnOff Set message to a node
     *
     * Note: The application must be connected to a mesh proxy before sending messages
     *
     * @param address unicast address of the node
     * @param appKeyIndex index of the application key
     * @param onOffvalue on/off value to set
     * @param tId transaction id
     * @param transitionStep transition step
     * @param transitionResolution transition resolution
     * @param delay delay before the message is sent
     * @param acknowledgement whether to send an acknowledgement
     *
     * @return Boolean whether the message was sent successfully
     */
    fun sendGenericOnOffSet(
        address: Int,
        appKeyIndex: Int,
        onOffvalue: Boolean,
        tId: Int = 0,
        transitionStep: Int? = 0,
        transitionResolution: Int? = 0,
        delay: Int = 0,
        acknowledgement: Boolean = false
    ): Boolean {
        if (!bleMeshManager.isConnected) {
            Log.e(tag, "Not connected to a mesh proxy")
            return false
        }

        var meshMessage: MeshMessage? = null

        if (acknowledgement) {
            meshMessage = GenericOnOffSet(
                meshManagerApi.meshNetwork!!.getAppKey(appKeyIndex),
                onOffvalue,
                tId,
                transitionStep,
                transitionResolution,
                delay
            )
        } else {
            meshMessage = GenericOnOffSetUnacknowledged(
                meshManagerApi.meshNetwork!!.getAppKey(appKeyIndex),
                onOffvalue,
                tId,
                transitionStep,
                transitionResolution,
                delay
            )
        }

        meshManagerApi.createMeshPdu(address, meshMessage)
        return true
    }

    /**
     * Send Generic OnOff Get message to a node
     *
     * Note: The application must be connected to a mesh proxy before sending messages
     *
     * @param address unicast address of the node
     * @param appKeyIndex index of the application key
     *
     * @return Boolean whether the message was sent successfully
     */
    fun sendGenericOnOffGet(
        address: Int,
        appKeyIndex: Int
    ): Boolean {
        if (!bleMeshManager.isConnected) {
            Log.e(tag, "Not connected to a mesh proxy")
            return false
        }

        val meshMessage = GenericOnOffGet(
            meshManagerApi.meshNetwork!!.getAppKey(appKeyIndex),
        )

        meshManagerApi.createMeshPdu(address, meshMessage)
        return true
    }

    /**
     * Send a Generic Level Set message to a node
     *
     * Note: The application must be connected to a mesh proxy before sending messages
     *
     * @param address unicast address of the node
     * @param appKeyIndex index of the application key
     * @param level level to set
     * @param tId transaction id
     * @param transitionStep transition step
     * @param transitionResolution transition resolution
     * @param delay delay before the message is sent
     * @param acknowledgement whether to send an acknowledgement
     *
     * @return Boolean whether the message was sent successfully
     */
    fun sendGenericLevelSet(
        address: Int,
        appKeyIndex: Int,
        level: Int,
        tId: Int = 0,
        transitionStep: Int? = 0,
        transitionResolution: Int? = 0,
        delay: Int = 0,
        acknowledgement: Boolean = false
    ): Boolean {
        if (!bleMeshManager.isConnected) {
            Log.e(tag, "Not connected to a mesh proxy")
            return false
        }

        var meshMessage: MeshMessage? = null

        if (acknowledgement) {
            meshMessage = GenericLevelSet(
                meshManagerApi.meshNetwork!!.getAppKey(appKeyIndex),
                transitionStep,
                transitionResolution,
                delay,
                level,
                tId,
            )
        } else {
            meshMessage = GenericLevelSetUnacknowledged(
                meshManagerApi.meshNetwork!!.getAppKey(appKeyIndex),
                transitionStep,
                transitionResolution,
                delay,
                level,
                tId,
            )
        }

        meshManagerApi.createMeshPdu(address, meshMessage)
        return true
    }

    /**
     * Send a Generic Power Level Set message to a node
     *
     * Note: The application must be connected to a mesh proxy before sending messages
     *
     * @param address unicast address of the node
     * @param appKeyIndex index of the application key
     * @param powerLevel power level to set
     * @param tId transaction id
     * @param transitionStep transition step
     * @param transitionResolution transition resolution
     * @param delay delay before the message is sent
     * @param acknowledgement whether to send an acknowledgement
     *
     * @return Boolean whether the message was sent successfully
     */
    fun sendGenericPowerLevelSet(
        address: Int,
        appKeyIndex: Int,
        powerLevel: Int,
        tId: Int = 0,
        transitionStep: Int? = 0,
        transitionResolution: Int? = 0,
        delay: Int = 0,
        acknowledgement: Boolean = false
    ): Boolean {
        if (!bleMeshManager.isConnected) {
            Log.e(tag, "Not connected to a mesh proxy")
            return false
        }

        var meshMessage: MeshMessage? = null

        if (acknowledgement) {
            meshMessage = GenericPowerLevelSet(
                meshManagerApi.meshNetwork!!.getAppKey(appKeyIndex),
                transitionStep,
                transitionResolution,
                delay,
                powerLevel,
                tId
            )
        } else {
            meshMessage = GenericPowerLevelSetUnacknowledged(
                meshManagerApi.meshNetwork!!.getAppKey(appKeyIndex),
                transitionStep,
                transitionResolution,
                delay,
                powerLevel,
                tId
            )
        }
        meshManagerApi.createMeshPdu(address, meshMessage)
        return true
    }

    /**
     * Send a Generic Power Level Get message to a node
     *
     * Note: The application must be connected to a mesh proxy before sending messages
     *
     * @param address unicast address of the node
     * @param appKeyIndex index of the application key
     *
     * @return Boolean whether the message was sent successfully
     */
    fun sendGenericPowerLevelGet(
        address: Int,
        appKeyIndex: Int
    ): Boolean {
        if (!bleMeshManager.isConnected) {
            Log.e(tag, "Not connected to a mesh proxy")
            return false
        }

        val meshMessage = GenericPowerLevelGet(
            meshManagerApi.meshNetwork!!.getAppKey(appKeyIndex),
        )

        meshManagerApi.createMeshPdu(address, meshMessage)
        return true
    }

    /**
     * Send a Light HSL Set message to a node
     *
     * Note: The application must be connected to a mesh proxy before sending messages
     *
     * @param address unicast address of the node
     * @param appKeyIndex index of the application key
     * @param hue hue value to set
     * @param saturation saturation value to set
     * @param lightness lightness value to set
     * @param tId transaction id
     * @param transitionStep transition step
     * @param transitionResolution transition resolution
     * @param delay delay before the message is sent
     * @param acknowledgement whether to send an acknowledgement
     *
     * @return Boolean whether the message was sent successfully
     */
    fun sendLightHslSet(
        address: Int,
        appKeyIndex: Int,
        hue: Int,
        saturation: Int,
        lightness: Int,
        tId: Int = 0,
        transitionStep: Int? = 0,
        transitionResolution: Int? = 0,
        delay: Int = 0,
        acknowledgement: Boolean = false
    ): Boolean {
        if (!bleMeshManager.isConnected) {
            Log.e(tag, "Not connected to a mesh proxy")
            return false
        }

        var meshMessage: MeshMessage? = null

        if (acknowledgement) {
            meshMessage = LightHslSet(
                meshManagerApi.meshNetwork!!.getAppKey(appKeyIndex),
                transitionStep,
                transitionResolution,
                delay,
                lightness,
                hue,
                saturation,
                tId
            )
        } else {
            meshMessage = LightHslSetUnacknowledged(
                meshManagerApi.meshNetwork!!.getAppKey(appKeyIndex),
                transitionStep,
                transitionResolution,
                delay,
                lightness,
                hue,
                saturation,
                tId
            )
        }
        meshManagerApi.createMeshPdu(address, meshMessage)
        return true
    }

    fun sendLightHslGet(
        address: Int,
        appKeyIndex: Int
    ): Boolean {
        if (!bleMeshManager.isConnected) {
            Log.e(tag, "Not connected to a mesh proxy")
            return false
        }

        val meshMessage = LightHslGet(
            meshManagerApi.meshNetwork!!.getAppKey(appKeyIndex),
        )

        meshManagerApi.createMeshPdu(address, meshMessage)
        return true
    }

    /**
     * Send a Light CTL Set message to a node
     *
     * Note: The application must be connected to a mesh proxy before sending messages
     *
     * @param address unicast address of the node
     * @param appKeyIndex index of the application key
     * @param lightness lightness value to set
     * @param temperature temperature value to set
     * @param deltaUv delta uv value to set
     * @param tId transaction id
     * @param transitionStep transition step
     * @param transitionResolution transition resolution
     * @param delay delay before the message is sent
     * @param acknowledgement whether to send an acknowledgement
     *
     * @return Boolean whether the message was sent successfully
     */
    fun sendLightCtlSet(
        address: Int,
        appKeyIndex: Int,
        lightness: Int,
        temperature: Int,
        deltaUv: Int,
        tId: Int = 0,
        transitionStep: Int? = 0,
        transitionResolution: Int? = 0,
        delay: Int = 0,
        acknowledgement: Boolean = false
    ): Boolean {
        if (!bleMeshManager.isConnected) {
            Log.e(tag, "Not connected to a mesh proxy")
            return false
        }

        var meshMessage: MeshMessage? = null

        if (acknowledgement) {
            meshMessage = LightCtlSet(
                meshManagerApi.meshNetwork!!.getAppKey(appKeyIndex),
                transitionStep,
                transitionResolution,
                delay,
                lightness,
                temperature,
                deltaUv,
                tId
            )
        } else {
            meshMessage = LightCtlSetUnacknowledged(
                meshManagerApi.meshNetwork!!.getAppKey(appKeyIndex),
                transitionStep,
                transitionResolution,
                delay,
                lightness,
                temperature,
                deltaUv,
                tId
            )
        }
        meshManagerApi.createMeshPdu(address, meshMessage)
        return true
    }

    /**
     * Send a Light CTL Get message to a node
     *
     * Note: The application must be connected to a mesh proxy before sending messages
     *
     * @param address unicast address of the node
     * @param appKeyIndex index of the application key
     *
     * @return Boolean whether the message was sent successfully
     */
    fun sendLightCtlGet(
        address: Int,
        appKeyIndex: Int
    ): Boolean {
        if (!bleMeshManager.isConnected) {
            Log.e(tag, "Not connected to a mesh proxy")
            return false
        }

        val meshMessage = LightCtlGet(
            meshManagerApi.meshNetwork!!.getAppKey(appKeyIndex),
        )

        meshManagerApi.createMeshPdu(address, meshMessage)
        return true
    }

    /**
     * Send a Light CTL Temperature Range Set message to a node
     *
     * Note: The application must be connected to a mesh proxy before sending messages
     *
     * @param address unicast address of the node
     * @param appKeyIndex index of the application key
     * @param rangeMin minimum temperature value
     * @param rangeMax maximum temperature value
     *
     * @return Boolean whether the message was sent successfully
     */
    fun sendLightCtlTemperatureRangeSet(
        address: Int,
        appKeyIndex: Int,
        rangeMin: Int,
        rangeMax: Int,
        acknowledgement: Boolean = false
    ): Boolean {
        if (!bleMeshManager.isConnected) {
            Log.e(tag, "Not connected to a mesh proxy")
            return false
        }

        var meshMessage: MeshMessage? = null

        if (acknowledgement) {
            meshMessage = LightCtlTemperatureRangeSet(
                meshManagerApi.meshNetwork!!.getAppKey(appKeyIndex),
                rangeMin,
                rangeMax
            )
        } else {
            meshMessage = LightCtlTemperatureRangeSetUnacknowledged(
                meshManagerApi.meshNetwork!!.getAppKey(appKeyIndex),
                rangeMin,
                rangeMax
            )
        }
        meshManagerApi.createMeshPdu(address, meshMessage)
        return true
    }

    /**
     * Send a Light CTL Temperature Range Get message to a node
     *
     * Note: The application must be connected to a mesh proxy before sending messages
     *
     * @param address unicast address of the node
     * @param appKeyIndex index of the application key
     *
     * @return Boolean whether the message was sent successfully
     */
    fun sendLightCtlTemperatureRangeGet(address: Int, appKeyIndex: Int): Boolean {
        if (!bleMeshManager.isConnected) {
            Log.e(tag, "Not connected to a mesh proxy")
            return false
        }

        val meshMessage = LightCtlTemperatureRangeGet(
            meshManagerApi.meshNetwork!!.getAppKey(appKeyIndex),
        )

        meshManagerApi.createMeshPdu(address, meshMessage)
        return true
    }

    /**
     * Send a Vendor Model message to a node
     *
     * Note: The application must be connected to a mesh proxy before sending messages
     *
     * @param address unicast address of the node
     * @param appKeyIndex index of the application key
     * @param modelId model id
     * @param companyIdentifier company identifier
     * @param opCode operation code
     * @param payload parameters of the message
     * @param acknowledgement whether to send an acknowledgement
     *
     * @return Boolean whether the message was sent successfully
     */
    fun sendVendorModelMessage(
        address: Int,
        appKeyIndex: Int,
        modelId: Int,
        companyIdentifier: Int,
        opCode: Int,
        payload: ByteArray = byteArrayOf(),
        acknowledgement: Boolean = false
    ): Boolean {
        if (!bleMeshManager.isConnected) {
            Log.e(tag, "Not connected to a mesh proxy")
            return false
        }

        var meshMessage: MeshMessage? = null

        if (acknowledgement) {
            meshMessage = VendorModelMessageAcked(
                meshManagerApi.meshNetwork!!.getAppKey(appKeyIndex),
                modelId,
                companyIdentifier,
                opCode,
                payload
            )
        } else {
            meshMessage = VendorModelMessageUnacked(
                meshManagerApi.meshNetwork!!.getAppKey(appKeyIndex),
                modelId,
                companyIdentifier,
                opCode,
                payload
            )
        }
        meshManagerApi.createMeshPdu(address, meshMessage)
        return true
    }
}

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
import kotlinx.coroutines.delay
import no.nordicsemi.android.mesh.MeshManagerApi
import no.nordicsemi.android.mesh.provisionerstates.UnprovisionedMeshNode
import no.nordicsemi.android.mesh.transport.ConfigAppKeyAdd
import no.nordicsemi.android.mesh.transport.ConfigAppKeyStatus
import no.nordicsemi.android.mesh.transport.ConfigCompositionDataGet
import no.nordicsemi.android.mesh.transport.ConfigCompositionDataStatus
import no.nordicsemi.android.mesh.transport.ConfigModelAppBind
import no.nordicsemi.android.mesh.transport.ConfigNodeReset
import no.nordicsemi.android.mesh.transport.ConfigNodeResetStatus
import no.nordicsemi.android.mesh.transport.GenericOnOffSet
import no.nordicsemi.android.mesh.transport.GenericPowerLevelSet
import no.nordicsemi.android.mesh.transport.LightHslSet
import no.nordicsemi.android.mesh.transport.MeshMessage
import no.nordicsemi.android.mesh.transport.ProvisionedMeshNode
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class NrfMeshManager(private var context: Context) {
    private val tag: String = NrfMeshManager::class.java.simpleName

    private val meshCallbacksManager: MeshCallbacksManager
    private val meshProvisioningCallbacksManager: MeshProvisioningCallbacksManager
    private val meshStatusCallbacksManager: MeshStatusCallbacksManager
    private val bleCallbacksManager: BleCallbacksManager
    private val scannerRepository: ScannerRepository

    private val unprovisionedMeshNodes: ArrayList<UnprovisionedMeshNode> = ArrayList()
    private val provisionedMeshNodes: ArrayList<ProvisionedMeshNode> = ArrayList()
    private val unprovisionedBluetoothDevices: ArrayList<ExtendedBluetoothDevice> = ArrayList()
    private val provisionedBluetoothDevices: ArrayList<ExtendedBluetoothDevice> = ArrayList()

    var bleMeshManager: BleMeshManager = BleMeshManager(context)
    var meshManagerApi: MeshManagerApi = MeshManagerApi(context)

    var currentProvisionedMeshNode: ProvisionedMeshNode? = null
    private val provisioningCapabilitiesMap = ConcurrentHashMap<UUID, CompletableDeferred<UnprovisionedMeshNode?>>()
    private val provisioningStatusMap = ConcurrentHashMap<String, CompletableDeferred<BleMeshDevice?>>()
    private val unprovisionStatusMap = ConcurrentHashMap<Int, CompletableDeferred<Boolean?>>()
    private val compositionDataStatusMap = ConcurrentHashMap<Int, CompletableDeferred<Boolean?>>()

    init {
        meshCallbacksManager = MeshCallbacksManager(bleMeshManager)
        meshProvisioningCallbacksManager =
            MeshProvisioningCallbacksManager(unprovisionedMeshNodes, this)
        meshStatusCallbacksManager = MeshStatusCallbacksManager(this)
        bleCallbacksManager = BleCallbacksManager(meshManagerApi)
        scannerRepository = ScannerRepository(context, meshManagerApi)

        meshManagerApi.setMeshManagerCallbacks(meshCallbacksManager)
        meshManagerApi.setProvisioningStatusCallbacks(meshProvisioningCallbacksManager)
        meshManagerApi.setMeshStatusCallbacks(meshStatusCallbacksManager)
        bleMeshManager.setGattCallbacks(bleCallbacksManager)

        meshManagerApi.loadMeshNetwork()
    }

    fun connectBle(bluetoothDevice: BluetoothDevice): Boolean {
        bleMeshManager.connect(bluetoothDevice).retry(3, 200).await()
        return bleMeshManager.isConnected
    }

    fun disconnectBle() {
        bleMeshManager.disconnect().enqueue()
    }

    private fun connectToUnprovisionedDevice(uuid: String): Boolean {
        val bluetoothDevice = scannerRepository.unprovisionedDevices.firstOrNull { device ->
            device.scanResult?.let {
                val serviceData = Utils.getServiceData(it, MeshManagerApi.MESH_PROVISIONING_UUID)
                val deviceUuid = meshManagerApi.getDeviceUuid(serviceData!!)
                deviceUuid.toString() == uuid
            } ?: false
        } ?: return false

        bleMeshManager.connect(bluetoothDevice.device!!).retry(3, 200).await()
        return true
    }

    private fun connectToProvisionedDevice(unicastAddress: Int): Boolean {
        val provisionedNode = meshManagerApi.meshNetwork?.getNode(unicastAddress)

        if (provisionedNode == null) {
            Log.e(tag, "Provisioned node not found")
            return false
        }

        val bluetoothDevice = scannerRepository.unprovisionedDevices.firstOrNull { device ->
            device.scanResult?.let {
                val serviceData = Utils.getServiceData(it, MeshManagerApi.MESH_PROVISIONING_UUID)
                val deviceUuid = meshManagerApi.getDeviceUuid(serviceData!!)
                deviceUuid.toString() == provisionedNode.uuid
            } ?: false
        } ?: return false

        bleMeshManager.connect(bluetoothDevice.device!!).retry(3, 200).await()
        return true
    }

    private suspend fun resetScanner() {
        scannerRepository.provisionedDevices.clear()
        scannerRepository.unprovisionedDevices.clear()
        scannerRepository.startScanDevices()
        delay(3000)
    }

    @SuppressLint("MissingPermission")
    suspend fun searchProxyMesh(): BluetoothDevice? {
        if (bleMeshManager.isConnected) {
            val serviceUuids = bleMeshManager.bluetoothDevice?.uuids

            val isMeshProxy = serviceUuids?.any { uuid ->
                uuid.uuid == MeshManagerApi.MESH_PROXY_UUID
            } == true

            if (isMeshProxy) {
                return bleMeshManager.bluetoothDevice
            }
        }

        if (!scannerRepository.isScanning) {
            Log.i(tag, "Before delay")
            resetScanner()
            Log.i(tag, "After delay")
        }

        if (scannerRepository.provisionedDevices.isNotEmpty()) {
            scannerRepository.provisionedDevices.sortBy { device -> device.scanResult?.rssi }
            val device = scannerRepository.provisionedDevices.first().device
            return device
        }
        return null
    }


    suspend fun scanUnprovisionedDevices(scanDurationMs: Int = 5000): List<ExtendedBluetoothDevice> {
        scannerRepository.startScanDevices()
        delay(scanDurationMs.toLong())
        return scannerRepository.unprovisionedDevices
    }

    suspend fun scanProvisionedDevices(scanDurationMs: Int = 5000): List<ExtendedBluetoothDevice> {
        scannerRepository.startScanDevices()
        delay(scanDurationMs.toLong())
        return scannerRepository.provisionedDevices
    }

    fun getProvisioningCapabilities(uuid: UUID): CompletableDeferred<UnprovisionedMeshNode?> {
        val deferred = CompletableDeferred<UnprovisionedMeshNode?>()
        provisioningCapabilitiesMap[uuid] = deferred

        val result = connectToUnprovisionedDevice(uuid.toString())

        if (!result) {
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

        val result = connectToUnprovisionedDevice(uuid.toString())
        if (!result) {
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
                unprovisionedMeshNodes.firstOrNull { node ->
                    node.deviceUuid.toString() == uuid
                }?.let {
                    unprovisionedMeshNodes.remove(it)
                    provisionedMeshNodes.add(bleMeshDevice.node)
                }
                unprovisionedBluetoothDevices.firstOrNull { device ->
                    device.scanResult?.let {
                        val serviceData = Utils.getServiceData(it, MeshManagerApi.MESH_PROVISIONING_UUID)
                        val deviceUuid = meshManagerApi.getDeviceUuid(serviceData!!)
                        deviceUuid.toString() == uuid
                    } ?: false
                }?.let {
                    unprovisionedBluetoothDevices.remove(it)
                    provisionedBluetoothDevices.add(it)
                }
                currentProvisionedMeshNode = bleMeshDevice.node
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

    fun unprovisionDevice(unicastAddress: Int): CompletableDeferred<Boolean?> {
        val differed = CompletableDeferred<Boolean?>()
        unprovisionStatusMap[unicastAddress] = differed

        val result = connectToProvisionedDevice(unicastAddress)
        if (!result) {
            Log.e(tag, "Failed to connect to provisioned device")
            differed.cancel()
            return differed
        } else {
            val configNodeReset = ConfigNodeReset()
            meshManagerApi.createMeshPdu(unicastAddress, configNodeReset)
        }

        return differed
    }

    fun onNodeResetStatusReceived(meshMessage: ConfigNodeResetStatus) {
        val unicastAddress = meshMessage.src

        val operarionSucceded = meshMessage.statusCode == 0

        if (operarionSucceded) {
            unprovisionStatusMap[unicastAddress]?.complete(true)
            unprovisionStatusMap.remove(unicastAddress)
        }
    }

    fun createApplicationKey(): Boolean {
        val applicationKey = meshManagerApi.meshNetwork?.createAppKey()
        return meshManagerApi.meshNetwork?.addAppKey(applicationKey!!) ?: false
    }

    fun removeApplicationKey(appKeyIndex: Int): Boolean {
        return meshManagerApi.meshNetwork?.getAppKey(appKeyIndex)?.let {
            meshManagerApi.meshNetwork?.removeAppKey(it)
        } ?: false
    }

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

    fun bindApplicationKeyToModel(elementAddress: Int, appKeyIndex: Int, modelId: Int): Boolean {
        if (!bleMeshManager.isConnected) {
            Log.e(tag, "Not connected to a mesh proxy")
            return false
        }

        val configModelAppBind = ConfigModelAppBind(elementAddress, modelId, appKeyIndex)
        meshManagerApi.createMeshPdu(elementAddress, configModelAppBind)

        return true
    }

    fun onAppKeyStatusReceived(meshMessage: ConfigAppKeyStatus) {
        Log.d(tag, "onAppKeyStatusReceived")
    }

    fun handleNotifications(mtu: Int, pdu: ByteArray) {
        meshManagerApi.handleNotifications(mtu, pdu)
    }

    fun handleWriteCallbacks(mtu: Int, pdu: ByteArray) {
        meshManagerApi.handleWriteCallbacks(mtu, pdu)
    }

    fun importMeshNetworkJson(json: String) {
        meshManagerApi.importMeshNetworkJson(json)
    }

    fun exportMeshNetwork(): String? {
        return meshManagerApi.exportMeshNetwork()
    }

    fun compositionDataGet(unicastAddress: Int): CompletableDeferred<Boolean?> {
        val deferred = CompletableDeferred<Boolean?>()
        compositionDataStatusMap[unicastAddress] = deferred

        if (!bleMeshManager.isConnected) {
            Log.e(tag, "Not connected to a mesh proxy")
            deferred.cancel()
            return deferred
        }

        val configCompositionDataGet = ConfigCompositionDataGet()
        meshManagerApi.createMeshPdu(unicastAddress, configCompositionDataGet)

        return deferred
    }

    fun onCompositionDataStatusReceived(meshMessage: ConfigCompositionDataStatus) {
        Log.d(tag, "onCompositionDataStatusReceived")
        val unicastAddress = meshMessage.src
        val operationSucceeded = meshMessage.statusCode == 0

        if (operationSucceeded) {
            compositionDataStatusMap[unicastAddress]?.complete(true)
            compositionDataStatusMap.remove(unicastAddress)
        }
    }

    fun resetMeshNetwork() {
        meshManagerApi.resetMeshNetwork()
    }

    fun getSequenceNumberForAddress(address: Int): Int {
        val node = meshManagerApi.meshNetwork!!.getNode(address)
        return node.sequenceNumber
    }

    fun setSequenceNumberForAddress(address: Int, sequenceNumber: Int) {
        val currentMeshNetwork = meshManagerApi.meshNetwork!!
        val node: ProvisionedMeshNode = currentMeshNetwork.getNode(address)
        node.sequenceNumber = sequenceNumber
    }

    fun sendConfigModelAppBind(nodeId: Int, elementId: Int, modelId: Int, appKeyIndex: Int) {
        val configModelAppBind = ConfigModelAppBind(elementId, modelId, appKeyIndex)
        meshManagerApi.createMeshPdu(nodeId, configModelAppBind)
    }

    fun sendGenericOnOffSet(
        address: Int,
        value: Boolean,
        keyIndex: Int,
        tId: Int,
        transitionStep: Int? = 0,
        transitionResolution: Int? = 0,
        delay: Int = 0
    ): Boolean {
        if (!bleMeshManager.isConnected) {
            Log.e(tag, "Not connected to a mesh proxy")
            return false
        }

        val meshMessage: MeshMessage = GenericOnOffSet(
            meshManagerApi.meshNetwork!!.getAppKey(keyIndex),
            value,
            tId,
            transitionStep,
            transitionResolution,
            delay
        )
        meshManagerApi.createMeshPdu(address, meshMessage)

        return true
    }

    fun sendGenericPowerLevelSet(
        address: Int,
        powerLevel: Int,
        keyIndex: Int,
        tId: Int,
        transitionStep: Int? = 0,
        transitionResolution: Int? = 0,
        delay: Int = 0
    ): Boolean {
        if (!bleMeshManager.isConnected) {
            Log.e(tag, "Not connected to a mesh proxy")
            return false
        }

        val meshMessage: MeshMessage = GenericPowerLevelSet(
            meshManagerApi.meshNetwork!!.getAppKey(keyIndex),
            tId,
            transitionStep,
            transitionResolution,
            powerLevel,
            delay
        )
        meshManagerApi.createMeshPdu(address, meshMessage)

        return true
    }

    fun sendLightHslSet(
        address: Int,
        hue: Int,
        saturation: Int,
        lightness: Int,
        keyIndex: Int,
        tId: Int,
        transitionStep: Int? = 0,
        transitionResolution: Int? = 0,
        delay: Int = 0
    ): Boolean {
        if (!bleMeshManager.isConnected) {
            Log.e(tag, "Not connected to a mesh proxy")
            return false
        }

        val meshMessage: MeshMessage = LightHslSet(
            meshManagerApi.meshNetwork!!.getAppKey(keyIndex),
            transitionStep,
            transitionResolution,
            delay,
            lightness,
            hue,
            saturation,
            tId
        )
        meshManagerApi.createMeshPdu(address, meshMessage)

        return true
    }
}

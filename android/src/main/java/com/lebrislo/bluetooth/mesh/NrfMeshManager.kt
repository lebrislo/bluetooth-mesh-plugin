package com.lebrislo.bluetooth.mesh

import android.content.Context
import android.util.Log
import com.lebrislo.bluetooth.mesh.ble.BleCallbacksManager
import com.lebrislo.bluetooth.mesh.ble.BleMeshManager
import com.lebrislo.bluetooth.mesh.models.ExtendedBluetoothDevice
import com.lebrislo.bluetooth.mesh.models.MeshDevice
import com.lebrislo.bluetooth.mesh.scanner.ScanCallback
import com.lebrislo.bluetooth.mesh.scanner.ScannerRepository
import com.lebrislo.bluetooth.mesh.utils.Permissions
import com.lebrislo.bluetooth.mesh.utils.Utils
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import no.nordicsemi.android.mesh.MeshManagerApi
import no.nordicsemi.android.mesh.provisionerstates.UnprovisionedMeshNode
import no.nordicsemi.android.mesh.transport.ConfigModelAppBind
import no.nordicsemi.android.mesh.transport.ConfigNodeReset
import no.nordicsemi.android.mesh.transport.GenericOnOffSet
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
    private val scanScope = CoroutineScope(Dispatchers.Main + Job())
    private val scannerRepository: ScannerRepository
    private val unprovisionedMeshNodes: ArrayList<UnprovisionedMeshNode> = ArrayList()
    private val unprovisionedBluetoothDevices: ArrayList<ExtendedBluetoothDevice> = ArrayList()

    var bleMeshManager: BleMeshManager = BleMeshManager(context)
    var meshManagerApi: MeshManagerApi = MeshManagerApi(context)

    var currentProvisionedMeshNode: ProvisionedMeshNode? = null
    private val provisioningCapabilitiesMap = ConcurrentHashMap<UUID, CompletableDeferred<UnprovisionedMeshNode?>>()
    private val provisioningStatusMap = ConcurrentHashMap<String, CompletableDeferred<MeshDevice?>>()

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

    fun echo(value: String): String {
        Log.i(tag, value)
        return value
    }

    fun scanUnprovisionedDevices(callback: ScanCallback, timeoutMs: Int = 5000) {
        if (!Permissions.isBleEnabled(context)) {
            return
        }

        if (!Permissions.isLocationGranted(context)) {
            return
        }

        if (scannerRepository.isScanning) {
            return
        }

        scanScope.launch {
            try {
                val results: MutableMap<String, ExtendedBluetoothDevice> =
                    scannerRepository.startScan(MeshManagerApi.MESH_PROVISIONING_UUID, timeoutMs)
                Log.d(tag, "scanUnprovisionedDevices: ${results.keys}")

                val bluetoothDevices = results.map { (macAddress, bluetoothDevice) ->
                    bluetoothDevice
                }

                unprovisionedBluetoothDevices.clear()
                unprovisionedBluetoothDevices.addAll(bluetoothDevices)

                callback.onScanCompleted(bluetoothDevices)
            } catch (e: Exception) {
                callback.onScanFailed(e.message ?: "Unknown Error")
            }
        }
    }

    fun scanProvisionedDevices(callback: ScanCallback, timeoutMs: Int = 5000) {
        if (!Permissions.isBleEnabled(context)) {
            return
        }

        if (!Permissions.isLocationGranted(context)) {
            return
        }

        if (scannerRepository.isScanning) {
            return
        }

        scanScope.launch {
            try {
                val results: MutableMap<String, ExtendedBluetoothDevice> =
                    scannerRepository.startScan(MeshManagerApi.MESH_PROXY_UUID, timeoutMs)
                Log.d(tag, "scanProvisionedDevices: ${results.keys}")

                val bluetoothDevices = results.map { (macAddress, bluetoothDevice) ->
                    bluetoothDevice
                }

                callback.onScanCompleted(bluetoothDevices)
            } catch (e: Exception) {
                callback.onScanFailed(e.message ?: "Unknown Error")
            }
        }
    }

    fun getProvisioningCapabilities(uuid: UUID): CompletableDeferred<UnprovisionedMeshNode?> {
        val deferred = CompletableDeferred<UnprovisionedMeshNode?>()
        provisioningCapabilitiesMap[uuid] = deferred

        val bluetoothDevice = unprovisionedBluetoothDevices.firstOrNull { device ->
            device.scanResult?.let {
                val serviceData = Utils.getServiceData(it, MeshManagerApi.MESH_PROVISIONING_UUID)
                val deviceUuid = meshManagerApi.getDeviceUuid(serviceData!!)
                deviceUuid == uuid
            } ?: false
        }

        try {
            bluetoothDevice?.device?.let {
                bleMeshManager.connect(it).retry(3, 200).await()
                meshManagerApi.identifyNode(uuid)
            } ?: deferred.complete(null)
        } catch (e: Exception) {
            deferred.completeExceptionally(e)
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

    fun provisionDevice(uuid: UUID): CompletableDeferred<MeshDevice?>? {
        val deferred = CompletableDeferred<MeshDevice?>()
        provisioningStatusMap[uuid.toString()] = deferred

        val unprovisionedMeshNode = unprovisionedMeshNodes.firstOrNull { node ->
            node.deviceUuid == uuid
        }

        if (unprovisionedMeshNode == null) {
            Log.e(tag, "Unprovisioned Mesh Node not found, try identifying the node first")
            return null
        }

        val provisioner = meshManagerApi.meshNetwork?.selectedProvisioner
        val unicastAddress = meshManagerApi.meshNetwork?.nextAvailableUnicastAddress(
            unprovisionedMeshNode.numberOfElements,
            provisioner!!
        )
        meshManagerApi.meshNetwork?.assignUnicastAddress(unicastAddress!!)

        meshManagerApi.startProvisioning(unprovisionedMeshNode)

        return deferred
    }

    fun onProvisioningFinish(meshDevice: MeshDevice?) {
        when (meshDevice) {
            is MeshDevice.Provisioned -> {
                val uuid = meshDevice.node.uuid
                provisioningStatusMap[uuid]?.complete(meshDevice)
                provisioningStatusMap.remove(uuid)
                currentProvisionedMeshNode = meshDevice.node
            }

            is MeshDevice.Unprovisioned -> {
                val uuid = meshDevice.node.deviceUuid
                provisioningStatusMap[uuid.toString()]?.complete(meshDevice)
                provisioningStatusMap.remove(uuid.toString())
            }

            null -> {
                Log.e(tag, "Unknown provisioning  state")
            }
        }
    }

    fun unprovisionDevice(unicastAddress: Int) {
        val provisionedNode = meshManagerApi.meshNetwork?.getNode(unicastAddress) ?: return

        val configNodeReset = ConfigNodeReset()
        meshManagerApi.createMeshPdu(unicastAddress, configNodeReset)
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

    fun resetMeshNetwork() {
        meshManagerApi.resetMeshNetwork()
    }

    fun identifyNode(uuid: UUID) {
        meshManagerApi.identifyNode(uuid)
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
        sequenceNumber: Int,
        transitionStep: Int? = 0,
        transitionResolution: Int? = 0,
        delay: Int = 0
    ) {
        val meshMessage: MeshMessage = GenericOnOffSet(
            meshManagerApi.meshNetwork!!.getAppKey(keyIndex),
            value,
            sequenceNumber,
            transitionStep,
            transitionResolution,
            delay
        )
        meshManagerApi.createMeshPdu(address, meshMessage)
    }
}

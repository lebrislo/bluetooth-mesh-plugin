package com.lebrislo.bluetooth.mesh

import android.content.Context
import android.util.Log
import com.lebrislo.bluetooth.mesh.ble.BleCallbacksManager
import com.lebrislo.bluetooth.mesh.ble.BleMeshManager
import com.lebrislo.bluetooth.mesh.models.ExtendedBluetoothDevice
import com.lebrislo.bluetooth.mesh.scanner.ScanCallback
import com.lebrislo.bluetooth.mesh.scanner.ScannerRepository
import com.lebrislo.bluetooth.mesh.utils.Permissions
import com.lebrislo.bluetooth.mesh.utils.Utils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import no.nordicsemi.android.mesh.MeshManagerApi
import no.nordicsemi.android.mesh.provisionerstates.UnprovisionedMeshNode
import no.nordicsemi.android.mesh.transport.ConfigModelAppBind
import no.nordicsemi.android.mesh.transport.GenericOnOffSet
import no.nordicsemi.android.mesh.transport.MeshMessage
import no.nordicsemi.android.mesh.transport.ProvisionedMeshNode
import java.util.UUID

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

    fun provisionDevice(uuid: UUID) {
        val bluetoothDevice: ExtendedBluetoothDevice? =
            unprovisionedBluetoothDevices.firstOrNull { device ->
                val serviceData = Utils.getServiceData(
                    device.scanResult!!,
                    MeshManagerApi.MESH_PROVISIONING_UUID
                )
                val deviceUuid: UUID = meshManagerApi.getDeviceUuid(serviceData!!)
                deviceUuid == uuid
            }

        bleMeshManager.connect(bluetoothDevice?.device!!).retry(3, 200).await()
        meshManagerApi.identifyNode(uuid)
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

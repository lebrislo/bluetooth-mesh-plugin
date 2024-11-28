package com.lebrislo.bluetooth.mesh

import android.bluetooth.BluetoothDevice
import android.util.Log
import com.lebrislo.bluetooth.mesh.ble.BleCallbacksManager
import com.lebrislo.bluetooth.mesh.ble.BleMeshManager
import com.lebrislo.bluetooth.mesh.models.ExtendedBluetoothDevice
import com.lebrislo.bluetooth.mesh.scanner.ScannerRepository
import com.lebrislo.bluetooth.mesh.utils.Utils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import no.nordicsemi.android.mesh.MeshManagerApi

class BleController(private val bleMeshManager: BleMeshManager, private val meshManagerApi: MeshManagerApi) {
    private val tag: String = BleController::class.java.simpleName

    private val bleCallbacksManager: BleCallbacksManager = BleCallbacksManager(meshManagerApi)
    private val scannerRepository: ScannerRepository = ScannerRepository(meshManagerApi)

    private var autoReconnect: Boolean = true

    init {
        bleMeshManager.setGattCallbacks(bleCallbacksManager)

        bleCallbacksManager.setDisconnectionCallback { onBluetoothDeviceDisconnected() }
        scannerRepository.setMeshProxyScannedCallback { onMeshProxyScanned(it) }
    }

    private fun onBluetoothDeviceDisconnected() {
        Log.i(tag, "Bluetooth is disconnected, restarting scan ${this.autoReconnect}")
        if (this.autoReconnect) this.restartMeshDevicesScan()
    }

    private fun onMeshProxyScanned(proxy: ExtendedBluetoothDevice) {
        if (!bleMeshManager.isConnected && proxy.device != null && this.autoReconnect) {
            Log.i(tag, "Bluetooth disconnected : Connecting to mesh proxy ${proxy.device.address}")
            CoroutineScope(Dispatchers.IO).launch {
                connectBle(proxy.device)
            }
        }
    }

    /**
     * Connect to a Bluetooth device
     *
     * @param bluetoothDevice BluetoothDevice to connect to
     * @param autoReconnect whether to auto reconnect
     *
     * @return Boolean whether the connection was successful
     */
    fun connectBle(bluetoothDevice: BluetoothDevice, autoReconnect: Boolean = true): Boolean {
        try {
            this.autoReconnect = autoReconnect
            bleMeshManager.connect(bluetoothDevice).retry(3, 1000).await()
            return bleMeshManager.isConnected
        } catch (e: Exception) {
            Log.e(tag, "Failed to connect to bluetooth device ${bluetoothDevice.address}")
            return false
        }
    }

    /**
     * Disconnect from a Bluetooth device
     *
     * @param autoReconnect whether to auto reconnect
     */
    fun disconnectBle(autoReconnect: Boolean = true) {
        try {
            this.autoReconnect = autoReconnect
            bleMeshManager.disconnect().await()
        } catch (e: Exception) {
            Log.e(tag, "Failed to disconnect from bluetooth device")
        }
    }

    /**
     * Check if the application is connected to a Bluetooth device
     *
     * @return Boolean
     */
    fun isBleConnected(): Boolean {
        return bleMeshManager.isConnected
    }

    /**
     * Get the connected Bluetooth device
     *
     * @return BluetoothDevice?
     */
    fun connectedDevice(): BluetoothDevice? {
        return bleMeshManager.bluetoothDevice
    }

    /**
     * Stop scanning for mesh devices
     */
    fun stopScan() {
        scannerRepository.stopScanDevices()
    }

    /**
     * Scan for mesh devices and return a list of freshly scanned devices
     *
     * @param scanDurationMs duration of the scan in milliseconds
     *
     * @return List<ExtendedBluetoothDevice>
     */
    suspend fun getMeshDevices(scanDurationMs: Int = 5000): List<ExtendedBluetoothDevice> {
        scannerRepository.unprovisionedDevices.clear()
        scannerRepository.provisionedDevices.clear()
        scannerRepository.stopScanDevices()
        scannerRepository.startScanDevices()
        delay(scanDurationMs.toLong())
        return scannerRepository.unprovisionedDevices + scannerRepository.provisionedDevices
    }

    /**
     * Restart scanning for mesh devices
     */
    fun restartMeshDevicesScan() {
        scannerRepository.unprovisionedDevices.clear()
        scannerRepository.provisionedDevices.clear()
        scannerRepository.stopScanDevices()
        scannerRepository.startScanDevices()
    }

    /**
     * Search for a mesh proxy to connect to
     *
     * @return BluetoothDevice?
     */
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
                    disconnectBle(false)
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
}
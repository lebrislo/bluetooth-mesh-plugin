package com.lebrislo.bluetooth.mesh

import android.bluetooth.BluetoothDevice
import android.util.Log
import com.lebrislo.bluetooth.mesh.ble.BleCallbacksManager
import com.lebrislo.bluetooth.mesh.ble.BleMeshManager
import com.lebrislo.bluetooth.mesh.models.ExtendedBluetoothDevice
import com.lebrislo.bluetooth.mesh.scanner.DeviceScanner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import no.nordicsemi.android.mesh.MeshManagerApi
import no.nordicsemi.android.mesh.transport.GenericOnOffGet

class BleController(private val bleMeshManager: BleMeshManager, private val meshManagerApi: MeshManagerApi) {
    private val tag: String = BleController::class.java.simpleName

    private val bleCallbacksManager: BleCallbacksManager = BleCallbacksManager(meshManagerApi)
    private val scannerRepository: DeviceScanner = DeviceScanner(meshManagerApi)

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
                meshManagerApi.createMeshPdu(0xFFFF, GenericOnOffGet(meshManagerApi.meshNetwork!!.getAppKey(0)))
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
        Log.i(tag, "Connecting to bluetooth device ${bluetoothDevice.address} with autoReconnect $autoReconnect")
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
        Log.i(tag, "Disconnecting from bluetooth device with autoReconnect $autoReconnect")
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
     * Get the list of unprovisioned devices
     *
     * @return List<ExtendedBluetoothDevice>
     */
    fun getUnprovisionedDevices(): List<ExtendedBluetoothDevice> {
        return scannerRepository.getUnprovisionedDevices()
    }

    /**
     * Get the list of provisioned devices
     *
     * @return List<ExtendedBluetoothDevice>
     */
    fun getProvisionedDevices(): List<ExtendedBluetoothDevice> {
        return scannerRepository.getProvisionedDevices()
    }

    /**
     * Restart scanning for mesh devices
     */
    fun restartMeshDevicesScan() {
        scannerRepository.clearDevices()
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

            val isMeshProxy = scannerRepository.getProvisionedDevices().any() { device ->
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

        Log.d(tag, "searchProxyMesh : Provisioned devices: ${scannerRepository.getProvisionedDevices().size}")

        if (scannerRepository.getProvisionedDevices().isNotEmpty()) {
            val device = scannerRepository.getProvisionedDevices()
                .maxByOrNull { device -> device.scanResult?.rssi ?: Int.MIN_VALUE }?.device
            Log.i(tag, "searchProxyMesh : Found a mesh proxy ${device?.address}")
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
            if (scannerRepository.getUnprovisionedDevices()
                    .any { device -> device.scanResult?.device?.address == macAddress }
            ) {
                return bleMeshManager.bluetoothDevice
            } else {
                withContext(Dispatchers.IO) {
                    disconnectBle(false)
                }
            }
        }

        return scannerRepository.getUnprovisionedDevices().firstOrNull { device ->
            device.scanResult?.let {
                device.getDeviceUuid().toString() == uuid
            } ?: false
        }?.device
    }
}
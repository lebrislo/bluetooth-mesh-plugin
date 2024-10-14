package com.lebrislo.bluetooth.mesh.scanner

import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import com.lebrislo.bluetooth.mesh.models.ExtendedBluetoothDevice
import com.lebrislo.bluetooth.mesh.utils.Utils
import no.nordicsemi.android.mesh.MeshManagerApi
import no.nordicsemi.android.mesh.MeshNetwork
import no.nordicsemi.android.support.v18.scanner.BluetoothLeScannerCompat
import no.nordicsemi.android.support.v18.scanner.ScanCallback
import no.nordicsemi.android.support.v18.scanner.ScanFilter
import no.nordicsemi.android.support.v18.scanner.ScanResult
import no.nordicsemi.android.support.v18.scanner.ScanSettings
import java.util.UUID


/**
 * Repository for scanning for bluetooth mesh devices
 */
class ScannerRepository(
    private val context: Context,
    private val meshManagerApi: MeshManagerApi
) {
    private val tag: String = ScannerRepository::class.java.simpleName

    var isScanning: Boolean = false

    val unprovisionedDevices: MutableList<ExtendedBluetoothDevice> = mutableListOf()
    val provisionedDevices: MutableList<ExtendedBluetoothDevice> = mutableListOf()

    private val scanCallback: ScanCallback = object : ScanCallback() {

        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val serviceUuid = result.scanRecord?.serviceUuids?.get(0)?.uuid

            if (serviceUuid == MeshManagerApi.MESH_PROVISIONING_UUID) {
                Log.v(tag, "Unprovisioned device discovered: ${result.device.address}")
                unprovDeviceDiscovered(result)
            } else if (serviceUuid == MeshManagerApi.MESH_PROXY_UUID) {
                val serviceData: ByteArray? = Utils.getServiceData(result, MeshManagerApi.MESH_PROXY_UUID)
                Log.v(tag, "Proxy discovered: ${result.device.address}")
                if (meshManagerApi.isAdvertisingWithNetworkIdentity(serviceData)) {
                    if (meshManagerApi.networkIdMatches(serviceData)) {
                        provDeviceDiscovered(result)
                    }
                } else if (meshManagerApi.isAdvertisedWithNodeIdentity(serviceData)) {
                    if (checkIfNodeIdentityMatches(serviceData!!)) {
                        provDeviceDiscovered(result)
                    }
                }
            }
        }

        override fun onBatchScanResults(results: List<ScanResult>) {
            // Batch scan is disabled (report delay = 0)
        }

        override fun onScanFailed(errorCode: Int) {
            stopScan()
        }
    }

    private fun unprovDeviceDiscovered(result: ScanResult) {
        val device: ExtendedBluetoothDevice
        val scanRecord = result.scanRecord

        if (scanRecord != null) {
            if (scanRecord.bytes != null && scanRecord.serviceUuids != null) {
                device = ExtendedBluetoothDevice(result)
                if (!unprovisionedDevices.contains(device)) {
                    Log.d(tag, "Unprovisioned device discovered: ${result.device.address} ")
                    unprovisionedDevices.add(device)

                    // Delete the node from the mesh network if it was previously provisioned
                    val serviceData = Utils.getServiceData(
                        device.scanResult!!,
                        MeshManagerApi.MESH_PROVISIONING_UUID
                    )

                    if (serviceData == null || serviceData.size < 18) return

                    val deviceUuid: UUID = meshManagerApi.getDeviceUuid(serviceData)
                    meshManagerApi.meshNetwork?.nodes?.forEach { node ->
                        if (node.uuid == deviceUuid.toString()) {
                            meshManagerApi.meshNetwork?.deleteNode(node)
                        }
                    }
                }
            }
        }
    }

    private fun provDeviceDiscovered(result: ScanResult) {
        val device: ExtendedBluetoothDevice
        val scanRecord = result.scanRecord

        if (scanRecord != null) {
            if (scanRecord.bytes != null && scanRecord.serviceUuids != null) {
                device = ExtendedBluetoothDevice(result)
                if (!provisionedDevices.contains(device)) {
                    Log.d(tag, "Provisioned device discovered: ${result.device.address} ")
                    provisionedDevices.add(device)
                }
            }
        }
    }

    /**
     * Start scanning for bluetooth devices that are advertising with the Mesh Provisioning or Mesh Proxy service UUIDs.
     *
     * MESH_PROVISIONING_UUID: 00001827-0000-1000-8000-00805F9B34FB
     * MESH_PROXY_UUID: 00001828-0000-1000-8000-00805F9B34FB
     */
    fun startScanDevices() {
        if (isScanning) {
            return
        }

        isScanning = true

        //Scanning settings
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY) // Refresh the devices list every second
            .setReportDelay(0) // Hardware filtering has some issues on selected devices
            .setUseHardwareFilteringIfSupported(false) // Samsung S6 and S6 Edge report equal value of RSSI for all devices. In this app we ignore the RSSI.
            .build()

        //Let's use the filter to scan only for unprovisioned mesh nodes.
        val filters: MutableList<ScanFilter> = ArrayList()
        filters.add(
            ScanFilter.Builder().setServiceUuid(
                ParcelUuid(
                    (MeshManagerApi.MESH_PROVISIONING_UUID)
                )
            ).build()
        )
        filters.add(
            ScanFilter.Builder().setServiceUuid(
                ParcelUuid(
                    (MeshManagerApi.MESH_PROXY_UUID)
                )
            ).build()
        )

        val scanner: BluetoothLeScannerCompat = BluetoothLeScannerCompat.getScanner()
        scanner.startScan(filters, settings, scanCallback)
    }

    /**
     * stop scanning for bluetooth devices.
     */
    fun stopScan() {
        val scanner: BluetoothLeScannerCompat = BluetoothLeScannerCompat.getScanner()
        scanner.stopScan(scanCallback)
    }

    /**
     * Check if node identity matches
     *
     * @param serviceData service data received from the advertising data
     * @return true if the node identity matches or false otherwise
     */
    private fun checkIfNodeIdentityMatches(serviceData: ByteArray): Boolean {
        val network: MeshNetwork? = meshManagerApi.meshNetwork
        if (network != null) {
            for (node in network.nodes) {
                if (meshManagerApi.nodeIdentityMatches(node, serviceData)) {
                    return true
                }
            }
        }
        return false
    }
}

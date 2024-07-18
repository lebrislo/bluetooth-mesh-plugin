package com.lebrislo.bluetooth.mesh.scanner

import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import com.lebrislo.bluetooth.mesh.models.ExtendedBluetoothDevice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import no.nordicsemi.android.mesh.MeshBeacon
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
    private var filterUuid: UUID? = null
    private var scanJob: Job? = null
    var isScanning: Boolean = false
    var discoveredDevices: MutableMap<String, ExtendedBluetoothDevice> = mutableMapOf()

    private val scanCallbacks: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            try {
                if (filterUuid == MeshManagerApi.MESH_PROVISIONING_UUID) {
                    updateScannerData(result)
                } else if (filterUuid == MeshManagerApi.MESH_PROXY_UUID) {
//                    TODO: Process the scan result
//                    val serviceData: ByteArray? =
//                        Utils.getServiceData(result, MeshManagerApi.MESH_PROXY_UUID)
//                    if (meshManagerApi.isAdvertisingWithNetworkIdentity(serviceData)) {
//                        if (meshManagerApi.networkIdMatches(serviceData)) {
//                            updateScannedData(result)
//                        }
//                    } else if (meshManagerApi.isAdvertisedWithNodeIdentity(serviceData)) {
//                        if (checkIfNodeIdentityMatches(serviceData!!)) {
//                            updateScannedData(result)
//                        }
//                    }
                    updateScannerData(result)
                }
            } catch (ex: Exception) {
                Log.e(tag, "Error: " + ex.message)
            }
        }

        override fun onBatchScanResults(results: List<ScanResult>) {
            // Batch scan is disabled (report delay = 0)
        }

        override fun onScanFailed(errorCode: Int) {
            stopScan()
        }
    }

    private fun updateScannerData(result: ScanResult) {
        val scanRecord = result.scanRecord
        if (scanRecord != null) {
            if (scanRecord.bytes != null) {
                val beaconData = meshManagerApi.getMeshBeaconData(scanRecord.bytes!!)
                if (beaconData != null) {
                    deviceDiscovered(result, meshManagerApi.getMeshBeacon(beaconData)!!)
                } else {
                    deviceDiscovered(result)
                }
            }
        }
    }

    private fun deviceDiscovered(result: ScanResult, meshBeacon: MeshBeacon) {
        val device: ExtendedBluetoothDevice
        val scanRecord = result.scanRecord

        if (scanRecord != null) {
            if (scanRecord.bytes != null && scanRecord.serviceUuids != null) {
                device = ExtendedBluetoothDevice(result, meshBeacon)
                discoveredDevices[result.device.address] = device
            }
        }
    }

    private fun deviceDiscovered(result: ScanResult) {
        val device: ExtendedBluetoothDevice
        val scanRecord = result.scanRecord

        if (scanRecord != null) {
            if (scanRecord.bytes != null && scanRecord.serviceUuids != null) {
                device = ExtendedBluetoothDevice(result)
                discoveredDevices[result.device.address] = device
            }
        }
    }

    /**
     * Start scanning for Bluetooth devices.
     *
     * @param filterUuid UUID to filter scan results with
     */
    private fun scanDevices(filterUuid: UUID, timeoutMs: Int) {
        this.filterUuid = filterUuid
        discoveredDevices = mutableMapOf()

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
                    (filterUuid)
                )
            ).build()
        )

        val scanner: BluetoothLeScannerCompat = BluetoothLeScannerCompat.getScanner()
        scanner.startScan(filters, settings, scanCallbacks)

        scanJob = CoroutineScope(Dispatchers.Main).launch {
            delay(timeoutMs.toLong())
            stopScan()
        }
    }

    /**
     * Suspend function to scan for Bluetooth devices and return results after a timeout.
     *
     * @param filterUuid UUID to filter scan results with
     * @param timeout The duration (in milliseconds) for which the scan should run
     * @return List of ScanResult
     */
    suspend fun startScan(
        filterUuid: UUID,
        timeout: Int
    ): MutableMap<String, ExtendedBluetoothDevice> {
        scanDevices(filterUuid, timeout)
        delay(timeout.toLong())
        stopScan()
        return discoveredDevices
    }

    /**
     * stop scanning for bluetooth devices.
     */
    fun stopScan() {
        val scanner: BluetoothLeScannerCompat = BluetoothLeScannerCompat.getScanner()
        scanner.stopScan(scanCallbacks)
        isScanning = false
        scanJob?.cancel()
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

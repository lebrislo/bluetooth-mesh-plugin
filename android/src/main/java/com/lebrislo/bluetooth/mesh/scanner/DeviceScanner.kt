package com.lebrislo.bluetooth.mesh.scanner

import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.util.Log
import com.getcapacitor.JSArray
import com.getcapacitor.JSObject
import com.lebrislo.bluetooth.mesh.BluetoothMeshPlugin
import com.lebrislo.bluetooth.mesh.models.ExtendedBluetoothDevice
import com.lebrislo.bluetooth.mesh.permissions.PermissionsManager
import com.lebrislo.bluetooth.mesh.utils.NotificationManager
import com.lebrislo.bluetooth.mesh.utils.Utils
import no.nordicsemi.android.mesh.MeshManagerApi
import no.nordicsemi.android.mesh.MeshNetwork
import no.nordicsemi.android.support.v18.scanner.BluetoothLeScannerCompat
import no.nordicsemi.android.support.v18.scanner.ScanCallback
import no.nordicsemi.android.support.v18.scanner.ScanFilter
import no.nordicsemi.android.support.v18.scanner.ScanResult
import no.nordicsemi.android.support.v18.scanner.ScanSettings
import java.util.concurrent.ConcurrentHashMap


/**
 * DeviceScanner is a class that provides the functionality to scan for bluetooth devices
 * advertising with the Mesh Provisioning or Mesh Proxy service UUIDs.
 */
class DeviceScanner(
    private val meshManagerApi: MeshManagerApi
) {
    private val tag: String = "DeviceScanner"
    private var meshProxyScannedCallback: ((proxy: ExtendedBluetoothDevice) -> Unit)? = null
    private var isScanning: Boolean = false

    private val unprovisionedDevices: ConcurrentHashMap<String, ExtendedBluetoothDevice> = ConcurrentHashMap()
    private val provisionedDevices: ConcurrentHashMap<String, ExtendedBluetoothDevice> = ConcurrentHashMap()
    private val handler = Handler(Looper.getMainLooper())
    private val deviceTimeouts: ConcurrentHashMap<String, Runnable> = ConcurrentHashMap()

    private val scanCallback: ScanCallback = object : ScanCallback() {

        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val serviceUuid = result.scanRecord?.serviceUuids?.get(0)?.uuid
            when (serviceUuid) {
                MeshManagerApi.MESH_PROVISIONING_UUID -> {
                    Log.v(tag, "Unprovisioned device discovered: ${result.device.address}")
                    unprovDeviceDiscovered(result)
                }

                MeshManagerApi.MESH_PROXY_UUID -> {
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
        }

        override fun onScanFailed(errorCode: Int) {
            stopScanDevices()
        }
    }

    private fun unprovDeviceDiscovered(result: ScanResult) {
        val device = ExtendedBluetoothDevice(result)
        val address = device.address

        synchronized(unprovisionedDevices) {
            if (!unprovisionedDevices.containsKey(address)) {
                unprovisionedDevices[address] = device
                Log.d(tag, "Added unprovisioned device: $address")
                synchronized(provisionedDevices) {
                    provisionedDevices.remove(address)?.let {
                        Log.d(tag, "Removed from provisioned devices: $address")
                    }
                }
                notifyMeshDeviceScanned()
            }
            resetDeviceTimeout(address, unprovisionedDevices)
        }
    }

    private fun provDeviceDiscovered(result: ScanResult) {
        val device = ExtendedBluetoothDevice(result)
        val address = device.address

        synchronized(provisionedDevices) {
            if (!provisionedDevices.containsKey(address)) {
                provisionedDevices[address] = device
                Log.d(tag, "Added provisioned device: $address")
                synchronized(unprovisionedDevices) {
                    unprovisionedDevices.remove(address)?.let {
                        Log.d(tag, "Removed from unprovisioned devices: $address")
                    }
                }
                notifyMeshDeviceScanned()
            }
            resetDeviceTimeout(address, provisionedDevices)
        }
    }

    private fun resetDeviceTimeout(address: String, deviceMap: ConcurrentHashMap<String, ExtendedBluetoothDevice>) {
        deviceTimeouts[address]?.let { handler.removeCallbacks(it) }

        val timeoutRunnable = Runnable {
            synchronized(deviceMap) {
                if (deviceMap.containsKey(address)) {
                    deviceMap.remove(address)
                    Log.d(tag, "Removed device due to timeout: $address")
                    notifyMeshDeviceScanned()
                }
            }
        }

        deviceTimeouts[address] = timeoutRunnable
        handler.postDelayed(timeoutRunnable, 10000)
    }

    private fun notifyMeshDeviceScanned() {
        val scanNotification = JSObject().apply {
            put("unprovisioned", JSArray().apply {
                unprovisionedDevices.values.forEach {
                    put(JSObject().apply {
                        put("uuid", it.getDeviceUuid().toString())
                        put("macAddress", it.address)
                        put("rssi", it.rssi)
                        put("name", it.name)
                    })
                }
            })
            put("provisioned", JSArray().apply {
                provisionedDevices.values.forEach {
                    put(JSObject().apply {
                        put("uuid", it.getDeviceUuid().toString())
                        put("macAddress", it.address)
                        put("rssi", it.rssi)
                        put("name", it.name)
                    })
                }
            })
        }

        NotificationManager.getInstance().sendNotification(BluetoothMeshPlugin.MESH_DEVICE_SCAN_EVENT, scanNotification)
    }

    fun getUnprovisionedDevices(): List<ExtendedBluetoothDevice> {
        return unprovisionedDevices.values.toList()
    }

    fun getProvisionedDevices(): List<ExtendedBluetoothDevice> {
        return provisionedDevices.values.toList()
    }

    fun clearDevices() {
        unprovisionedDevices.clear()
        provisionedDevices.clear()
    }

    fun setMeshProxyScannedCallback(callback: (proxy: ExtendedBluetoothDevice) -> Unit) {
        this.meshProxyScannedCallback = callback
    }

    fun startScanDevices() {
        val permission = PermissionsManager.getInstance().checkPermissions()
        Log.i(tag, "Permission to scan devices: $permission")
        if (permission != PackageManager.PERMISSION_GRANTED) {
            PermissionsManager.getInstance().requestPermissions()
            return
        }

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setReportDelay(0)
            .setUseHardwareFilteringIfSupported(false)
            .build()

        val filters: MutableList<ScanFilter> = ArrayList()
        filters.add(ScanFilter.Builder().setServiceUuid(ParcelUuid(MeshManagerApi.MESH_PROVISIONING_UUID)).build())
        filters.add(ScanFilter.Builder().setServiceUuid(ParcelUuid(MeshManagerApi.MESH_PROXY_UUID)).build())

        synchronized(this) {
            if (isScanning) return
            isScanning = true
            BluetoothLeScannerCompat.getScanner().startScan(filters, settings, scanCallback)
        }
    }

    fun stopScanDevices() {
        synchronized(this) {
            if (!isScanning) return
            isScanning = false
            BluetoothLeScannerCompat.getScanner().stopScan(scanCallback)
            deviceTimeouts.values.forEach { handler.removeCallbacks(it) }
            deviceTimeouts.clear()
        }
    }

    private fun checkIfNodeIdentityMatches(serviceData: ByteArray): Boolean {
        val network: MeshNetwork? = meshManagerApi.meshNetwork
        return network?.nodes?.any { meshManagerApi.nodeIdentityMatches(it, serviceData) } ?: false
    }
}


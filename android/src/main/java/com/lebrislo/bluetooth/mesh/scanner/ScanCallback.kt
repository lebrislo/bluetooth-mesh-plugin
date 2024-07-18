package com.lebrislo.bluetooth.mesh.scanner

import com.lebrislo.bluetooth.mesh.models.ExtendedBluetoothDevice

interface ScanCallback {
    fun onScanCompleted(bleMeshDevices: List<ExtendedBluetoothDevice>?)
    fun onScanFailed(error: String)
}

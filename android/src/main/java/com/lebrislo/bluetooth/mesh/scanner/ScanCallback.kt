package com.lebrislo.bluetooth.mesh.scanner

import com.lebrislo.bluetooth.mesh.models.BleMeshDevice

interface ScanCallback {
    fun onScanCompleted(bleMeshDevices: List<BleMeshDevice>)
    fun onScanFailed(error: String)
}

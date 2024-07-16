package com.lebrislo.bluetooth.mesh.scanner

import com.lebrislo.bluetooth.mesh.models.UnprovisionedDevice

interface ScanCallback {
    fun onScanCompleted(unprovisionedDevices: List<UnprovisionedDevice>)
    fun onScanFailed(error: String)
}

package com.lebrislo.bluetooth.mesh.models

class BleMeshDevice(
    val rssi: Int,
    val macAddress: String,
    val name: String,
    val uuid: ByteArray? = null,
    val advData: ByteArray
) {
}
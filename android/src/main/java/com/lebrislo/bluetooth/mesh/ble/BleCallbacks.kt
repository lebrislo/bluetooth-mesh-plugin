package com.lebrislo.bluetooth.mesh.ble

import android.bluetooth.BluetoothDevice
import no.nordicsemi.android.ble.BleManagerCallbacks

interface BleCallbacks : BleManagerCallbacks {
    /**
     * Called when the node sends some data back to the provisioner
     * @param bluetoothDevice
     * @param mtu
     * @param pdu the data received from the device
     */
    fun onDataReceived(bluetoothDevice: BluetoothDevice?, mtu: Int, pdu: ByteArray?)

    /**
     * Called when the data has been sent to the connected device.
     * @param device
     * @param mtu
     * @param pdu that was sent to the node
     */
    fun onDataSent(device: BluetoothDevice?, mtu: Int, pdu: ByteArray?)
}

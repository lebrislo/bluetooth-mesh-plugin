package com.lebrislo.bluetooth.mesh.ble

import android.bluetooth.BluetoothDevice
import android.util.Log
import no.nordicsemi.android.mesh.MeshManagerApi

class BleCallbacksManager(val meshManagerApi: MeshManagerApi) : BleCallbacks {
    private val tag: String = BleCallbacksManager::class.java.simpleName
    private var disconnectionCallback: (() -> Unit)? = null

    fun setDisconnectionCallback(callback: () -> Unit) {
        disconnectionCallback = callback
    }

    override fun onDataReceived(bluetoothDevice: BluetoothDevice?, mtu: Int, pdu: ByteArray?) {
        meshManagerApi.handleNotifications(mtu, pdu!!)
    }

    override fun onDataSent(device: BluetoothDevice?, mtu: Int, pdu: ByteArray?) {
        meshManagerApi.handleWriteCallbacks(mtu, pdu!!)
    }

    override fun onDeviceConnecting(device: BluetoothDevice) {
        Log.d(tag, "onDeviceConnecting")
    }

    override fun onDeviceConnected(device: BluetoothDevice) {
        Log.d(tag, "onDeviceConnected")
    }

    override fun onDeviceDisconnecting(device: BluetoothDevice) {
        Log.d(tag, "onDeviceDisconnecting")
    }

    override fun onDeviceDisconnected(device: BluetoothDevice) {
        Log.d(tag, "onDeviceDisconnected")
        disconnectionCallback?.invoke()
    }

    override fun onLinkLossOccurred(device: BluetoothDevice) {
        Log.d(tag, "onLinkLossOccurred")
    }

    override fun onServicesDiscovered(device: BluetoothDevice, optionalServicesFound: Boolean) {
        Log.d(tag, "onServicesDiscovered")
    }

    override fun onDeviceReady(device: BluetoothDevice) {
        Log.d(tag, "onDeviceReady")
    }

    override fun onBondingRequired(device: BluetoothDevice) {
        Log.d(tag, "onBondingRequired")
    }

    override fun onBonded(device: BluetoothDevice) {

    }

    override fun onBondingFailed(device: BluetoothDevice) {

    }

    override fun onError(device: BluetoothDevice, message: String, errorCode: Int) {

    }

    override fun onDeviceNotSupported(device: BluetoothDevice) {

    }
}
package com.lebrislo.bluetooth.mesh.utils

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.getcapacitor.JSObject
import com.lebrislo.bluetooth.mesh.NrfMeshPlugin
import com.lebrislo.bluetooth.mesh.NrfMeshPlugin.Companion.BLUETOOTH_ADAPTER_EVENT_STRING
import com.lebrislo.bluetooth.mesh.NrfMeshPlugin.Companion.BLUETOOTH_CONNECTION_EVENT_STRING

class BluetoothStateReceiver(private val plugin: NrfMeshPlugin) : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
        when (state) {
            BluetoothAdapter.STATE_ON -> {
                NotificationManager.getInstance()
                    .sendNotification(BLUETOOTH_ADAPTER_EVENT_STRING, JSObject().put("enabled", true))
                plugin.restartScan()
            }

            BluetoothAdapter.STATE_OFF -> {
                NotificationManager.getInstance()
                    .sendNotification(BLUETOOTH_ADAPTER_EVENT_STRING, JSObject().put("enabled", false))
                plugin.stopScan()
            }
        }

        when (intent.action) {
            BluetoothDevice.ACTION_ACL_CONNECTED -> {
                NotificationManager.getInstance()
                    .sendNotification(BLUETOOTH_CONNECTION_EVENT_STRING, JSObject().put("connected", true))
            }

            BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                NotificationManager.getInstance()
                    .sendNotification(BLUETOOTH_CONNECTION_EVENT_STRING, JSObject().put("connected", false))
            }
        }
    }
}
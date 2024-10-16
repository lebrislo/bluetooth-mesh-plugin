package com.lebrislo.bluetooth.mesh.utils

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

class Permissions {

    companion object {

        fun isLocationGranted(context: Context): Boolean {
            val fineLocationGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            val coarseLocationGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            return fineLocationGranted || coarseLocationGranted
        }

        fun isBleEnabled(context: Context): Boolean {
            val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val adapter: BluetoothAdapter = btManager.adapter
            return adapter.isEnabled
        }
    }
}
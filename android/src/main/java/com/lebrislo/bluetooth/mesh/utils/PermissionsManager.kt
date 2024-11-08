package com.lebrislo.bluetooth.mesh.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat

import java.lang.ref.WeakReference

class PermissionsManager private constructor() {

    private val tag: String = PermissionsManager::class.java.simpleName
    private var activityRef: WeakReference<Activity>? = null
    private var contextRef: WeakReference<Context>? = null

    companion object {
        @Volatile
        private var instance: PermissionsManager? = null

        fun getInstance() =
            instance ?: synchronized(this) {
                instance ?: PermissionsManager().also { instance = it }
            }
    }

    fun setActivity(activity: Activity) {
        this.activityRef = WeakReference(activity)
    }

    fun setContext(context: Context) {
        this.contextRef = WeakReference(context)
    }

    private val activity: Activity?
        get() = activityRef?.get()

    private val context: Context?
        get() = contextRef?.get()

    fun requestPermissions(): Int {
        val bluetoothPermission = requestBluetoothPermissions()
        val locationPermission = requestLocationPermissions()

        return if (bluetoothPermission == PackageManager.PERMISSION_GRANTED && locationPermission == PackageManager.PERMISSION_GRANTED) {
            PackageManager.PERMISSION_GRANTED
        } else {
            PackageManager.PERMISSION_DENIED
        }
    }

    private fun requestBluetoothPermissions(): Int {
        val ctx = context ?: return PackageManager.PERMISSION_DENIED
        val act = activity ?: return PackageManager.PERMISSION_DENIED

        if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.R) {
            val result = ActivityCompat.checkSelfPermission(ctx, Manifest.permission.BLUETOOTH_ADMIN)
            Log.i(tag, "BLUETOOTH_ADMIN Permission : $result")

            if (result != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    act,
                    arrayOf(Manifest.permission.BLUETOOTH_ADMIN),
                    0
                )
            }

            return ActivityCompat.checkSelfPermission(ctx, Manifest.permission.BLUETOOTH_ADMIN)
        } else {
            val result = ActivityCompat.checkSelfPermission(ctx, Manifest.permission.BLUETOOTH_SCAN)
            Log.i(tag, "BLUETOOTH_SCAN Permission : $result")

            if (result != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    act,
                    arrayOf(Manifest.permission.BLUETOOTH_SCAN),
                    0
                )
            }

            return ActivityCompat.checkSelfPermission(ctx, Manifest.permission.BLUETOOTH_SCAN)
        }
    }

    private fun requestLocationPermissions(): Int {
        val ctx = context ?: return PackageManager.PERMISSION_DENIED
        val act = activity ?: return PackageManager.PERMISSION_DENIED

        if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.R) {
            val result = ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION)
            Log.i(tag, "ACCESS_FINE_LOCATION Permission : $result")

            if (result != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    act,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    0
                )
            }

            return ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION)
        }
        return PackageManager.PERMISSION_GRANTED
    }
}

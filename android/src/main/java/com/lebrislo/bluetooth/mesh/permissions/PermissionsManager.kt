package com.lebrislo.bluetooth.mesh.permissions

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.core.app.ActivityCompat
import java.lang.ref.WeakReference
import java.util.LinkedList
import java.util.Queue

class PermissionsManager private constructor() {

    private val tag: String = PermissionsManager::class.java.simpleName

    private var activityRef: WeakReference<Activity>? = null
    private var contextRef: WeakReference<Context>? = null

    private val permissionQueue: Queue<String> = LinkedList()

    companion object {
        @Volatile
        private var instance: PermissionsManager? = null
        private lateinit var permissionsToRequest: Array<String>

        fun getInstance() =
            instance ?: synchronized(this) {
                permissionsToRequest = if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.R) {
                    arrayOf(
                        Manifest.permission.BLUETOOTH_ADMIN,
                        Manifest.permission.BLUETOOTH,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    )
                } else {
                    arrayOf(
                        Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.BLUETOOTH_CONNECT,
                    )
                }
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

    fun requestPermissions() {
        val act = activity ?: return

        // Clear previous permissions from the queue
        permissionQueue.clear()

        // Add only the permissions that are not yet granted to the queue
        permissionsToRequest.forEach { permission ->
            if (ActivityCompat.checkSelfPermission(act, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionQueue.add(permission)
            }
        }

        // Start processing the permissions queue
        processNextPermission()
    }

    private fun processNextPermission() {
        val act = activity ?: return

        if (permissionQueue.isEmpty()) {
            // All permissions have been processed
            return
        }

        val nextPermission = permissionQueue.poll() ?: return

        val isPermanentlyDeclined = !ActivityCompat.shouldShowRequestPermissionRationale(act, nextPermission)

        // Show the permission dialog for the current permission
        val dialog = createPermissionDialog(
            activity = act,
            permissionTextProvider = when (nextPermission) {
                Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN -> BluetoothPermissionTextProvider()
                Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION -> LocationPermissionTextProvider()
                else -> return
            },
            isPermanentlyDeclined = isPermanentlyDeclined,
            onOkClick = {
                // Request the permission if not permanently declined
                ActivityCompat.requestPermissions(act, arrayOf(nextPermission), 0)
            },
            onGoToAppSettingsClick = {
                // Open app settings if permission is permanently declined
                openAppSettings(act)
            },
            onDismiss = {
                permissionQueue.clear()
            }
        )

        dialog.show()
    }

    fun checkPermissions(): Int {
        val ctx = context ?: return PackageManager.PERMISSION_DENIED

        // check if every permissions are granted
        return if (permissionsToRequest.all { permission ->
                ActivityCompat.checkSelfPermission(ctx, permission) == PackageManager.PERMISSION_GRANTED
            }) {
            PackageManager.PERMISSION_GRANTED
        } else {
            PackageManager.PERMISSION_DENIED
        }
    }

    private fun openAppSettings(activity: Activity) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", activity.packageName, null)
        }
        activity.startActivity(intent)
    }
}

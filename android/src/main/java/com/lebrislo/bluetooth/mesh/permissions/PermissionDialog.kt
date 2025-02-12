package com.lebrislo.bluetooth.mesh.permissions

import android.app.Activity
import android.app.AlertDialog

fun createPermissionDialog(
    activity: Activity,
    permissionTextProvider: PermissionTextProvider,
    isPermanentlyDeclined: Boolean,
    onOkClick: () -> Unit,
    onGoToAppSettingsClick: () -> Unit,
    onDismiss: () -> Unit
): AlertDialog {
    val dialogBuilder = AlertDialog.Builder(activity)
        .setTitle("Permission Required")
        .setMessage(permissionTextProvider.getDescription(isPermanentlyDeclined))
        .setPositiveButton(if (isPermanentlyDeclined) "Go to Settings" else "OK") { _, _ ->
            if (isPermanentlyDeclined) {
                onGoToAppSettingsClick()
            } else {
                onOkClick()
            }
        }
        .setOnDismissListener {
            onDismiss()
        }

    return dialogBuilder.create()
}


interface PermissionTextProvider {
    fun getDescription(isPermanentlyDeclined: Boolean): String
}

class BluetoothPermissionTextProvider : PermissionTextProvider {
    override fun getDescription(isPermanentlyDeclined: Boolean): String {
        return if (isPermanentlyDeclined) {
            "It seems you permanently declined Bluetooth permission. " +
                    "You can go to the app settings to grant it."
        } else {
            "This app needs access to your Bluetooth so you can connect the devices."
        }
    }
}

class LocationPermissionTextProvider : PermissionTextProvider {
    override fun getDescription(isPermanentlyDeclined: Boolean): String {
        return if (isPermanentlyDeclined) {
            "It seems you permanently declined location permission. " +
                    "You can go to the app settings to grant it."
        } else {
            "This app needs access to your location to scan for nearby devices." + "This authorization don't allow us to access your location." +
                    "Please grant the location permission."
        }
    }
}
package com.lebrislo.bluetooth.mesh

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import no.nordicsemi.android.mesh.MeshProvisioningStatusCallbacks
import no.nordicsemi.android.mesh.provisionerstates.ProvisioningState
import no.nordicsemi.android.mesh.provisionerstates.UnprovisionedMeshNode
import no.nordicsemi.android.mesh.transport.ProvisionedMeshNode

class MeshProvisioningCallbacksManager(
    var unprovisionedMeshNodes: ArrayList<UnprovisionedMeshNode>,
    var nrfMeshManager: NrfMeshManager
) :
    MeshProvisioningStatusCallbacks {
    private val tag: String = MeshProvisioningCallbacksManager::class.java.simpleName

    private val _deviceProvisioningState = MutableStateFlow<DeviceProvisioningStateData?>(null)
    val deviceProvisioningState: StateFlow<DeviceProvisioningStateData?> get() = _deviceProvisioningState

    override fun onProvisioningStateChanged(
        meshNode: UnprovisionedMeshNode?,
        state: ProvisioningState.States?,
        data: ByteArray?
    ) {
        Log.d(tag, "onProvisioningStateChanged" + meshNode?.toString())
        if (state == ProvisioningState.States.PROVISIONING_CAPABILITIES) {
            _deviceProvisioningState.value = DeviceProvisioningStateData.Success(meshNode!!, state)
        }
    }

    override fun onProvisioningFailed(
        meshNode: UnprovisionedMeshNode?,
        state: ProvisioningState.States?,
        data: ByteArray?
    ) {
        Log.d(tag, "onProvisioningFailed" + meshNode?.toString())
    }

    override fun onProvisioningCompleted(
        meshNode: ProvisionedMeshNode?,
        state: ProvisioningState.States?,
        data: ByteArray?
    ) {
        Log.d(tag, "onProvisioningCompleted" + meshNode?.toString())
    }
}

sealed class DeviceProvisioningStateData {
    data class Success(val meshNode: UnprovisionedMeshNode, val state: ProvisioningState.States) :
        DeviceProvisioningStateData()

    data class Failure(val exception: Exception) : DeviceProvisioningStateData()
}
package com.lebrislo.bluetooth.mesh

import android.util.Log
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

    override fun onProvisioningStateChanged(
        meshNode: UnprovisionedMeshNode?,
        state: ProvisioningState.States?,
        data: ByteArray?
    ) {
        Log.d(tag, "onProvisioningStateChanged" + meshNode?.toString())
        if (state == ProvisioningState.States.PROVISIONING_CAPABILITIES) {
            nrfMeshManager.meshManagerApi.startProvisioning(meshNode!!)
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
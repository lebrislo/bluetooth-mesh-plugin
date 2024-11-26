package com.lebrislo.bluetooth.mesh

import android.util.Log
import com.lebrislo.bluetooth.mesh.ble.BleMeshManager
import no.nordicsemi.android.mesh.MeshManagerCallbacks
import no.nordicsemi.android.mesh.MeshNetwork
import no.nordicsemi.android.mesh.provisionerstates.UnprovisionedMeshNode

class MeshCallbacksManager(val bleMeshManager: BleMeshManager) : MeshManagerCallbacks {
    private val tag: String = MeshCallbacksManager::class.java.simpleName

    private var meshNetwork: MeshNetwork? = null

    override fun onNetworkLoaded(meshNetwork: MeshNetwork?) {
        Log.d(tag, "onNetworkLoaded")

        if (meshNetwork == null) {
            return
        }

        Log.d(tag, "Mesh network UUID Loaded : " + meshNetwork.meshUUID)

        this.meshNetwork = meshNetwork

        if (!this.meshNetwork!!.isProvisionerSelected()) {
            val provisioner = this.meshNetwork!!.provisioners[0]
            this.meshNetwork!!.selectProvisioner(provisioner)
        }
    }

    override fun onNetworkUpdated(meshNetwork: MeshNetwork?) {

    }

    override fun onNetworkLoadFailed(error: String?) {

    }

    override fun onNetworkImported(meshNetwork: MeshNetwork?) {

    }

    override fun onNetworkImportFailed(error: String?) {

    }

    override fun sendProvisioningPdu(meshNode: UnprovisionedMeshNode?, pdu: ByteArray?) {
        bleMeshManager.sendPdu(pdu)
    }

    override fun onMeshPduCreated(pdu: ByteArray?) {
        bleMeshManager.sendPdu(pdu)
    }

    override fun getMtu(): Int {
        return bleMeshManager.maximumPacketSize
    }
}
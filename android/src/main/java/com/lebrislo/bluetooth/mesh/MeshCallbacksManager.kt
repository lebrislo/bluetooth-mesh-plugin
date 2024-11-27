package com.lebrislo.bluetooth.mesh

import android.util.Log
import com.lebrislo.bluetooth.mesh.ble.BleMeshManager
import com.lebrislo.bluetooth.mesh.utils.NodesOnlineStateManager
import no.nordicsemi.android.mesh.MeshManagerCallbacks
import no.nordicsemi.android.mesh.MeshNetwork
import no.nordicsemi.android.mesh.provisionerstates.UnprovisionedMeshNode

class MeshCallbacksManager(val bleMeshManager: BleMeshManager) : MeshManagerCallbacks {
    private val tag: String = MeshCallbacksManager::class.java.simpleName

    override fun onNetworkLoaded(meshNetwork: MeshNetwork?) {
        Log.d(tag, "onNetworkLoaded")

        if (meshNetwork == null) {
            return
        }
        Log.d(tag, "Mesh network UUID Loaded : " + meshNetwork.meshUUID)

        if (!meshNetwork.isProvisionerSelected()) {
            val provisioner = meshNetwork.provisioners[0]
            meshNetwork.selectProvisioner(provisioner)
        }

        /* Clear and add every node to NodesOnlineStateManager, except the provisioner */
        NodesOnlineStateManager.getInstance().clearNodes()
        meshNetwork.nodes.forEach { node ->
            if (node.uuid != meshNetwork.selectedProvisioner?.provisionerUuid) {
                NodesOnlineStateManager.getInstance().addNode(node.unicastAddress)
            }
        }
    }

    override fun onNetworkUpdated(meshNetwork: MeshNetwork?) {

    }

    override fun onNetworkLoadFailed(error: String?) {

    }

    override fun onNetworkImported(meshNetwork: MeshNetwork?) {
        if (meshNetwork == null) return
        Log.d(tag, "Mesh network UUID Imported : " + meshNetwork.meshUUID)

        /* Clear and add every node to NodesOnlineStateManager, except the provisioner */
        NodesOnlineStateManager.getInstance().clearNodes()
        meshNetwork.nodes.forEach { node ->
            if (node.uuid != meshNetwork.selectedProvisioner?.provisionerUuid) {
                NodesOnlineStateManager.getInstance().addNode(node.unicastAddress)
            }
        }
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
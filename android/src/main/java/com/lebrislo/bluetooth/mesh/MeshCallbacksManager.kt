package com.lebrislo.bluetooth.mesh

import android.util.Log
import no.nordicsemi.android.mesh.MeshManagerCallbacks
import no.nordicsemi.android.mesh.MeshNetwork
import no.nordicsemi.android.mesh.provisionerstates.UnprovisionedMeshNode

class MeshCallbacksManager : MeshManagerCallbacks {
    private val tag: String = MeshCallbacksManager::class.java.simpleName

    override fun onNetworkLoaded(meshNetwork: MeshNetwork?) {
        Log.d(tag, "onNetworkLoaded")
    }

    override fun onNetworkUpdated(meshNetwork: MeshNetwork?) {
        Log.d(tag, "onNetworkUpdated")
    }

    override fun onNetworkLoadFailed(error: String?) {
        Log.d(tag, "onNetworkLoadFailed")
    }

    override fun onNetworkImported(meshNetwork: MeshNetwork?) {
        Log.d(tag, "onNetworkImported")
    }

    override fun onNetworkImportFailed(error: String?) {
        Log.d(tag, "onNetworkImportFailed")
    }

    override fun sendProvisioningPdu(meshNode: UnprovisionedMeshNode?, pdu: ByteArray?) {
        Log.d(tag, "sendProvisioningPdu")
    }

    override fun onMeshPduCreated(pdu: ByteArray?) {
        Log.d(tag, "onMeshPduCreated")
    }

    override fun getMtu(): Int {
        Log.d(tag, "getMtu")
        return 0
    }
}
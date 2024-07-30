package com.lebrislo.bluetooth.mesh

import android.util.Log
import no.nordicsemi.android.mesh.MeshStatusCallbacks
import no.nordicsemi.android.mesh.transport.ConfigNodeResetStatus
import no.nordicsemi.android.mesh.transport.ControlMessage
import no.nordicsemi.android.mesh.transport.MeshMessage

class MeshStatusCallbacksManager(var nrfMeshManager: NrfMeshManager) : MeshStatusCallbacks {
    private val tag: String = MeshStatusCallbacksManager::class.java.simpleName

    override fun onTransactionFailed(dst: Int, hasIncompleteTimerExpired: Boolean) {
        Log.d(tag, "onTransactionFailed")
    }

    override fun onUnknownPduReceived(src: Int, accessPayload: ByteArray?) {
        Log.d(tag, "onUnknownPduReceived")
    }

    override fun onBlockAcknowledgementProcessed(dst: Int, message: ControlMessage) {
        Log.d(tag, "onBlockAcknowledgementProcessed")
    }

    override fun onBlockAcknowledgementReceived(src: Int, message: ControlMessage) {
        Log.d(tag, "onBlockAcknowledgementReceived")
    }

    override fun onMeshMessageProcessed(dst: Int, meshMessage: MeshMessage) {
        Log.d(tag, "onMeshMessageProcessed")
    }

    override fun onMeshMessageReceived(src: Int, meshMessage: MeshMessage) {
        Log.d(tag, "onMeshMessageReceived")
        if (meshMessage is ConfigNodeResetStatus) {
            nrfMeshManager.onNodeResetStatusReceived(meshMessage)
        }
    }

    override fun onMessageDecryptionFailed(meshLayer: String?, errorMessage: String?) {
        Log.d(tag, "onMessageDecryptionFailed")
    }
}

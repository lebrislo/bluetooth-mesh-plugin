package com.lebrislo.bluetooth.mesh

import android.util.Log
import com.lebrislo.bluetooth.mesh.plugin.PluginCallManager
import no.nordicsemi.android.mesh.MeshStatusCallbacks
import no.nordicsemi.android.mesh.transport.ConfigAppKeyStatus
import no.nordicsemi.android.mesh.transport.ConfigCompositionDataStatus
import no.nordicsemi.android.mesh.transport.ConfigHeartbeatPublicationStatus
import no.nordicsemi.android.mesh.transport.ConfigModelAppStatus
import no.nordicsemi.android.mesh.transport.ConfigNodeResetStatus
import no.nordicsemi.android.mesh.transport.ControlMessage
import no.nordicsemi.android.mesh.transport.GenericOnOffStatus
import no.nordicsemi.android.mesh.transport.GenericPowerLevelStatus
import no.nordicsemi.android.mesh.transport.LightCtlStatus
import no.nordicsemi.android.mesh.transport.LightCtlTemperatureRangeStatus
import no.nordicsemi.android.mesh.transport.LightHslStatus
import no.nordicsemi.android.mesh.transport.MeshMessage
import no.nordicsemi.android.mesh.transport.VendorModelMessageStatus

class MeshStatusCallbacksManager() : MeshStatusCallbacks {
    private val tag: String = MeshStatusCallbacksManager::class.java.simpleName

    override fun onTransactionFailed(dst: Int, hasIncompleteTimerExpired: Boolean) {

    }

    override fun onUnknownPduReceived(src: Int, accessPayload: ByteArray?) {

    }

    override fun onBlockAcknowledgementProcessed(dst: Int, message: ControlMessage) {

    }

    override fun onBlockAcknowledgementReceived(src: Int, message: ControlMessage) {

    }

    override fun onMeshMessageProcessed(dst: Int, meshMessage: MeshMessage) {
        Log.d(tag, "onMeshMessageProcessed ${meshMessage.javaClass.simpleName}")
    }

    override fun onMeshMessageReceived(src: Int, meshMessage: MeshMessage) {
        Log.d(tag, "onMeshMessageReceived ${meshMessage.javaClass.simpleName}")
        when (meshMessage) {
            is ConfigNodeResetStatus, is ConfigModelAppStatus, is ConfigAppKeyStatus, is ConfigCompositionDataStatus -> {
                PluginCallManager.getInstance().resolveConfigPluginCall(meshMessage)
            }

            is GenericOnOffStatus, is GenericPowerLevelStatus, is LightHslStatus, is LightCtlStatus, is LightCtlTemperatureRangeStatus -> {
                PluginCallManager.getInstance().resolveSigPluginCall(meshMessage)
            }

            is VendorModelMessageStatus -> {
                PluginCallManager.getInstance().resolveVendorPluginCall(meshMessage)
            }

            is ConfigHeartbeatPublicationStatus -> {
                Log.d(tag, "Heartbeat publication status: ${meshMessage.heartbeatPublication.toString()}")
            }
        }
    }

    override fun onMessageDecryptionFailed(meshLayer: String?, errorMessage: String?) {

    }
}

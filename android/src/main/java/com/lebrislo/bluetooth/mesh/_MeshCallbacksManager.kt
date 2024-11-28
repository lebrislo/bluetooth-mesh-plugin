package com.lebrislo.bluetooth.mesh

import android.util.Log
import com.lebrislo.bluetooth.mesh.ble.BleMeshManager
import com.lebrislo.bluetooth.mesh.models.BleMeshDevice
import com.lebrislo.bluetooth.mesh.plugin.PluginCallManager
import com.lebrislo.bluetooth.mesh.utils.NodesOnlineStateManager
import no.nordicsemi.android.mesh.MeshManagerCallbacks
import no.nordicsemi.android.mesh.MeshNetwork
import no.nordicsemi.android.mesh.MeshProvisioningStatusCallbacks
import no.nordicsemi.android.mesh.MeshStatusCallbacks
import no.nordicsemi.android.mesh.provisionerstates.ProvisioningState
import no.nordicsemi.android.mesh.provisionerstates.UnprovisionedMeshNode
import no.nordicsemi.android.mesh.transport.ConfigAppKeyStatus
import no.nordicsemi.android.mesh.transport.ConfigCompositionDataStatus
import no.nordicsemi.android.mesh.transport.ConfigModelAppStatus
import no.nordicsemi.android.mesh.transport.ConfigNodeResetStatus
import no.nordicsemi.android.mesh.transport.ControlMessage
import no.nordicsemi.android.mesh.transport.GenericOnOffStatus
import no.nordicsemi.android.mesh.transport.GenericPowerLevelStatus
import no.nordicsemi.android.mesh.transport.LightCtlStatus
import no.nordicsemi.android.mesh.transport.LightCtlTemperatureRangeStatus
import no.nordicsemi.android.mesh.transport.LightHslStatus
import no.nordicsemi.android.mesh.transport.MeshMessage
import no.nordicsemi.android.mesh.transport.ProvisionedMeshNode
import no.nordicsemi.android.mesh.transport.VendorModelMessageStatus

class _MeshCallbacksManager(
    private val bleMeshManager: BleMeshManager,
    private val pluginCallsManager: PluginCallManager
) : MeshManagerCallbacks,
    MeshProvisioningStatusCallbacks, MeshStatusCallbacks {
    private val tag: String = _MeshCallbacksManager::class.java.simpleName

    private var provisioningCapabilitiesReceivedCallback: ((node: UnprovisionedMeshNode) -> Unit)? = null
    private var provisioningFinish: ((bleMeshDevice: BleMeshDevice) -> Unit)? = null

    fun setProvisioningCapabilitiesReceivedCallback(callback: (node: UnprovisionedMeshNode) -> Unit) {
        this.provisioningCapabilitiesReceivedCallback = callback
    }

    fun setProvisioningFinishCallback(callback: (bleMeshDevice: BleMeshDevice) -> Unit) {
        provisioningFinish = callback
    }

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

    override fun onProvisioningStateChanged(
        meshNode: UnprovisionedMeshNode?,
        state: ProvisioningState.States?,
        data: ByteArray?
    ) {
        Log.d(tag, "onProvisioningStateChanged : ${meshNode?.deviceUuid}  ${state?.name}")
        if (state == ProvisioningState.States.PROVISIONING_CAPABILITIES && meshNode != null) {
            provisioningCapabilitiesReceivedCallback?.invoke(meshNode)
        }
    }

    override fun onProvisioningFailed(
        meshNode: UnprovisionedMeshNode?,
        state: ProvisioningState.States?,
        data: ByteArray?
    ) {
        Log.d(tag, "onProvisioningFailed : " + meshNode?.deviceUuid)
        if (state == ProvisioningState.States.PROVISIONING_FAILED && meshNode != null) {
            provisioningFinish?.invoke(BleMeshDevice.Unprovisioned(meshNode))
        }
    }

    override fun onProvisioningCompleted(
        meshNode: ProvisionedMeshNode?,
        state: ProvisioningState.States?,
        data: ByteArray?
    ) {
        Log.d(tag, "onProvisioningCompleted : " + meshNode?.uuid)
        if (state == ProvisioningState.States.PROVISIONING_COMPLETE && meshNode != null) {
            provisioningFinish?.invoke(BleMeshDevice.Provisioned(meshNode))
        }
    }

    override fun onTransactionFailed(dst: Int, hasIncompleteTimerExpired: Boolean) {

    }

    override fun onUnknownPduReceived(src: Int, accessPayload: ByteArray?) {

    }

    override fun onBlockAcknowledgementProcessed(dst: Int, message: ControlMessage) {

    }

    override fun onBlockAcknowledgementReceived(src: Int, message: ControlMessage) {

    }

    override fun onHeartbeatMessageReceived(src: Int, message: ControlMessage) {
        Log.d(tag, "onHeartbeatMessageReceived")
        NodesOnlineStateManager.getInstance().heartbeatReceived(src)
    }

    override fun onMeshMessageProcessed(dst: Int, meshMessage: MeshMessage) {
        Log.d(tag, "onMeshMessageProcessed ${meshMessage.javaClass.simpleName}")
    }

    override fun onMeshMessageReceived(src: Int, meshMessage: MeshMessage) {
        Log.d(tag, "onMeshMessageReceived ${meshMessage.javaClass.simpleName}")
        when (meshMessage) {
            is ConfigNodeResetStatus, is ConfigModelAppStatus, is ConfigAppKeyStatus, is ConfigCompositionDataStatus -> {
                pluginCallsManager.resolveConfigPluginCall(meshMessage)
            }

            is GenericOnOffStatus, is GenericPowerLevelStatus, is LightHslStatus, is LightCtlStatus, is LightCtlTemperatureRangeStatus -> {
                pluginCallsManager.resolveSigPluginCall(meshMessage)
            }

            is VendorModelMessageStatus -> {
                pluginCallsManager.resolveVendorPluginCall(meshMessage)
            }
        }

        if (meshMessage is ConfigNodeResetStatus) {
            NodesOnlineStateManager.getInstance().removeNode(src)
        }
    }

    override fun onMessageDecryptionFailed(meshLayer: String?, errorMessage: String?) {

    }
}
package com.lebrislo.bluetooth.mesh.models

import no.nordicsemi.android.mesh.provisionerstates.UnprovisionedMeshNode
import no.nordicsemi.android.mesh.transport.ProvisionedMeshNode

sealed class MeshDevice {
    data class Provisioned(val node: ProvisionedMeshNode) : MeshDevice()
    data class Unprovisioned(val node: UnprovisionedMeshNode) : MeshDevice()
}

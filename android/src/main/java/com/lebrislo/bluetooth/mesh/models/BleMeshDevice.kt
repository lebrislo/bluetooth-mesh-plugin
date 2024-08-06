package com.lebrislo.bluetooth.mesh.models

import no.nordicsemi.android.mesh.provisionerstates.UnprovisionedMeshNode
import no.nordicsemi.android.mesh.transport.ProvisionedMeshNode

sealed class BleMeshDevice {
    data class Provisioned(val node: ProvisionedMeshNode) : BleMeshDevice()
    data class Unprovisioned(val node: UnprovisionedMeshNode) : BleMeshDevice()
}

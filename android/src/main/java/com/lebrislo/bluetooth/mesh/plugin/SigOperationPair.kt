package com.lebrislo.bluetooth.mesh.plugin

import no.nordicsemi.android.mesh.opcodes.ApplicationMessageOpCodes.GENERIC_ON_OFF_GET
import no.nordicsemi.android.mesh.opcodes.ApplicationMessageOpCodes.GENERIC_ON_OFF_SET
import no.nordicsemi.android.mesh.opcodes.ApplicationMessageOpCodes.GENERIC_ON_OFF_SET_UNACKNOWLEDGED
import no.nordicsemi.android.mesh.opcodes.ApplicationMessageOpCodes.GENERIC_ON_OFF_STATUS
import no.nordicsemi.android.mesh.opcodes.ApplicationMessageOpCodes.LIGHT_HSL_GET
import no.nordicsemi.android.mesh.opcodes.ApplicationMessageOpCodes.LIGHT_HSL_SET
import no.nordicsemi.android.mesh.opcodes.ApplicationMessageOpCodes.LIGHT_HSL_SET_UNACKNOWLEDGED
import no.nordicsemi.android.mesh.opcodes.ApplicationMessageOpCodes.LIGHT_HSL_STATUS

class SigOperationPair {
    companion object {
        fun getSigOperationPair(operationCode: Int): Int {
            return when (operationCode) {
                GENERIC_ON_OFF_GET, GENERIC_ON_OFF_SET, GENERIC_ON_OFF_SET_UNACKNOWLEDGED -> GENERIC_ON_OFF_STATUS
                LIGHT_HSL_GET, LIGHT_HSL_SET, LIGHT_HSL_SET_UNACKNOWLEDGED -> LIGHT_HSL_STATUS
                else -> 0
            }
        }
    }
}
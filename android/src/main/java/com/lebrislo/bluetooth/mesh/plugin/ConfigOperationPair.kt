package com.lebrislo.bluetooth.mesh.plugin

import no.nordicsemi.android.mesh.opcodes.ConfigMessageOpCodes.CONFIG_APPKEY_ADD
import no.nordicsemi.android.mesh.opcodes.ConfigMessageOpCodes.CONFIG_APPKEY_DELETE
import no.nordicsemi.android.mesh.opcodes.ConfigMessageOpCodes.CONFIG_APPKEY_STATUS
import no.nordicsemi.android.mesh.opcodes.ConfigMessageOpCodes.CONFIG_APPKEY_UPDATE
import no.nordicsemi.android.mesh.opcodes.ConfigMessageOpCodes.CONFIG_COMPOSITION_DATA_GET
import no.nordicsemi.android.mesh.opcodes.ConfigMessageOpCodes.CONFIG_COMPOSITION_DATA_STATUS
import no.nordicsemi.android.mesh.opcodes.ConfigMessageOpCodes.CONFIG_NODE_RESET
import no.nordicsemi.android.mesh.opcodes.ConfigMessageOpCodes.CONFIG_NODE_RESET_STATUS

/**
 * This class is used to retrieve the status operation code for a given operation code.
 */
class ConfigOperationPair {
    companion object {
        /**
         * Returns the status operation code for a given operation code.
         *
         * @param operationCode Operation code.
         */
        fun getConfigOperationPair(operationCode: Int): Int {
            return when (operationCode) {
                CONFIG_APPKEY_ADD, CONFIG_APPKEY_UPDATE, CONFIG_APPKEY_DELETE -> CONFIG_APPKEY_STATUS
                CONFIG_COMPOSITION_DATA_GET -> CONFIG_COMPOSITION_DATA_STATUS
                CONFIG_NODE_RESET -> CONFIG_NODE_RESET_STATUS
                else -> 0
            }
        }
    }
}
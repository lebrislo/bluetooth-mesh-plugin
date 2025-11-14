//
//  OperationPairs.swift
//  BluetoothMeshPlugin
//
//  Created by LE BRIS Loris on 13/11/2025.
//

import Foundation

final class OperationPairs {
    /// Returns the status operation code for a given configuration operation code.
    ///
    /// - Parameter operationCode: The operation code.
    /// - Returns: The corresponding status operation code, or 0 if none is defined.
    static func getConfigOperationPair(_ operationCode: UInt32) -> UInt32 {
        switch operationCode {
        case CONFIG_APPKEY_ADD,
            CONFIG_MODEL_APP_BIND,
            CONFIG_APPKEY_UPDATE,
            CONFIG_APPKEY_DELETE:
            return CONFIG_APPKEY_STATUS

        case CONFIG_COMPOSITION_DATA_GET:
            return CONFIG_COMPOSITION_DATA_STATUS

        case CONFIG_NODE_RESET:
            return CONFIG_NODE_RESET_STATUS

        default:
            return 0
        }
    }

    static func getSigOperationPair(_ operationCode: UInt32) -> UInt32 {
        switch operationCode {
        case GENERIC_ON_OFF_GET,
            GENERIC_ON_OFF_SET,
            GENERIC_ON_OFF_SET_UNACKNOWLEDGED:
            return GENERIC_ON_OFF_STATUS

        case GENERIC_LEVEL_GET,
            GENERIC_LEVEL_SET,
            GENERIC_LEVEL_SET_UNACKNOWLEDGED:
            // Same as your Kotlin: uses GENERIC_POWER_LEVEL_STATUS
            return GENERIC_POWER_LEVEL_STATUS

        case GENERIC_POWER_LEVEL_GET,
            GENERIC_POWER_LEVEL_SET,
            GENERIC_POWER_LEVEL_SET_UNACKNOWLEDGED:
            return GENERIC_POWER_LEVEL_STATUS

        case LIGHT_HSL_GET,
            LIGHT_HSL_SET,
            LIGHT_HSL_SET_UNACKNOWLEDGED:
            return LIGHT_HSL_STATUS

        case LIGHT_CTL_GET,
            LIGHT_CTL_SET,
            LIGHT_CTL_SET_UNACKNOWLEDGED:
            return LIGHT_CTL_STATUS

        case LIGHT_CTL_TEMPERATURE_RANGE_GET,
            LIGHT_CTL_TEMPERATURE_RANGE_SET,
            LIGHT_CTL_TEMPERATURE_RANGE_SET_UNACKNOWLEDGED:
            return LIGHT_CTL_TEMPERATURE_RANGE_STATUS

        case HEALTH_FAULT_GET:
            return HEALTH_FAULT_STATUS

        default:
            return 0
        }
    }
}

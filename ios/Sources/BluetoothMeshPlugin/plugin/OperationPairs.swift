//
//  OperationPairs.swift
//  BluetoothMeshPlugin
//
//  Created by LE BRIS Loris on 13/11/2025.
//

import Foundation
import NordicMesh

final class OperationPairs {
    /// Returns the status operation code for a given configuration operation code.
    ///
    /// - Parameter operationCode: The operation code.
    /// - Returns: The corresponding status operation code, or 0 if none is defined.
    static func getConfigOperationPair(_ operationCode: UInt32) -> UInt32 {
        switch operationCode {
        case ConfigAppKeyAdd.opCode,
            ConfigModelAppBind.opCode,
            ConfigAppKeyUpdate.opCode,
            ConfigAppKeyDelete.opCode:
            return ConfigAppKeyStatus.opCode

        case ConfigCompositionDataGet.opCode:
            return ConfigCompositionDataStatus.opCode

        case ConfigNodeReset.opCode:
            return ConfigNodeResetStatus.opCode

        default:
            return 0
        }
    }

    static func getSigOperationPair(_ operationCode: UInt32) -> UInt32 {
        switch operationCode {
        case GenericOnOffGet.opCode,
            GenericOnOffSet.opCode,
            GenericOnOffSetUnacknowledged.opCode:
            return GenericOnOffStatus.opCode

        case GenericLevelGet.opCode,
            GenericLevelSet.opCode,
            GenericLevelSetUnacknowledged.opCode:
            // Same as your Kotlin: uses GenericPowerLevelStatus opcode.
            return GenericPowerLevelStatus.opCode

        case GenericPowerLevelGet.opCode,
            GenericPowerLevelSet.opCode,
            GenericPowerLevelSetUnacknowledged.opCode:
            return GenericPowerLevelStatus.opCode

        case LightHSLGet.opCode,
            LightHSLSet.opCode,
            LightHSLSetUnacknowledged.opCode:
            return LightHSLStatus.opCode

        case LightCTLGet.opCode,
            LightCTLSet.opCode,
            LightCTLSetUnacknowledged.opCode:
            return LightCTLStatus.opCode

        case LightCTLTemperatureRangeGet.opCode,
            LightCTLTemperatureRangeSet.opCode,
            LightCTLTemperatureRangeSetUnacknowledged.opCode:
            return LightCTLTemperatureRangeStatus.opCode

        case HealthFaultGet.opCode:
            return HealthFaultStatus.opCode

        default:
            return 0
        }
    }
}

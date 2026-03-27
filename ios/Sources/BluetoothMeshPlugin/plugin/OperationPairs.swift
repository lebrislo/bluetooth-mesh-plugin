//
//  OperationPairs.swift
//  BluetoothMeshPlugin
//
//  Created by LE BRIS Loris on 13/11/2025.
//

import Foundation
import NordicMesh

final class OperationPairs {
    static func getMeshOperationPair(_ operationCode: UInt32) -> UInt32 {
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
            
        case HealthFaultGet.opCode:
            return HealthFaultStatus.opCode
            
        case GenericOnOffGet.opCode,
            GenericOnOffSet.opCode,
            GenericOnOffSetUnacknowledged.opCode:
            return GenericOnOffStatus.opCode

        case GenericLevelGet.opCode,
            GenericLevelSet.opCode,
            GenericLevelSetUnacknowledged.opCode:
            return GenericLevelStatus.opCode

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

        default:
            return 0
        }
    }
}

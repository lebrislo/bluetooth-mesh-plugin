//
//  SigPluginCall.swift
//  BluetoothMeshPlugin
//
//  Created by LE BRIS Loris on 14/11/2025.
//

import Capacitor
import Foundation
import NordicMesh

public class SigPluginCall: BasePluginCall {
    public let meshOperationCallback: UInt32
    public let meshAddress: UInt16

    public init(_ meshOperationCallback: UInt32, _ meshAddress: UInt16, _ call: CAPPluginCall) {
        self.meshOperationCallback = meshOperationCallback
        self.meshAddress = meshAddress
        super.init(call: call)
    }

    public static func generateSigPluginCallResponse(_ response: RoutedMeshMessage) -> PluginCallResultData {
        var result: PluginCallResultData = [:]
        result["src"] = response.src
        result["dst"] = response.dst
        result["opcode"] = response.message.opCode

        if let msg = response.message as? GenericOnOffStatus {
            result["data"] = genericOnOffStatusResponse(msg)
        } else if let msg = response.message as? GenericLevelStatus {
            result["data"] = genericLevelStatusResponse(msg)
        } else if let msg = response.message as? GenericPowerLevelStatus {
            result["data"] = genericPowerLevelStatusResponse(msg)
        } else if let msg = response.message as? LightHSLStatus {
            result["data"] = lightHslStatusResponse(msg)
        } else if let msg = response.message as? LightCTLStatus {
            result["data"] = lightCtlStatusResponse(msg)
        } else if let msg = response.message as? LightCTLTemperatureRangeStatus {
            result["data"] = lightCtlTemperatureRangeStatusResponse(msg)
        } else if let msg = response.message as? HealthFaultStatus {
            result["data"] = healthFaultStatusResponse(msg)
        } else if let msg = response.message as? HealthCurrentStatus {
            result["data"] = healthCurrentStatusResponse(msg)
        } else {
            result["data"] = PluginCallResultData()
        }
        return result
    }

    private static func genericOnOffStatusResponse(_ msg: GenericOnOffStatus) -> PluginCallResultData {
        var data: PluginCallResultData = [:]
        data["onOff"] = msg.isOn
        return data
    }

    private static func genericLevelStatusResponse(_ msg: GenericLevelStatus) -> PluginCallResultData {
        var data: PluginCallResultData = [:]
        data["level"] = msg.level
        return data
    }

    private static func genericPowerLevelStatusResponse(_ msg: GenericPowerLevelStatus) -> PluginCallResultData {
        var data: PluginCallResultData = [:]
        data["powerLevel"] = msg.power
        return data
    }

    private static func lightHslStatusResponse(_ msg: LightHSLStatus) -> PluginCallResultData {
        var data: PluginCallResultData = [:]
        data["hue"] = msg.hue
        data["saturation"] = msg.saturation
        data["lightness"] = msg.lightness
        return data
    }

    private static func lightCtlStatusResponse(_ msg: LightCTLStatus) -> PluginCallResultData {
        var data: PluginCallResultData = [:]
        data["lightness"] = msg.lightness
        data["temperature"] = msg.temperature
        return data
    }

    private static func lightCtlTemperatureRangeStatusResponse(_ msg: LightCTLTemperatureRangeStatus)
        -> PluginCallResultData
    {
        var data: PluginCallResultData = [:]
        data["status"] = msg.status
        data["min"] = msg.min
        data["max"] = msg.max
        return data
    }

    private static func healthFaultStatusResponse(_ msg: HealthFaultStatus) -> PluginCallResultData {
        var data: PluginCallResultData = [:]
        data["testId"] = msg.testId
        data["companyId"] = msg.companyIdentifier
        data["faults"] = msg.faults
        return data
    }

    private static func healthCurrentStatusResponse(_ msg: HealthCurrentStatus) -> PluginCallResultData {
        var data: PluginCallResultData = [:]
        data["testId"] = msg.testId
        data["companyId"] = msg.companyIdentifier
        data["faults"] = msg.faults
        return data
    }
}

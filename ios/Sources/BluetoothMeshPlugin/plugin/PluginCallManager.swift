//
//  PluginCallManager.swift
//  BluetoothMeshPlugin
//
//  Created by LE BRIS Loris on 30/10/2025.
//

import Capacitor
import Foundation
import NordicMesh

public class PluginCallManager {
    static let shared = PluginCallManager()

    private var pluginCalls: [BasePluginCall] = []

    public func removePluginCall(_ call: BasePluginCall) {
        if let index = pluginCalls.firstIndex(where: { $0 === call }) {
            pluginCalls.remove(at: index)
        }
    }

    public func addSigPluginCall(_ meshOperation: UInt32, _ meshAddress: UInt16, _ call: CAPPluginCall) {
        let operationPair = OperationPairs.getSigOperationPair(meshOperation)
        pluginCalls.append(
            SigPluginCall(operationPair, meshAddress, call)
        )
    }

    public func resolveSigPluginCall(_ response: RoutedMeshMessage) {
        let callResponse = SigPluginCall.generateSigPluginCallResponse(response)

        let index = pluginCalls.firstIndex { call in
            guard let c = call as? SigPluginCall else { return false }
            return c.meshOperationCallback == response.message.opCode && c.meshAddress == response.src
        }

        if let index = index, let sigCall = pluginCalls[index] as? SigPluginCall {
            pluginCalls.remove(at: index)
            sigCall.resolve(callResponse)
        }

        NotificationManager.shared.sendNotification(
            event: "meshModelMessageEvent",
            data: callResponse
        )
    }

    public func addConfigPluginCall(_ meshOperation: UInt32, _ meshAddress: UInt16, _ call: CAPPluginCall) {
        let operationPair = OperationPairs.getConfigOperationPair(meshOperation)
        pluginCalls.append(
            ConfigPluginCall(operationPair, meshAddress, call)
        )
    }

    public func resolveConfigPluginCall(_ response: RoutedMeshMessage) {
        let callResponse = ConfigPluginCall.generateConfigPluginCallResponse(response)

        let index = pluginCalls.firstIndex { call in
            guard let c = call as? ConfigPluginCall else { return false }
            return c.meshOperationCallback == response.message.opCode && c.meshAddress == response.src
        }

        if let index = index, let configCall = pluginCalls[index] as? ConfigPluginCall {
            pluginCalls.remove(at: index)
            configCall.resolve(callResponse)
        }

        NotificationManager.shared.sendNotification(
            event: "meshModelMessageEvent",
            data: callResponse
        )
    }

    public func addVendorPluginCall(
        _ modelId: UInt32,
        _ opCode: UInt32,
        _ opPairCode: UInt32,
        _ meshAddress: UInt16,
        _ call: CAPPluginCall
    ) {
        pluginCalls.append(VendorPluginCall(modelId, opCode, opPairCode, meshAddress, call))
    }

    public func resolveVendorPluginCall(_ response: RoutedMeshMessage) {
        let callResponse = VendorPluginCall.generateVendorPluginCallResponse(response)

        let index = pluginCalls.firstIndex { call in
            guard let c = call as? VendorPluginCall else { return false }
            return c.meshOperationCallback == response.message.opCode && c.meshAddress == response.src
        }

        if let index = index, let vendorCall = pluginCalls[index] as? VendorPluginCall {
            pluginCalls.remove(at: index)
            vendorCall.resolve(callResponse)
        }

        NotificationManager.shared.sendNotification(
            event: "meshModelMessageEvent",
            data: callResponse
        )
    }
}

extension PluginCallManager: MeshNetworkDelegate {
    public func meshNetworkManager(
        _ manager: MeshNetworkManager,
        didReceiveMessage message: MeshMessage,
        sentFrom source: Address,
        to destination: MeshAddress
    ) {
        print("Received message \(message) from \(source) to \(destination)")

        let routedMessage = RoutedMeshMessage(message: message, src: source, dst: destination)

        switch message {
        case is ConfigNodeResetStatus,
            is ConfigModelAppStatus,
            is ConfigAppKeyStatus,
            is ConfigCompositionDataStatus:
            PluginCallManager.shared.resolveConfigPluginCall(routedMessage)
            break

        case is GenericOnOffStatus,
            is GenericPowerLevelStatus,
            is LightHSLStatus,
            is LightCTLStatus,
            is LightCTLTemperatureRangeStatus,
            is HealthFaultStatus,
            is HealthCurrentStatus:
            PluginCallManager.shared.resolveSigPluginCall(routedMessage)
            break

        case is VendorResponse, is UnknownMessage:
            PluginCallManager.shared.resolveVendorPluginCall(routedMessage)
            break

        default:
            break
        }

        if message is ConfigNodeResetStatus {
            NodesOnlineStateManager.shared.removeNode(unicastAddress: source)
        }
    }
}

public struct RoutedMeshMessage {
    public let message: MeshMessage
    public let src: Int
    public let dst: Int

    public init(message: MeshMessage, src: Address, dst: MeshAddress) {
        self.message = message
        self.src = Int(src)
        self.dst = Int(dst.address)
    }
}

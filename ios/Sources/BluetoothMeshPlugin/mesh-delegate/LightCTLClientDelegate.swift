//
//  LightCTLClientDelegate.swift
//  BluetoothMeshPlugin
//

import Foundation
import NordicMesh

final class LightCTLClientDelegate: ModelDelegate {
    var messageTypes: [UInt32 : MeshMessage.Type] {
        [
            LightCTLStatus.opCode: LightCTLStatus.self,
            LightCTLTemperatureRangeStatus.opCode: LightCTLTemperatureRangeStatus.self
        ]
    }

    var isSubscriptionSupported: Bool { true }
    var publicationMessageComposer: MessageComposer? { nil }

    func model(
        _ model: Model,
        didReceiveResponse response: any MeshResponse,
        toAcknowledgedMessage request: any AcknowledgedMeshMessage,
        from source: Address
    ) {
        if let status = response as? LightCTLStatus {
            print("LightCTLStatus response from \(source), lightness=\(status.lightness), temperature=\(status.temperature)")
            return
        }

        if let status = response as? LightCTLTemperatureRangeStatus {
            print("LightCTLTemperatureRangeStatus response from \(source), range=\(status.range.lowerBound)...\(status.range.upperBound)")
        }
    }

    func model(
        _ model: Model,
        didReceiveAcknowledgedMessage request: any AcknowledgedMeshMessage,
        from source: Address,
        sentTo destination: MeshAddress
    ) throws -> any MeshResponse {
        throw ModelError.invalidMessage
    }

    func model(
        _ model: Model,
        didReceiveUnacknowledgedMessage message: any UnacknowledgedMeshMessage,
        from source: Address,
        sentTo destination: MeshAddress
    ) {
        if let status = message as? LightCTLStatus {
            print("LightCTLStatus publication from \(source), lightness=\(status.lightness), temperature=\(status.temperature)")
        } else if let status = message as? LightCTLTemperatureRangeStatus {
            print("LightCTLTemperatureRangeStatus publication from \(source), range=\(status.range.lowerBound)...\(status.range.upperBound)")
        } else {
            print("Unacknowledged message from \(source): \(message)")
        }
    }
}

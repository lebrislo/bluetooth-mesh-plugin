//
//  LightCTLTemperatureClientDelegate.swift
//  BluetoothMeshPlugin
//

import Foundation
import NordicMesh

final class LightCTLTemperatureClientDelegate: ModelDelegate {
    var messageTypes: [UInt32 : MeshMessage.Type] {
        [
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
        guard let status = response as? LightCTLTemperatureRangeStatus else { return }

        print("LightCTLTemperatureRangeStatus response from \(source), range=\(status.range.lowerBound)...\(status.range.upperBound)")
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
        if let status = message as? LightCTLTemperatureRangeStatus {
            print("LightCTLTemperatureRangeStatus publication from \(source), range=\(status.range.lowerBound)...\(status.range.upperBound)")
        } else {
            print("Unacknowledged message from \(source): \(message)")
        }
    }
}

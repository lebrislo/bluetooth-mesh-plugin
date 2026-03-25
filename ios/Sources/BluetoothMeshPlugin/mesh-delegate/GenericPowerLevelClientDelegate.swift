//
//  GenericPowerLevelClientDelegate.swift
//  BluetoothMeshPlugin
//
//  Created by LE BRIS Loris on 25/03/2026.
//

import Foundation
import NordicMesh

final class GenericPowerLevelClientDelegate: ModelDelegate {
    var messageTypes: [UInt32 : MeshMessage.Type] {
        [
            GenericPowerLevelStatus.opCode: GenericPowerLevelStatus.self
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
        guard let status = response as? GenericPowerLevelStatus else { return }

        print("GenericPowerLevelStatus response from \(source), power=\(status.power)")
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
        if let status = message as? GenericPowerLevelStatus {
            print("GenericPowerLevelStatus publication from \(source), power=\(status.power)")
        } else {
            print("Unacknowledged message from \(source): \(message)")
        }
    }
}

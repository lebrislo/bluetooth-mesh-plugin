//
//  LightHSLClientDelegate.swift
//  BluetoothMeshPlugin
//
//  Created by LE BRIS Loris on 26/03/2026.
//

import Foundation
import NordicMesh

final class LightHSLClientDelegate: ModelDelegate {
    var messageTypes: [UInt32 : MeshMessage.Type] {
        [
            LightHSLStatus.opCode: LightHSLStatus.self
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
        guard let status = response as? LightHSLStatus else { return }
        print("LightHSLStatus response from \(source), lightness=\(status.lightness), hue=\(status.hue), saturation=\(status.saturation)")
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
        if let status = message as? LightHSLStatus {
            print("LightHSLStatus publication from \(source), lightness=\(status.lightness), hue=\(status.hue), saturation=\(status.saturation)")
        } else {
            print("Unacknowledged message from \(source): \(message)")
        }
    }
}

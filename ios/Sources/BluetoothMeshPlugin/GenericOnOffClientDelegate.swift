//
//  GenericOnOffCliDelegate.swift
//  BluetoothMeshPlugin
//
//  Created by LE BRIS Loris on 20/03/2026.
//

import Foundation
import NordicMesh

class GenericOnOffClientDelegate: ModelDelegate {
    let messageTypes: [UInt32: MeshMessage.Type]

    let isSubscriptionSupported: Bool = true

    var publicationMessageComposer: MessageComposer? = nil

    init() {
        let types: [StaticMeshMessage.Type] = [
            GenericOnOffStatus.self
        ]
        self.messageTypes = types.toMap()
    }

    func model(
        _ model: Model,
        didReceiveAcknowledgedMessage request: AcknowledgedMeshMessage,
        from source: Address,
        sentTo destination: MeshAddress
    ) -> MeshResponse {
        fatalError("Not supported")
    }

    func model(
        _ model: Model,
        didReceiveUnacknowledgedMessage message: UnacknowledgedMeshMessage,
        from source: Address,
        sentTo destination: MeshAddress
    ) {
    }

    func model(
        _ model: Model,
        didReceiveResponse response: MeshResponse,
        toAcknowledgedMessage request: AcknowledgedMeshMessage,
        from source: Address
    ) {
    }
}

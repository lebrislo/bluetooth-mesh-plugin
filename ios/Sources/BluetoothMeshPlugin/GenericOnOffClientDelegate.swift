//
//  GenericOnOffCliDelegate.swift
//  BluetoothMeshPlugin
//
//  Created by LE BRIS Loris on 20/03/2026.
//

import Foundation
import NordicMesh

class GenericOnOffClientDelegate: ModelDelegate {
    
    let messageTypes: [UInt32 : MeshMessage.Type]
    let isSubscriptionSupported: Bool = true
    
    var publicationMessageComposer: MessageComposer? {
        func compose() -> MeshMessage {
            return GenericOnOffSetUnacknowledged(self.state)
        }
        let request = compose()
        return {
            return request
        }
    }
    
    /// The current state of the Generic On Off Client model.
    var state: Bool = false {
        didSet {
            publish(using: MeshNetworkManager.instance)
        }
    }
    
    init() {
        let types: [StaticMeshMessage.Type] = [
            GenericOnOffStatus.self
        ]
        messageTypes = types.toMap()
    }
    
    // MARK: - Message handlers
    
    func model(_ model: Model, didReceiveAcknowledgedMessage request: AcknowledgedMeshMessage,
               from source: Address, sentTo destination: MeshAddress) -> MeshResponse {
        fatalError("Not possible")
    }
    
    func model(_ model: Model, didReceiveUnacknowledgedMessage message: UnacknowledgedMeshMessage,
               from source: Address, sentTo destination: MeshAddress) {
        // The status message may be received here if the Generic OnOff Server model
        // has been configured to publish. Ignore this message.
    }
    
    func model(_ model: Model, didReceiveResponse response: MeshResponse,
               toAcknowledgedMessage request: AcknowledgedMeshMessage,
               from source: Address) {
        // Ignore.
    }
    
}


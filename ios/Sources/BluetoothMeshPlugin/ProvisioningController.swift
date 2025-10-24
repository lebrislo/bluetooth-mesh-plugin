//
//  ProvisioningManager.swift
//  BluetoothMeshPlugin
//
//  Created by LE BRIS Loris on 24/10/2025.
//

import Foundation

class ProvisioningController {
    let meshNetowrkManager: MeshNetworkManager!

    var unprovisionedDevice: UnprovisionedDevice!
    var bearer: ProvisioningBearer!
    var previousNode: Node?

    private var publicKey: PublicKey?
    private var authenticationMethod: AuthenticationMethod?

    private var provisioningManager: ProvisioningManager!
    private var capabilitiesReceived = false

    init(meshNetowrkManager: MeshNetworkManager!) {
        self.meshNetowrkManager = meshNetowrkManager
    }

    public func provision(_ device: UnprovisionedDevice, _ provisioningBearer: ProvisioningBearer) {
        do {
            self.provisioningManager = try self.meshNetowrkManager.provision(
                unprovisionedDevice: unprovisionedDevice,
                over: bearer
            )
        } catch {
            print("Provisioning failed to start: \(error)")
            return
        }

        self.provisioningManager.delegate = self
        bearer.delegate = self

        do {
            try self.provisioningManager.identify(andAttractFor: 10)
        } catch {
            print("Failed to start identification: \(error)")
        }
    }
}

extension ProvisioningController: ProvisioningDelegate {

    func provisioningState(of unprovisionedDevice: UnprovisionedDevice, didChangeTo state: ProvisioningState) {
    }

    func authenticationActionRequired(_ action: AuthAction) {}

    func inputComplete() {}
}

extension ProvisioningController: GattBearerDelegate {

    public func bearerDidConnect(_ bearer: Bearer) {
        print("Bearer connected")
    }

    public func bearerDidDiscoverServices(_ bearer: Bearer) {
        print("Bearer discovered services")
    }

    public func bearerDidOpen(_ bearer: Bearer) {
        print("Bearer opened")
    }

    public func bearer(_ bearer: Bearer, didClose error: Error?) {
        print("Bearer \(bearer) closed with error: \(String(describing: error))")
    }
}

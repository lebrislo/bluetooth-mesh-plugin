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

    private var provisioningManager: ProvisioningManager!
    private var capabilitiesReceived = false

    private var completion: ((Result<Node, Error>) -> Void)?

    init(meshNetowrkManager: MeshNetworkManager!) {
        self.meshNetowrkManager = meshNetowrkManager
    }

    public func openProvisioningBearer(
        _ provisioningBearer: ProvisioningBearer,
        completion: @escaping (Result<Node, Error>) -> Void
    ) {
        self.completion = completion
        self.bearer = provisioningBearer
        provisioningBearer.delegate = self

        do {
            try provisioningBearer.open()
        } catch {
            finish(.failure(error))
        }
    }

    private func provision(_ device: UnprovisionedDevice, _ provisioningBearer: ProvisioningBearer) {
        self.bearer = provisioningBearer
        self.unprovisionedDevice = device

        do {
            self.provisioningManager = try self.meshNetowrkManager.provision(
                unprovisionedDevice: unprovisionedDevice,
                over: bearer
            )
        } catch {
            finish(.failure(error))
            return
        }

        self.provisioningManager.delegate = self
        self.bearer.delegate = self

        do {
            try self.provisioningManager.identify(andAttractFor: 10)
        } catch {
            finish(.failure(error))
        }
    }

    private func finish(_ result: Result<Node, Error>) {
        guard let completion = self.completion else { return }
        self.completion = nil
        completion(result)
    }
}

extension ProvisioningController: ProvisioningDelegate {

    func provisioningState(of unprovisionedDevice: UnprovisionedDevice, didChangeTo state: ProvisioningState) {
        print("Provisioning state changed to: \(state)")
        switch state {
        case .requestingCapabilities:
            print("Requesting capabilities...")
            break

        case .capabilitiesReceived(let capabilities):
            print("Capabilities received: \(capabilities)")
            do {
                try self.provisioningManager
                    .provision(
                        usingAlgorithm: capabilities.algorithms.strongest,
                        publicKey: .noOobPublicKey,
                        authenticationMethod: .noOob
                    )
            } catch {
                finish(.failure(error))
            }
            break

        case .complete:
            print("Provisioning complete!")
            try? self.bearer.close()
            break

        case .failed(let error):
            print("Provisioning failed with error: \(error)")
            finish(.failure(error))
            try? self.bearer.close()
            break

        default:
            break
        }
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
        guard let provisioningBearer = bearer as? ProvisioningBearer else {
            let err = NSError(
                domain: "Provisioning",
                code: -10,
                userInfo: [NSLocalizedDescriptionKey: "Bearer is not a ProvisioningBearer"]
            )
            finish(.failure(err))
            return
        }
        guard let unprovDevice = DeviceRepository.shared.unprovDevices.first?.device else {
            let err = NSError(
                domain: "Provisioning",
                code: -11,
                userInfo: [NSLocalizedDescriptionKey: "No unprovisioned device found"]
            )
            finish(.failure(err))
            return
        }
        self.provision(unprovDevice, provisioningBearer)
    }

    public func bearer(_ bearer: Bearer, didClose error: Error?) {
        print("Bearer \(bearer) closed with error: \(String(describing: error))")
        if self.meshNetowrkManager.save() {
            let connection = BluetoothMeshPlugin.connection!
            guard let pbGattBearer = self.bearer as? PBGattBearer else {
                print("bearer is not a PBGattBearer")
                return
            }
            connection.disconnect()
            // The bearer has closed. Attempt to send a message
            // will fail, but the Proxy Filter will receive .bearerClosed
            // error, upon which it will clear the filter list and notify
            // the delegate.
            self.meshNetowrkManager.proxyFilter.proxyDidDisconnect()
            self.meshNetowrkManager.proxyFilter.clear()

            let gattBearer = GattBearer(targetWithIdentifier: pbGattBearer.identifier)

            connection.use(proxy: gattBearer)
            if let net = meshNetowrkManager.meshNetwork,
                let node = net.node(for: unprovisionedDevice)
            {
                finish(.success(node))
            } else {
                finish(
                    .failure(
                        NSError(
                            domain: "Provisioning",
                            code: -12,
                            userInfo: [NSLocalizedDescriptionKey: "Provisioned node not found"]
                        )
                    )
                )
            }

        }
    }
}

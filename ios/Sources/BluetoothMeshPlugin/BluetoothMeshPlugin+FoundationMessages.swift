import Capacitor
import Foundation
import NordicMesh

extension BluetoothMeshPlugin {

    @objc func createApplicationKey(_ call: CAPPluginCall) {
        guard let network = meshNetworkManager.meshNetwork else {
            call.reject("Mesh network not initialized")
            return
        }

        let appKeyCount = network.applicationKeys.count
        let key = Data.random128BitKey()

        do {
            try network.add(
                applicationKey: key,
                withIndex: UInt16(appKeyCount),
                name: "AppKey \(appKeyCount)"
            )

            if meshNetworkManager.save() {
                call.resolve(["appKeyIndex": appKeyCount])
            } else {
                throw NSError(
                    domain: "BluetoothMeshPlugin",
                    code: 1,
                    userInfo: [NSLocalizedDescriptionKey: "Failed to save mesh network"]
                )
            }
        } catch {
            call.reject("Failed to create application key: \(error.localizedDescription)")
        }
    }

    @objc func removeApplicationKey(_ call: CAPPluginCall) {
        guard let appKeyIndex = call.getInt("appKeyIndex") else {
            call.reject("appKeyIndex is required")
            return
        }

        do {
            try meshNetworkManager.meshNetwork?.remove(applicationKeyAt: appKeyIndex)
            if meshNetworkManager.save() {
                call.resolve()
            } else {
                throw NSError(
                    domain: "BluetoothMeshPlugin",
                    code: 2,
                    userInfo: [NSLocalizedDescriptionKey: "Failed to save mesh network"]
                )
            }
        } catch {
            call.reject("Failed to remove application key: \(error.localizedDescription)")
        }
    }

    @objc func addApplicationKeyToNode(_ call: CAPPluginCall) {
        guard let appKeyIndex = call.getInt("appKeyIndex") else {
            call.reject("appKeyIndex is required")
            return
        }

        guard let unicastAddress = call.getInt("unicastAddress") else {
            call.reject("unicastAddress is required")
            return
        }

        let acknowledgement = call.getBool("acknowledgement") ?? false

        guard
            let applicationKey = meshNetworkManager.meshNetwork?.applicationKeys.first(where: {
                $0.index == UInt16(appKeyIndex)
            })
        else {
            call.reject("Application key with index \(appKeyIndex) not found")
            return
        }

        let message = ConfigAppKeyAdd(applicationKey: applicationKey)
        let targetAddress = UInt16(unicastAddress)

        ensureProxyConnection(for: call) { [weak self] in
            guard let self = self else { return }

            do {
                PluginCallManager.shared.addConfigPluginCall(
                    ConfigAppKeyAdd.opCode,
                    targetAddress,
                    call
                )

                try self.meshNetworkManager.send(message, to: targetAddress)

                if !acknowledgement {
                    call.resolve()
                }
            } catch {
                call.reject("Failed to add application key to node: \(error.localizedDescription)")
            }
        }
    }
    
    @objc func sendAppKeyGet(_ call: CAPPluginCall) {
        
        guard
            let netKeyIndex = requiredUInt16("netKeyIndex", in: call),
            let unicastAddress = call.getInt("unicastAddress")
        else {
            return call.reject("Missing required parameters: netKeyIndex, unicastAddress")
        }
        
        guard let networkKey = meshNetworkManager.meshNetwork?.networkKeys.first(where: { $0.index == netKeyIndex })
        else {
            return call.reject("Network key with index \(netKeyIndex) not found")
        }

        let message = ConfigAppKeyGet(networkKey: networkKey)
        let targetAddress = UInt16(unicastAddress)

        ensureProxyConnection(for: call) { [weak self] in
            guard let self = self else { return }

            do {
                PluginCallManager.shared.addConfigPluginCall(
                    ConfigAppKeyGet.opCode,
                    targetAddress,
                    call
                )

                try self.meshNetworkManager.send(message, to: targetAddress)

            } catch {
                call.reject("Failed to get application key from node: \(error.localizedDescription)")
            }
        }
    }

    @objc func sendConfigHeartbeatPublicationSet(_ call: CAPPluginCall) {
        guard let unicastAddress = call.getInt("unicastAddress") else {
            call.reject("unicastAddress is required")
            return
        }
        guard let destinationAddress = call.getInt("destinationAddress") else {
            call.reject("destinationAddress is required")
            return
        }
        guard let count = call.getInt("count") else {
            call.reject("count is required")
            return
        }
        guard let period = call.getInt("period") else {
            call.reject("period is required")
            return
        }
        guard let ttl = call.getInt("ttl") else {
            call.reject("ttl is required")
            return
        }
        guard let netKeyIndex = call.getInt("netKeyIndex") else {
            call.reject("netKeyIndex is required")
            return
        }

        call.resolve()
    }

    @objc func sendHealthFaultGet(_ call: CAPPluginCall) {
        guard
            let destination = requiredUInt16("unicastAddress", in: call),
            let appKey = requiredAppKey(in: call),
                let companyId = requiredUInt16("companyId", in: call)
        else { return }
        
        ensureProxyConnection(for: call) { [weak self] in
            guard let self = self else { return }

            do {
                PluginCallManager.shared.addConfigPluginCall(
                    HealthFaultGet.opCode,
                    destination,
                    call
                )

                try self.meshNetworkManager.send(
                    HealthFaultGet(for: companyId),
                    to: MeshAddress(destination),
                    using: appKey
                )
            } catch {
                call.reject("Failed to add application key to node: \(error.localizedDescription)")
            }
        }
    }
    
    @objc func sendHealthFaultClear(_ call: CAPPluginCall) {
        guard
            let destination = requiredUInt16("unicastAddress", in: call),
            let appKey = requiredAppKey(in: call),
                let companyId = requiredUInt16("companyId", in: call)
        else { return }
        
        ensureProxyConnection(for: call) { [weak self] in
            guard let self = self else { return }

            do {
                PluginCallManager.shared.addConfigPluginCall(
                    HealthFaultClear.opCode,
                    destination,
                    call
                )

                try self.meshNetworkManager.send(
                    HealthFaultClear(for: companyId),
                    to: MeshAddress(destination),
                    using: appKey
                )
            } catch {
                call.reject("Failed to add application key to node: \(error.localizedDescription)")
            }
        }
    }
}

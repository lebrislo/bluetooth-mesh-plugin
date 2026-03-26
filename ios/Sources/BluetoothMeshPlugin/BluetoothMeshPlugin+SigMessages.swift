import Capacitor
import Foundation
import NordicMesh

extension BluetoothMeshPlugin {

    @objc func sendGenericOnOffSet(_ call: CAPPluginCall) {
        guard
            let destination = requiredUInt16("unicastAddress", in: call),
            let appKey = requiredAppKey(in: call),
            let onOff = requiredBool("onOff", in: call)
        else {
            return
        }

        let acknowledgement = call.getBool("acknowledgement", false)

        sendSigModelMessage(
            meshOperation: GENERIC_ON_OFF_SET,
            destination: destination,
            appKey: appKey,
            acknowledgement: acknowledgement,
            failureDescription: "Generic OnOff Set",
            call: call
        ) { [weak self] destination, appKey in
            guard let self = self else { return }
            try self.meshNetworkManager.send(
                GenericOnOffSet(onOff),
                to: MeshAddress(destination),
                using: appKey
            )
        }
    }

    @objc func sendGenericOnOffGet(_ call: CAPPluginCall) {
        guard
            let destination = requiredUInt16("unicastAddress", in: call),
            let appKey = requiredAppKey(in: call)
        else {
            return
        }

        sendSigModelMessage(
            meshOperation: GENERIC_ON_OFF_GET,
            destination: destination,
            appKey: appKey,
            acknowledgement: true,
            failureDescription: "Generic OnOff Get",
            call: call
        ) { [weak self] destination, appKey in
            guard let self = self else { return }
            try self.meshNetworkManager.send(
                GenericOnOffGet(),
                to: MeshAddress(destination),
                using: appKey
            )
        }
    }

    @objc func sendGenericPowerLevelSet(_ call: CAPPluginCall) {
        guard
            let destination = requiredUInt16("unicastAddress", in: call),
            let appKey = requiredAppKey(in: call),
            let powerLevel = requiredUInt16("powerLevel", in: call)
        else {
            return
        }

        let acknowledgement = call.getBool("acknowledgement", false)

        sendSigModelMessage(
            meshOperation: GENERIC_POWER_LEVEL_SET,
            destination: destination,
            appKey: appKey,
            acknowledgement: acknowledgement,
            failureDescription: "Generic Power Level Set",
            call: call
        ) { [weak self] destination, appKey in
            guard let self = self else { return }
            try self.meshNetworkManager.send(
                GenericPowerLevelSet(power: powerLevel),
                to: MeshAddress(destination),
                using: appKey
            )
        }
    }

    @objc func sendGenericPowerLevelGet(_ call: CAPPluginCall) {
        guard
            let destination = requiredUInt16("unicastAddress", in: call),
            let appKey = requiredAppKey(in: call)
        else {
            return
        }

        sendSigModelMessage(
            meshOperation: GENERIC_POWER_LEVEL_GET,
            destination: destination,
            appKey: appKey,
            acknowledgement: true,
            failureDescription: "Generic Power Level Get",
            call: call
        ) { [weak self] destination, appKey in
            guard let self = self else { return }
            try self.meshNetworkManager.send(
                GenericPowerLevelGet(),
                to: MeshAddress(destination),
                using: appKey
            )
        }
    }

    @objc func sendLightHslSet(_ call: CAPPluginCall) {
        guard
            let destination = requiredUInt16("unicastAddress", in: call),
            let appKey = requiredAppKey(in: call),
            let hue = requiredUInt16("hue", in: call),
            let saturation = requiredUInt16("saturation", in: call),
            let lightness = requiredUInt16("lightness", in: call)
        else {
            return
        }

        let acknowledgement = call.getBool("acknowledgement", false)

        sendSigModelMessage(
            meshOperation: LIGHT_HSL_SET,
            destination: destination,
            appKey: appKey,
            acknowledgement: acknowledgement,
            failureDescription: "Light HSL Set",
            call: call
        ) { [weak self] destination, appKey in
            guard let self = self else { return }
            try self.meshNetworkManager.send(
                LightHSLSet(lightness: lightness, hue: hue, saturation: saturation),
                to: MeshAddress(destination),
                using: appKey
            )
        }
    }

    @objc func sendLightHslGet(_ call: CAPPluginCall) {
        guard
            let destination = requiredUInt16("unicastAddress", in: call),
            let appKey = requiredAppKey(in: call)
        else {
            return
        }

        sendSigModelMessage(
            meshOperation: LIGHT_HSL_GET,
            destination: destination,
            appKey: appKey,
            acknowledgement: true,
            failureDescription: "Light HSL Get",
            call: call
        ) { [weak self] destination, appKey in
            guard let self = self else { return }
            try self.meshNetworkManager.send(
                LightHSLGet(),
                to: MeshAddress(destination),
                using: appKey
            )
        }
    }
}

private extension BluetoothMeshPlugin {
    func requiredUInt16(_ key: String, in call: CAPPluginCall) -> UInt16? {
        guard let value = call.getInt(key) else {
            call.reject("\(key) is required")
            return nil
        }
        return UInt16(value)
    }

    func requiredBool(_ key: String, in call: CAPPluginCall) -> Bool? {
        guard let value = call.getBool(key) else {
            call.reject("\(key) is required")
            return nil
        }
        return value
    }

    func requiredAppKey(in call: CAPPluginCall) -> ApplicationKey? {
        guard let appKeyIndex = call.getInt("appKeyIndex") else {
            call.reject("appKeyIndex is required")
            return nil
        }

        guard let network = meshNetworkManager.meshNetwork else {
            call.reject("Mesh network not initialized")
            return nil
        }

        guard let appKey = network.applicationKeys.first(where: { $0.index == UInt16(appKeyIndex) }) else {
            call.reject("Application key with index \(appKeyIndex) not found")
            return nil
        }

        return appKey
    }

    func sendSigModelMessage(
        meshOperation: UInt32,
        destination: UInt16,
        appKey: ApplicationKey,
        acknowledgement: Bool,
        failureDescription: String,
        call: CAPPluginCall,
        send: @escaping (_ destination: UInt16, _ appKey: ApplicationKey) throws -> Void
    ) {
        ensureProxyConnection(for: call) {
            do {
                PluginCallManager.shared.addSigPluginCall(meshOperation, destination, call)
                try send(destination, appKey)

                if !acknowledgement {
                    call.resolve()
                }
            } catch {
                call.reject("Failed to send \(failureDescription) message: \(error.localizedDescription)")
            }
        }
    }
}

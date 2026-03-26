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
            meshOperation: GenericOnOffSet.opCode,
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
            meshOperation: GenericOnOffGet.opCode,
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
            meshOperation: GenericPowerLevelSet.opCode,
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
            meshOperation: GenericPowerLevelGet.opCode,
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
            meshOperation: LightHSLSet.opCode,
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
            meshOperation: LightHSLGet.opCode,
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

    @objc func sendLightCtlSet(_ call: CAPPluginCall) {
        guard
            let destination = requiredUInt16("unicastAddress", in: call),
            let appKey = requiredAppKey(in: call),
            let lightness = requiredUInt16("lightness", in: call),
            let temperature = requiredUInt16("temperature", in: call),
            let deltaUv = requiredInt16("deltaUv", in: call)
        else {
            return
        }

        let acknowledgement = call.getBool("acknowledgement", false)

        sendSigModelMessage(
            meshOperation: LightCTLSet.opCode,
            destination: destination,
            appKey: appKey,
            acknowledgement: acknowledgement,
            failureDescription: "Light HSL Set",
            call: call
        ) {
            [weak self]
            destination,
            appKey in
            guard let self = self else { return }
            try self.meshNetworkManager.send(
                LightCTLSet(lightness: lightness, temperature: temperature, deltaUV: deltaUv),
                to: MeshAddress(destination),
                using: appKey
            )
        }
    }

    @objc func sendLightCtlGet(_ call: CAPPluginCall) {
        guard
            let destination = requiredUInt16("unicastAddress", in: call),
            let appKey = requiredAppKey(in: call)
        else {
            return
        }

        sendSigModelMessage(
            meshOperation: LightCTLGet.opCode,
            destination: destination,
            appKey: appKey,
            acknowledgement: true,
            failureDescription: "Light CTL Get",
            call: call
        ) { [weak self] destination, appKey in
            guard let self = self else { return }
            try self.meshNetworkManager.send(
                LightCTLGet(),
                to: MeshAddress(destination),
                using: appKey
            )
        }
    }

    @objc func sendLightCtlTemperatureRangeSet(_ call: CAPPluginCall) {
        guard
            let destination = requiredUInt16("unicastAddress", in: call),
            let appKey = requiredAppKey(in: call),
            let rangeMin = requiredUInt16("rangeMin", in: call),
            let rangeMax = requiredUInt16("rangeMax", in: call)
        else {
            return
        }

        let acknowledgement = call.getBool("acknowledgement", false)

        sendSigModelMessage(
            meshOperation: LightCTLTemperatureRangeSet.opCode,
            destination: destination,
            appKey: appKey,
            acknowledgement: acknowledgement,
            failureDescription: "Light HSL Set",
            call: call
        ) {
            [weak self]
            destination,
            appKey in
            guard let self = self else { return }
            try self.meshNetworkManager.send(
                LightCTLTemperatureRangeSet(rangeMin...rangeMax),
                to: MeshAddress(destination),
                using: appKey
            )
        }
    }

    @objc func sendLightCtlTemperatureRangeGet(_ call: CAPPluginCall) {
        guard
            let destination = requiredUInt16("unicastAddress", in: call),
            let appKey = requiredAppKey(in: call)
        else {
            return
        }

        sendSigModelMessage(
            meshOperation: LightCTLTemperatureRangeGet.opCode,
            destination: destination,
            appKey: appKey,
            acknowledgement: true,
            failureDescription: "Light CTL Temperature Range Get",
            call: call
        ) { [weak self] destination, appKey in
            guard let self = self else { return }
            try self.meshNetworkManager.send(
                LightCTLTemperatureRangeGet(),
                to: MeshAddress(destination),
                using: appKey
            )
        }
    }
}

extension BluetoothMeshPlugin {
    fileprivate func sendSigModelMessage(
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

import Capacitor
import CoreBluetooth
import Foundation
import NordicMesh

@objc(BluetoothMeshPlugin)
public class BluetoothMeshPlugin: CAPPlugin, CAPBridgedPlugin {

    public let identifier = "BluetoothMeshPlugin"
    public let jsName = "BluetoothMesh"
    public let pluginMethods: [CAPPluginMethod] = [
        CAPPluginMethod(name: "reloadScanMeshDevices", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "getNodesOnlineStates", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "provisionDevice", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "createApplicationKey", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "removeApplicationKey", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "addApplicationKeyToNode", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "initMeshNetwork", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "importMeshNetwork", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "exportMeshNetwork", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "sendGenericOnOffSet", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "sendGenericOnOffGet", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "sendGenericPowerLevelSet", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "sendGenericPowerLevelGet", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "sendLightHslSet", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "sendLightHslGet", returnType: CAPPluginReturnPromise),
    ]

    public static var sharedMeshNetworkManager: MeshNetworkManager!

    var meshNetworkManager: MeshNetworkManager!
    static var connection: NetworkConnection?
    var provisioningController: ProvisioningController!

    private struct PendingProxyOperation {
        let id: UUID
        let call: CAPPluginCall
        let execute: () -> Void
    }

    private var pendingProxyOperations: [PendingProxyOperation] = []
    private var isWaitingForProxyConnection = false
    private let proxyConnectionTimeout: TimeInterval = 10.0

    public override func load() {
        super.load()

        print("BluetoothMeshPlugin.load called")
        NotificationManager.shared.setPlugin(self)

        meshNetworkManager = MeshNetworkManager()
        BluetoothMeshPlugin.sharedMeshNetworkManager = meshNetworkManager

        configureNetworkParameters()
        provisioningController = ProvisioningController(meshNetowrkManager: meshNetworkManager)

        do {
            let loaded = try meshNetworkManager.load()
            if loaded, meshNetworkManager.meshNetwork != nil {
                setupLocalNode()
                setupConnection()
            } else {
                print("BluetoothMeshPlugin: no stored mesh network, waiting for init/import from JS")
            }
        } catch {
            print("BluetoothMeshPlugin: failed to load mesh network: \(error)")
        }

        self.setupNodeOnlineStateMonitoring()
        NodesOnlineStateManager.shared.resetStatus()
        NodesOnlineStateManager.shared.startMonitoring()
    }

    // MARK: - Private helpers

    private func configureNetworkParameters() {
        meshNetworkManager.networkParameters = .basic { parameters in
            parameters.setDefaultTtl(5)
            parameters.discardIncompleteSegmentedMessages(after: 10.0)
            parameters.transmitSegmentAcknowledgmentMessage(
                usingSegmentReceptionInterval: 0.06,
                multipliedByMinimumDelayIncrement: 2.5
            )
            parameters.retransmitSegmentAcknowledgmentMessages(
                exactly: 1,
                timesWhenNumberOfSegmentsIsGreaterThan: 3
            )
            parameters.transmitSegments(withInterval: 0.06)
            parameters.retransmitUnacknowledgedSegmentsToUnicastAddress(
                atMost: 2,
                timesAndWithoutProgress: 2,
                timesWithRetransmissionInterval: 0.200,
                andIncrement: 2.5
            )
            parameters.retransmitAllSegmentsToGroupAddress(exactly: 3, timesWithInterval: 0.250)
            parameters.retransmitAcknowledgedMessage(after: 4.2)
            parameters.discardAcknowledgedMessages(after: 40.0)
        }
        meshNetworkManager.logger = MeshLogger()
    }

    private func setupLocalNode() {
        guard meshNetworkManager.meshNetwork != nil else {
            print("BluetoothMeshPlugin: setupLocalNode called with no meshNetwork")
            return
        }

        let primaryElement = Element(
            name: "Primary Element",
            location: .first,
            models: [
                Model(sigModelId: .genericOnOffClientModelId, delegate: GenericOnOffClientDelegate()),
                Model(sigModelId: .genericPowerLevelClientModelId, delegate: GenericPowerLevelClientDelegate()),
                Model(sigModelId: .lightHSLClientModelId, delegate: LightHSLClientDelegate()),
            ]
        )
        meshNetworkManager.localElements = [primaryElement]
    }

    private func setupConnection() {
        guard let network = meshNetworkManager.meshNetwork else {
            print("BluetoothMeshPlugin: setupConnection called with no meshNetwork")
            return
        }

        BluetoothMeshPlugin.connection?.close()

        meshNetworkManager.delegate = PluginCallManager.shared
        meshNetworkManager.heartbeatDelegate = NodesOnlineStateManager.shared

        let connection = NetworkConnection(to: network)
        connection.dataDelegate = meshNetworkManager
        connection.delegate = self

        BluetoothMeshPlugin.connection = connection
        meshNetworkManager.transmitter = connection

        connection.open()
    }

    private func ensureProxyConnection(
        for call: CAPPluginCall,
        perform operation: @escaping () -> Void
    ) {
        guard let connection = BluetoothMeshPlugin.connection else {
            call.reject("Mesh proxy connection is not initialized")
            return
        }

        if connection.isConnected {
            operation()
            return
        }

        let operationId = UUID()
        let pending = PendingProxyOperation(id: operationId, call: call, execute: operation)
        pendingProxyOperations.append(pending)

        if !isWaitingForProxyConnection {
            isWaitingForProxyConnection = true
            connection.open()
        }

        DispatchQueue.main.asyncAfter(deadline: .now() + proxyConnectionTimeout) { [weak self] in
            guard let self = self else { return }

            guard let index = self.pendingProxyOperations.firstIndex(where: { $0.id == operationId }) else {
                return
            }

            self.pendingProxyOperations.remove(at: index)

            if self.pendingProxyOperations.isEmpty {
                self.isWaitingForProxyConnection = false
            }

            call.reject("Timed out while waiting for a mesh proxy connection")
        }
    }

    private func flushPendingProxyOperations() {
        let operations = pendingProxyOperations
        pendingProxyOperations.removeAll()
        isWaitingForProxyConnection = false

        operations.forEach { $0.execute() }
    }

    private func rejectPendingProxyOperations(_ message: String) {
        let operations = pendingProxyOperations
        pendingProxyOperations.removeAll()
        isWaitingForProxyConnection = false

        operations.forEach { $0.call.reject(message) }
    }

    private func exportCurrentNetworkAsJSONObject() throws -> Any {
        let data = try meshNetworkManager.export()
        return try JSONSerialization.jsonObject(with: data, options: [])
    }

    // MARK: - Plugin methods

    @objc func reloadScanMeshDevices(_ call: CAPPluginCall) {
        DeviceRepository.shared.clearDevices()
        DeviceScanner.shared.stopScan()
        DeviceScanner.shared.startScan()
        call.resolve()
    }

    @objc func getNodesOnlineStates(_ call: CAPPluginCall) {
        let nodesOnlineState = NodesOnlineStateManager.shared.getNodesOnlineStates()

        return call.resolve(nodesOnlineState)
    }

    @objc func provisionDevice(_ call: CAPPluginCall) {
        guard let uuidString = call.getString("uuid") else {
            call.reject("UUID is required")
            return
        }

        guard let selectedPeripheral = DeviceRepository.shared.getPeripheral(uuidString: uuidString) else {
            call.reject("Device with UUID \(uuidString) not found")
            return
        }

        guard let onlyBearer = selectedPeripheral.bearer.first else {
            call.reject("No bearer available for peripheral \(uuidString)")
            return
        }

        provisioningController.openProvisioningBearer(onlyBearer) { result in
            switch result {
            case .success(let node):
                /* Add the newly provisioned node to the online state manager so that its state can be tracked and emitted to JS */
                NodesOnlineStateManager.shared.addNode(unicastAddress: node.primaryUnicastAddress)
                call.resolve([
                    "provisioningComplete": true,
                    "uuid": uuidString,
                    "unicastAddress": node.primaryUnicastAddress,
                ])

            case .failure(let error):
                print("Provisioning failed with error: \(error)")
                call.resolve([
                    "provisioningComplete": false,
                    "uuid": uuidString,
                ])
            }
        }
    }

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
                    CONFIG_APPKEY_ADD,
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

    @objc func initMeshNetwork(_ call: CAPPluginCall) {
        guard let networkName = call.getString("networkName") else {
            call.reject("networkName is required")
            return
        }

        _ = meshNetworkManager.clear()

        let provisioner = Provisioner(
            name: UIDevice.current.name,
            allocatedUnicastRange: [AddressRange(0x0001...0x199A)],
            allocatedGroupRange: [AddressRange(0xC000...0xCC9A)],
            allocatedSceneRange: [SceneRange(0x0001...0x3333)]
        )

        _ = meshNetworkManager.createNewMeshNetwork(withName: networkName, by: provisioner)

        setupLocalNode()
        _ = meshNetworkManager.save()
        setupConnection()

        do {
            let json = try exportCurrentNetworkAsJSONObject()
            call.resolve(["meshNetwork": json])
        } catch {
            call.reject("Failed to serialize mesh network: \(error.localizedDescription)")
        }
    }

    @objc func importMeshNetwork(_ call: CAPPluginCall) {
        guard let meshNetwork = call.getString("meshNetwork") else {
            call.reject("meshNetwork is required")
            return
        }

        do {
            guard let data = meshNetwork.data(using: .utf8) else {
                call.reject("Failed to encode meshNetwork as UTF-8")
                return
            }

            _ = try meshNetworkManager.import(from: data)

            _ = meshNetworkManager.save()
            setupConnection()

            self.setupNodeOnlineStateMonitoring()

            call.resolve()
        } catch {
            call.reject("Failed to import mesh network: \(error.localizedDescription)")
        }
    }

    private func setupNodeOnlineStateMonitoring() {
        NodesOnlineStateManager.shared.clearNodes()
        meshNetworkManager.meshNetwork?.nodes.forEach { node in
            if node.uuid != meshNetworkManager.meshNetwork?.localProvisioner?.uuid {
                NodesOnlineStateManager.shared.addNode(unicastAddress: node.primaryUnicastAddress)
            }
        }
    }

    @objc func exportMeshNetwork(_ call: CAPPluginCall) {
        guard meshNetworkManager.meshNetwork != nil else {
            call.reject("Mesh network not initialized")
            return
        }

        do {
            let json = try exportCurrentNetworkAsJSONObject()
            call.resolve(["meshNetwork": json])
        } catch {
            call.reject("Failed to serialize mesh network: \(error.localizedDescription)")
        }
    }

    @objc func sendGenericOnOffSet(_ call: CAPPluginCall) {
        guard let unicastAddress = call.getInt("unicastAddress") else {
            call.reject("unicastAddress is required")
            return
        }
        guard let appKeyIndex = call.getInt("appKeyIndex") else {
            call.reject("appKeyIndex is required")
            return
        }
        guard let onOff = call.getBool("onOff") else {
            call.reject("onOff is required")
            return
        }
        let acknowledgement = call.getBool("acknowledgement", false)

        guard let network = meshNetworkManager.meshNetwork else {
            call.reject("Mesh network not initialized")
            return
        }

        guard let appKey = network.applicationKeys.first(where: { $0.index == UInt16(appKeyIndex) }) else {
            call.reject("Application key with index \(appKeyIndex) not found")
            return
        }

        ensureProxyConnection(for: call) { [weak self] in
            guard let self = self else { return }

            do {
                PluginCallManager.shared.addSigPluginCall(
                    GENERIC_ON_OFF_SET,
                    UInt16(unicastAddress),
                    call
                )

                // Send the Generic OnOff Set message to the target node
                try self.meshNetworkManager
                    .send(
                        GenericOnOffSet(onOff),
                        to: MeshAddress(UInt16(unicastAddress)),
                        using: appKey
                    )

                if !acknowledgement {
                    call.resolve()
                }
            } catch {
                call.reject("Failed to send Generic OnOff Set message: \(error.localizedDescription)")
            }
        }
    }

    @objc func sendGenericOnOffGet(_ call: CAPPluginCall) {
        guard let unicastAddress = call.getInt("unicastAddress") else {
            call.reject("unicastAddress is required")
            return
        }
        guard let appKeyIndex = call.getInt("appKeyIndex") else {
            call.reject("appKeyIndex is required")
            return
        }

        guard let network = meshNetworkManager.meshNetwork else {
            call.reject("Mesh network not initialized")
            return
        }

        guard let appKey = network.applicationKeys.first(where: { $0.index == UInt16(appKeyIndex) }) else {
            call.reject("Application key with index \(appKeyIndex) not found")
            return
        }

        ensureProxyConnection(for: call) { [weak self] in
            guard let self = self else { return }

            do {
                PluginCallManager.shared.addSigPluginCall(
                    GENERIC_ON_OFF_GET,
                    UInt16(unicastAddress),
                    call
                )

                // Send the Generic OnOff Get message to the target address
                try self.meshNetworkManager
                    .send(
                        GenericOnOffGet(),
                        to: MeshAddress(UInt16(unicastAddress)),
                        using: appKey
                    )
            } catch {
                call.reject("Failed to send Generic OnOff Get message: \(error.localizedDescription)")
            }
        }
    }

    @objc func sendGenericPowerLevelSet(_ call: CAPPluginCall) {
        guard let unicastAddress = call.getInt("unicastAddress") else {
            call.reject("unicastAddress is required")
            return
        }
        guard let appKeyIndex = call.getInt("appKeyIndex") else {
            call.reject("appKeyIndex is required")
            return
        }
        guard let powerLevel = call.getInt("powerLevel") else {
            call.reject("powerLevel is required")
            return
        }
        let acknowledgement = call.getBool("acknowledgement", false)

        guard let network = meshNetworkManager.meshNetwork else {
            call.reject("Mesh network not initialized")
            return
        }

        guard let appKey = network.applicationKeys.first(where: { $0.index == UInt16(appKeyIndex) }) else {
            call.reject("Application key with index \(appKeyIndex) not found")
            return
        }

        ensureProxyConnection(for: call) { [weak self] in
            guard let self = self else { return }

            do {
                PluginCallManager.shared.addSigPluginCall(
                    GENERIC_POWER_LEVEL_SET,
                    UInt16(unicastAddress),
                    call
                )

                // Send the Generic Power Level Set message to the target node
                try self.meshNetworkManager
                    .send(
                        GenericPowerLevelSet(power: UInt16(powerLevel)),
                        to: MeshAddress(UInt16(unicastAddress)),
                        using: appKey
                    )

                if !acknowledgement {
                    call.resolve()
                }
            } catch {
                call.reject("Failed to send Generic Power Level Set message: \(error.localizedDescription)")
            }
        }
    }

    @objc func sendGenericPowerLevelGet(_ call: CAPPluginCall) {
        guard let unicastAddress = call.getInt("unicastAddress") else {
            call.reject("unicastAddress is required")
            return
        }
        guard let appKeyIndex = call.getInt("appKeyIndex") else {
            call.reject("appKeyIndex is required")
            return
        }

        guard let network = meshNetworkManager.meshNetwork else {
            call.reject("Mesh network not initialized")
            return
        }

        guard let appKey = network.applicationKeys.first(where: { $0.index == UInt16(appKeyIndex) }) else {
            call.reject("Application key with index \(appKeyIndex) not found")
            return
        }

        ensureProxyConnection(for: call) { [weak self] in
            guard let self = self else { return }

            do {
                PluginCallManager.shared.addSigPluginCall(
                    GENERIC_POWER_LEVEL_GET,
                    UInt16(unicastAddress),
                    call
                )

                // Send the Generic Power Level Get message to the target address
                try self.meshNetworkManager
                    .send(
                        GenericPowerLevelGet(),
                        to: MeshAddress(UInt16(unicastAddress)),
                        using: appKey
                    )
            } catch {
                call.reject("Failed to send Generic Power Level Get message: \(error.localizedDescription)")
            }
        }
    }

    @objc func sendLightHslSet(_ call: CAPPluginCall) {
        guard let unicastAddress = call.getInt("unicastAddress") else {
            call.reject("unicastAddress is required")
            return
        }
        guard let appKeyIndex = call.getInt("appKeyIndex") else {
            call.reject("appKeyIndex is required")
            return
        }
        guard let hue = call.getInt("hue") else {
            call.reject("hue is required")
            return
        }
        guard let saturation = call.getInt("saturation") else {
            call.reject("saturation is required")
            return
        }
        guard let lightness = call.getInt("lightness") else {
            call.reject("lightness is required")
            return
        }
        let acknowledgement = call.getBool("acknowledgement", false)

        guard let network = meshNetworkManager.meshNetwork else {
            call.reject("Mesh network not initialized")
            return
        }

        guard let appKey = network.applicationKeys.first(where: { $0.index == UInt16(appKeyIndex) }) else {
            call.reject("Application key with index \(appKeyIndex) not found")
            return
        }

        ensureProxyConnection(for: call) { [weak self] in
            guard let self = self else { return }

            do {
                PluginCallManager.shared.addSigPluginCall(
                    LIGHT_HSL_SET,
                    UInt16(unicastAddress),
                    call
                )

                // Send the Generic Power Level Set message to the target node
                try self.meshNetworkManager
                    .send(
                        LightHSLSet(lightness: UInt16(lightness), hue: UInt16(hue), saturation: UInt16(saturation)),
                        to: MeshAddress(UInt16(unicastAddress)),
                        using: appKey
                    )

                if !acknowledgement {
                    call.resolve()
                }
            } catch {
                call.reject("Failed to send Light HSL Set message: \(error.localizedDescription)")
            }
        }
    }

    @objc func sendLightHslGet(_ call: CAPPluginCall) {
        guard let unicastAddress = call.getInt("unicastAddress") else {
            call.reject("unicastAddress is required")
            return
        }
        guard let appKeyIndex = call.getInt("appKeyIndex") else {
            call.reject("appKeyIndex is required")
            return
        }

        guard let network = meshNetworkManager.meshNetwork else {
            call.reject("Mesh network not initialized")
            return
        }

        guard let appKey = network.applicationKeys.first(where: { $0.index == UInt16(appKeyIndex) }) else {
            call.reject("Application key with index \(appKeyIndex) not found")
            return
        }

        ensureProxyConnection(for: call) { [weak self] in
            guard let self = self else { return }

            do {
                PluginCallManager.shared.addSigPluginCall(
                    LIGHT_HSL_GET,
                    UInt16(unicastAddress),
                    call
                )

                // Send the Generic Power Level Get message to the target address
                try self.meshNetworkManager
                    .send(
                        LightHSLGet(),
                        to: MeshAddress(UInt16(unicastAddress)),
                        using: appKey
                    )
            } catch {
                call.reject("Failed to send Light HSL Get message: \(error.localizedDescription)")
            }
        }
    }

    func sendNotification(event: String, data: PluginCallResultData? = nil) {
        let payload = data ?? PluginCallResultData()

        DispatchQueue.main.async { [weak self] in
            guard let self = self else { return }
            print("BluetoothMeshPlugin: notifyListeners event=\(event), payload=\(payload)")
            self.notifyListeners(event, data: payload, retainUntilConsumed: true)
        }
    }
}

// MARK: - BearerDelegate

extension BluetoothMeshPlugin: BearerDelegate {
    public func bearerDidOpen(_ bearer: Bearer) {
        print("BluetoothMeshPlugin: proxy bearer opened")
        flushPendingProxyOperations()
    }

    public func bearer(_ bearer: Bearer, didClose error: Error?) {
        if let error = error {
            print("BluetoothMeshPlugin: proxy bearer closed with error: \(error.localizedDescription)")
            rejectPendingProxyOperations("Mesh proxy connection closed: \(error.localizedDescription)")
        } else {
            print("BluetoothMeshPlugin: proxy bearer closed")
            rejectPendingProxyOperations("Mesh proxy connection closed")
        }
    }
}

extension MeshNetworkManager {
    static var instance: MeshNetworkManager {
        BluetoothMeshPlugin.sharedMeshNetworkManager
    }
}

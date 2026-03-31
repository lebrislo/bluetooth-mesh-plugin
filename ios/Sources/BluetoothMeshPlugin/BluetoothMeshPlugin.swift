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
        CAPPluginMethod(name: "sendAppKeyGet", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "initMeshNetwork", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "importMeshNetwork", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "exportMeshNetwork", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "sendGenericOnOffSet", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "sendGenericOnOffGet", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "sendGenericPowerLevelSet", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "sendGenericPowerLevelGet", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "sendLightHslSet", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "sendLightHslGet", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "sendLightCtlSet", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "sendLightCtlGet", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "sendLightCtlTemperatureRangeSet", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "sendLightCtlTemperatureRangeGet", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "sendVendorModelMessage", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "sendHealthFaultGet", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "sendHealthFaultClear", returnType: CAPPluginReturnPromise),
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

        NotificationManager.shared.setPlugin(self)

        meshNetworkManager = MeshNetworkManager()
        meshNetworkManager.logger = MeshLogger()
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
                atMost: 1,
                timesAndWithoutProgress: 1,
                timesWithRetransmissionInterval: 0.200,
                andIncrement: 2.5
            )
            parameters.retransmitAllSegmentsToGroupAddress(exactly: 1, timesWithInterval: 0.250)
            parameters.retransmitAcknowledgedMessage(after: 4.2)
            parameters.discardAcknowledgedMessages(after: 40.0)
        }
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

    func ensureProxyConnection(
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
        guard let deviceId = call.getString("deviceId") else {
            call.reject("deviceId is required")
            return
        }

        guard let selectedPeripheral = DeviceRepository.shared.getPeripheral(deviceId: deviceId) else {
            call.reject("Device with deviceId \(deviceId) not found")
            return
        }

        guard let onlyBearer = selectedPeripheral.bearer.first else {
            call.reject("No bearer available for peripheral \(deviceId)")
            return
        }

        provisioningController.openProvisioningBearer(onlyBearer) { result in
            switch result {
            case .success(let node):
                /* Add the newly provisioned node to the online state manager so that its state can be tracked and emitted to JS */
                NodesOnlineStateManager.shared.addNode(unicastAddress: node.primaryUnicastAddress)
                call.resolve([
                    "provisioningComplete": true,
                    "deviceId": deviceId,
                    "unicastAddress": node.primaryUnicastAddress,
                ])

            case .failure(let error):
                print("Provisioning failed with error: \(error)")
                call.resolve([
                    "provisioningComplete": false,
                    "deviceId": deviceId,
                ])
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

    func sendNotification(event: String, data: PluginCallResultData? = nil) {
        let payload = data ?? PluginCallResultData()

        DispatchQueue.main.async { [weak self] in
            guard let self = self else { return }
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

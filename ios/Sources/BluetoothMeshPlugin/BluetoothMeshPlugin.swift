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
        CAPPluginMethod(name: "provisionDevice", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "createApplicationKey", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "removeApplicationKey", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "addApplicationKeyToNode", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "initMeshNetwork", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "importMeshNetwork", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "exportMeshNetwork", returnType: CAPPluginReturnPromise),
    ]

    var meshNetworkManager: MeshNetworkManager!
    static var connection: NetworkConnection!
    var provisioningController: ProvisioningController!
    //private var configClientHandler: ConfigurationClientHandler?

    public override init() {
        super.init()

        NotificationManager.shared.setPlugin(self)

        // 1. Create manager and configure parameters
        meshNetworkManager = MeshNetworkManager()
        configureNetworkParameters()

        // 2. Create provisioning controller
        provisioningController = ProvisioningController(meshNetowrkManager: meshNetworkManager)

        // 3. Try to load existing mesh network from storage
        do {
            let loaded = try meshNetworkManager.load()
            if loaded, meshNetworkManager.meshNetwork != nil {
                // Rebuild local node + connection
                setupLocalNode()
                setupConnection()
            } else {
                print("BluetoothMeshPlugin: no stored mesh network, waiting for init/import from JS")
            }
        } catch {
            print("BluetoothMeshPlugin: failed to load mesh network: \(error)")
        }
    }

    // MARK: - Private helpers

    /// Setup Nordic mesh network parameters
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

    /// Create the local node’s primary element and attach the Config Client.
    /// Call this **after** a mesh network has been created, loaded or imported.
    private func setupLocalNode() {
        guard let network = meshNetworkManager.meshNetwork else {
            print("BluetoothMeshPlugin: setupLocalNode called with no meshNetwork")
            return
        }

        let primaryElement = Element(name: "Primary Element", location: .first,
                models: [
                    // Generic OnOff Client model:
                    Model(sigModelId: .genericOnOffClientModelId, delegate: GenericOnOffClientDelegate()),
                ]
        )
        meshNetworkManager.localElements = [primaryElement]
    }

    /// Setup the GATT Proxy connection + delegates.
    /// Call this whenever the mesh network changes (init/import/load).
    private func setupConnection() {
        guard let network = meshNetworkManager.meshNetwork else {
            print("BluetoothMeshPlugin: setupConnection called with no meshNetwork")
            return
        }

        // Close previous connection if any
        BluetoothMeshPlugin.connection?.close()

        meshNetworkManager.delegate = PluginCallManager.shared
        BluetoothMeshPlugin.connection = NetworkConnection(to: network)
        BluetoothMeshPlugin.connection.dataDelegate = meshNetworkManager
        meshNetworkManager.transmitter = BluetoothMeshPlugin.connection

        BluetoothMeshPlugin.connection.open()
    }

    /// Convenience: export current network as JSON-serializable object
    private func exportCurrentNetworkAsJSONObject() throws -> Any {
        let data = meshNetworkManager.export()
        return try JSONSerialization.jsonObject(with: data, options: [])
    }

    @objc func reloadScanMeshDevices(_ call: CAPPluginCall) {
        DeviceRepository.shared.clearDevices()
        DeviceScanner.shared.stopScan()
        DeviceScanner.shared.startScan()
        call.resolve()
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

        do {
            PluginCallManager.shared.addConfigPluginCall(CONFIG_APPKEY_ADD, UInt16(unicastAddress), call)
            try meshNetworkManager.send(message, to: UInt16(unicastAddress))

            if !acknowledgement {
                call.resolve()
            }
        } catch {
            call.reject("Failed to add application key to node: \(error.localizedDescription)")
        }
    }

    /// Create a brand-new mesh network and return it to JS (also persisted & connected).
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

        // Local node + save + connection
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

    /// Import a mesh network JSON from JS (e.g. one obtained via export or nRF Mesh app).
    ///
    /// Expected TS side:
    ///   BluetoothMesh.importMeshNetwork({ meshNetwork: exportedObject })
    @objc func importMeshNetwork(_ call: CAPPluginCall) {
        guard let meshNetworkObject = call.getObject("meshNetwork") else {
            call.reject("meshNetwork is required")
            return
        }

        do {
            let data = try JSONSerialization.data(withJSONObject: meshNetworkObject, options: [])

            // Replace current network in manager
            _ = try meshNetworkManager.`import`(from: data)

            // Rebuild local node + save + connection
            setupLocalNode()
            _ = meshNetworkManager.save()
            setupConnection()

            call.resolve()
        } catch {
            call.reject("Failed to import mesh network: \(error.localizedDescription)")
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
        notifyListeners(event, data: data ?? PluginCallResultData())
    }
}

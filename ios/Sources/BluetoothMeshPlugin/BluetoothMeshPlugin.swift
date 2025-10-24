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
    ]

    var meshNetworkManager: MeshNetworkManager!
    var connection: NetworkConnection!
    var provisioningController: ProvisioningController!

    public override init() {
        super.init()

        NotificationManager.shared.setPlugin(self)

        meshNetworkManager = MeshNetworkManager()
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

        createNewMeshNetwork()
        
        self.provisioningController = ProvisioningController(meshNetowrkManager: self.meshNetworkManager)

        meshNetworkManager.delegate = DeviceScanner.shared
        connection = NetworkConnection(to: meshNetworkManager.meshNetwork!)
        connection!.dataDelegate = meshNetworkManager
        meshNetworkManager.transmitter = connection
        connection!.open()
    }

    func createNewMeshNetwork() {
        let provisioner = Provisioner(
            name: UIDevice.current.name,
            allocatedUnicastRange: [AddressRange(0x0001...0x199A)],
            allocatedGroupRange: [AddressRange(0xC000...0xCC9A)],
            allocatedSceneRange: [SceneRange(0x0001...0x3333)]
        )
        _ = meshNetworkManager.createNewMeshNetwork(withName: "nRF Mesh Network", by: provisioner)
        _ = meshNetworkManager.save()
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

        switch selectedPeripheral.bearer.count {
        case 0:
            call.reject("No bearer found for device with UUID \(uuidString)")
            return
        case 1:
            let onlyBearer = selectedPeripheral.bearer.first!
            do {
                onlyBearer.delegate = self
                try onlyBearer.open()
            } catch {
                call.reject("Failed to open bearer: \(error.localizedDescription)")
                return
            }
        default:
            let meshNetwork = self.meshNetworkManager.meshNetwork!
            selectedPeripheral.bearer.map { bearer in
                (bearer as? PBRemoteBearer).map {
                    guard let node = meshNetwork.node(withAddress: $0.address) else {
                        return "PB Remote (using 0x\($0.address.hex))"
                    }
                    return "PB Remote via \(node.name ?? "Unknown Node") (0x\($0.address.hex))"
                } ?? "PB GATT"
                do {
                    bearer.delegate = self
                    try bearer.open()
                } catch {
                    call.reject("Failed to open bearer: \(error.localizedDescription)")
                    return
                }
            }
        }
    }

    func sendNotification(event: String, data: PluginCallResultData? = nil) {
        self.notifyListeners(event, data: data ?? PluginCallResultData())
    }
}

extension BluetoothMeshPlugin: GattBearerDelegate {
    
    public func bearerDidConnect(_ bearer: Bearer) {
        print("Bearer connected")
    }
    
    public func bearerDidDiscoverServices(_ bearer: Bearer) {
        print("Bearer discovered services")
    }
        
    public func bearerDidOpen(_ bearer: Bearer) {
        print("Bearer opened")
        guard let unprovDevice = DeviceRepository.shared.unprovDevices.first?.device else {
            print("No unprovisioned device found")
            return
        }
        guard let provisioningBearer = bearer as? ProvisioningBearer else {
            print("Bearer is not a ProvisioningBearer")
            return
        }
        self.provisioningController.provision(unprovDevice, provisioningBearer)
    }
    
    public func bearer(_ bearer: Bearer, didClose error: Error?) {
        print("Bearer \(bearer) closed with error: \(String(describing: error))")
    }
}

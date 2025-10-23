import Foundation
import CoreBluetooth
import Capacitor
import NordicMesh

@objc(BluetoothMeshPlugin)
public class BluetoothMeshPlugin: CAPPlugin, CAPBridgedPlugin {
    public let identifier = "BluetoothMeshPlugin"
    public let jsName = "BluetoothMesh"
    public let pluginMethods: [CAPPluginMethod] = [
        CAPPluginMethod(name: "reloadScanMeshDevices", returnType: CAPPluginReturnPromise)
    ]

    var meshNetworkManager: MeshNetworkManager!
    var connection: NetworkConnection!

    public override init() {
        super.init()
        
        NotificationManager.shared.setPlugin(self)

        meshNetworkManager = MeshNetworkManager()
        meshNetworkManager.networkParameters = .basic { parameters in
            parameters.setDefaultTtl(5)
            parameters.discardIncompleteSegmentedMessages(after: 10.0)
            parameters.transmitSegmentAcknowledgmentMessage(
                usingSegmentReceptionInterval: 0.06,
                multipliedByMinimumDelayIncrement: 2.5)
            parameters.retransmitSegmentAcknowledgmentMessages(
                exactly: 1, timesWhenNumberOfSegmentsIsGreaterThan: 3)
            parameters.transmitSegments(withInterval: 0.06)
            parameters.retransmitUnacknowledgedSegmentsToUnicastAddress(
                atMost: 2, timesAndWithoutProgress: 2,
                timesWithRetransmissionInterval: 0.200, andIncrement: 2.5)
            parameters.retransmitAllSegmentsToGroupAddress(exactly: 3, timesWithInterval: 0.250)
            parameters.retransmitAcknowledgedMessage(after: 4.2)
            parameters.discardAcknowledgedMessages(after: 40.0)
        }
        
        createNewMeshNetwork()
        
        meshNetworkManager.delegate = DeviceScanner.shared
        connection = NetworkConnection(to: meshNetworkManager.meshNetwork!)
        connection!.dataDelegate = meshNetworkManager
        meshNetworkManager.transmitter = connection
        connection!.open()
    }
    
    func createNewMeshNetwork() {
        let provisioner = Provisioner(name: UIDevice.current.name,
                                      allocatedUnicastRange: [AddressRange(0x0001...0x199A)],
                                      allocatedGroupRange:   [AddressRange(0xC000...0xCC9A)],
                                      allocatedSceneRange:   [SceneRange(0x0001...0x3333)])
        let network = meshNetworkManager.createNewMeshNetwork(withName: "nRF Mesh Network", by: provisioner)
        _ = meshNetworkManager.save()
    }

    @objc func reloadScanMeshDevices(_ call: CAPPluginCall) {
        DeviceRepository.shared.clearDevices()
        DeviceScanner.shared.stopScan()
        DeviceScanner.shared.startScan()
        call.resolve()
    }
    
    func sendNotification(event: String, data: PluginCallResultData? = nil) {
        self.notifyListeners(event, data: data ?? PluginCallResultData())
    }
}

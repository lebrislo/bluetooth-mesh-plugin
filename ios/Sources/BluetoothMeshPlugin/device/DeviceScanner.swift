import Capacitor
import CoreBluetooth
import Foundation
import NordicMesh

protocol DeviceScannerDelegate: AnyObject {
    func deviceScanner(_ scanner: DeviceScanner, didUpdateBluetoothState state: CBManagerState)
    func deviceScannerDidLoseBluetooth(_ scanner: DeviceScanner)
}

class DeviceScanner: NSObject {
    static let shared = DeviceScanner()
    
    weak var delegate: DeviceScannerDelegate?

    private var centralManager: CBCentralManager!
    private let deviceStore = DeviceRepository.shared

    override init() {
        super.init()
        self.centralManager = CBCentralManager(delegate: self, queue: .main)
    }
    
    public var isBluetoothEnabled: Bool {
            centralManager.state == .poweredOn
        }

    public func startScan() {
        print("Central state: \(centralManager.state) / auth: \(CBCentralManager.authorization)")

        guard centralManager.state == .poweredOn else {
            print("Bluetooth is not powered on (state=\(centralManager.state)).")
            return
        }

        guard CBCentralManager.authorization == .allowedAlways else {
            print("Bluetooth permission not granted (authorization=\(CBCentralManager.authorization)).")
            return
        }

        centralManager.scanForPeripherals(
            withServices: [MeshProvisioningService.uuid, MeshProxyService.uuid],
            options: [CBCentralManagerScanOptionAllowDuplicatesKey: true]
        )

        DispatchQueue.main.asyncAfter(deadline: .now() + 30) { [weak self] in
            self?.stopScan()
        }
    }

    public func stopScan() {
        centralManager.stopScan()
        print("Stopped scanning for devices.")
    }
}

extension DeviceScanner: CBCentralManagerDelegate {
    public func centralManagerDidUpdateState(_ central: CBCentralManager) {
        print(
            "Central Manager did update state: \(central.state.rawValue), auth: \(CBCentralManager.authorization.rawValue)"
        )
        
        delegate?.deviceScanner(self, didUpdateBluetoothState: central.state)

        switch central.state {
        case .poweredOn:
            startScan()
        case .poweredOff:
            print("Bluetooth is OFF. Ask the user to enable it in Control Center.")
        case .unauthorized:
            print("Bluetooth unauthorized. Check Info.plist usage string and app settings.")
        case .unsupported:
            print("Bluetooth unsupported on this device.")
        case .resetting, .unknown:
            break
        @unknown default:
            break
        }
    }

    public func centralManager(
        _ central: CBCentralManager,
        didDiscover peripheral: CBPeripheral,
        advertisementData: [String: Any],
        rssi RSSI: NSNumber
    ) {
        deviceStore.peripheralsDiscovered(peripheral, advertisementData, RSSI)
    }
}

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
    private var autoStopWorkItem: DispatchWorkItem?
    private var scanSessionId: Int = 0

    override init() {
        super.init()
        self.centralManager = CBCentralManager(delegate: self, queue: .main)
    }
    
    public var isBluetoothEnabled: Bool {
            centralManager.state == .poweredOn
        }

    public func startScan() {
        // Invalidate any pending auto-stop from a previous scan session.
        autoStopWorkItem?.cancel()
        autoStopWorkItem = nil
        scanSessionId += 1
        let currentSessionId = scanSessionId

        let now = ISO8601DateFormatter().string(from: Date())
        print("Starting scan at \(now) [session=\(currentSessionId)]")
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

        let workItem = DispatchWorkItem { [weak self] in
            guard let self else { return }
            guard self.scanSessionId == currentSessionId else { return }
            self.stopScan(reason: "auto-timeout")
        }
        autoStopWorkItem = workItem
        DispatchQueue.main.asyncAfter(deadline: .now() + 30, execute: workItem)
    }

    public func stopScan() {
        stopScan(reason: "manual")
    }

    private func stopScan(reason: String) {
        autoStopWorkItem?.cancel()
        autoStopWorkItem = nil
        centralManager.stopScan()
        let now = ISO8601DateFormatter().string(from: Date())
        print("Stopped scanning for devices at \(now) [reason=\(reason)] [session=\(scanSessionId)]")
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

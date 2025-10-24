//
//  DeviceScanner.swift
//  Pods
//
//  Created by LE BRIS Loris on 23/10/2025.
//

import Capacitor
import CoreBluetooth
import Foundation

class DeviceScanner: NSObject {
    static let shared = DeviceScanner()

    private var centralManager: CBCentralManager!
    private let deviceStore = DeviceRepository.shared

    override init() {
        super.init()
        self.centralManager = CBCentralManager(
            delegate: self,
            queue: .main,
            options: [CBCentralManagerOptionShowPowerAlertKey: true]
        )
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
    }

    public func stopScan() {
        centralManager.stopScan()
        print("Stopped scanning for devices.")
    }
}

// MARK: - CBCentralManagerDelegate
extension DeviceScanner: CBCentralManagerDelegate {
    public func centralManagerDidUpdateState(_ central: CBCentralManager) {
        print("Central Manager did update state: \(central.state.rawValue), auth: \(CBCentralManager.authorization.rawValue)")

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

extension DeviceScanner: MeshNetworkDelegate {
    
    func meshNetworkManager(_ manager: MeshNetworkManager,
                            didReceiveMessage message: MeshMessage,
                            sentFrom source: Address, to destination: MeshAddress) {
        print("Received message \(message) from \(source) to \(destination)")
    }
}

//
//  DeviceRepository.swift
//  Pods
//
//  Created by LE BRIS Loris on 23/10/2025.
//

import Capacitor
import CoreBluetooth
import Foundation

final class DeviceRepository {
    static let shared = DeviceRepository()
    private init() {}

    private var devices: [DiscoveredPeripheral] = []

    public func peripheralsDiscovered(_ peripheral: CBPeripheral, _ advertisementData: [String: Any], _ RSSI: NSNumber)
    {
        guard let uuid = advertisementData.unprovisionedDeviceUUID else {
            return
        }
        // Check if a device with the same UUID was already scanned before.
        if let index = devices.firstIndex(where: { $0.device?.uuid == uuid }) {
            // Update the device name.
            // The name is only available when the device is advertising using
            // Service Data and Local Name ADs.
            let device = devices[index].device
            device?.name = advertisementData.localName
            
            // Check if the PB GATT Bearer already exists.
            if let bearerIndex = devices[index].bearer.firstIndex(where: { $0 is PBGattBearer }) {
                // If so, just update the RSSI value.
                devices[index].rssi[bearerIndex] = RSSI
            } else {
                // If the PB GATT Bearer doesn't exist, add it and corresponding RSSI value.
                let bearer = PBGattBearer(target: peripheral)
//                bearer.logger = MeshNetworkManager.instance.logger
                devices[index].bearer.append(bearer)
                devices[index].rssi.append(RSSI)
            }
        } else {
            if let unprovisionedDevice = UnprovisionedDevice(advertisementData: advertisementData) {
                let bearer = PBGattBearer(target: peripheral)
//                bearer.logger = MeshNetworkManager.instance.logger
                
                let discoveredPeripheral = DiscoveredPeripheral(
                    peripheral: peripheral,
                    device: unprovisionedDevice,
                    bearer: [bearer],
                    rssi: [RSSI]
                )
                devices.append(discoveredPeripheral)
                
                print("New device added: \(discoveredPeripheral.getJSObject())")
                notifyDeviceScanned()
            }
        }
    }
    
    public func getPeripheral(uuidString: String) -> DiscoveredPeripheral? {
        return devices.first(where: { $0.getMeshUUID() == uuidString })
    }

    public func clearDevices() -> Void {
        devices.removeAll()
    }

    //    public var allDevices: [DiscoveredPeripheral] {
    //        return Array(devices.values)
    //    }

    public var unprovDevices: [DiscoveredPeripheral] {
        return devices.filter { !$0.isProvisioned() }
    }

    public var provDevices: [DiscoveredPeripheral] {
        //        return devices.values.filter { $0.isProvisioned() }
        return []
    }

    private func notifyDeviceScanned() {
        NotificationManager.shared.sendNotification(
            event: "meshDeviceScanEvent",
            data: [
                "unprovisioned": unprovDevices.map { $0.getJSObject() },
                "provisioned": provDevices.map { $0.getJSObject() },
            ]
        )
    }
}

struct DiscoveredPeripheral {
    let peripheral: CBPeripheral
    let device: UnprovisionedDevice?
    var bearer: [ProvisioningBearer]
    var rssi: [NSNumber]

    func getJSObject() -> PluginCallResultData {
        return [
            "uuid": getMeshUUID(),
            "name": peripheral.name ?? "Unknown",
            "rssi": rssi.last ?? [NSNumber(value: 0)]
        ]
    }

    func getMeshUUID() -> String {
        return device?.uuid.uuidString ?? peripheral.identifier.uuidString
    }

    func isProvisioned() -> Bool {
        // Adjust as needed for your actual logic
        return peripheral.services?.contains(where: { $0.uuid == MeshProxyService.uuid }) ?? false
    }
}

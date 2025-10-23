//
//  DeviceRepository.swift
//  Pods
//
//  Created by LE BRIS Loris on 23/10/2025.
//

import CoreBluetooth
import Capacitor
import Foundation

final class DeviceRepository {
    static let shared = DeviceRepository()
    private init() {}

    private var devices: [String: CBPeripheral] = [:]
    
    private var unprovisionnedDevices: [UnprovisionedDevice] = []

    public func addOrUpdate(_ peripheral: CBPeripheral) {
        let uuid = peripheral.identifier.uuidString
        let isNew = devices[uuid] == nil
        devices[uuid] = peripheral

        if isNew {
            print("New device added: \(peripheral.name ?? "Unknown") - UUID: \(uuid)")
            notifyDeviceScanned()
        }
    }
    
    public func clearDevices() {
        devices.removeAll()
    }

    public var allDevices: [CBPeripheral] {
        return Array(devices.values)
    }

    public var unprovDevices: [CBPeripheral] {
        return devices.values.filter { !$0.isProvisioned() }
    }

    public var provDevices: [CBPeripheral] {
        return devices.values.filter { $0.isProvisioned() }
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

extension CBPeripheral {
    public func getJSObject() -> PluginCallResultData {
        return [
            "uuid": self.getDeviceUUID(),
            "name": self.name ?? "Unknown"
        ]
    }
    
    public func getDeviceUUID() -> String {
        return self.identifier.uuidString
    }
    
    public func isProvisioned() -> Bool {
        return self.services?.contains(where: { $0.uuid == MeshProxyService.uuid }) ?? false
    }
}

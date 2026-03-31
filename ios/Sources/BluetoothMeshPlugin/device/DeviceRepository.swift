import Capacitor
import CoreBluetooth
import Foundation
import NordicMesh

final class DeviceRepository {
    static let shared = DeviceRepository()

    private init() {}

    private let repositoryQueue = DispatchQueue(label: "com.lebrislo.bluetooth.mesh.deviceRepository")
    private let deviceTimeoutInterval: TimeInterval = 10.0

    private var devices: [DiscoveredPeripheral] = []
    private var deviceTimeouts: [String: DispatchWorkItem] = [:]

    public func peripheralsDiscovered(
        _ peripheral: CBPeripheral,
        _ advertisementData: [String: Any],
        _ RSSI: NSNumber
    ) {
        repositoryQueue.async {
            let meshUUID = advertisementData.unprovisionedDeviceUUID?.uuidString
                ?? peripheral.identifier.uuidString

            if let index = self.devices.firstIndex(where: { $0.getMeshUUID() == meshUUID }) {
                self.updateExistingDevice(
                    at: index,
                    peripheral: peripheral,
                    advertisementData: advertisementData,
                    rssi: RSSI
                )

                // No notification here:
                // rediscovery only refreshes the device and its timeout
                self.resetDeviceTimeout(for: meshUUID)
            } else {
                self.addNewDevice(
                    peripheral: peripheral,
                    advertisementData: advertisementData,
                    rssi: RSSI
                )

                self.resetDeviceTimeout(for: meshUUID)

                // Notify only when a new device is added
                self.notifyDeviceScanned()
            }
        }
    }

    public func getPeripheral(uuidString: String) -> DiscoveredPeripheral? {
        repositoryQueue.sync {
            devices.first(where: { $0.getMeshUUID() == uuidString })
        }
    }
    
    public func getPeripheral(deviceId: String) -> DiscoveredPeripheral? {
        repositoryQueue.sync {
            devices.first(where: { $0.peripheral.identifier.uuidString == deviceId })
        }
    }

    public func clearDevices() {
        repositoryQueue.async {
            let hadDevices = !self.devices.isEmpty

            self.cancelAllTimeouts()
            self.devices.removeAll()

            if hadDevices {
                self.notifyDeviceScanned()
            }
        }
    }

    public var unprovDevices: [DiscoveredPeripheral] {
        repositoryQueue.sync {
            devices.filter { $0.isUnprovisioned }
        }
    }

    public var provDevices: [DiscoveredPeripheral] {
        repositoryQueue.sync {
            devices.filter { !$0.isUnprovisioned }
        }
    }

    private func updateExistingDevice(
        at index: Int,
        peripheral: CBPeripheral,
        advertisementData: [String: Any],
        rssi: NSNumber
    ) {
        devices[index].peripheral = peripheral

        if devices[index].isUnprovisioned {
            devices[index].device?.name = advertisementData.localName
        }

        if let bearerIndex = devices[index].bearer.firstIndex(where: { $0 is PBGattBearer }) {
            devices[index].bearer[bearerIndex] = PBGattBearer(target: peripheral)
            devices[index].rssi[bearerIndex] = rssi
        } else {
            devices[index].bearer.append(PBGattBearer(target: peripheral))
            devices[index].rssi.append(rssi)
        }
    }

    private func addNewDevice(
        peripheral: CBPeripheral,
        advertisementData: [String: Any],
        rssi: NSNumber
    ) {
        let unprovisionedDevice = UnprovisionedDevice(advertisementData: advertisementData)
        let bearer = PBGattBearer(target: peripheral)

        let discoveredPeripheral = DiscoveredPeripheral(
            peripheral: peripheral,
            device: unprovisionedDevice,
            isUnprovisioned: unprovisionedDevice != nil,
            bearer: [bearer],
            rssi: [rssi]
        )

        devices.append(discoveredPeripheral)
        print("New device added: \(discoveredPeripheral.getJSObject())")
    }

    private func resetDeviceTimeout(for meshUUID: String) {
        deviceTimeouts[meshUUID]?.cancel()

        let workItem = DispatchWorkItem { [weak self] in
            guard let self = self else { return }

            self.repositoryQueue.async {
                let initialCount = self.devices.count
                self.devices.removeAll { $0.getMeshUUID() == meshUUID }
                self.deviceTimeouts.removeValue(forKey: meshUUID)

                if self.devices.count != initialCount {
                    print("Removed device due to timeout: \(meshUUID)")
                    self.notifyDeviceScanned()
                }
            }
        }

        deviceTimeouts[meshUUID] = workItem
        repositoryQueue.asyncAfter(deadline: .now() + deviceTimeoutInterval, execute: workItem)
    }

    private func cancelAllTimeouts() {
        deviceTimeouts.values.forEach { $0.cancel() }
        deviceTimeouts.removeAll()
    }

    private func notifyDeviceScanned() {
        let unprovisioned = devices
            .filter { $0.isUnprovisioned }
            .map { $0.getJSObject() }

        let provisioned = devices
            .filter { !$0.isUnprovisioned }
            .map { $0.getJSObject() }

        DispatchQueue.main.async {
            NotificationManager.shared.sendNotification(
                event: "meshDeviceScanEvent",
                data: [
                    "unprovisioned": unprovisioned,
                    "provisioned": provisioned
                ]
            )
        }
    }
}

struct DiscoveredPeripheral {
    var peripheral: CBPeripheral
    var device: UnprovisionedDevice?
    let isUnprovisioned: Bool
    var bearer: [ProvisioningBearer]
    var rssi: [NSNumber]

    func getJSObject() -> PluginCallResultData {
        [
            "meshUuid": getMeshUUID(),
            "deviceId": peripheral.identifier.uuidString,
            "name": device?.name ?? peripheral.name ?? "Unknown",
            "rssi": rssi.last ?? NSNumber(value: 0)
        ]
    }

    func getMeshUUID() -> String {
        device?.uuid.uuidString ?? peripheral.identifier.uuidString
    }
}

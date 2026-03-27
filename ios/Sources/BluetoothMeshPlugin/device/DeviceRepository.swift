import Capacitor
import CoreBluetooth
import Foundation
import NordicMesh

final class DeviceRepository {
    static let shared = DeviceRepository()
    private init() {}

    private var devices: [DiscoveredPeripheral] = []

    public func peripheralsDiscovered(
        _ peripheral: CBPeripheral,
        _ advertisementData: [String: Any],
        _ RSSI: NSNumber
    ) {
        let meshUUID = advertisementData.unprovisionedDeviceUUID?.uuidString
            ?? peripheral.identifier.uuidString

        if let index = devices.firstIndex(where: { $0.getMeshUUID() == meshUUID }) {
            devices[index].peripheral = peripheral
            let device = devices[index].device
            device?.name = advertisementData.localName

            if let bearerIndex = devices[index].bearer.firstIndex(where: { $0 is PBGattBearer }) {
                devices[index].bearer[bearerIndex] = PBGattBearer(target: peripheral)
                devices[index].rssi[bearerIndex] = RSSI
            } else {
                let bearer = PBGattBearer(target: peripheral)
                devices[index].bearer.append(bearer)
                devices[index].rssi.append(RSSI)
            }
        } else {
            let unprovisionedDevice = UnprovisionedDevice(advertisementData: advertisementData)
            let bearer = PBGattBearer(target: peripheral)

            let discoveredPeripheral = DiscoveredPeripheral(
                peripheral: peripheral,
                device: unprovisionedDevice,
                isUnprovisioned: unprovisionedDevice != nil,
                bearer: [bearer],
                rssi: [RSSI]
            )

            devices.append(discoveredPeripheral)
            print("New device added: \(discoveredPeripheral.getJSObject())")
        }

        notifyDeviceScanned()
    }

    public func getPeripheral(uuidString: String) -> DiscoveredPeripheral? {
        devices.first(where: { $0.getMeshUUID() == uuidString })
    }

    public func clearDevices() {
        devices.removeAll()
    }

    public var unprovDevices: [DiscoveredPeripheral] {
        devices.filter { $0.isUnprovisioned }
    }

    public var provDevices: [DiscoveredPeripheral] {
        devices.filter { !$0.isUnprovisioned }
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
    var peripheral: CBPeripheral
    let device: UnprovisionedDevice?
    let isUnprovisioned: Bool
    var bearer: [ProvisioningBearer]
    var rssi: [NSNumber]

    func getJSObject() -> PluginCallResultData {
        [
            "uuid": getMeshUUID(),
            "name": device?.name ?? peripheral.name ?? "Unknown",
            "rssi": rssi.last ?? NSNumber(value: 0),
        ]
    }

    func getMeshUUID() -> String {
        device?.uuid.uuidString ?? peripheral.identifier.uuidString
    }
}

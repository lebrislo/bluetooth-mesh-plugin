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
        guard let uuid = advertisementData.unprovisionedDeviceUUID else {
            return
        }

        if let index = devices.firstIndex(where: { $0.device?.uuid == uuid }) {
            let device = devices[index].device
            device?.name = advertisementData.localName

            if let bearerIndex = devices[index].bearer.firstIndex(where: { $0 is PBGattBearer }) {
                devices[index].rssi[bearerIndex] = RSSI
            } else {
                let bearer = PBGattBearer(target: peripheral)
                devices[index].bearer.append(bearer)
                devices[index].rssi.append(RSSI)
            }
        } else {
            if let unprovisionedDevice = UnprovisionedDevice(advertisementData: advertisementData) {
                let bearer = PBGattBearer(target: peripheral)

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
        devices.first(where: { $0.getMeshUUID() == uuidString })
    }

    public func clearDevices() {
        devices.removeAll()
    }

    public var unprovDevices: [DiscoveredPeripheral] {
        devices.filter { !$0.isProvisioned() }
    }

    public var provDevices: [DiscoveredPeripheral] {
        []
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
        [
            "uuid": getMeshUUID(),
            "name": peripheral.name ?? "Unknown",
            "rssi": rssi.last ?? NSNumber(value: 0),
        ]
    }

    func getMeshUUID() -> String {
        device?.uuid.uuidString ?? peripheral.identifier.uuidString
    }

    func isProvisioned() -> Bool {
        peripheral.services?.contains(where: { $0.uuid == MeshProxyService.uuid }) ?? false
    }
}

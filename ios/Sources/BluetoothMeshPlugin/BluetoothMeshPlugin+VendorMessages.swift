//
//  BluetoothMeshPlugin+VendorMessages.swift
//  BluetoothMeshPlugin
//
//  Created by LE BRIS Loris on 26/03/2026.
//

import Capacitor
import Foundation
import NordicMesh

private enum VendorOpcodeBuilder {
    static func build(companyId: UInt16, vendorOpcode: UInt32) throws -> UInt32 {
        guard vendorOpcode <= 0x3F else {
            throw NSError(
                domain: "BluetoothMeshPlugin",
                code: 1,
                userInfo: [
                    NSLocalizedDescriptionKey:
                        "Vendor opcode must be in range 0...63 when using Android-style input"
                ]
            )
        }

        // Bluetooth Mesh vendor opcode on the wire:
        // octet0 = 0b11xxxxxx
        // octet1 = companyId LSB
        // octet2 = companyId MSB
        let firstOctet = UInt32(0xC0 | UInt8(vendorOpcode & 0x3F))
        let companyLsb = UInt32(companyId & 0x00FF)
        let companyMsb = UInt32((companyId >> 8) & 0x00FF)

        return (firstOctet << 16) | (companyLsb << 8) | companyMsb
    }
}

private struct RuntimeUnackVendorMessage: UnacknowledgedVendorMessage {
    let modelId: UInt32
    let companyId: UInt16
    let vendorOpcode: UInt32

    let opCode: UInt32
    let parameters: Data?

    var isSegmented: Bool = false
    var security: MeshMessageSecurity = .low

    init(modelId: UInt32, companyId: UInt16, opCode: UInt32, parameters: Data) throws {
        let modelCompanyId = UInt16((modelId >> 16) & 0xFFFF)
        guard modelCompanyId == companyId else {
            throw NSError(
                domain: "BluetoothMeshPlugin",
                code: 2,
                userInfo: [
                    NSLocalizedDescriptionKey:
                        "companyId does not match modelId upper 16 bits"
                ]
            )
        }

        self.modelId = modelId
        self.companyId = companyId
        self.vendorOpcode = opCode
        self.opCode = try VendorOpcodeBuilder.build(companyId: companyId, vendorOpcode: opCode)
        self.parameters = parameters
    }

    init?(parameters: Data) { nil }
}

private struct RuntimeAckVendorMessage: AcknowledgedVendorMessage {
    let modelId: UInt32
    let companyId: UInt16
    let vendorOpcode: UInt32
    let responseVendorOpcode: UInt32

    let opCode: UInt32
    let responseOpCode: UInt32
    let parameters: Data?

    var isSegmented: Bool = false
    var security: MeshMessageSecurity = .low

    init(
        modelId: UInt32,
        companyId: UInt16,
        opCode: UInt32,
        responseOpCode: UInt32,
        parameters: Data
    ) throws {
        let modelCompanyId = UInt16((modelId >> 16) & 0xFFFF)
        guard modelCompanyId == companyId else {
            throw NSError(
                domain: "BluetoothMeshPlugin",
                code: 2,
                userInfo: [
                    NSLocalizedDescriptionKey:
                        "companyId does not match modelId upper 16 bits"
                ]
            )
        }

        self.modelId = modelId
        self.companyId = companyId
        self.vendorOpcode = opCode
        self.responseVendorOpcode = responseOpCode

        self.opCode = try VendorOpcodeBuilder.build(companyId: companyId, vendorOpcode: opCode)
        self.responseOpCode = try VendorOpcodeBuilder.build(
            companyId: companyId,
            vendorOpcode: responseOpCode
        )
        self.parameters = parameters
    }

    init?(parameters: Data) { nil }
}

extension BluetoothMeshPlugin {
    private func extractPayloadData(from call: CAPPluginCall) -> Data? {
        // Case 1: payload arrives as a plain JS array / number[]
        if let arr = call.getArray("payload", Int.self) {
            return Data(arr.map { UInt8($0 & 0xFF) })
        }

        // Case 2: payload arrives as NSArray of NSNumber
        if let arr = call.getArray("payload") as? [NSNumber] {
            return Data(arr.map { UInt8(truncating: $0) })
        }

        // Case 3: payload arrives as object like { "0": 1, "1": 2, "2": 255 }
        if let obj = call.getObject("payload") {
            let sortedKeys = obj.keys.sorted {
                (Int($0) ?? Int.max) < (Int($1) ?? Int.max)
            }

            var bytes = [UInt8]()
            bytes.reserveCapacity(sortedKeys.count)

            for key in sortedKeys {
                if let value = obj[key] as? Int {
                    bytes.append(UInt8(value & 0xFF))
                } else if let value = obj[key] as? NSNumber {
                    bytes.append(UInt8(truncating: value))
                } else {
                    return nil
                }
            }

            return Data(bytes)
        }

        // Case 4: absent payload = empty payload
        return Data()
    }

    @objc func sendVendorModelMessage(_ call: CAPPluginCall) {
        guard
            let destination = requiredUInt16("unicastAddress", in: call),
            let appKey = requiredAppKey(in: call),
            let modelId = requiredUInt32("modelId", in: call),
            let rawOpcode = requiredUInt32("opcode", in: call)
        else {
            return call.reject("Missing required parameter")
        }

        let derivedCompanyId = UInt16((modelId >> 16) & 0xFFFF)
        let companyId: UInt16

        if let explicitCompanyId = call.getInt("companyIdentifier") {
            companyId = UInt16(explicitCompanyId & 0xFFFF)
        } else {
            companyId = derivedCompanyId
        }

        let rawResponseOpcode = call.getInt("opPairCode")

        guard let payloadData = extractPayloadData(from: call) else {
            call.reject("Invalid payload format")
            return
        }

        ensureProxyConnection(for: call) {
            Task {
                do {
                    if let rawResponseOpcode {
                        let msg = try RuntimeAckVendorMessage(
                            modelId: modelId,
                            companyId: companyId,
                            opCode: rawOpcode,
                            responseOpCode: UInt32(rawResponseOpcode),
                            parameters: payloadData
                        )

                        PluginCallManager.shared.addVendorPluginCall(
                            modelId,
                            msg.opCode,
                            msg.responseOpCode,
                            destination,
                            call
                        )

                        try await self.meshNetworkManager.send(
                            msg,
                            to: MeshAddress(destination),
                            using: appKey
                        )
                    } else {
                        let msg = try RuntimeUnackVendorMessage(
                            modelId: modelId,
                            companyId: companyId,
                            opCode: rawOpcode,
                            parameters: payloadData
                        )

                        try await self.meshNetworkManager.send(
                            msg,
                            to: MeshAddress(destination),
                            using: appKey
                        )

                        call.resolve()
                    }
                } catch {
                    call.reject("Failed to send Vendor Model Message: \(error.localizedDescription)")
                }
            }
        }
    }
}

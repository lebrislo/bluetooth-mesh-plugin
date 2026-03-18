//
//  VendorPluginCall.swift
//  BluetoothMeshPlugin
//
//  Created by LE BRIS Loris on 14/11/2025.
//

import Capacitor
import Foundation
import NordicMesh

public class VendorPluginCall: BasePluginCall {
    let modelId: UInt32
    let meshOperation: UInt32
    let meshOperationCallback: UInt32
    let meshAddress: UInt16
    let pluginCall: CAPPluginCall

    init(
        _ modelId: UInt32,
        _ meshOperation: UInt32,
        _ meshOperationCallback: UInt32,
        _ meshAddress: UInt16,
        _ call: CAPPluginCall
    ) {
        self.modelId = modelId
        self.meshOperation = meshOperation
        self.meshOperationCallback = meshOperationCallback
        self.meshAddress = meshAddress
        self.pluginCall = call
        super.init(call: call)
    }

    static func generateVendorPluginCallResponse(_ response: RoutedMeshMessage) -> PluginCallResultData {
        var result = PluginCallResultData()
        result["src"] = response.src
        result["dst"] = response.dst
        result["opcode"] = response.message.opCode
        result["vendorModelId"] = (response.message as? VendorResponse)?.companyIdentifier
        result["data"] = response.message.parameters
        return result
    }
}

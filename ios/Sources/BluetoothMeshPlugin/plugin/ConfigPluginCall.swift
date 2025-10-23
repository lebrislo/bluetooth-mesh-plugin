////
////  ConfigPluginCall.swift
////  BluetoothMeshPlugin
////
////  Created by LE BRIS Loris on 22/10/2025.
////
//
//import Foundation
//import Capacitor
//
//class ConfigPluginCall: BasePluginCall {
//    let meshOperationCallback: Int
//    let meshAddress: Int
//
//    init(meshOperationCallback: Int, meshAddress: Int, call: CAPPluginCall) {
//        self.meshOperationCallback = meshOperationCallback
//        self.meshAddress = meshAddress
//        super.init(call: call)
//    }
//
//    static func generateConfigPluginCallResponse(meshMessage: MeshMessage) -> PluginCallResultData {
//        var result = PluginCallResultData()
//        result["src"] = meshMessage.
//        result["dst"] = meshMessage.dst
//        result["opcode"] = meshMessage.opCode
//
//        if let msg = meshMessage as? ConfigAppKeyStatus {
//            result["data"] = configAppKeyStatusResponse(msg)
//        } else if let msg = meshMessage as? ConfigNodeResetStatus {
//            result["data"] = configNodeResetStatusResponse(msg)
//        } else if let msg = meshMessage as? ConfigModelAppStatus {
//            result["data"] = configModelAppStatusResponse(msg)
//        } else if let msg = meshMessage as? ConfigCompositionDataStatus {
//            result["data"] = configCompositionDataStatusResponse(msg)
//        } else {
//            result["data"] = PluginCallResultData()
//        }
//        return result
//    }
//
//    private static func configAppKeyStatusResponse(_ meshMessage: ConfigAppKeyStatus) -> PluginCallResultData {
//        var data = PluginCallResultData()
//        data["status"] = meshMessage.status
//        data["netKeyIndex"] = meshMessage.networkKeyIndex
//        data["appKeyIndex"] = meshMessage.applicationKeyIndex
//        return data
//    }
//
//    private static func configNodeResetStatusResponse(_ meshMessage: ConfigNodeResetStatus) -> PluginCallResultData {
//        var data = PluginCallResultData()
//        data["status"] = meshMessage.status
//        return data
//    }
//
//    private static func configModelAppStatusResponse(_ meshMessage: ConfigModelAppStatus) -> PluginCallResultData {
//        var data = PluginCallResultData()
//        data["status"] = meshMessage.status
//        data["elementAddress"] = meshMessage.elementAddress
//        data["modelId"] = meshMessage.modelIdentifier
//        data["appKeyIndex"] = meshMessage.applicationKeyIndex
//        return data
//    }
//
//    private static func configCompositionDataStatusResponse(_ meshMessage: ConfigCompositionDataStatus) -> PluginCallResultData {
//        var data = PluginCallResultData()
//        data["status"] = meshMessage
//        data["companyIdentifier"] = meshMessage.companyIdentifier
//        data["productIdentifier"] = meshMessage.productIdentifier
//        data["versionIdentifier"] = meshMessage.versionIdentifier
//        data["crpl"] = meshMessage.crpl
//        data["features"] = meshMessage.features
//        data["relayFeatureSupported"] = meshMessage.isRelayFeatureSupported
//        data["proxyFeatureSupported"] = meshMessage.isProxyFeatureSupported
//        data["friendFeatureSupported"] = meshMessage.isFriendFeatureSupported
//        data["lowPowerFeatureSupported"] = meshMessage.isLowPowerFeatureSupported
//
//        var elements: JSArray = []
//        for element in meshMessage.elements {
//            var elementData = PluginCallResultData()
//            elementData["locationDescriptor"] = element.value.locationDescriptor
//            elementData["elementAddress"] = element.value.elementAddress
//
//            var models: JSArray = []
//            for model in element.value.meshModels {
//                var modelData: PluginCallResultData = [:]
//                modelData["modelId"] = model.value.modelId
//                modelData["modelName"] = model.value.modelName
//                models.append(modelData)
//            }
//            elementData["models"] = models
//            elements.append(elementData)
//        }
//        data["elements"] = elements
//        return data
//    }
//}

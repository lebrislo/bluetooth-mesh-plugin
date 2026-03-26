//
//  BluetoothMeshPlugin+MethodParams.swift
//  BluetoothMeshPlugin
//
//  Created by LE BRIS Loris on 26/03/2026.
//

import Foundation
import Capacitor
import NordicMesh

extension BluetoothMeshPlugin {
    func requiredUInt16(_ key: String, in call: CAPPluginCall) -> UInt16? {
        guard let value = call.getInt(key) else {
            call.reject("\(key) is required")
            return nil
        }
        return UInt16(value)
    }
    
    func requiredInt16(_ key: String, in call: CAPPluginCall) -> Int16? {
        guard let value = call.getInt(key) else {
            call.reject("\(key) is required")
            return nil
        }
        return Int16(value)
    }
    
    func requiredUInt32(_ key: String, in call: CAPPluginCall) -> UInt32? {
        guard let value = call.getInt(key) else {
            call.reject("\(key) is required")
            return nil
        }
        return UInt32(value)
    }
    
    func requiredBool(_ key: String, in call: CAPPluginCall) -> Bool? {
        guard let value = call.getBool(key) else {
            call.reject("\(key) is required")
            return nil
        }
        return value
    }
    
    func requiredAppKey(in call: CAPPluginCall) -> ApplicationKey? {
        guard let appKeyIndex = call.getInt("appKeyIndex") else {
            call.reject("appKeyIndex is required")
            return nil
        }
        
        guard let network = meshNetworkManager.meshNetwork else {
            call.reject("Mesh network not initialized")
            return nil
        }
        
        guard let appKey = network.applicationKeys.first(where: { $0.index == UInt16(appKeyIndex) }) else {
            call.reject("Application key with index \(appKeyIndex) not found")
            return nil
        }
        
        return appKey
    }
}

//
//  PluginCallManager.swift
//  BluetoothMeshPlugin
//
//  Created by LE BRIS Loris on 30/10/2025.
//

import Capacitor
import Foundation

public class PluginCallManager {
    static let shared = PluginCallManager()

    private var pluginCalls: [CAPPluginCall] = []

    public func removePluginCall(_ call: CAPPluginCall) {
        if let index = pluginCalls.firstIndex(where: { $0 === call }) {
            pluginCalls.remove(at: index)
        }
    }

    
}

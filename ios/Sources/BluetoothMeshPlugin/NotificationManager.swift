//
//  NotificationManager.swift
//  Pods
//
//  Created by LE BRIS Loris on 23/10/2025.
//

import Foundation
import Capacitor

class NotificationManager {
    static let shared = NotificationManager()
    private init() {}

    private var plugin: BluetoothMeshPlugin?

    private func assertPlugin() throws {
        guard plugin != nil else {
            throw NSError(domain: "NotificationManager", code: 1, userInfo: [NSLocalizedDescriptionKey: "Plugin not set"])
        }
    }

    /// Set the plugin, must be called before any other method.
    func setPlugin(_ plugin: BluetoothMeshPlugin) {
        self.plugin = plugin
    }

    func sendNotification(event: String, data: PluginCallResultData? = nil) {
        do {
            try assertPlugin()
            plugin?.sendNotification(event: event, data: data)
        } catch {
            print(error.localizedDescription)
        }
    }
}

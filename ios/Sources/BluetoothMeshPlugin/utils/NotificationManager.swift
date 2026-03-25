//
//  NotificationManager.swift
//  Pods
//
//  Created by LE BRIS Loris on 23/10/2025.
//

import Capacitor
import Foundation

final class NotificationManager {
    static let shared = NotificationManager()
    private init() {}

    private weak var plugin: BluetoothMeshPlugin?

    func setPlugin(_ plugin: BluetoothMeshPlugin) {
        self.plugin = plugin
        print("NotificationManager: plugin set")
    }

    func sendNotification(event: String, data: PluginCallResultData? = nil) {
        guard let plugin = plugin else {
            print("NotificationManager: plugin is nil for event \(event)")
            return
        }

        plugin.sendNotification(event: event, data: data)
    }
}

//
//  BasePluginCall.swift
//  BluetoothMeshPlugin
//
//  Created by LE BRIS Loris on 22/10/2025.
//

import Capacitor
import Foundation

public class BasePluginCall {
    public static let defaultTimeout: TimeInterval = 3.0

    let call: CAPPluginCall
    private let timeout: TimeInterval
    private var isResolved = false
    private var timeoutWorkItem: DispatchWorkItem?

    init(call: CAPPluginCall, timeout: TimeInterval? = nil) {
        self.call = call
        self.timeout = timeout ?? Self.defaultTimeout
        startTimeout()
    }

    private func startTimeout() {
        let workItem = DispatchWorkItem { [weak self] in
            guard let self = self, !self.isResolved else { return }
            var rejectObject = PluginCallResultData()
            rejectObject["methodName"] = self.call.methodName
            for (key, value) in self.call.options {
                guard let key = key as? String else { continue }
                rejectObject[key] = value
            }
            self.reject(message: "Operation timed out", data: rejectObject)
        }
        timeoutWorkItem = workItem
        DispatchQueue.main.asyncAfter(deadline: .now() + timeout, execute: workItem)
    }

    public func resolve(_ result: PluginCallResultData) {
        guard !isResolved else { return }
        isResolved = true
        timeoutWorkItem?.cancel()
        call.resolve(result)
        PluginCallManager.shared.removePluginCall(self)
    }

    private func reject(message: String, data: PluginCallResultData? = nil) {
        guard !isResolved else { return }
        isResolved = true
        timeoutWorkItem?.cancel()
        call.reject(message, nil, nil, data)
        PluginCallManager.shared.removePluginCall(self)
    }
}

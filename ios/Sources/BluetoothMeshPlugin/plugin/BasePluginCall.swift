////
////  BasePluginCall.swift
////  BluetoothMeshPlugin
////
////  Created by LE BRIS Loris on 22/10/2025.
////
//
//import Foundation
//import Capacitor
//
//class BasePluginCall {
//    let call: CAPPluginCall
//    private let timeout: TimeInterval
//    private var isResolved = false
//    private var timer: Timer?
//
//    init(call: CAPPluginCall, timeout: TimeInterval = 10.0) {
//        self.call = call
//        self.timeout = timeout
//        self.startTimeout()
//    }
//
//    private func startTimeout() {
//        timer = Timer.scheduledTimer(withTimeInterval: timeout, repeats: false) { [weak self] _ in
//            guard let self = self, !self.isResolved else { return }
//            var rejectObject = PluginCallResultData()
//            rejectObject["methodName"] = self.call.methodName
//            for (key, value) in self.call.options {
//                rejectObject[key as! String] = value
//            }
//            self.reject(message: "Operation timed out", data: rejectObject)
//        }
//    }
//
//    func resolve(result: JSObject) {
//        isResolved = true
//        timer?.invalidate()
//        call.resolve(result)
//    }
//
////    private func reject(message: String, data: PluginCallResultData? = nil) {
////        timer?.invalidate()
////        call.reject(message, data)
////        PluginCallManager.shared.removePluginCall(self)
////    }
//}

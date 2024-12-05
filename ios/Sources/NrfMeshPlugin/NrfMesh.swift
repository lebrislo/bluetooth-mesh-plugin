import Foundation

@objc public class BluetoothMesh: NSObject {
    @objc public func echo(_ value: String) -> String {
        print(value)
        return value
    }
}

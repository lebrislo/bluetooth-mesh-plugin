import Foundation

@objc public class NrfMesh: NSObject {
    @objc public func echo(_ value: String) -> String {
        print(value)
        return value
    }
}

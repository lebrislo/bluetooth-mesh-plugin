import Foundation
import NordicMesh

/// Custom logger implementation conforming to MeshLoggerDelegate
/// This logger handles all mesh network debug and error messages
class MeshLogger: LoggerDelegate {
    
    
    func log(message: String, ofCategory category: NordicMesh.LogCategory, withLevel level: NordicMesh.LogLevel) {
        let levelString: String
        switch level {
        case .verbose:
            levelString = "VERBOSE"
        case .debug:
            levelString = "DEBUG"
        case .info:
            levelString = "INFO"
        case .warning:
            levelString = "WARNING"
        case .error:
            levelString = "ERROR"
        @unknown default:
            levelString = "UNKNOWN"
        }
        
        #if DEBUG
        if level == .verbose || level == .debug {
            print("[NordicMesh \(levelString)] [\(category)] \(message)")
        } else {
            print("[NordicMesh \(levelString)] [\(category)] \(message)")
        }
        #else
        if level != .verbose && level != .debug {
            print("[NordicMesh \(levelString)] [\(category)] \(message)")
        }
        #endif
    }
}

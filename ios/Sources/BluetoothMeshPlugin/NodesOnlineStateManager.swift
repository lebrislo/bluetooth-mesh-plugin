//
//  File.swift
//  BluetoothMeshPlugin
//
//  Created by LE BRIS Loris on 25/03/2026.
//

import Capacitor
import Foundation
import NordicMesh

final class NodesOnlineStateManager {

    static let shared = NodesOnlineStateManager()

    static let meshNodeOnlineStateEvent = "meshNodeOnlineStateEvent"

    /// Timeout offline en millisecondes (par défaut: 8000ms)
    private let offlineTimeoutMs: Int64

    /// Queue série: garantit la cohérence des dictionnaires (équivalent de ConcurrentHashMap + discipline d'accès)
    private let stateQueue = DispatchQueue(label: "com.lebrislo.bluetooth.mesh.NodesOnlineStateManager.state")

    /// Etat online/offline par nœud
    private var nodes: [UInt16: Bool] = [:]

    /// Dernier heartbeat reçu (epoch ms) par nœud
    private var heartbeatTimestampsMs: [UInt16: Int64] = [:]

    /// Timer de monitoring
    private var monitoringTimer: DispatchSourceTimer?

    private init(offlineTimeoutMs: Int64 = 8_000) {
        self.offlineTimeoutMs = offlineTimeoutMs
    }

    // MARK: - Gestion des nœuds

    func addNode(unicastAddress: UInt16) {
        stateQueue.sync {
            guard nodes[unicastAddress] == nil else { return }
            NSLog("NodesOnlineStateManager: add node \(unicastAddress)")
            nodes[unicastAddress] = false
            heartbeatTimestampsMs[unicastAddress] = 0
        }
    }

    func removeNode(unicastAddress: UInt16) {
        stateQueue.sync {
            NSLog("NodesOnlineStateManager: remove node \(unicastAddress)")
            nodes.removeValue(forKey: unicastAddress)
            heartbeatTimestampsMs.removeValue(forKey: unicastAddress)
        }
    }

    func clearNodes() {
        stateQueue.sync {
            NSLog("NodesOnlineStateManager: clear nodes")
            nodes.removeAll()
            heartbeatTimestampsMs.removeAll()
        }
    }

    /// Re-cadre tous les nœuds à offline (ne touche pas les timestamps).
    func resetStatus() {
        stateQueue.sync {
            // Snapshot des clés pour éviter tout piège d’itération + mutation
            let keys = Array(nodes.keys)
            for addr in keys {
                nodes[addr] = false
            }
        }
    }

    // MARK: - Heartbeat

    /// Appelé quand un heartbeat (ou un message servant de heartbeat) est reçu.
    func heartbeatReceived(unicastAddress: UInt16) {
        let now = Self.nowMs()
        stateQueue.sync {
            guard nodes[unicastAddress] != nil else {
                NSLog("NodesOnlineStateManager: unknown node \(unicastAddress)")
                return
            }
            heartbeatTimestampsMs[unicastAddress] = now
        }
    }

    // MARK: - Monitoring périodique

    func startMonitoring() {
        stateQueue.sync {
            guard monitoringTimer == nil else { return }

            // Le timer exécute son handler sur stateQueue => accès aux dictionnaires sans locks additionnels
            let timer = DispatchSource.makeTimerSource(queue: stateQueue)
            timer.schedule(deadline: .now(), repeating: .seconds(1))
            timer.setEventHandler { [weak self] in
                self?.tickLocked()
            }
            timer.resume()

            monitoringTimer = timer
        }
    }

    func stopMonitoring() {
        stateQueue.sync {
            monitoringTimer?.cancel()
            monitoringTimer = nil
        }
    }

    // MARK: - Snapshot JSON pour JS

    func getNodesOnlineStates() -> JSObject {
        stateQueue.sync {
            makeSnapshotLocked()
        }
    }

    /// Equivalent Kotlin notifyNetwork(): envoie un snapshot courant.
    func notifyNetwork() {
        let snapshot = getNodesOnlineStates()
        NotificationManager.shared.sendNotification(
            event: NodesOnlineStateManager.meshNodeOnlineStateEvent,
            data: snapshot
        )
    }

    // MARK: - Internals (toujours appelés sur stateQueue)

    private func tickLocked() {
        let now = Self.nowMs()
        let addresses = Array(nodes.keys)

        var hasStateChanged = false

        for addr in addresses {
            let lastHeartbeat = heartbeatTimestampsMs[addr] ?? 0
            let isOnline = (now - lastHeartbeat) < offlineTimeoutMs

            if nodes[addr] != isOnline {
                nodes[addr] = isOnline
                hasStateChanged = true
            }
        }

        if hasStateChanged {
            let snapshot = makeSnapshotLocked()
            NotificationManager.shared.sendNotification(
                event: NodesOnlineStateManager.meshNodeOnlineStateEvent,
                data: snapshot
            )
        }
    }

    private func makeSnapshotLocked() -> JSObject {
        var states: JSArray = []
        states.reserveCapacity(nodes.count)

        for (unicastAddress, isOnline) in nodes {
            let state: JSObject = [
                "unicastAddress": Int(unicastAddress),
                "isOnline": isOnline,
            ]
            states.append(state)
        }

        return ["states": states]
    }

    private static func nowMs() -> Int64 {
        Int64(Date().timeIntervalSince1970 * 1000)
    }
}

extension NodesOnlineStateManager: MeshHeartbeatDelegate {
    public func meshNetworkManager(
        _ manager: MeshNetworkManager,
        didReceiveHeartbeat heartbeat: MeshHeartbeat
    ) {

        let src = UInt16(heartbeat.source)  // Address est typiquement UInt16-like
        NodesOnlineStateManager.shared.heartbeatReceived(unicastAddress: src)
    }
}

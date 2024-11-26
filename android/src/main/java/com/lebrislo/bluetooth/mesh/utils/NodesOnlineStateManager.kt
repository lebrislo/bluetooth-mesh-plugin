package com.lebrislo.bluetooth.mesh.utils

import com.getcapacitor.JSObject
import com.lebrislo.bluetooth.mesh.NrfMeshPlugin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.util.concurrent.ConcurrentHashMap

class NodesOnlineStateManager(
    private val offlineTimeout: Long = 10_000L, // Timeout in milliseconds
) {
    private val nodes = ConcurrentHashMap<Int, Boolean>() // Tracks node states (online/offline)
    private val heartbeatTimestamps = ConcurrentHashMap<Int, Long>() // Tracks last heartbeat timestamps
    private var monitoringJob: Job? = null

    companion object {
        @Volatile
        private var instance: NodesOnlineStateManager? = null

        fun getInstance() =
            instance ?: synchronized(this) {
                instance ?: NodesOnlineStateManager().also { instance = it }
            }
    }

    // Add a new node to the network
    fun addNode(unicastAddress: Int) {
        if (!nodes.containsKey(unicastAddress)) {
            nodes[unicastAddress] = false // Initialize as offline
            heartbeatTimestamps[unicastAddress] = 0L
        }
    }

    // Remove a node from the network
    fun removeNode(unicastAddress: Int) {
        nodes.remove(unicastAddress)
        heartbeatTimestamps.remove(unicastAddress)
    }

    fun clearNodes() {
        nodes.clear()
        heartbeatTimestamps.clear()
    }

    // Record a heartbeat for a node
    fun heartbeatReceived(unicastAddress: Int) {
        if (nodes.containsKey(unicastAddress)) {
            heartbeatTimestamps[unicastAddress] = System.currentTimeMillis()
        } else {
            println("Unknown node: $unicastAddress")
        }
    }

    // Start monitoring node states
    fun startMonitoring() {
        monitoringJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                val currentTime = System.currentTimeMillis()
                var hasStateChanged = false

                nodes.keys.forEach { unicastAddress ->
                    val lastHeartbeat = heartbeatTimestamps[unicastAddress] ?: 0L
                    val isOnline = currentTime - lastHeartbeat < offlineTimeout

                    if (nodes[unicastAddress] != isOnline) {
                        nodes[unicastAddress] = isOnline
                        hasStateChanged = true
                    }
                }

                // Notify the network if any state changed
                if (hasStateChanged) {
                    notifyNetwork() // Send a snapshot of the current node states
                }

                delay(1000) // Check every second
            }
        }
    }

    // Stop monitoring node states
    fun stopMonitoring() {
        monitoringJob?.cancel()
    }


    fun notifyNetwork() {
        val notification = JSObject()
        val states = JSONArray()
        nodes.forEach { nodeState ->
            val state = JSObject()
            state.put("unicastAddress", nodeState.key)
            state.put("isOnline", nodeState.value)
            states.put(state)
        }
        notification.put("nodesStates", states)
        NotificationManager.getInstance().sendNotification(NrfMeshPlugin.MESH_NODE_ONLINE_STATE_EVENT, notification)
    }
}

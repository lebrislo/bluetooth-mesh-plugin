package com.lebrislo.bluetooth.mesh.utils

import android.util.Log
import com.getcapacitor.JSObject
import com.lebrislo.bluetooth.mesh.BluetoothMeshPlugin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.util.concurrent.ConcurrentHashMap

class NodesOnlineStateManager private constructor(
    private val offlineTimeout: Long = 10_000L, // Timeout in milliseconds
) {
    private val tag: String = NodesOnlineStateManager::class.java.simpleName

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
            Log.i(tag, "add node to online state manager: $unicastAddress")
            nodes[unicastAddress] = false // Initialize as offline
            heartbeatTimestamps[unicastAddress] = 0L
        }
    }

    // Remove a node from the network
    fun removeNode(unicastAddress: Int) {
        Log.i(tag, "remove node from online state manager: $unicastAddress")
        nodes.remove(unicastAddress)
        heartbeatTimestamps.remove(unicastAddress)
    }

    fun clearNodes() {
        Log.i(tag, "clear nodes from online state manager")
        nodes.clear()
        heartbeatTimestamps.clear()
    }

    fun resetStatus() {
        nodes.keys.forEach { unicastAddress ->
            nodes[unicastAddress] = false
        }
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

    fun getNodesOnlineStates(): JSObject {
        val result = JSObject()
        val states = JSONArray()
        nodes.forEach { nodeState ->
            val state = JSObject()
            state.put("unicastAddress", nodeState.key)
            state.put("isOnline", nodeState.value)
            states.put(state)
        }
        result.put("states", states)

        return result
    }

    fun notifyNetwork() {
        val notification = getNodesOnlineStates()
        NotificationManager.getInstance()
            .sendNotification(BluetoothMeshPlugin.MESH_NODE_ONLINE_STATE_EVENT, notification)
    }
}

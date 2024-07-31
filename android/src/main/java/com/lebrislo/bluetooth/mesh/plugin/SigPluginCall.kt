package com.lebrislo.bluetooth.mesh.plugin

import com.getcapacitor.PluginCall

class SigPluginCall : BasePluginCall {
    constructor(call: PluginCall, meshOperation: Int, meshAddress: Int) : super(call) {
    }
}
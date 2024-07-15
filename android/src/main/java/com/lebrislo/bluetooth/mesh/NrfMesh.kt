package com.lebrislo.bluetooth.mesh

import android.util.Log

class NrfMesh {
    fun echo(value: String): String {
        Log.i("Echo", value)
        return value
    }
}

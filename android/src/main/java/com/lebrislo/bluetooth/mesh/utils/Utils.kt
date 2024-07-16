package com.lebrislo.bluetooth.mesh.utils

import android.os.Build
import android.os.ParcelUuid
import no.nordicsemi.android.support.v18.scanner.ScanRecord
import no.nordicsemi.android.support.v18.scanner.ScanResult
import java.util.UUID


class Utils {
    companion object {
        fun getServiceData(result: ScanResult, serviceUuid: UUID): ByteArray? {
            val scanRecord: ScanRecord? = result.scanRecord
            if (scanRecord != null) {
                return scanRecord.getServiceData(ParcelUuid((serviceUuid)))
            }
            return null
        }

        fun isWithinMarshmallowAndR(): Boolean {
            return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Build.VERSION.SDK_INT <= Build.VERSION_CODES.R
        }
    }
}
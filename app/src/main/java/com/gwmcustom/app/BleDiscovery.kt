package com.gwmcustom.app

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.location.LocationManagerCompat

data class NearbyDevice(val name: String?, val signal: Int) {
    val thinkDiag: Boolean
        get() = name.orEmpty().replace(" ", "").contains("thinkdiag", ignoreCase = true)
}

class BleDiscovery(private val context: Context) {
    private val handler = Handler(Looper.getMainLooper())
    private val seen = mutableMapOf<String, NearbyDevice>()
    private var scanner: BluetoothLeScanner? = null
    private var callback: ScanCallback? = null
    private val timeout = Runnable { stop() }

    var devices by mutableStateOf(emptyList<NearbyDevice>())
        private set
    var scanning by mutableStateOf(false)
        private set
    var status by mutableStateOf("Scan for nearby Bluetooth LE advertisements.")
        private set

    val permissions: Array<String>
        get() = if (Build.VERSION.SDK_INT >= 31) {
            arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }

    fun hasPermissions() = permissions.all {
        context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED
    }

    fun permissionDenied() {
        status = "Allow the requested permission in Android app settings to scan."
    }

    @SuppressLint("MissingPermission")
    fun start() {
        if (scanning) return
        if (!hasPermissions()) {
            permissionDenied()
            return
        }

        try {
            val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter
            if (adapter == null ||
                !context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)
            ) {
                status = "Bluetooth LE is unavailable on this device."
                return
            }
            if (!adapter.isEnabled) {
                status = "Turn on Bluetooth in Android settings and scan again."
                return
            }
            if (Build.VERSION.SDK_INT < 31 &&
                context.getSystemService(LocationManager::class.java)
                    ?.let { !LocationManagerCompat.isLocationEnabled(it) } == true
            ) {
                status = "Turn on Location in Android settings and scan again."
                return
            }
            val activeScanner = adapter.bluetoothLeScanner
            if (activeScanner == null) {
                status = "The Bluetooth scanner is unavailable. Try again."
                return
            }

            val activeCallback = object : ScanCallback() {
                override fun onScanResult(callbackType: Int, result: ScanResult) {
                    handler.post {
                        if (callback === this) addResult(result)
                    }
                }

                override fun onScanFailed(errorCode: Int) {
                    handler.post {
                        if (callback === this) stop("Bluetooth scan failed (code $errorCode). Try again.")
                    }
                }
            }
            seen.clear()
            devices = emptyList()
            scanner = activeScanner
            callback = activeCallback
            scanning = true
            status = "Scanning for up to 12 seconds…"
            activeScanner.startScan(
                emptyList(),
                ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build(),
                activeCallback
            )
            handler.postDelayed(timeout, 12_000)
        } catch (_: SecurityException) {
            stop("Bluetooth permission is unavailable. Check Android app settings.")
        } catch (_: IllegalStateException) {
            stop("Bluetooth became unavailable. Try again.")
        }
    }

    private fun addResult(result: ScanResult) {
        val address = result.device.address
        val name = result.scanRecord?.deviceName?.takeIf { it.isNotBlank() }
            ?: seen[address]?.name
        seen[address] = NearbyDevice(name, result.rssi)
        devices = seen.values.sortedWith(
            compareByDescending<NearbyDevice> { it.thinkDiag }.thenByDescending { it.signal }
        )
    }

    @SuppressLint("MissingPermission")
    fun stop(message: String? = null) {
        handler.removeCallbacks(timeout)
        val wasScanning = scanning
        val activeCallback = callback
        callback = null
        if (activeCallback != null) {
            try {
                scanner?.stopScan(activeCallback)
            } catch (_: SecurityException) {
                // Android can revoke scan permission while the app is running.
            } catch (_: IllegalStateException) {
                // Bluetooth can be turned off during a scan.
            }
        }
        scanner = null
        scanning = false
        if (message != null) status = message
        else if (wasScanning) status = if (devices.isEmpty()) {
            "Scan ended. No Bluetooth LE advertisements found."
        } else {
            "Scan ended. ${devices.size} nearby device(s) found."
        }
    }

    fun clear() {
        stop()
        seen.clear()
        devices = emptyList()
        status = "Scan for nearby Bluetooth LE advertisements."
    }
}

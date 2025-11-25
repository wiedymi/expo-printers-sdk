package expo.modules.printers.rongta.network

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import com.rt.printerlibrary.ipscan.IpScanner
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class RongtaNetworkScanner(
    private val context: Context
) {
    private var multicastLock: WifiManager.MulticastLock? = null

    fun scan(): Flow<IpScanner.DeviceBean> {
        return callbackFlow {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager

            // Acquire multicast lock for UDP broadcast (only during scan)
            synchronized(this@RongtaNetworkScanner) {
                if (multicastLock == null) {
                    multicastLock = wifiManager?.createMulticastLock("RongtaNetworkScan")?.apply {
                        setReferenceCounted(false)
                    }
                }

                if (multicastLock?.isHeld == false) {
                    multicastLock?.acquire()
                    Log.d(TAG, "Multicast lock acquired")
                }
            }

            val scanner = RongtaIpScanner(
                object : RongtaIpScannerCallbacks {
                    override fun onSearchStart() {
                        Log.d(TAG, "Rongta network scan started")
                    }

                    override fun onSearchFinish(devices: List<IpScanner.DeviceBean>) {
                        Log.d(TAG, "Rongta network scan finished, found ${devices.size} devices")
                        devices.forEach { device ->
                            Log.d(TAG, "Found device: IP=${device.deviceIp}, Port=${device.devicePort}, MAC=${device.macAddress}")
                            trySend(device)
                        }
                        // Release lock immediately after scan completes
                        releaseLock()
                        close()
                    }

                    override fun onSearchError(message: String) {
                        Log.e(TAG, "Rongta network scan error: $message")
                        releaseLock()
                        close(IllegalStateException(message))
                    }
                }
            )

            // Start the scanner thread
            scanner.start()

            awaitClose {
                runCatching {
                    scanner.interrupt()
                    scanner.join(1000) // Wait up to 1 second for thread to finish
                }
                releaseLock()
            }
        }
    }

    private fun releaseLock() {
        synchronized(this) {
            if (multicastLock?.isHeld == true) {
                multicastLock?.release()
                Log.d(TAG, "Multicast lock released")
            }
        }
    }

    companion object {
        private const val TAG = "RongtaNetworkScanner"
    }

    private class RongtaIpScanner(
        private val callbacks: RongtaIpScannerCallbacks,
    ) : IpScanner() {

        override fun onSearchStart() {
            callbacks.onSearchStart()
        }

        override fun onSearchFinish(p0: List<*>?) {
            val devices = p0?.mapNotNull { it as? DeviceBean } ?: emptyList()
            callbacks.onSearchFinish(devices)
        }

        override fun onSearchError(p0: String?) {
            callbacks.onSearchError(p0 ?: "Unknown error")
        }
    }

    private interface RongtaIpScannerCallbacks {
        fun onSearchStart()
        fun onSearchFinish(devices: List<IpScanner.DeviceBean>)
        fun onSearchError(message: String)
    }
}
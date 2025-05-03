package expo.modules.printers.rongta.network

import com.rt.printerlibrary.ipscan.IpScanner
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class RongtaNetworkScanner {

    fun scan(): Flow<IpScanner.DeviceBean> {
        return callbackFlow {
            val scanner = RongtaIpScanner(
                object : RongtaIpScannerCallbacks {
                    override fun onSearchStart() {
                        // Handle search start
                    }

                    override fun onSearchFinish(devices: List<IpScanner.DeviceBean>) {
                        devices.forEach { device ->
                            trySend(device)
                        }
                    }

                    override fun onSearchError(message: String) {
                        close(IllegalStateException(message))
                    }
                }
            )

            awaitClose {
                runCatching {
                    scanner.interrupt()
                }
            }
        }
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
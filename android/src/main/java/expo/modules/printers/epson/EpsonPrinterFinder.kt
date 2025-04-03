package expo.modules.printers.epson

import android.content.Context
import android.util.Log
import expo.modules.printers.commons.PrinterConnectionType
import expo.modules.printers.commons.PrinterDeviceData
import expo.modules.printers.commons.PrinterFinder
import com.epson.epos2.discovery.Discovery
import com.epson.epos2.discovery.FilterOption
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.toSet
import kotlin.time.Duration.Companion.seconds

class EpsonPrinterFinder(
    private val appContext: Context,
) : PrinterFinder<PrinterDeviceData.EPSON> {

    override suspend fun search(connectionType: PrinterConnectionType): List<PrinterDeviceData.EPSON> =
        coroutineScope {
            searchPrinters(connectionType).toSet().toList()
        }

    private fun searchPrinters(connectionType: PrinterConnectionType): Flow<PrinterDeviceData.EPSON> {
        return callbackFlow {
            Log.i(TAG, "searching for printers with connection type: $connectionType")
            runCatching {
                Discovery.start(appContext, connectionType.toFilterOption()) { deviceInfo ->
                    val deviceData = PrinterDeviceData.EPSON(
                        connectionType = connectionType,
                        deviceType = deviceInfo.deviceType,
                        target = deviceInfo.target,
                        deviceName = deviceInfo.deviceName,
                        ipAddress = deviceInfo.ipAddress,
                        macAddress = deviceInfo.macAddress,
                        bdAddress = deviceInfo.bdAddress,
                    )
                    trySend(deviceData)
                }

                delay(30.seconds) // Wait for a while to receive devices
                close()
            }.onFailure { throwable ->
                Log.e(TAG, "Failed to start discovery: $throwable")
            }

            awaitClose {
                Log.i(TAG, "Stopping discovery")
                runCatching {
                    Discovery.stop()
                }.onFailure { throwable ->
                    Log.e(TAG, "Failed to stop discovery: $throwable")
                }
            }
        }
    }

    private fun PrinterConnectionType.toFilterOption(): FilterOption {
        val discoveryPortType = when (this) {
            PrinterConnectionType.Bluetooth -> Discovery.PORTTYPE_BLUETOOTH
            PrinterConnectionType.Network -> Discovery.PORTTYPE_TCP
            PrinterConnectionType.USB -> Discovery.PORTTYPE_USB
        }
        return FilterOption()
            .apply {
                portType = discoveryPortType
                // Docs: Hybrid model printers can be detected as deviceType of Discovery.TYPE_PRINTER and Discovery.TYPE_HYBRID_PRINTER.
                deviceType = Discovery.TYPE_PRINTER
                epsonFilter = Discovery.FILTER_NAME
                usbDeviceName = Discovery.TRUE
            }
    }

    companion object {
        private const val TAG = "EpsonPrinterFinder"
    }
}
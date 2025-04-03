package expo.modules.printers.starmicronics

import android.content.Context
import android.util.Log
import expo.modules.printers.commons.PrinterConnectionType
import expo.modules.printers.commons.PrinterDeviceData
import expo.modules.printers.commons.PrinterFinder
import com.starmicronics.stario.StarIOPort
import com.starmicronics.stario.StarIOPortException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration.Companion.seconds

class StarMicronicsFinder(
    private val appContext: Context
) : PrinterFinder<PrinterDeviceData.Star> {

    override suspend fun search(connectionType: PrinterConnectionType): List<PrinterDeviceData.Star> =
        coroutineScope {
            runCatching {
                withTimeout(30.seconds) {
                    StarIOPort.searchPrinter(connectionType.toStarInterfaceType(), appContext)
                        .map { portInfo ->
                            PrinterDeviceData.Star(
                                connectionType = connectionType,
                                modelName = portInfo.modelName.orEmpty(),
                                portName = portInfo.portName.orEmpty(),
                                macAddress = portInfo.macAddress.orEmpty(),
                                usbSerialNumber = portInfo.usbSerialNumber.orEmpty(),
                            )
                        }
                }
            }.onFailure { e ->
                when (e) {
                    is StarIOPortException -> Log.e(TAG, "Failed to search for printers: ${e.message}")
                    else -> Log.e(TAG, "Error during printer search", e)
                }
            }.getOrDefault(emptyList())
        }

    private fun PrinterConnectionType.toStarInterfaceType(): String {
        return when (this) {
            PrinterConnectionType.Bluetooth -> "BT:"
            PrinterConnectionType.Network -> "TCP:"
            PrinterConnectionType.USB -> "USB:"
        }
    }

    companion object {
        private const val TAG = "StarMicronicsFinder"
    }
}
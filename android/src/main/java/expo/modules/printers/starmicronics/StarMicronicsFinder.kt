package expo.modules.printers.starmicronics

import android.content.Context
import expo.modules.printers.commons.PrinterConnectionType
import expo.modules.printers.commons.PrinterDeviceData
import expo.modules.printers.commons.PrinterFinder
import com.starmicronics.stario.StarIOPort
import kotlinx.coroutines.coroutineScope

class StarMicronicsFinder(
    private val appContext: Context
) : PrinterFinder<PrinterDeviceData.Star> {

    override suspend fun search(connectionType: PrinterConnectionType): List<PrinterDeviceData.Star> =
        coroutineScope {
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

    private fun PrinterConnectionType.toStarInterfaceType(): String {
        return when (this) {
            PrinterConnectionType.Bluetooth -> "BT:"
            PrinterConnectionType.Network -> "TCP:"
            PrinterConnectionType.USB -> "USB:"
        }
    }
}
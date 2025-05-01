package expo.modules.printers.rongta

import android.Manifest
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresPermission
import expo.modules.printers.commons.PrinterConnectionType
import expo.modules.printers.commons.PrinterDeviceData
import expo.modules.printers.commons.PrinterFinder
import expo.modules.printers.rongta.bluetooth.RongtaBluetoothScanner
import expo.modules.printers.rongta.network.RongtaNetworkScanner
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.toSet

class RongtaFinder(
    appContext: Context
) : PrinterFinder<PrinterDeviceData.Rongta> {

    private val bluetoothScanner = RongtaBluetoothScanner(appContext)
    private val networkScanner = RongtaNetworkScanner()

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override suspend fun search(connectionType: PrinterConnectionType): List<PrinterDeviceData.Rongta> =
        coroutineScope {
            when (connectionType) {
                PrinterConnectionType.Bluetooth -> searchBlueToothPrinters()
                PrinterConnectionType.Network -> searchNetworkPrinters()
                PrinterConnectionType.USB -> emptyList() // TODO: Implement USB scanning
            }
        }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private suspend fun searchBlueToothPrinters(): List<PrinterDeviceData.Rongta> {
        Log.i(TAG, "Rongta bluetooth scanning...")
        return runCatching {
            bluetoothScanner.scan().toSet().toList()
                .map { btDevice ->
                    val alias = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        btDevice.alias.orEmpty().ifEmpty { btDevice.name.orEmpty() }
                    } else {
                        btDevice.name.orEmpty()
                    }
                    PrinterDeviceData.Rongta(
                        connectionType = PrinterConnectionType.Bluetooth,
                        type = PrinterDeviceData.Rongta.Type.Bluetooth(
                            alias = alias,
                            name = btDevice.name.orEmpty(),
                            address = btDevice.address.orEmpty(),
                        ),
                    )
                }
        }.onFailure { throwable ->
            Log.e(TAG, "Rongta bluetooth scanning failed", throwable)
        }
            .getOrDefault(emptyList())
    }

    private suspend fun searchNetworkPrinters(): List<PrinterDeviceData.Rongta> {
        return runCatching {
            networkScanner.scan().toSet().toList()
                .map { device ->
                    PrinterDeviceData.Rongta(
                        connectionType = PrinterConnectionType.Network,
                        type = PrinterDeviceData.Rongta.Type.Network(
                            ipAddress = device.deviceIp,
                            port = device.devicePort,
                        )
                    )
                 }
        }.onFailure { throwable ->
            Log.e(TAG, "Rongta network scanning failed", throwable)
        }
        .getOrDefault(emptyList())
    }

    companion object {
        private const val TAG = "RongtaFinder"
    }
}
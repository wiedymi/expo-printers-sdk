package expo.modules.printers.rongta

import android.Manifest
import android.content.Context
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresPermission
import expo.modules.printers.commons.PrinterConnectionType
import expo.modules.printers.commons.PrinterDeviceData
import expo.modules.printers.commons.PrinterFinder
import expo.modules.printers.rongta.bluetooth.RongtaBluetoothScanner
import expo.modules.printers.rongta.network.RongtaNetworkScanner
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.toSet

class RongtaFinder(
    private val appContext: Context
) : PrinterFinder<PrinterDeviceData.Rongta> {

    private val bluetoothScanner = RongtaBluetoothScanner(appContext)
    private val networkScanner = RongtaNetworkScanner(appContext)

    @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN])
    override suspend fun search(connectionType: PrinterConnectionType): List<PrinterDeviceData.Rongta> =
        coroutineScope {
            when (connectionType) {
                PrinterConnectionType.Bluetooth -> searchBlueToothPrinters()
                PrinterConnectionType.Network -> searchNetworkPrinters()
                PrinterConnectionType.USB -> searchUsbPrinters()
            }
        }

    @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN])
    private suspend fun searchBlueToothPrinters(): List<PrinterDeviceData.Rongta> {
        Log.i(TAG, "Rongta bluetooth scanning...")
        return runCatching {
            val classicFlow = bluetoothScanner.scan()
            classicFlow.toList().distinctBy { it.address }
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
        Log.i(TAG, "Rongta network scanning...")
        return runCatching {
            networkScanner.scan().toSet().toList()
                .map { device ->
                    Log.d(TAG, "Network printer found: ${device.deviceIp}:${device.devicePort}")
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

    private fun searchUsbPrinters(): List<PrinterDeviceData.Rongta> {
        Log.i(TAG, "Rongta USB scanning...")
        return runCatching {
            val usbManager = appContext.getSystemService(Context.USB_SERVICE) as UsbManager
            usbManager.deviceList.values
                .filter { device ->
                    (0 until device.interfaceCount).any { i ->
                        device.getInterface(i).interfaceClass == UsbConstants.USB_CLASS_PRINTER
                    }
                }
                .map { device ->
                    PrinterDeviceData.Rongta(
                        connectionType = PrinterConnectionType.USB,
                        type = PrinterDeviceData.Rongta.Type.Usb(
                            name = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                device.productName ?: device.deviceName
                            } else {
                                device.deviceName
                            },
                            vendorId = device.vendorId,
                            productId = device.productId
                        )
                    )
                }
        }.onFailure { throwable ->
            Log.e(TAG, "Rongta USB scanning failed", throwable)
        }.getOrDefault(emptyList())
    }

    companion object {
        private const val TAG = "RongtaFinder"
    }
}
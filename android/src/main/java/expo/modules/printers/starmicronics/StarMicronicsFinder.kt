package expo.modules.printers.starmicronics

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
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
                            var modelName = portInfo.modelName.orEmpty()
                            val macAddress = portInfo.macAddress.orEmpty()

                            // For Bluetooth, if modelName is empty, try multiple fallback methods
                            if (modelName.isEmpty() && connectionType == PrinterConnectionType.Bluetooth) {
                                // Method 1: Try to extract from portName
                                // Bluetooth portName format: "BT:TSP100-C2648" or "BT:00:12:3f:XX:XX:XX"
                                val portName = portInfo.portName.orEmpty()
                                if (portName.startsWith("BT:")) {
                                    val btDeviceName = portName.substring(3)
                                    // Check if it looks like a model name (not a MAC address)
                                    if (!btDeviceName.matches(Regex("[0-9A-Fa-f]{2}(:[0-9A-Fa-f]{2}){5}"))) {
                                        modelName = btDeviceName
                                        Log.d(TAG, "Extracted Bluetooth model name from portName: $modelName")
                                    }
                                }

                                // Method 2: Query Bluetooth device name from MAC address if still empty
                                if (modelName.isEmpty() && macAddress.isNotEmpty()) {
                                    modelName = queryBluetoothDeviceName(macAddress)
                                    if (modelName.isNotEmpty()) {
                                        Log.d(TAG, "Queried Bluetooth device name from MAC $macAddress: $modelName")
                                    }
                                }
                            }

                            // Check if printer model is supported
                            val isSupported = modelName.isNotEmpty() &&
                                (StarModelCapability.getModelIdx(modelName) != StarModelCapability.NONE ||
                                 StarModelCapability.getModelIdxByTitle(modelName) != StarModelCapability.NONE)

                            val unsupportedReason = if (!isSupported) {
                                when {
                                    modelName.isEmpty() -> "Unable to identify printer model"
                                    else -> "Printer model '$modelName' is not supported. Supported models: ${StarModelCapability.getSupportedModels().joinToString(", ")}"
                                }
                            } else {
                                null
                            }

                            PrinterDeviceData.Star(
                                connectionType = connectionType,
                                modelName = modelName,
                                portName = portInfo.portName.orEmpty(),
                                macAddress = macAddress,
                                usbSerialNumber = portInfo.usbSerialNumber.orEmpty(),
                                isSupported = isSupported,
                                unsupportedReason = unsupportedReason,
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

    private fun queryBluetoothDeviceName(macAddress: String): String {
        return runCatching {
            val bluetoothAdapter = runCatching {
                appContext.getSystemService(BluetoothManager::class.java)?.adapter
            }.getOrNull() ?: return@runCatching ""

            val device = bluetoothAdapter.getRemoteDevice(macAddress)
            device.name.orEmpty()
        }.onFailure { e ->
            Log.d(TAG, "Failed to query Bluetooth device name for MAC $macAddress: ${e.message}")
        }.getOrDefault("")
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
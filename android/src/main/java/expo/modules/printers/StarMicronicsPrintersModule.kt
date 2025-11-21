package expo.modules.printers

import android.content.Context
import android.util.Log
import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition
import expo.modules.printers.commons.NetworkValidator
import expo.modules.printers.commons.PrinterConnectionType
import expo.modules.printers.commons.PrinterDeviceData
import expo.modules.printers.commons.safeGetString
import expo.modules.printers.commons.safeGetStringOrDefault
import expo.modules.printers.commons.toPrinterConnectionType
import expo.modules.printers.starmicronics.StarMicronicsPrinter
import expo.modules.printers.starmicronics.StarMicronicsFinder
import expo.modules.printers.starmicronics.StarMicronicsPrintResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StarMicronicsPrintersModule : Module() {
    private val TAG = "StarMicronicsPrintersModule"
    private var printer: StarMicronicsPrinter? = null
    private var printerFinder: StarMicronicsFinder? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    private val context: Context
        get() = requireNotNull(appContext.reactContext)

    override fun definition() = ModuleDefinition {
        Name("StarMicronicsPrintersModule")

        Events("onPrintersFound", "onPrintImage")

        OnCreate {
            printer = StarMicronicsPrinter(context)
            printerFinder = StarMicronicsFinder(context)
        }

        OnDestroy {
            printer = null
            printerFinder = null
        }

        AsyncFunction("connectManually") { ipAddress: String, port: Int? ->
            val printerPort = port ?: 9100

            when (val validation = NetworkValidator.validateNetworkConnection(ipAddress, printerPort)) {
                is NetworkValidator.ValidationResult.Error -> {
                    Log.e(TAG, "Invalid network connection parameters: ${validation.message}")
                    throw IllegalArgumentException(validation.message)
                }
                NetworkValidator.ValidationResult.Valid -> {
                    // Star SDK uses TCP:IP:PORT format for network printers
                    val portName = "TCP:$ipAddress:$printerPort"
                    mapOf(
                        "deviceName" to "Manual Connection",
                        "portName" to portName,
                        "macAddress" to "",
                        "usbSerialNumber" to "",
                        "connectionType" to "Network"
                    )
                }
            }
        }

        AsyncFunction("findPrinters") { connectionType: String ->
            runCatching {
                val type = try {
                    connectionType.toPrinterConnectionType()
                } catch (e: IllegalArgumentException) {
                    Log.e(TAG, "Invalid connection type: $connectionType", e)
                    return@AsyncFunction false
                }
                coroutineScope.launch(Dispatchers.IO) {
                    val printers = printerFinder?.search(type)?.map { deviceData ->
                        mapOf(
                            "deviceName" to deviceData.modelName,
                            "portName" to deviceData.portName,
                            "macAddress" to deviceData.macAddress,
                            "usbSerialNumber" to deviceData.usbSerialNumber,
                            "connectionType" to deviceData.connectionType.name
                        )
                    } ?: emptyList()

                    sendEvent("onPrintersFound", mapOf("printers" to printers))
                }
                true
            }.onFailure { e ->
                Log.e(TAG, "Failed to find printers", e)
            }.getOrElse { false }
        }

        AsyncFunction("printImage") { base64Image: String, deviceData: Map<String, Any> ->
            if (printer == null) {
                return@AsyncFunction false
            }

            runCatching {
                // Safe data extraction with utility functions
                val connectionType = deviceData.safeGetString("connectionType").toPrinterConnectionType()

                val starDeviceData = PrinterDeviceData.Star(
                    connectionType = connectionType,
                    modelName = deviceData.safeGetStringOrDefault("deviceName"),
                    portName = deviceData.safeGetStringOrDefault("portName"),
                    macAddress = deviceData.safeGetStringOrDefault("macAddress"),
                    usbSerialNumber = deviceData.safeGetStringOrDefault("usbSerialNumber")
                )

                coroutineScope.launch(Dispatchers.IO) {
                    val result = printer?.printImage(base64Image, starDeviceData)
                    val success = result == StarMicronicsPrintResult.Success
                    
                    sendEvent("onPrintImage", mapOf(
                        "success" to success,
                        "error" to when (result) {
                            StarMicronicsPrintResult.ErrorInvalidImage -> "Invalid image data"
                            StarMicronicsPrintResult.ErrorOpenPort -> "Failed to open printer port"
                            StarMicronicsPrintResult.ErrorPrinterOffline -> "Printer is offline"
                            StarMicronicsPrintResult.ErrorCoverOpened -> "Printer cover is open"
                            StarMicronicsPrintResult.ErrorPaperEmpty -> "Printer is out of paper"
                            StarMicronicsPrintResult.ErrorPaperJam -> "Paper jam"
                            StarMicronicsPrintResult.ErrorUnknown -> "Unknown error occurred"
                            StarMicronicsPrintResult.Success -> null
                            null -> "Printer not initialized"
                        }
                    ))
                }
                true
            }.onFailure { e ->
                Log.e(TAG, "Print failed", e)
                sendEvent("onPrintImage", mapOf(
                    "success" to false,
                    "error" to (e.localizedMessage ?: "Unknown error during printing")
                ))
            }.getOrElse { false }
        }
    }
} 
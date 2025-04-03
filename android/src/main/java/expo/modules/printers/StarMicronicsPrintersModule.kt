package expo.modules.printers

import android.content.Context
import android.util.Log
import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition
import expo.modules.printers.commons.PrinterConnectionType
import expo.modules.printers.commons.PrinterDeviceData
import expo.modules.printers.starmicronics.StarMicronicsPinter
import expo.modules.printers.starmicronics.StarMicronicsFinder
import expo.modules.printers.starmicronics.StarMicronicsPrintResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StarMicronicsPrintersModule : Module() {
    private val TAG = "StarMicronicsPrintersModule"
    private var printer: StarMicronicsPinter? = null
    private var printerFinder: StarMicronicsFinder? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    private val context: Context
        get() = requireNotNull(appContext.reactContext)

    override fun definition() = ModuleDefinition {
        Name("StarMicronicsPrintersModule")

        Events("onPrintersFound", "onPrintImage")

        OnCreate {
            printer = StarMicronicsPinter(context)
            printerFinder = StarMicronicsFinder(context)
        }

        OnDestroy {
            printer = null
            printerFinder = null
        }

        AsyncFunction("findPrinters") { connectionType: String ->
            if (context == null) {
                return@AsyncFunction emptyList<Map<String, Any>>()
            }

            runCatching {
                val type = PrinterConnectionType.valueOf(connectionType)
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
                false
            }.getOrNull() ?: false
        }

        AsyncFunction("printImage") { base64Image: String, deviceData: Map<String, Any> ->
            if (printer == null) {
                return@AsyncFunction false
            }

            runCatching {
                val starDeviceData = PrinterDeviceData.Star(
                    connectionType = PrinterConnectionType.valueOf(deviceData["connectionType"] as String),
                    modelName = deviceData["deviceName"] as String,
                    portName = deviceData["portName"] as String,
                    macAddress = deviceData["macAddress"] as String,
                    usbSerialNumber = deviceData["usbSerialNumber"] as String,
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
                false
            }.getOrNull() ?: false
        }
    }
} 
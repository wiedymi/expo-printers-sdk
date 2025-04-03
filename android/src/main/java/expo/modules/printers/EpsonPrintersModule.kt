package expo.modules.printers

import android.content.Context
import android.util.Log
import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition
import expo.modules.printers.commons.PrinterConnectionType
import expo.modules.printers.commons.PrinterDeviceData
import expo.modules.printers.epson.EpsonPrinter
import expo.modules.printers.epson.EpsonPrinterFinder
import expo.modules.printers.epson.EpsonPrintResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EpsonPrintersModule : Module() {
    private val TAG = "EpsonPrintersModule"
    private var printer: EpsonPrinter? = null
    private var printerFinder: EpsonPrinterFinder? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    private val context: Context
        get() = requireNotNull(appContext.reactContext)

    override fun definition() = ModuleDefinition {
        Name("EpsonPrintersModule")

        Events("onPrintersFound", "onPrintImage")

        OnCreate {
            printer = EpsonPrinter(context)
            printerFinder = EpsonPrinterFinder(context)
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
                            "deviceName" to deviceData.deviceName,
                            "target" to deviceData.target,
                            "ip" to deviceData.ipAddress,
                            "mac" to deviceData.macAddress,
                            "bdAddress" to deviceData.bdAddress,
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
                val epsonDeviceData = PrinterDeviceData.EPSON(
                    connectionType = PrinterConnectionType.valueOf(deviceData["connectionType"] as String),
                    deviceType = deviceData["deviceType"] as Int,
                    target = deviceData["target"] as String,
                    deviceName = deviceData["deviceName"] as String,
                    ipAddress = deviceData["ipAddress"] as String,
                    macAddress = deviceData["macAddress"] as String,
                    bdAddress = deviceData["bdAddress"] as String
                )

                coroutineScope.launch(Dispatchers.IO) {
                    val result = printer?.printImage(base64Image, epsonDeviceData)
                    val success = result == EpsonPrintResult.Success
                    
                    sendEvent("onPrintImage", mapOf(
                        "success" to success,
                        "error" to when (result) {
                            EpsonPrintResult.ErrorInvalidImage -> "Invalid image data"
                            EpsonPrintResult.ErrorPrinterOffline -> "Printer is offline"
                            EpsonPrintResult.ErrorUnknown -> "Unknown error occurred"
                            EpsonPrintResult.Success -> null
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
                         
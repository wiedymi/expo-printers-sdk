package expo.modules.printers

import android.content.Context
import android.util.Log
import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition
import expo.modules.printers.commons.PrinterConnectionType
import expo.modules.printers.commons.PrinterDeviceData
import expo.modules.printers.commons.safeGetInt
import expo.modules.printers.commons.safeGetString
import expo.modules.printers.commons.safeGetStringOrDefault
import expo.modules.printers.commons.toPrinterConnectionType
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

        AsyncFunction("connectManually") { ipAddress: String, port: Int? ->
            runCatching {
                val target = "TCP:$ipAddress"
                mapOf(
                    "deviceName" to "Manual Connection",
                    "target" to target,
                    "ipAddress" to ipAddress,
                    "macAddress" to "",
                    "bdAddress" to "",
                    "connectionType" to "Network",
                    "deviceType" to 0
                )
            }.onFailure { e ->
                Log.e(TAG, "Failed to create manual connection", e)
            }.getOrNull()
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
                            "deviceName" to deviceData.deviceName,
                            "target" to deviceData.target,
                            "ipAddress" to deviceData.ipAddress,
                            "macAddress" to deviceData.macAddress,
                            "bdAddress" to deviceData.bdAddress,
                            "connectionType" to deviceData.connectionType.name,
                            "deviceType" to deviceData.deviceType
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
                val deviceType = deviceData.safeGetInt("deviceType")

                val epsonDeviceData = PrinterDeviceData.EPSON(
                    connectionType = connectionType,
                    deviceType = deviceType,
                    target = deviceData.safeGetStringOrDefault("target"),
                    deviceName = deviceData.safeGetStringOrDefault("deviceName"),
                    ipAddress = deviceData.safeGetStringOrDefault("ipAddress"),
                    macAddress = deviceData.safeGetStringOrDefault("macAddress"),
                    bdAddress = deviceData.safeGetStringOrDefault("bdAddress")
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
            }.getOrElse { false }
        }
    }
}

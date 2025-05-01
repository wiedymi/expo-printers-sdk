package expo.modules.printers

import android.content.Context
import android.util.Log
import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition
import expo.modules.printers.commons.PrinterConnectionType
import expo.modules.printers.commons.PrinterDeviceData
import expo.modules.printers.rongta.RongtaPrinter
import expo.modules.printers.rongta.RongtaFinder
import expo.modules.printers.rongta.RongtaPrintResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RongtaPrintersModule : Module() {
    private val TAG = "RongtaPrintersModule"
    private var printer: RongtaPrinter? = null
    private var printerFinder: RongtaFinder? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    private val context: Context
        get() = requireNotNull(appContext.reactContext)

    override fun definition() = ModuleDefinition {
        Name("RongtaPrintersModule")

        Events("onPrintersFound", "onPrintImage")

        OnCreate {
            printer = RongtaPrinter(context)
            printerFinder = RongtaFinder(context)
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
                        when (deviceData.type) {
                            is PrinterDeviceData.Rongta.Type.Bluetooth -> mapOf(
                                "connectionType" to deviceData.connectionType.name,
                                "type" to mapOf(
                                    "type" to "BLUETOOTH",
                                    "alias" to deviceData.type.alias,
                                    "name" to deviceData.type.name,
                                    "address" to deviceData.type.address
                                )
                            )
                            is PrinterDeviceData.Rongta.Type.Network -> mapOf(
                                "connectionType" to deviceData.connectionType.name,
                                "type" to mapOf(
                                    "type" to "NETWORK",
                                    "ipAddress" to deviceData.type.ipAddress,
                                    "port" to deviceData.type.port
                                )
                            )
                        }
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
                val connectionType = PrinterConnectionType.valueOf(deviceData["connectionType"] as String)
                val rongtaDeviceData = when (connectionType) {
                    PrinterConnectionType.Bluetooth -> PrinterDeviceData.Rongta(
                        connectionType = connectionType,
                        type = PrinterDeviceData.Rongta.Type.Bluetooth(
                            alias = deviceData["alias"] as String,
                            name = deviceData["deviceName"] as String,
                            address = deviceData["address"] as String
                        )
                    )
                    PrinterConnectionType.Network -> PrinterDeviceData.Rongta(
                        connectionType = connectionType,
                        type = PrinterDeviceData.Rongta.Type.Network(
                            ipAddress = deviceData["address"] as String,
                            port = (deviceData["port"] as? Number)?.toInt() ?: 9100
                        )
                    )
                    else -> throw IllegalArgumentException("Unsupported connection type: $connectionType")
                }

                coroutineScope.launch(Dispatchers.IO) {
                    val result = printer?.printImage(base64Image, rongtaDeviceData)
                    val success = result == RongtaPrintResult.Success
                    
                    sendEvent("onPrintImage", mapOf(
                        "success" to success,
                        "error" to when (result) {
                            RongtaPrintResult.ErrorInvalidImage -> "Invalid image data"
                            RongtaPrintResult.ErrorConnection -> "Failed to connect to printer"
                            RongtaPrintResult.ErrorUnknown -> "Unknown error occurred"
                            RongtaPrintResult.Success -> null
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
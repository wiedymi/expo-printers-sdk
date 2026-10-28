package expo.modules.printers

import android.content.Context
import expo.modules.printers.commons.PrinterLog
import expo.modules.kotlin.functions.Coroutine
import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition
import expo.modules.printers.commons.NetworkValidator
import expo.modules.printers.commons.PrintOptions
import expo.modules.printers.commons.printNotStartedEvent
import expo.modules.printers.commons.toPrintImageEvent
import expo.modules.printers.commons.PrinterConnectionType
import expo.modules.printers.commons.PrinterDeviceData
import expo.modules.printers.commons.safeGetIntOrDefault
import expo.modules.printers.commons.safeGetMap
import expo.modules.printers.commons.safeGetString
import expo.modules.printers.commons.toPrinterConnectionType
import expo.modules.printers.rongta.RongtaPrinter
import expo.modules.printers.rongta.RongtaFinder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RongtaPrintersModule : Module() {
    private val TAG = "RongtaPrintersModule"
    private val logSink = PrinterLog.Sink("Rongta") { entry -> sendEvent("onLog", entry) }
    private var printer: RongtaPrinter? = null
    private var printerFinder: RongtaFinder? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    private val context: Context
        get() = requireNotNull(appContext.reactContext)

    override fun definition() = ModuleDefinition {
        Name("RongtaPrintersModule")

        Events("onPrintersFound", "onPrintImage", "onLog")

        OnCreate {
            PrinterLog.addSink(logSink)
            printer = RongtaPrinter(context)
            printerFinder = RongtaFinder(context)
        }

        OnDestroy {
            PrinterLog.removeSink(logSink)
            printer = null
            printerFinder = null
        }

        AsyncFunction("connectManually") { connectionType: String, connectionDetails: Map<String, Any> ->
            val type = connectionType.toPrinterConnectionType()
            if (type != PrinterConnectionType.Network) {
                throw IllegalArgumentException("Manual connection supports Network only for Rongta")
            }

            val ipAddress = connectionDetails.safeGetString("ipAddress", "IP address")
            val printerPort = connectionDetails.safeGetIntOrDefault("port", 9100)

            when (val validation = NetworkValidator.validateNetworkConnection(ipAddress, printerPort)) {
                is NetworkValidator.ValidationResult.Error -> {
                    PrinterLog.e(TAG, "Invalid network connection parameters: ${validation.message}")
                    throw IllegalArgumentException(validation.message)
                }
                NetworkValidator.ValidationResult.Valid -> {
                    mapOf(
                        "connectionType" to "Network",
                        "isSupported" to true,
                        "unsupportedReason" to "",
                        "type" to mapOf(
                            "type" to "NETWORK",
                            "ipAddress" to ipAddress,
                            "port" to printerPort
                        )
                    )
                }
            }
        }

        AsyncFunction("findPrinters") { connectionType: String ->
            runCatching {
                val type = try {
                    connectionType.toPrinterConnectionType()
                } catch (e: IllegalArgumentException) {
                    PrinterLog.e(TAG, "Invalid connection type: $connectionType", e)
                    return@AsyncFunction false
                }
                coroutineScope.launch(Dispatchers.IO) {
                    val printers = printerFinder?.search(type)?.map { deviceData ->
                        val baseMap = mapOf(
                            "connectionType" to deviceData.connectionType.name,
                            "isSupported" to deviceData.isSupported,
                            "unsupportedReason" to (deviceData.unsupportedReason ?: "")
                        )
                        when (deviceData.type) {
                            is PrinterDeviceData.Rongta.Type.Bluetooth -> baseMap + mapOf(
                                "type" to mapOf(
                                    "type" to "BLUETOOTH",
                                    "alias" to deviceData.type.alias,
                                    "name" to deviceData.type.name,
                                    "address" to deviceData.type.address
                                )
                            )
                            is PrinterDeviceData.Rongta.Type.Network -> baseMap + mapOf(
                                "type" to mapOf(
                                    "type" to "NETWORK",
                                    "ipAddress" to deviceData.type.ipAddress,
                                    "port" to deviceData.type.port
                                )
                            )
                            is PrinterDeviceData.Rongta.Type.Usb -> baseMap + mapOf(
                                "type" to mapOf(
                                    "type" to "USB",
                                    "name" to deviceData.type.name,
                                    "vendorId" to deviceData.type.vendorId,
                                    "productId" to deviceData.type.productId
                                )
                            )
                        }
                    } ?: emptyList()

                    sendEvent("onPrintersFound", mapOf("printers" to printers))
                }
                true
            }.onFailure { e ->
                PrinterLog.e(TAG, "Failed to find printers", e)
            }.getOrElse { false }
        }

        AsyncFunction("printImage") { base64Image: String, deviceData: Map<String, Any>, options: PrintOptions? ->
            if (printer == null) {
                return@AsyncFunction false
            }

            runCatching {
                val rongtaDeviceData = deviceData.toRongtaDeviceData()

                coroutineScope.launch(Dispatchers.IO) {
                    val report = printer?.printImage(base64Image, rongtaDeviceData, options?.paperWidth, options?.jobId)
                    sendEvent(
                        "onPrintImage",
                        report?.toPrintImageEvent(options?.jobId)
                            ?: printNotStartedEvent(options?.jobId, "NOT_INITIALIZED", null),
                    )
                }
                true
            }.onFailure { e ->
                PrinterLog.e(TAG, "Print failed", e)
                sendEvent("onPrintImage", printNotStartedEvent(options?.jobId, "INVALID_REQUEST", e))
            }.getOrElse { false }
        }

        AsyncFunction("getStatus") Coroutine { deviceData: Map<String, Any> ->
            val printer = requireNotNull(printer) { "Printer not initialized" }
            withContext(Dispatchers.IO) {
                printer.getStatus(deviceData.toRongtaDeviceData()).toMap()
            }
        }
    }

    private fun Map<String, Any>.toRongtaDeviceData(): PrinterDeviceData.Rongta {
        val connectionType = safeGetString("connectionType").toPrinterConnectionType()
        val typeData = safeGetMap("type", "printer type data")

        return when (connectionType) {
            PrinterConnectionType.Bluetooth -> {
                PrinterDeviceData.Rongta(
                    connectionType = connectionType,
                    type = PrinterDeviceData.Rongta.Type.Bluetooth(
                        alias = typeData.safeGetString("alias", "Bluetooth alias"),
                        name = typeData.safeGetString("name", "Bluetooth name"),
                        address = typeData.safeGetString("address", "Bluetooth address")
                    )
                )
            }
            PrinterConnectionType.Network -> {
                PrinterDeviceData.Rongta(
                    connectionType = connectionType,
                    type = PrinterDeviceData.Rongta.Type.Network(
                        ipAddress = typeData.safeGetString("ipAddress", "Network IP address"),
                        port = typeData.safeGetIntOrDefault("port", 9100)
                    )
                )
            }
            PrinterConnectionType.USB -> {
                PrinterDeviceData.Rongta(
                    connectionType = connectionType,
                    type = PrinterDeviceData.Rongta.Type.Usb(
                        name = typeData.safeGetString("name", "USB name"),
                        vendorId = typeData.safeGetIntOrDefault("vendorId", 0),
                        productId = typeData.safeGetIntOrDefault("productId", 0)
                    )
                )
            }
        }
    }
} 

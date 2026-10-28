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
import expo.modules.printers.commons.safeGetString
import expo.modules.printers.commons.safeGetStringOrDefault
import expo.modules.printers.commons.toPrinterConnectionType
import expo.modules.printers.starmicronics.StarMicronicsPrinter
import expo.modules.printers.starmicronics.StarMicronicsFinder
import expo.modules.printers.starmicronics.internals.StarModelCapability
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StarMicronicsPrintersModule : Module() {
    private val TAG = "StarMicronicsPrintersModule"
    private val logSink = PrinterLog.Sink("StarMicronics") { entry -> sendEvent("onLog", entry) }
    private var printer: StarMicronicsPrinter? = null
    private var printerFinder: StarMicronicsFinder? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    private val context: Context
        get() = requireNotNull(appContext.reactContext)

    override fun definition() = ModuleDefinition {
        Name("StarMicronicsPrintersModule")

        Events("onPrintersFound", "onPrintImage", "onLog")

        OnCreate {
            PrinterLog.addSink(logSink)
            printer = StarMicronicsPrinter(context)
            printerFinder = StarMicronicsFinder(context)
        }

        OnDestroy {
            PrinterLog.removeSink(logSink)
            printer = null
            printerFinder = null
        }

        AsyncFunction("connectManually") { connectionType: String, connectionDetails: Map<String, Any> ->
            val type = connectionType.toPrinterConnectionType()
            if (type != PrinterConnectionType.Network) {
                throw IllegalArgumentException("Manual connection supports Network only for Star Micronics")
            }

            val ipAddress = connectionDetails.safeGetString("ipAddress", "IP address")
            val printerPort = connectionDetails.safeGetIntOrDefault("port", STAR_NETWORK_PORT)
            val modelName = connectionDetails.safeGetString("modelName", "modelName")

            val modelIndex = StarModelCapability.getModelIdx(modelName)
                .takeIf { it != StarModelCapability.NONE }
                ?: StarModelCapability.getModelIdxByTitle(modelName)

            if (printerPort != STAR_NETWORK_PORT) {
                throw IllegalArgumentException("Star network printers use port $STAR_NETWORK_PORT")
            }

            if (modelIndex == StarModelCapability.NONE) {
                val supported = StarModelCapability.getSupportedModels().joinToString()
                throw IllegalArgumentException("Unsupported Star printer model: $modelName. Supported models: $supported")
            }

            when (val validation = NetworkValidator.validateNetworkConnection(ipAddress, printerPort)) {
                is NetworkValidator.ValidationResult.Error -> {
                    PrinterLog.e(TAG, "Invalid network connection parameters: ${validation.message}")
                    throw IllegalArgumentException(validation.message)
                }
                NetworkValidator.ValidationResult.Valid -> {
                    val portName = "TCP:$ipAddress"
                    mapOf(
                        "deviceName" to modelName,
                        "portName" to portName,
                        "macAddress" to "",
                        "usbSerialNumber" to "",
                        "connectionType" to PrinterConnectionType.Network.name,
                        "isSupported" to true,
                        "unsupportedReason" to ""
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
                        mapOf(
                            "deviceName" to deviceData.modelName,
                            "portName" to deviceData.portName,
                            "macAddress" to deviceData.macAddress,
                            "usbSerialNumber" to deviceData.usbSerialNumber,
                            "connectionType" to deviceData.connectionType.name,
                            "isSupported" to deviceData.isSupported,
                            "unsupportedReason" to (deviceData.unsupportedReason ?: "")
                        )
                    } ?: emptyList()

                    sendEvent("onPrintersFound", mapOf("printers" to printers))
                }
                true
            }.onFailure { e ->
                PrinterLog.e(TAG, "Failed to find printers", e)
            }.getOrElse { false }
        }

        AsyncFunction("getSupportedModels") {
            StarModelCapability.getSupportedModels()
        }

        AsyncFunction("printImage") { base64Image: String, deviceData: Map<String, Any>, options: PrintOptions? ->
            if (printer == null) {
                return@AsyncFunction false
            }

            runCatching {
                val starDeviceData = deviceData.toStarDeviceData()

                coroutineScope.launch(Dispatchers.IO) {
                    val report = printer?.printImage(base64Image, starDeviceData, options?.paperWidth, options?.jobId)
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
                printer.getStatus(deviceData.toStarDeviceData()).toMap()
            }
        }
    }

    private fun Map<String, Any>.toStarDeviceData() = PrinterDeviceData.Star(
        connectionType = safeGetString("connectionType").toPrinterConnectionType(),
        modelName = safeGetStringOrDefault("deviceName"),
        portName = safeGetStringOrDefault("portName"),
        macAddress = safeGetStringOrDefault("macAddress"),
        usbSerialNumber = safeGetStringOrDefault("usbSerialNumber")
    )

    companion object {
        private const val STAR_NETWORK_PORT = 9100
    }
} 

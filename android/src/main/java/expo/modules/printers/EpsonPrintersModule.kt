package expo.modules.printers

import android.content.Context
import android.graphics.Bitmap
import android.util.Base64
import expo.modules.printers.commons.PrinterLog
import expo.modules.kotlin.functions.Coroutine
import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition
import expo.modules.printers.commons.ImageConversion
import expo.modules.printers.commons.NetworkValidator
import expo.modules.printers.commons.PrintOptions
import expo.modules.printers.commons.printNotStartedEvent
import expo.modules.printers.commons.toPrintImageEvent
import expo.modules.printers.commons.PrinterConnectionType
import expo.modules.printers.commons.PrinterDeviceData
import expo.modules.printers.commons.safeGetInt
import expo.modules.printers.commons.safeGetIntOrDefault
import expo.modules.printers.commons.safeGetString
import expo.modules.printers.commons.safeGetStringOrDefault
import expo.modules.printers.commons.toPrinterConnectionType
import expo.modules.printers.epson.EpsonPrinter
import expo.modules.printers.epson.EpsonPrinterFinder
import expo.modules.printers.epson.internals.EpsonModelCapability
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class EpsonPrintersModule : Module() {
    private val TAG = "EpsonPrintersModule"
    private val logSink = PrinterLog.Sink("Epson") { entry -> sendEvent("onLog", entry) }
    private var printer: EpsonPrinter? = null
    private var printerFinder: EpsonPrinterFinder? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    private val context: Context
        get() = requireNotNull(appContext.reactContext)

    override fun definition() = ModuleDefinition {
        Name("EpsonPrintersModule")

        Events("onPrintersFound", "onPrintImage", "onLog")

        OnCreate {
            PrinterLog.addSink(logSink)
            printer = EpsonPrinter(context)
            printerFinder = EpsonPrinterFinder(context)
        }

        OnDestroy {
            PrinterLog.removeSink(logSink)
            printer = null
            printerFinder = null
        }

        AsyncFunction("connectManually") { connectionType: String, connectionDetails: Map<String, Any> ->
            val type = connectionType.toPrinterConnectionType()
            if (type != PrinterConnectionType.Network) {
                throw IllegalArgumentException("Manual connection supports Network only for Epson")
            }

            val ipAddress = connectionDetails.safeGetString("ipAddress", "IP address")
            val printerPort = connectionDetails.safeGetIntOrDefault("port", 9100)
            val modelName = connectionDetails.safeGetString("modelName", "modelName")

            if (EpsonModelCapability.printerSeriesByName(modelName) == EpsonModelCapability.UNKNOWN) {
                val supported = EpsonModelCapability.getSupportedModels().joinToString()
                throw IllegalArgumentException("Unsupported Epson model: $modelName. Supported models: $supported")
            }

            when (val validation = NetworkValidator.validateNetworkConnection(ipAddress, printerPort)) {
                is NetworkValidator.ValidationResult.Error -> {
                    PrinterLog.e(TAG, "Invalid network connection parameters: ${validation.message}")
                    throw IllegalArgumentException(validation.message)
                }
                NetworkValidator.ValidationResult.Valid -> {
                    // Epson SDK can target non-default ports using TCP:IP:PORT
                    val target = "TCP:$ipAddress:$printerPort"
                    mapOf(
                        "deviceName" to modelName,
                        "target" to target,
                        "ipAddress" to ipAddress,
                        "macAddress" to "",
                        "bdAddress" to "",
                        "connectionType" to PrinterConnectionType.Network.name,
                        "deviceType" to 0,
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
                            "deviceName" to deviceData.deviceName,
                            "target" to deviceData.target,
                            "ipAddress" to deviceData.ipAddress,
                            "macAddress" to deviceData.macAddress,
                            "bdAddress" to deviceData.bdAddress,
                            "connectionType" to deviceData.connectionType.name,
                            "deviceType" to deviceData.deviceType,
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
            EpsonModelCapability.getSupportedModels()
        }

        AsyncFunction("printImage") { base64Image: String, deviceData: Map<String, Any>, options: PrintOptions? ->
            if (printer == null) {
                return@AsyncFunction false
            }

            runCatching {
                val epsonDeviceData = deviceData.toEpsonDeviceData()

                coroutineScope.launch(Dispatchers.IO) {
                    val report = printer?.printImage(
                        base64Image,
                        epsonDeviceData,
                        options?.paperWidth,
                        options?.jobId,
                        options?.imageConversion ?: ImageConversion.DEFAULT,
                    )
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
                printer.getStatus(deviceData.toEpsonDeviceData()).toMap()
            }
        }

        // Returns the image that printImage sends to the printer, as a base64 PNG with its size in dots.
        // React Native smooths an image that it shows larger, so the dots of a small preview become blurred. With
        // targetWidth, the width in pixels of the view that shows the preview, each dot becomes a square of `scale`
        // pixels, and the app shows the PNG at its own size.
        AsyncFunction("previewImage") Coroutine { base64Image: String, deviceData: Map<String, Any>, options: PrintOptions?, targetWidth: Int? ->
            val printer = requireNotNull(printer) { "Printer not initialized" }
            withContext(Dispatchers.Default) {
                val dots = printer.previewImage(
                    base64Image,
                    deviceData.toEpsonDeviceData(),
                    options?.paperWidth,
                    options?.imageConversion ?: ImageConversion.DEFAULT,
                )
                val scale = previewScale(dots, targetWidth)
                val image = if (scale == 1) {
                    dots
                } else {
                    Bitmap.createScaledBitmap(dots, dots.width * scale, dots.height * scale, false)
                }
                val png = ByteArrayOutputStream().also { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
                mapOf(
                    "base64" to Base64.encodeToString(png.toByteArray(), Base64.NO_WRAP),
                    "width" to dots.width,
                    "height" to dots.height,
                    "scale" to scale,
                )
            }
        }
    }

    // The largest whole number of pixels for each dot that fits targetWidth, and keeps the PNG at most
    // MAX_PREVIEW_HEIGHT pixels high.
    private fun previewScale(dots: Bitmap, targetWidth: Int?): Int =
        if (targetWidth == null) 1 else minOf(targetWidth / dots.width, MAX_PREVIEW_HEIGHT / dots.height).coerceAtLeast(1)

    private fun Map<String, Any>.toEpsonDeviceData() = PrinterDeviceData.EPSON(
        connectionType = safeGetString("connectionType").toPrinterConnectionType(),
        deviceType = safeGetInt("deviceType"),
        target = safeGetStringOrDefault("target"),
        deviceName = safeGetStringOrDefault("deviceName"),
        ipAddress = safeGetStringOrDefault("ipAddress"),
        macAddress = safeGetStringOrDefault("macAddress"),
        bdAddress = safeGetStringOrDefault("bdAddress")
    )

    companion object {
        // Many Android GPUs cannot draw a bitmap that is higher than this.
        private const val MAX_PREVIEW_HEIGHT = 8192
    }
}

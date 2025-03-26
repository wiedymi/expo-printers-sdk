package expo.modules.printers

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import com.epson.epos2.Epos2Exception
import com.epson.epos2.discovery.DeviceInfo
import com.epson.epos2.discovery.Discovery
import com.epson.epos2.discovery.DiscoveryListener
import com.epson.epos2.discovery.FilterOption
import com.epson.epos2.printer.Printer
import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition
import java.io.ByteArrayInputStream

class EpsonPrintersModule : Module() {
    private var printer: Printer? = null
    private val TAG = "EpsonPrintersModule"

    private val context: Context
        get() = requireNotNull(appContext.reactContext)

    override fun definition() = ModuleDefinition {
        Name("EpsonPrintersModule")

        Constants(
                "ALIGN_LEFT" to Printer.ALIGN_LEFT,
                "ALIGN_CENTER" to Printer.ALIGN_CENTER,
                "ALIGN_RIGHT" to Printer.ALIGN_RIGHT,
                "LANG_EN" to Printer.LANG_EN,
                "LANG_JA" to Printer.LANG_JA,
                "LANG_ZH_CN" to Printer.LANG_ZH_CN,
                "LANG_ZH_TW" to Printer.LANG_ZH_TW,
                "LANG_KO" to Printer.LANG_KO,
                "LANG_TH" to Printer.LANG_TH,
                "FONT_A" to Printer.FONT_A,
                "FONT_B" to Printer.FONT_B
        )

        Events("onPrinterStatusChange", "onPrintSuccess", "onPrintError", "onDiscovery")

        // Helper functions (no visibility modifiers needed as they are local)
        fun sendErrorEvent(error: String, code: Int) {
            sendEvent("onPrintError", mapOf("error" to error, "code" to code))
        }

        fun sendDiscoveryEvent(deviceInfo: DeviceInfo) {
            val printerInfo =
                    mapOf(
                            "deviceName" to (deviceInfo.deviceName ?: "Unknown Device"),
                            "target" to (deviceInfo.target ?: ""),
                            "ip" to (deviceInfo.ipAddress ?: ""),
                            "mac" to (deviceInfo.macAddress ?: ""),
                            "bdAddress" to (deviceInfo.bdAddress ?: "")
                    )
            sendEvent("onDiscovery", printerInfo)
        }

        fun sendSuccessEvent() {
            sendEvent("onPrintSuccess", mapOf())
        }

        fun sendStatusChangeEvent(status: Map<String, Any>) {
            sendEvent("onPrinterStatusChange", status)
        }

        fun getErrorMessage(errorCode: Int): String {
            return when (errorCode) {
                Epos2Exception.ERR_PARAM -> "Invalid parameter"
                Epos2Exception.ERR_CONNECT -> "Connection error"
                Epos2Exception.ERR_TIMEOUT -> "Connection timeout"
                Epos2Exception.ERR_MEMORY -> "Memory allocation error"
                Epos2Exception.ERR_ILLEGAL -> "Illegal operation"
                Epos2Exception.ERR_PROCESSING -> "Processing error"
                Epos2Exception.ERR_NOT_FOUND -> "Printer not found"
                Epos2Exception.ERR_IN_USE -> "Printer is in use"
                Epos2Exception.ERR_TYPE_INVALID -> "Invalid printer type"
                Epos2Exception.ERR_DISCONNECT -> "Printer disconnected"
                Epos2Exception.ERR_ALREADY_OPENED -> "Printer already open"
                Epos2Exception.ERR_ALREADY_USED -> "Printer already in use"
                Epos2Exception.ERR_BOX_COUNT_OVER -> "Too many box elements"
                Epos2Exception.ERR_BOX_CLIENT_OVER -> "Too many clients"
                Epos2Exception.ERR_UNSUPPORTED -> "Unsupported operation"
                Epos2Exception.ERR_FAILURE -> "General failure"
                else -> "Error code: $errorCode"
            }
        }

        AsyncFunction("startDiscovery") { type: Int ->
            try {
                if (context == null) {
                    sendErrorEvent("Context is null", -1)
                    return@AsyncFunction false
                }

                val filterOption =
                        FilterOption().apply {
                            deviceType = Discovery.TYPE_ALL
                            epsonFilter = Discovery.FILTER_NONE
                        }

                Discovery.start(
                        context,
                        filterOption,
                        object : DiscoveryListener {
                            override fun onDiscovery(deviceInfo: DeviceInfo) {
                                sendDiscoveryEvent(deviceInfo)
                            }
                        }
                )
                true
            } catch (e: Epos2Exception) {
                Log.e(TAG, "Discovery failed: ${e.errorStatus}", e)
                sendErrorEvent(getErrorMessage(e.errorStatus), e.errorStatus)
                false
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error during discovery", e)
                sendErrorEvent(e.localizedMessage ?: "Unknown error during discovery", -1)
                false
            }
        }

        AsyncFunction("stopDiscovery") {
            try {
                Discovery.stop()
                true
            } catch (e: Epos2Exception) {
                Log.e(TAG, "Failed to stop discovery: ${e.errorStatus}", e)
                sendErrorEvent(getErrorMessage(e.errorStatus), e.errorStatus)
                false
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error stopping discovery", e)
                sendErrorEvent(e.localizedMessage ?: "Unknown error stopping discovery", -1)
                false
            }
        }

        AsyncFunction("connectPrinter") { target: String ->
            try {
                if (context == null) {
                    sendErrorEvent("Context is null", -1)
                    return@AsyncFunction false
                }

                if (target.isBlank()) {
                    sendErrorEvent("Invalid printer target: empty or blank", -1)
                    return@AsyncFunction false
                }

                // Disconnect any existing printer
                printer?.let {
                    try {
                        it.disconnect()
                    } catch (e: Exception) {
                        Log.w(TAG, "Error disconnecting previous printer", e)
                    }
                }
                printer = Printer(Printer.TM_M30, Printer.MODEL_ANK, context)
                printer?.connect(target, Printer.PARAM_DEFAULT)
                true
            } catch (e: Epos2Exception) {
                Log.e(TAG, "Connection failed: ${e.errorStatus}", e)
                sendErrorEvent(getErrorMessage(e.errorStatus), e.errorStatus)
                printer = null
                false
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error during connection", e)
                sendErrorEvent(e.localizedMessage ?: "Unknown error during connection", -1)
                printer = null
                false
            }
        }

        AsyncFunction("disconnectPrinter") {
            if (printer == null) {
                return@AsyncFunction true
            }

            try {
                printer?.disconnect()
                printer = null
                true
            } catch (e: Epos2Exception) {
                Log.e(TAG, "Disconnect failed: ${e.errorStatus}", e)
                sendErrorEvent(getErrorMessage(e.errorStatus), e.errorStatus)
                printer = null
                false
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error during disconnect", e)
                sendErrorEvent(e.localizedMessage ?: "Unknown error during disconnect", -1)
                printer = null
                false
            }
        }

        AsyncFunction("printText") { text: String, options: Map<String, Any> ->
            if (printer == null) {
                sendErrorEvent("Printer not connected", -1)
                return@AsyncFunction false
            }

            if (text.isEmpty()) {
                sendErrorEvent("Cannot print empty text", -1)
                return@AsyncFunction false
            }

            try {
                printer?.let { p ->
                    p.addText(text)
                    val alignment = options["alignment"] as? Int ?: Printer.ALIGN_LEFT
                    val font = options["font"] as? Int ?: Printer.FONT_A
                    val lang = options["lang"] as? Int ?: Printer.LANG_EN
                    p.addTextAlign(alignment)
                    p.addTextFont(font)
                    p.addTextLang(lang)
                    p.addFeedLine(1)
                    p.beginTransaction()
                    p.sendData(Printer.PARAM_DEFAULT)
                    p.endTransaction()
                    sendSuccessEvent()
                    true
                }
                        ?: run {
                            sendErrorEvent("Printer not initialized", -1)
                            false
                        }
            } catch (e: Epos2Exception) {
                Log.e(TAG, "Text printing failed: ${e.errorStatus}", e)
                sendErrorEvent(getErrorMessage(e.errorStatus), e.errorStatus)
                false
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error during text printing", e)
                sendErrorEvent(e.localizedMessage ?: "Unknown error during text printing", -1)
                false
            }
        }

        AsyncFunction("printImage") { base64Image: String, options: Map<String, Any> ->
            if (printer == null) {
                sendErrorEvent("Printer not connected", -1)
                return@AsyncFunction false
            }

            if (base64Image.isEmpty()) {
                sendErrorEvent("Cannot print empty image data", -1)
                return@AsyncFunction false
            }

            try {
                val decodedString = Base64.decode(base64Image, Base64.DEFAULT)
                val inputStream = ByteArrayInputStream(decodedString)
                val bitmap =
                        BitmapFactory.decodeStream(inputStream)
                                ?: throw IllegalArgumentException(
                                        "Failed to decode image from base64"
                                )

                printer?.let { p ->
                    val alignment = options["alignment"] as? Int ?: Printer.ALIGN_LEFT
                    p.addImage(
                            bitmap,
                            0,
                            0,
                            bitmap.width,
                            bitmap.height,
                            Printer.PARAM_DEFAULT,
                            Printer.PARAM_DEFAULT,
                            alignment,
                            1.0,
                            1
                    )
                    p.addFeedLine(1)
                    p.beginTransaction()
                    p.sendData(Printer.PARAM_DEFAULT)
                    p.endTransaction()
                    sendSuccessEvent()
                    true
                }
                        ?: run {
                            sendErrorEvent("Printer not initialized", -1)
                            false
                        }
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "Invalid image data", e)
                sendErrorEvent("Invalid image data: ${e.localizedMessage}", -1)
                false
            } catch (e: Epos2Exception) {
                Log.e(TAG, "Image printing failed: ${e.errorStatus}", e)
                sendErrorEvent(getErrorMessage(e.errorStatus), e.errorStatus)
                false
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error during image printing", e)
                sendErrorEvent(e.localizedMessage ?: "Unknown error during image printing", -1)
                false
            }
        }

        AsyncFunction("getPrinterStatus") {
            if (printer == null) {
                sendErrorEvent("Printer not connected", -1)
                return@AsyncFunction mapOf("connected" to false)
            }

            try {
                printer?.let { p ->
                    val status = p.status
                    val statusMap =
                            mapOf(
                                    "connected" to true,
                                    "connection" to status.connection,
                                    "online" to status.online,
                                    "coverOpen" to status.coverOpen,
                                    "paper" to status.paper,
                                    "paperFeed" to status.paperFeed,
                                    "panelSwitch" to status.panelSwitch
                            )
                    sendStatusChangeEvent(statusMap)
                    statusMap
                }
                        ?: mapOf("connected" to false)
            } catch (e: Epos2Exception) {
                Log.e(TAG, "Failed to get printer status: ${e.errorStatus}", e)
                sendErrorEvent(getErrorMessage(e.errorStatus), e.errorStatus)
                mapOf("connected" to false, "error" to getErrorMessage(e.errorStatus))
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error getting printer status", e)
                sendErrorEvent(e.localizedMessage ?: "Unknown error getting status", -1)
                mapOf("connected" to false, "error" to (e.localizedMessage ?: "Unknown error"))
            }
        }
    }
}
                         
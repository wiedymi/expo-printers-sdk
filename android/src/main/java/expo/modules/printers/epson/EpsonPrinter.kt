package expo.modules.printers.epson

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import expo.modules.printers.commons.PaperWidth
import expo.modules.printers.commons.Printer
import expo.modules.printers.commons.PrinterDeviceData
import expo.modules.printers.epson.internals.EpsonModelCapability
import com.epson.epos2.Epos2CallbackCode
import com.epson.epos2.Epos2Exception
import com.epson.epos2.printer.Printer as EpsPrinter
import com.epson.epos2.printer.PrinterSettingListener
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.io.ByteArrayInputStream
import kotlin.coroutines.resume
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class EpsonPrinter(
    private val appContext: Context,
) : Printer<PrinterDeviceData.EPSON, EpsonPrintResult> {

    override suspend fun printImage(
        base64Image: String,
        deviceData: PrinterDeviceData.EPSON,
        paperWidth: PaperWidth?,
    ): EpsonPrintResult {
        val img: Bitmap = runCatching {
            val decodedString = Base64.decode(base64Image, Base64.DEFAULT)
            BitmapFactory.decodeStream(ByteArrayInputStream(decodedString))
        }.getOrNull()?.takeIf { it.width > 0 && it.height > 0 } ?: return EpsonPrintResult.ErrorInvalidImage

        val printerSeries = EpsonModelCapability.printerSeriesByName(deviceData.deviceName)
        if (printerSeries == EpsonModelCapability.UNKNOWN) return EpsonPrintResult.ErrorUnknown

        return runCatching {
            val epsonPrinter = EpsPrinter(
                printerSeries,
                EpsPrinter.LANG_EN,
                appContext
            )

            epsonPrinter.connect(deviceData.target, EpsPrinter.PARAM_DEFAULT)
            try {
                val paperWidthSetting = when {
                    paperWidth != null -> EpsonModelCapability.paperWidthSetting(paperWidth)
                    EpsonModelCapability.supportsPaperWidthQuery(deviceData.deviceName) -> epsonPrinter.queryPaperWidth()
                    else -> null
                }
                val printableWidth = EpsonModelCapability.printableWidth(printerSeries, paperWidthSetting)
                Log.i(
                    TAG,
                    "deviceName='${deviceData.deviceName}', paperWidth=$paperWidth, paperWidthSetting=$paperWidthSetting, " +
                        "printableWidth=$printableWidth, imageWidth=${img.width}",
                )
                // The ePOS SDK has no scaling option, and an image wider than the print area can print nothing.
                val scaledImg = img.scaledToWidth(printableWidth)

                with(epsonPrinter) {
                    addImage(
                        scaledImg, 0, 0,
                        scaledImg.width,
                        scaledImg.height,
                        EpsPrinter.COLOR_1,
                        EpsPrinter.MODE_MONO,
                        EpsPrinter.HALFTONE_THRESHOLD,
                        EpsPrinter.PARAM_DEFAULT.toDouble(),
                        EpsPrinter.COMPRESS_AUTO
                    )

                    addCut(EpsPrinter.CUT_FEED)

                    sendData(EpsPrinter.PARAM_DEFAULT)
                    setReceiveEventListener(null)
                }
            } finally {
                epsonPrinter.disconnectSafeWithRetry()
                epsonPrinter.clearCommandBuffer()
            }

            EpsonPrintResult.Success
        }.fold(
            onSuccess = { it },
            onFailure = { throwable ->
                when ((throwable as? Epos2Exception)?.errorStatus) {
                    Epos2Exception.ERR_CONNECT,
                    Epos2Exception.ERR_TIMEOUT,
                    Epos2Exception.ERR_NOT_FOUND ->
                        EpsonPrintResult.ErrorPrinterOffline

                    else -> EpsonPrintResult.ErrorUnknown
                }
            }
        )
    }

    // Returns a Printer.SETTING_PAPERWIDTH_* value, or null if the printer does not report it. Needs a connection.
    private suspend fun EpsPrinter.queryPaperWidth(): Int? =
        withTimeoutOrNull(PAPER_WIDTH_QUERY_TIMEOUT_MILLISECONDS + PAPER_WIDTH_CALLBACK_GRACE_MILLISECONDS) {
            suspendCancellableCoroutine { continuation ->
                try {
                    getPrinterSetting(
                        PAPER_WIDTH_QUERY_TIMEOUT_MILLISECONDS.toInt(),
                        EpsPrinter.SETTING_PAPERWIDTH,
                        object : PrinterSettingListener {
                            override fun onGetPrinterSetting(code: Int, type: Int, value: Int) {
                                if (code != Epos2CallbackCode.CODE_SUCCESS) {
                                    Log.w(TAG, "getPrinterSetting failed with code $code")
                                }
                                continuation.resume(value.takeIf { code == Epos2CallbackCode.CODE_SUCCESS })
                            }

                            override fun onSetPrinterSetting(code: Int) = Unit
                        }
                    )
                } catch (exception: Epos2Exception) {
                    Log.w(TAG, "getPrinterSetting failed with status ${exception.errorStatus}")
                    continuation.resume(null)
                }
            }
        }

    // Scales up or down to the width and keeps the aspect ratio.
    private fun Bitmap.scaledToWidth(targetWidth: Int): Bitmap {
        if (width == targetWidth) return this
        val targetHeight = (height.toLong() * targetWidth / width)
            .coerceIn(1, Int.MAX_VALUE.toLong())
            .toInt()
        return Bitmap.createScaledBitmap(this, targetWidth, targetHeight, true)
    }

    private suspend fun EpsPrinter.disconnectSafeWithRetry(
        delay: Duration = 500.milliseconds,
        attempts: Int = 5
    ) {
        runCatching {
            delay(delay)
            this.disconnect()
        }.onFailure { throwable ->
            if (throwable is Epos2Exception && throwable.errorStatus == Epos2Exception.ERR_PROCESSING) {
                if (attempts > 0) {
                    disconnectSafeWithRetry(delay, attempts - 1)
                } else {
                    Log.e(TAG, "failed to disconnect $throwable")
                }
            }
        }
    }

    companion object {
        private const val TAG = "EpsonPrinter"

        // The ePOS SDK accepts 5000 ms or more.
        private const val PAPER_WIDTH_QUERY_TIMEOUT_MILLISECONDS = 5_000L

        // The SDK calls the listener when its own timeout ends. This limit stops the wait if the call never comes.
        private const val PAPER_WIDTH_CALLBACK_GRACE_MILLISECONDS = 1_000L
    }
}

package expo.modules.printers.epson

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import expo.modules.printers.commons.PrinterLog
import expo.modules.printers.commons.PaperStatus
import expo.modules.printers.commons.PaperWidth
import expo.modules.printers.commons.PrintReport
import expo.modules.printers.commons.PrintStepTimeoutException
import expo.modules.printers.commons.PrintSteps
import expo.modules.printers.commons.Printer
import expo.modules.printers.commons.PrinterBusyException
import expo.modules.printers.commons.PrinterDeviceData
import expo.modules.printers.commons.PrinterError
import expo.modules.printers.commons.PrinterStatus
import expo.modules.printers.commons.PrinterStatusUnavailableException
import expo.modules.printers.commons.PrinterUnreachableException
import expo.modules.printers.epson.internals.EpsonModelCapability
import com.epson.epos2.Epos2CallbackCode
import com.epson.epos2.Epos2Exception
import com.epson.epos2.printer.Printer as EpsPrinter
import com.epson.epos2.printer.PrinterSettingListener
import com.epson.epos2.printer.PrinterStatusInfo
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.io.ByteArrayInputStream
import kotlin.coroutines.resume
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class EpsonPrinter(
    private val appContext: Context,
) : Printer<PrinterDeviceData.EPSON, EpsonPrintResult> {

    override suspend fun printImage(
        base64Image: String,
        deviceData: PrinterDeviceData.EPSON,
        paperWidth: PaperWidth?,
        jobId: String?,
    ): PrintReport<EpsonPrintResult> {
        val steps = PrintSteps(TAG, jobId, ::vendorCodeOf)
        return steps.report(print(base64Image, deviceData, paperWidth, steps))
    }

    private suspend fun print(
        base64Image: String,
        deviceData: PrinterDeviceData.EPSON,
        paperWidth: PaperWidth?,
        steps: PrintSteps,
    ): EpsonPrintResult {
        val img: Bitmap = runCatching {
            val decodedString = Base64.decode(base64Image, Base64.DEFAULT)
            BitmapFactory.decodeStream(ByteArrayInputStream(decodedString))
        }.getOrNull()?.takeIf { it.width > 0 && it.height > 0 }
            ?: return EpsonPrintResult.ErrorInvalidImage.also { steps.log("result", it.code) }

        val printerSeries = EpsonModelCapability.printerSeriesByName(deviceData.deviceName)
        if (printerSeries == EpsonModelCapability.UNKNOWN) {
            return EpsonPrintResult.ErrorUnsupportedModel.also { steps.log("result", it.code) }
        }

        return runCatching {
            val epsonPrinter = EpsPrinter(
                printerSeries,
                EpsPrinter.LANG_EN,
                appContext
            )

            epsonPrinter.connectWithTimeout(deviceData.target, steps)
            try {
                val paperWidthSetting = when {
                    paperWidth != null -> EpsonModelCapability.paperWidthSetting(paperWidth)
                    EpsonModelCapability.supportsPaperWidthQuery(deviceData.deviceName) ->
                        steps.measure("paper_width") { epsonPrinter.queryPaperWidth() }
                    else -> null
                }
                val printableWidth = EpsonModelCapability.printableWidth(printerSeries, paperWidthSetting)
                PrinterLog.i(
                    TAG,
                    "deviceName='${deviceData.deviceName}', paperWidth=$paperWidth, paperWidthSetting=$paperWidthSetting, " +
                        "printableWidth=$printableWidth, imageWidth=${img.width}",
                )
                // The ePOS SDK has no scaling option, and an image wider than the print area can print nothing.
                val scaledImg = img.scaledToWidth(printableWidth)

                steps.measure("add_commands") {
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
                    }
                }

                steps.runWithTimeout("send_data", SEND_DATA_TIMEOUT) { epsonPrinter.sendData(EpsPrinter.PARAM_DEFAULT) }
                epsonPrinter.setReceiveEventListener(null)
            } finally {
                epsonPrinter.disconnectSafeWithRetry(steps)
                epsonPrinter.clearCommandBuffer()
            }

            EpsonPrintResult.Success
        }.fold(
            onSuccess = { it },
            onFailure = { throwable ->
                steps.recordFailureIfNone("print", throwable)
                if (throwable is PrintStepTimeoutException) {
                    EpsonPrintResult.ErrorTimeout
                } else {
                    when ((throwable as? Epos2Exception)?.errorStatus) {
                        Epos2Exception.ERR_CONNECT,
                        Epos2Exception.ERR_TIMEOUT,
                        Epos2Exception.ERR_NOT_FOUND ->
                            EpsonPrintResult.ErrorPrinterOffline

                        else -> EpsonPrintResult.ErrorUnknown
                    }
                }
            }
        ).also { result -> steps.log("result", result.code) }
    }

    override suspend fun getStatus(deviceData: PrinterDeviceData.EPSON): PrinterStatus {
        val printerSeries = EpsonModelCapability.printerSeriesByName(deviceData.deviceName)
        require(printerSeries != EpsonModelCapability.UNKNOWN) { "Unsupported Epson model: ${deviceData.deviceName}" }

        val steps = PrintSteps(TAG, jobId = null, ::vendorCodeOf)
        val epsonPrinter = EpsPrinter(printerSeries, EpsPrinter.LANG_EN, appContext)
        try {
            epsonPrinter.connectWithTimeout(deviceData.target, steps)
        } catch (exception: Epos2Exception) {
            PrinterLog.e(TAG, "getStatus: connect failed with status ${exception.errorStatus}")
            throw if (exception.errorStatus == Epos2Exception.ERR_IN_USE) {
                PrinterBusyException(exception)
            } else {
                PrinterUnreachableException(exception)
            }
        } catch (exception: PrintStepTimeoutException) {
            throw PrinterUnreachableException(exception)
        }

        try {
            val statusInfo = steps.measure("printer_status") { epsonPrinter.status }
            if (statusInfo.connection != EpsPrinter.TRUE) throw PrinterStatusUnavailableException()
            return statusInfo.toPrinterStatus()
        } finally {
            runCatching { steps.runWithTimeout("disconnect", DISCONNECT_TIMEOUT, cleanup = true) { epsonPrinter.disconnect() } }
                .onFailure { throwable -> PrinterLog.e(TAG, "getStatus: failed to disconnect $throwable") }
        }
    }

    private fun vendorCodeOf(throwable: Throwable): Int? = (throwable as? Epos2Exception)?.errorStatus

    // A connection that opens after its step timed out is not used, so close it at once.
    private suspend fun EpsPrinter.connectWithTimeout(target: String, steps: PrintSteps) {
        steps.runWithTimeout(
            "connect",
            CONNECT_TIMEOUT,
            onLateResult = {
                runCatching { disconnect() }
                    .onFailure { throwable -> PrinterLog.e(TAG, "failed to close connection opened after timeout $throwable") }
            },
        ) {
            connect(target, EpsPrinter.PARAM_DEFAULT)
        }
    }

    private fun PrinterStatusInfo.toPrinterStatus() = PrinterStatus(
        online = online == EpsPrinter.TRUE,
        coverOpen = coverOpen == EpsPrinter.TRUE,
        paper = when (paper) {
            EpsPrinter.PAPER_OK -> PaperStatus.OK
            EpsPrinter.PAPER_NEAR_END -> PaperStatus.NEAR_END
            EpsPrinter.PAPER_EMPTY -> PaperStatus.EMPTY
            else -> PaperStatus.UNKNOWN
        },
        errors = listOfNotNull(
            when (errorStatus) {
                EpsPrinter.MECHANICAL_ERR -> PrinterError.MECHANICAL_ERROR
                EpsPrinter.AUTOCUTTER_ERR -> PrinterError.CUTTER_ERROR
                EpsPrinter.UNRECOVER_ERR -> PrinterError.UNRECOVERABLE_ERROR
                EpsPrinter.AUTORECOVER_ERR -> when (autoRecoverError) {
                    EpsPrinter.HEAD_OVERHEAT,
                    EpsPrinter.MOTOR_OVERHEAT,
                    EpsPrinter.BATTERY_OVERHEAT -> PrinterError.OVER_TEMPERATURE
                    // coverOpen already reports this.
                    EpsPrinter.COVER_OPEN -> null
                    else -> PrinterError.AUTO_RECOVERABLE_ERROR
                }
                EpsPrinter.NO_ERR, EpsPrinter.UNKNOWN -> null
                else -> PrinterError.UNKNOWN_ERROR
            }
        ),
    )

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
                                    PrinterLog.w(TAG, "getPrinterSetting failed with code $code")
                                }
                                continuation.resume(value.takeIf { code == Epos2CallbackCode.CODE_SUCCESS })
                            }

                            override fun onSetPrinterSetting(code: Int) = Unit
                        }
                    )
                } catch (exception: Epos2Exception) {
                    PrinterLog.w(TAG, "getPrinterSetting failed with status ${exception.errorStatus}")
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
        steps: PrintSteps,
        delay: Duration = 500.milliseconds,
        attempts: Int = 5
    ) {
        runCatching {
            delay(delay)
            steps.runWithTimeout("disconnect", DISCONNECT_TIMEOUT, cleanup = true) { this.disconnect() }
        }.onFailure { throwable ->
            if (throwable is Epos2Exception && throwable.errorStatus == Epos2Exception.ERR_PROCESSING) {
                if (attempts > 0) {
                    disconnectSafeWithRetry(steps, delay, attempts - 1)
                } else {
                    PrinterLog.e(TAG, "failed to disconnect $throwable")
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

        // Hard limits for blocking ePOS SDK calls. They are longer than the SDK defaults (connect 15 s,
        // sendData 10 s), because a call can block longer than its SDK timeout.
        private val CONNECT_TIMEOUT = 20.seconds
        private val SEND_DATA_TIMEOUT = 20.seconds
        private val DISCONNECT_TIMEOUT = 5.seconds
    }
}

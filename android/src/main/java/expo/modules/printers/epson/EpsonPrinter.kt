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
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import java.io.ByteArrayInputStream
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class EpsonPrinter(
    private val appContext: Context,
) : Printer<PrinterDeviceData.EPSON, EpsonPrintResult> {

    // A print keeps its connection until the paper is printed, and a second connection to a printer in use fails,
    // so the prints to one printer run one after another.
    private val printLocks = ConcurrentHashMap<String, Mutex>()

    override suspend fun printImage(
        base64Image: String,
        deviceData: PrinterDeviceData.EPSON,
        paperWidth: PaperWidth?,
        jobId: String?,
    ): PrintReport<EpsonPrintResult> {
        val steps = PrintSteps(TAG, jobId, ::vendorCodeOf)
        val waitStartNanos = System.nanoTime()
        val result = printLocks.computeIfAbsent(deviceData.target) { Mutex() }.withLock {
            steps.log("wait_for_printer", "ok", waitStartNanos)
            print(base64Image, deviceData, paperWidth, steps)
        }
        return steps.report(result)
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
                // The ePOS SDK has no scaling option, and an image wider than the print area can print nothing.
                val scaledImg = img.scaledToWidth(printableWidth)
                val printTimeLimit = EpsonModelCapability.printTimeLimit(printerSeries, scaledImg.height)
                PrinterLog.i(
                    TAG,
                    "deviceName='${deviceData.deviceName}', paperWidth=$paperWidth, paperWidthSetting=$paperWidthSetting, " +
                        "printableWidth=$printableWidth, imageWidth=${img.width}, scaledImageHeight=${scaledImg.height}, " +
                        "printTimeLimit=$printTimeLimit",
                )

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

                // The SDK gives the print result to the listener when the print ends or when printTimeLimit ends.
                // A disconnect before that stops the print. sendData can block while it sends the data, so it has
                // the same limit as the wait for the result.
                val printResult = CompletableDeferred<Int>()
                epsonPrinter.setReceiveEventListener { _, code, _, _ ->
                    if (!printResult.complete(code)) PrinterLog.w(TAG, "print result $code came after the first result")
                }
                val waitLimit = printTimeLimit + PRINT_RESULT_GRACE
                steps.runWithTimeout("send_data", waitLimit) {
                    epsonPrinter.sendData(printTimeLimit.inWholeMilliseconds.toInt())
                }
                awaitPrintResult(printResult, waitLimit, steps)
            } finally {
                epsonPrinter.setReceiveEventListener(null)
                epsonPrinter.disconnectWithRetry(steps)
                epsonPrinter.clearCommandBuffer()
            }
        }.getOrElse { throwable ->
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
        }.also { result -> steps.log("result", result.code) }
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
            epsonPrinter.disconnectWithRetry(steps)
        }
    }

    // waitLimit stops the wait if the SDK never gives the result.
    private suspend fun awaitPrintResult(
        printResult: Deferred<Int>,
        waitLimit: Duration,
        steps: PrintSteps,
    ): EpsonPrintResult {
        val waitStartNanos = System.nanoTime()
        val code = withTimeoutOrNull(waitLimit) { printResult.await() }
        if (code == null) {
            steps.failed("print_result", PrintStepTimeoutException("print_result", waitLimit), waitStartNanos, cleanup = false)
            return EpsonPrintResult.ErrorTimeout
        }
        if (code == Epos2CallbackCode.CODE_SUCCESS) {
            steps.log("print_result", "ok", waitStartNanos)
            return EpsonPrintResult.Success
        }
        steps.failed("print_result", PrintCallbackException(code), waitStartNanos, cleanup = false)
        return when (code) {
            Epos2CallbackCode.CODE_ERR_TIMEOUT -> EpsonPrintResult.ErrorTimeout
            Epos2CallbackCode.CODE_ERR_NOT_FOUND,
            Epos2CallbackCode.CODE_ERR_PORT,
            Epos2CallbackCode.CODE_ERR_DISCONNECT -> EpsonPrintResult.ErrorPrinterOffline
            else -> EpsonPrintResult.ErrorUnknown
        }
    }

    // A print that the SDK reports as failed. code is an Epos2CallbackCode value.
    private class PrintCallbackException(val code: Int) : Exception("Print ended with callback code $code")

    private fun vendorCodeOf(throwable: Throwable): Int? = when (throwable) {
        is Epos2Exception -> throwable.errorStatus
        is PrintCallbackException -> throwable.code
        else -> null
    }

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

    // disconnect fails with ERR_PROCESSING while the SDK still runs a print, for example after the wait for the
    // print result ended, so it tries again after a short delay.
    private suspend fun EpsPrinter.disconnectWithRetry(steps: PrintSteps) {
        repeat(DISCONNECT_ATTEMPTS) { attempt ->
            if (attempt > 0) delay(DISCONNECT_RETRY_DELAY)
            val failure = runCatching {
                steps.runWithTimeout("disconnect", DISCONNECT_TIMEOUT, cleanup = true) { disconnect() }
            }.exceptionOrNull() ?: return
            if ((failure as? Epos2Exception)?.errorStatus != Epos2Exception.ERR_PROCESSING) {
                PrinterLog.e(TAG, "failed to disconnect", failure)
                return
            }
        }
        PrinterLog.e(TAG, "failed to disconnect: the SDK still runs the print after $DISCONNECT_ATTEMPTS attempts")
    }

    companion object {
        private const val TAG = "EpsonPrinter"

        // The ePOS SDK accepts 5000 ms or more.
        private const val PAPER_WIDTH_QUERY_TIMEOUT_MILLISECONDS = 5_000L

        // The SDK calls the listener when its own timeout ends. This limit stops the wait if the call never comes.
        private const val PAPER_WIDTH_CALLBACK_GRACE_MILLISECONDS = 1_000L

        // Hard limits for blocking ePOS SDK calls. The connect limit is longer than the SDK timeout (15 s), because
        // a call can block longer than its SDK timeout.
        private val CONNECT_TIMEOUT = 20.seconds
        private val DISCONNECT_TIMEOUT = 5.seconds

        private const val DISCONNECT_ATTEMPTS = 6
        private val DISCONNECT_RETRY_DELAY = 500.milliseconds

        // Time to wait for the print result after the sendData time limit ends.
        private val PRINT_RESULT_GRACE = 5.seconds
    }
}

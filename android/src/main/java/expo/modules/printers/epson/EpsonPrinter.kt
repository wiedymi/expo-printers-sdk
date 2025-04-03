package expo.modules.printers.epson

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import expo.modules.printers.commons.Printer
import expo.modules.printers.commons.PrinterDeviceData
import expo.modules.printers.epson.internals.EpsonModelCapability
import com.epson.epos2.Epos2Exception
import com.epson.epos2.printer.Printer as EpsPrinter
import kotlinx.coroutines.delay
import java.io.ByteArrayInputStream
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class EpsonPrinter(
    private val appContext: Context,
) : Printer<PrinterDeviceData.EPSON, EpsonPrintResult> {

    override suspend fun printImage(
        base64Image: String,
        deviceData: PrinterDeviceData.EPSON
    ): EpsonPrintResult {
        val img: Bitmap = runCatching {
            val decodedString = Base64.decode(base64Image, Base64.DEFAULT)
            val inputStream = ByteArrayInputStream(decodedString)
            BitmapFactory.decodeStream(inputStream)
        }.getOrNull() ?: return EpsonPrintResult.ErrorInvalidImage

        val printerSeries = EpsonModelCapability.printerSeriesByName(deviceData.deviceName)
        if (printerSeries == EpsonModelCapability.UNKNOWN) return EpsonPrintResult.ErrorUnknown

        var printer: EpsPrinter? = null

        return runCatching {
            printer = EpsPrinter(
                printerSeries,
                EpsPrinter.LANG_EN,
                appContext
            )

            printer.addImage(
                img, 0, 0,
                img.width,
                img.height,
                EpsPrinter.COLOR_1,
                EpsPrinter.MODE_MONO,
                EpsPrinter.HALFTONE_THRESHOLD,
                EpsPrinter.PARAM_DEFAULT.toDouble(),
                EpsPrinter.COMPRESS_AUTO
            )

            printer.addCut(EpsPrinter.CUT_FEED)

            printer.connect(deviceData.target, EpsPrinter.PARAM_DEFAULT)
            printer.sendData(EpsPrinter.PARAM_DEFAULT)
            printer.setReceiveEventListener(null)
        }.fold(
            onSuccess = {
                printer?.disconnectSafeWithRetry()
                printer?.clearCommandBuffer()

                EpsonPrintResult.Success
            },
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
    }
}

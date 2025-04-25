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
        val maxWidth = 560
        val img: Bitmap = runCatching {
            val decodedString = Base64.decode(base64Image, Base64.DEFAULT)
            val inputStream = ByteArrayInputStream(decodedString)
            val decodedBitmap = BitmapFactory.decodeStream(inputStream)
            Bitmap.createScaledBitmap(decodedBitmap, maxWidth, (decodedBitmap.height * maxWidth / decodedBitmap.width), true)
        }.getOrNull() ?: return EpsonPrintResult.ErrorInvalidImage

        val printerSeries = EpsonModelCapability.printerSeriesByName(deviceData.deviceName)
        if (printerSeries == EpsonModelCapability.UNKNOWN) return EpsonPrintResult.ErrorUnknown

        return runCatching {
            val epsonPrinter = EpsPrinter(
                printerSeries,
                EpsPrinter.LANG_EN,
                appContext
            )

            with(epsonPrinter) {
                addImage(
                    img, 0, 0,
                    img.width,
                    img.height,
                    EpsPrinter.COLOR_1,
                    EpsPrinter.MODE_MONO,
                    EpsPrinter.HALFTONE_THRESHOLD,
                    EpsPrinter.PARAM_DEFAULT.toDouble(),
                    EpsPrinter.COMPRESS_AUTO
                )

                addCut(EpsPrinter.CUT_FEED)

                connect(deviceData.target, EpsPrinter.PARAM_DEFAULT)
                sendData(EpsPrinter.PARAM_DEFAULT)
                setReceiveEventListener(null)
            }

            epsonPrinter.disconnectSafeWithRetry()
            epsonPrinter.clearCommandBuffer()

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

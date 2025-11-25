package expo.modules.printers.starmicronics

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import expo.modules.printers.commons.Printer
import expo.modules.printers.commons.PrinterConnectionType
import expo.modules.printers.commons.PrinterDeviceData
import expo.modules.printers.starmicronics.internals.StarModelCapability
import com.starmicronics.stario.StarIOPort
import com.starmicronics.stario.StarPrinterStatus
import com.starmicronics.starioextension.ICommandBuilder
import com.starmicronics.starioextension.ICommandBuilder.CutPaperAction
import com.starmicronics.starioextension.StarIoExt
import kotlinx.coroutines.coroutineScope
import java.io.ByteArrayInputStream
import kotlin.time.Duration.Companion.seconds

class StarMicronicsPrinter(
    private val appContext: Context,
) : Printer<PrinterDeviceData.Star, StarMicronicsPrintResult> {

    override suspend fun printImage(
        base64Image: String,
        deviceData: PrinterDeviceData.Star
    ): StarMicronicsPrintResult = coroutineScope {
        // Try to identify printer using model name
        Log.d(TAG, "Attempting to identify printer - modelName: '${deviceData.modelName}', portName: '${deviceData.portName}', macAddress: '${deviceData.macAddress}'")

        var modelIndex = StarModelCapability.getModelIdx(deviceData.modelName)
        Log.d(TAG, "getModelIdx result: $modelIndex")

        // It could have happen for some BT printers when they don't provide model name
        if (modelIndex == StarModelCapability.NONE) {
            // So user can specify model manually
            modelIndex = StarModelCapability.getModelIdxByTitle(deviceData.modelName)
            Log.d(TAG, "getModelIdxByTitle result: $modelIndex")
        }

        if (modelIndex == StarModelCapability.NONE) {
            Log.e(TAG, "Failed to identify printer model from modelName: '${deviceData.modelName}'. Supported models: ${StarModelCapability.getSupportedModels()}")
            return@coroutineScope StarMicronicsPrintResult.ErrorUnknown
        }

        val portSettings = StarModelCapability.getPortSettings(modelIndex)
        val paperSize = StarModelCapability.getPaperSize(modelIndex)
        // --- Bluetooth ---
        // It can communicate with device name(Ex.BT:Star Micronics) using bluetooth.
        // If used Mac Address(Ex. BT:00:12:3f:XX:XX:XX) at Bluetooth, can choose destination target.
        val portName = if (deviceData.connectionType == PrinterConnectionType.Bluetooth) {
            "BT:" + deviceData.macAddress
        } else {
            deviceData.portName
        }
        val emulation = StarModelCapability.getEmulation(modelIndex)

        return@coroutineScope print(
            base64Image = base64Image,
            emulation = emulation,
            portName = portName,
            portSettings = portSettings,
            paperSize = paperSize,
        )
    }

    private fun print(
        base64Image: String,
        emulation: StarIoExt.Emulation,
        portName: String,
        portSettings: String,
        paperSize: Int,
    ): StarMicronicsPrintResult {
        val builder = StarIoExt.createCommandBuilder(emulation)

        builder.beginDocument()

        val img: Bitmap = runCatching {
            val decodedString = Base64.decode(base64Image, Base64.DEFAULT)
            val inputStream = ByteArrayInputStream(decodedString)
            val decodedBitmap = BitmapFactory.decodeStream(inputStream)

            val ratio = paperSize.toFloat() / decodedBitmap.width
            val targetHeight = (decodedBitmap.height * ratio).toInt()
            Bitmap.createScaledBitmap(decodedBitmap, paperSize, targetHeight, true)
        }.getOrNull() ?: return StarMicronicsPrintResult.ErrorInvalidImage

        builder.appendBitmap(
            img,
            false,      // diffusion (true = dithering, better for photos)
            paperSize,  // width in printer dots
            true,       // bothScale = preserve aspect ratio
            ICommandBuilder.BitmapConverterRotation.Normal
        )

        builder.appendCutPaper(CutPaperAction.PartialCutWithFeed)

        builder.endDocument()

        var result: StarMicronicsPrintResult = StarMicronicsPrintResult.ErrorOpenPort
        runCatching {
            val port = StarIOPort.getPort(
                portName,
                portSettings,
                connectionTimeout.inWholeSeconds.toInt(),
                appContext
            )

            var status: StarPrinterStatus = port.beginCheckedBlock()
            if (status.offline) {
                result = StarMicronicsPrintResult.ErrorPrinterOffline
            } else {
                val receiptData = builder.commands
                port.writePort(receiptData, 0, receiptData.size)
                port.setEndCheckedBlockTimeoutMillis(endCheckedBlockTimeout.inWholeSeconds.toInt())

                status = port.endCheckedBlock()
                when {
                    status.coverOpen -> {
                        result = StarMicronicsPrintResult.ErrorCoverOpened
                    }

                    status.receiptPaperEmpty -> {
                        result = StarMicronicsPrintResult.ErrorPaperEmpty
                    }

                    status.jamError -> {
                        result = StarMicronicsPrintResult.ErrorPaperJam
                    }

                    status.offline -> {
                        result = StarMicronicsPrintResult.ErrorPrinterOffline
                    }

                    status.unrecoverableError -> {
                        result = StarMicronicsPrintResult.ErrorUnknown
                    }

                    else -> {
                        result = StarMicronicsPrintResult.Success
                    }
                }
            }

            port
        }.onFailure { throwable ->
            result = if (throwable.message == PRINTER_IS_POWER_OFF) {
                StarMicronicsPrintResult.ErrorPrinterOffline
            } else {
                StarMicronicsPrintResult.ErrorUnknown
            }
        }.onSuccess { port ->
            runCatching {
                StarIOPort.releasePort(port)
            }.onFailure { throwable ->
                Log.e("StarMicronicsPrinter", "failed to release printer port, error - $throwable")
            }
        }

        return result
    }

    companion object {
        private const val TAG = "StarMicronicsPrinter"
        const val PRINTER_IS_POWER_OFF = "Printer is power off."
        private val connectionTimeout = 10.seconds
        private val endCheckedBlockTimeout = 30.seconds
    }
}
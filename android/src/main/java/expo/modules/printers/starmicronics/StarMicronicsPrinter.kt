package expo.modules.printers.starmicronics

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import com.starmicronics.stario.StarIOPort
import com.starmicronics.stario.StarIOPortException
import com.starmicronics.stario.StarPrinterStatus
import com.starmicronics.stario.StarResultCode
import com.starmicronics.starioextension.ICommandBuilder
import com.starmicronics.starioextension.ICommandBuilder.CutPaperAction
import com.starmicronics.starioextension.StarIoExt
import expo.modules.printers.commons.PaperStatus
import expo.modules.printers.commons.PaperWidth
import expo.modules.printers.commons.Printer
import expo.modules.printers.commons.PrinterBusyException
import expo.modules.printers.commons.PrinterConnectionType
import expo.modules.printers.commons.PrinterDeviceData
import expo.modules.printers.commons.PrinterError
import expo.modules.printers.commons.PrinterStatus
import expo.modules.printers.commons.PrinterStatusUnavailableException
import expo.modules.printers.commons.PrinterUnreachableException
import expo.modules.printers.starmicronics.internals.StarModelCapability
import java.io.ByteArrayInputStream

class StarMicronicsPrinter(
    private val appContext: Context,
) : Printer<PrinterDeviceData.Star, StarMicronicsPrintResult> {

    override suspend fun printImage(
        base64Image: String,
        deviceData: PrinterDeviceData.Star,
        paperWidth: PaperWidth?,
    ): StarMicronicsPrintResult {
        val modelIndex = modelIndexOf(deviceData) ?: return StarMicronicsPrintResult.UnsupportedModel

        return print(
            base64Image = base64Image,
            emulation = StarModelCapability.getEmulation(modelIndex),
            portName = portNameOf(deviceData),
            portSettings = StarModelCapability.getPortSettings(modelIndex),
            paperSize = StarModelCapability.getPaperSize(modelIndex, paperWidth),
        )
    }

    override suspend fun getStatus(deviceData: PrinterDeviceData.Star): PrinterStatus {
        val modelIndex = requireNotNull(modelIndexOf(deviceData)) { "Unsupported Star model: ${deviceData.modelName}" }
        val portName = portNameOf(deviceData)

        val port = try {
            StarIOPort.getPort(
                portName,
                StarModelCapability.getPortSettings(modelIndex),
                CONNECTION_TIMEOUT_MILLISECONDS,
                appContext,
            )
        } catch (throwable: Exception) {
            logPrinterError("open port for status", throwable, portName)
            throw if (mapPortException(throwable, StarMicronicsPrintResult.PortOpenFailed) == StarMicronicsPrintResult.PrinterInUse) {
                PrinterBusyException(throwable)
            } else {
                PrinterUnreachableException(throwable)
            }
        }

        return try {
            port.retreiveStatus().toPrinterStatus()
        } catch (throwable: Exception) {
            logPrinterError("retrieve status", throwable, portName)
            throw PrinterStatusUnavailableException(throwable)
        } finally {
            try {
                StarIOPort.releasePort(port)
            } catch (throwable: Exception) {
                logPrinterError("release port", throwable, portName)
            }
        }
    }

    private fun modelIndexOf(deviceData: PrinterDeviceData.Star): Int? {
        Log.d(
            TAG,
            "Identifying printer modelName='${deviceData.modelName}', portName='${deviceData.portName}'",
        )

        val modelIndex = StarModelCapability.getModelIdx(deviceData.modelName)
            .takeIf { it != StarModelCapability.NONE }
            ?: StarModelCapability.getModelIdxByTitle(deviceData.modelName)

        if (modelIndex == StarModelCapability.NONE) {
            Log.e(
                TAG,
                "Unsupported printer modelName='${deviceData.modelName}', supportedModels=${StarModelCapability.getSupportedModels()}",
            )
            return null
        }
        return modelIndex
    }

    private fun portNameOf(deviceData: PrinterDeviceData.Star): String =
        if (deviceData.connectionType == PrinterConnectionType.Bluetooth) {
            "BT:${deviceData.macAddress}"
        } else {
            deviceData.portName
        }

    private fun StarPrinterStatus.toPrinterStatus() = PrinterStatus(
        online = !offline,
        coverOpen = coverOpen,
        paper = when {
            receiptPaperEmpty -> PaperStatus.EMPTY
            receiptPaperNearEmptyInner || receiptPaperNearEmptyOuter -> PaperStatus.NEAR_END
            else -> PaperStatus.OK
        },
        errors = buildList {
            if (cutterError) add(PrinterError.CUTTER_ERROR)
            if (mechError || headThermistorError) add(PrinterError.MECHANICAL_ERROR)
            if (jamError || presenterPaperJamError) add(PrinterError.PAPER_JAM)
            if (overTemp) add(PrinterError.OVER_TEMPERATURE)
            if (voltageError) add(PrinterError.VOLTAGE_ERROR)
            if (unrecoverableError) add(PrinterError.UNRECOVERABLE_ERROR)
            if (
                pageModeCmdError || paperDetectionError || blackMarkError ||
                headUpError || rollPositionError || receiveBufferOverflow
            ) {
                add(PrinterError.UNKNOWN_ERROR)
            }
        },
    )

    private fun print(
        base64Image: String,
        emulation: StarIoExt.Emulation,
        portName: String,
        portSettings: String,
        paperSize: Int,
    ): StarMicronicsPrintResult {
        val builder = StarIoExt.createCommandBuilder(emulation)
        builder.beginDocument()

        val image = decodeImage(base64Image)
            ?: return StarMicronicsPrintResult.InvalidImage

        builder.appendBitmap(
            image,
            false,
            paperSize,
            true,
            ICommandBuilder.BitmapConverterRotation.Normal,
        )
        builder.appendCutPaper(CutPaperAction.PartialCutWithFeed)
        builder.endDocument()

        val port = try {
            StarIOPort.getPort(
                portName,
                portSettings,
                CONNECTION_TIMEOUT_MILLISECONDS,
                appContext,
            )
        } catch (throwable: Exception) {
            logPrinterError("open port", throwable, portName)
            return mapPortException(throwable, StarMicronicsPrintResult.PortOpenFailed)
        }

        return try {
            val initialResult = mapPrinterStatus(port.beginCheckedBlock())
            if (initialResult != StarMicronicsPrintResult.Success) {
                initialResult
            } else {
                val receiptData = builder.commands
                port.writePort(receiptData, 0, receiptData.size)
                port.setEndCheckedBlockTimeoutMillis(END_CHECKED_BLOCK_TIMEOUT_MILLISECONDS)
                mapPrinterStatus(port.endCheckedBlock())
            }
        } catch (throwable: Exception) {
            logPrinterError("print", throwable, portName)
            mapPortException(throwable, StarMicronicsPrintResult.CommunicationError)
        } finally {
            try {
                StarIOPort.releasePort(port)
            } catch (throwable: Exception) {
                logPrinterError("release port", throwable, portName)
            }
        }
    }

    // appendBitmap scales the image to the paper width and keeps the aspect ratio (bothScale = true).
    private fun decodeImage(base64Image: String): Bitmap? = runCatching {
        val decodedBytes = Base64.decode(base64Image, Base64.DEFAULT)
        BitmapFactory.decodeStream(ByteArrayInputStream(decodedBytes))
            ?.takeIf { it.width > 0 && it.height > 0 }
    }.getOrNull()

    private fun mapPrinterStatus(status: StarPrinterStatus): StarMicronicsPrintResult = when {
        status.coverOpen -> StarMicronicsPrintResult.CoverOpen
        status.receiptPaperEmpty -> StarMicronicsPrintResult.PaperEmpty
        status.jamError -> StarMicronicsPrintResult.PaperJam
        status.cutterError -> StarMicronicsPrintResult.CutterError
        status.headThermistorError -> StarMicronicsPrintResult.HeadThermistorError
        status.voltageError -> StarMicronicsPrintResult.VoltageError
        status.overTemp -> StarMicronicsPrintResult.OverTemperature
        status.receiveBufferOverflow -> StarMicronicsPrintResult.ReceiveBufferOverflow
        status.unrecoverableError -> StarMicronicsPrintResult.HardwareError
        status.offline -> StarMicronicsPrintResult.PrinterOffline
        else -> StarMicronicsPrintResult.Success
    }

    private fun mapPortException(
        throwable: Throwable,
        fallback: StarMicronicsPrintResult,
    ): StarMicronicsPrintResult {
        if (throwable is StarIOPortException && throwable.errorCode == StarResultCode.FAILURE_IN_USE) {
            return StarMicronicsPrintResult.PrinterInUse
        }

        val message = throwable.message.orEmpty()
        return when {
            message.equals(PRINTER_IS_POWER_OFF, ignoreCase = true) -> StarMicronicsPrintResult.PrinterOffline
            message.contains("timed out", ignoreCase = true) -> StarMicronicsPrintResult.ConnectionTimeout
            message.contains("timeout", ignoreCase = true) -> StarMicronicsPrintResult.ConnectionTimeout
            message.contains("no response", ignoreCase = true) -> StarMicronicsPrintResult.ConnectionTimeout
            else -> fallback
        }
    }

    private fun logPrinterError(operation: String, throwable: Throwable, portName: String) {
        val errorCode = (throwable as? StarIOPortException)?.errorCode
        Log.e(
            TAG,
            "Failed to $operation on $portName: type=${throwable::class.java.simpleName}, code=$errorCode, message=${throwable.message}",
            throwable,
        )
    }

    companion object {
        private const val TAG = "StarMicronicsPrinter"
        private const val PRINTER_IS_POWER_OFF = "Printer is power off."
        private const val CONNECTION_TIMEOUT_MILLISECONDS = 10_000
        private const val END_CHECKED_BLOCK_TIMEOUT_MILLISECONDS = 30_000
    }
}

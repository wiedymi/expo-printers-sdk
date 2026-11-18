package expo.modules.printers.starmicronics

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import expo.modules.printers.commons.PrinterLog
import com.starmicronics.stario.StarIOPort
import com.starmicronics.stario.StarIOPortException
import com.starmicronics.stario.StarPrinterStatus
import com.starmicronics.stario.StarResultCode
import com.starmicronics.starioextension.ICommandBuilder
import com.starmicronics.starioextension.ICommandBuilder.CutPaperAction
import com.starmicronics.starioextension.StarIoExt
import expo.modules.printers.commons.PaperStatus
import expo.modules.printers.commons.PaperWidth
import expo.modules.printers.commons.PrintReport
import expo.modules.printers.commons.PrintStepTimeoutException
import expo.modules.printers.commons.PrintSteps
import expo.modules.printers.commons.Printer
import expo.modules.printers.commons.PrinterBusyException
import expo.modules.printers.commons.PrinterConnectionType
import expo.modules.printers.commons.PrinterDeviceData
import expo.modules.printers.commons.PrinterError
import expo.modules.printers.commons.PrinterLocks
import expo.modules.printers.commons.PrinterStatus
import expo.modules.printers.commons.PrinterStatusUnavailableException
import expo.modules.printers.commons.PrinterUnreachableException
import expo.modules.printers.commons.withLockOrBusy
import expo.modules.printers.starmicronics.internals.StarModelCapability
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withTimeoutOrNull
import java.io.ByteArrayInputStream
import kotlin.time.Duration.Companion.seconds

class StarMicronicsPrinter(
    private val appContext: Context,
) : Printer<PrinterDeviceData.Star, StarMicronicsPrintResult> {

    // A second port to a printer in use fails.
    private val printLocks = PrinterLocks()

    override suspend fun printImage(
        base64Image: String,
        deviceData: PrinterDeviceData.Star,
        paperWidth: PaperWidth?,
        jobId: String?,
    ): PrintReport<StarMicronicsPrintResult> {
        val steps = PrintSteps(TAG, jobId, ::vendorCodeOf)
        val result = steps.withPrinterLock(printLocks.of(portNameOf(deviceData))) {
            print(base64Image, deviceData, paperWidth, steps)
        }
        return steps.report(result)
    }

    private suspend fun print(
        base64Image: String,
        deviceData: PrinterDeviceData.Star,
        paperWidth: PaperWidth?,
        steps: PrintSteps,
    ): StarMicronicsPrintResult {
        val modelIndex = modelIndexOf(deviceData)
            ?: return StarMicronicsPrintResult.UnsupportedModel.also { steps.log("result", it.code) }
        val image = decodeImage(base64Image)
            ?: return StarMicronicsPrintResult.InvalidImage.also { steps.log("result", it.code) }

        val commands = buildCommands(
            image = image,
            emulation = StarModelCapability.getEmulation(modelIndex),
            paperSize = StarModelCapability.getPaperSize(modelIndex, paperWidth),
        )
        val portName = portNameOf(deviceData)
        val portSettings = StarModelCapability.getPortSettings(modelIndex)
        steps.log("prepare", "ok port=$portName bytes=${commands.size}")

        val attempts = if (deviceData.connectionType == PrinterConnectionType.Bluetooth) BLUETOOTH_ATTEMPTS else 1
        var attempt = 1
        while (true) {
            val outcome = printOnce(commands, portName, portSettings, steps)
            if (!outcome.canRetry || attempt >= attempts) {
                steps.log("result", outcome.result.code)
                return outcome.result
            }
            attempt++
            steps.log("retry", "${outcome.result.code}, attempt $attempt of $attempts on a new connection")
            steps.clearFailure()
            delay(BLUETOOTH_RETRY_DELAY)
        }
    }

    override suspend fun getStatus(deviceData: PrinterDeviceData.Star): PrinterStatus {
        val modelIndex = requireNotNull(modelIndexOf(deviceData)) { "Unsupported Star model: ${deviceData.modelName}" }
        val portName = portNameOf(deviceData)
        return printLocks.of(portName).withLockOrBusy {
            readStatus(portName, StarModelCapability.getPortSettings(modelIndex))
        }
    }

    private suspend fun readStatus(portName: String, portSettings: String): PrinterStatus {
        val steps = PrintSteps(TAG, jobId = null, ::vendorCodeOf)
        val port = try {
            openPort(portName, portSettings, steps)
        } catch (throwable: Exception) {
            logPrinterError("open port for status", throwable, portName)
            throw if (openPortFailure(throwable) == StarMicronicsPrintResult.PrinterInUse) {
                PrinterBusyException(throwable)
            } else {
                PrinterUnreachableException(throwable)
            }
        }

        return try {
            steps.runWithTimeout("printer_status", PRINTER_STATUS_TIMEOUT) { port.retreiveStatus() }.toPrinterStatus()
        } catch (throwable: Exception) {
            logPrinterError("retrieve status", throwable, portName)
            throw PrinterStatusUnavailableException(throwable)
        } finally {
            releasePort(port, portName, steps)
        }
    }

    // canRetry is true when the attempt failed on the connection before any print data was sent, and not because a
    // time limit ended. A retry after a time limit makes the job, and the jobs that wait for this printer, much longer.
    private class Attempt(val result: StarMicronicsPrintResult, val canRetry: Boolean)

    private suspend fun printOnce(
        commands: ByteArray,
        portName: String,
        portSettings: String,
        steps: PrintSteps,
    ): Attempt {
        val port = try {
            openPort(portName, portSettings, steps)
        } catch (throwable: Exception) {
            logPrinterError("open port", throwable, portName)
            val result = openPortFailure(throwable)
            return Attempt(result, canRetry = result == StarMicronicsPrintResult.PortOpenFailed)
        }

        try {
            val status = try {
                steps.runWithTimeout("printer_status", PRINTER_STATUS_TIMEOUT) { port.beginCheckedBlock() }
            } catch (throwable: Exception) {
                logPrinterError("read status before print", throwable, portName)
                val result = stepFailure(throwable)
                return Attempt(
                    result,
                    canRetry = result != StarMicronicsPrintResult.PrintTimeout &&
                        result != StarMicronicsPrintResult.ConnectionTimeout,
                )
            }
            val statusResult = mapPrinterStatus(status)
            if (statusResult != StarMicronicsPrintResult.Success) {
                steps.log("printer_status", "printer reports ${statusResult.code}")
                return Attempt(statusResult, canRetry = false)
            }

            try {
                steps.runWithTimeout("send_data", SEND_DATA_TIMEOUT) { port.writePort(commands, 0, commands.size) }
            } catch (throwable: Exception) {
                logPrinterError("send data", throwable, portName)
                return Attempt(stepFailure(throwable), canRetry = false)
            }

            port.setEndCheckedBlockTimeoutMillis(END_CHECKED_BLOCK_TIMEOUT_MILLISECONDS)
            val printStatus = try {
                steps.runWithTimeout("print_result", PRINT_RESULT_TIMEOUT) { port.endCheckedBlock() }
            } catch (throwable: Exception) {
                logPrinterError("read print result", throwable, portName)
                return Attempt(stepFailure(throwable), canRetry = false)
            }
            return Attempt(mapPrinterStatus(printStatus), canRetry = false)
        } finally {
            releasePort(port, portName, steps)
        }
    }

    // A job waits for the open that runs and does not add a second open to the queue of the SDK lock.
    private suspend fun openPort(portName: String, portSettings: String, steps: PrintSteps): StarIOPort {
        val waitStartNanos = System.nanoTime()
        if (withTimeoutOrNull(OPEN_PORT_TIMEOUT) { portOpenLock.lock() } == null) {
            val timeoutException = PrintStepTimeoutException("wait_for_open", OPEN_PORT_TIMEOUT)
            steps.failed("wait_for_open", timeoutException, waitStartNanos, cleanup = false)
            throw timeoutException
        }
        steps.log("wait_for_open", "ok", waitStartNanos)

        // After a timeout the open continues, and onLateEnd unlocks when it ends.
        var unlockNow = true
        try {
            return steps.runWithTimeout(
                "open_port",
                OPEN_PORT_TIMEOUT,
                onLateEnd = { result ->
                    result.onSuccess { port -> releasePortAfterTimeout(port, portName) }
                    portOpenLock.unlock()
                },
            ) {
                StarIOPort.getPort(portName, portSettings, CONNECTION_TIMEOUT_MILLISECONDS, appContext)
            }
        } catch (timeoutException: PrintStepTimeoutException) {
            unlockNow = false
            throw timeoutException
        } finally {
            if (unlockNow) portOpenLock.unlock()
        }
    }

    private suspend fun releasePort(port: StarIOPort, portName: String, steps: PrintSteps) {
        try {
            steps.runWithTimeout("release_port", RELEASE_PORT_TIMEOUT, cleanup = true) { StarIOPort.releasePort(port) }
        } catch (throwable: Exception) {
            logPrinterError("release port", throwable, portName)
        }
    }

    // A port that opens after its step timed out is not used, so close it at once.
    private fun releasePortAfterTimeout(port: StarIOPort, portName: String) {
        try {
            StarIOPort.releasePort(port)
        } catch (throwable: Exception) {
            logPrinterError("release port opened after timeout", throwable, portName)
        }
    }

    private fun vendorCodeOf(throwable: Throwable): Int? = (throwable as? StarIOPortException)?.errorCode

    private fun openPortFailure(throwable: Throwable): StarMicronicsPrintResult =
        if (throwable is PrintStepTimeoutException) {
            StarMicronicsPrintResult.ConnectionTimeout
        } else {
            mapPortException(throwable, StarMicronicsPrintResult.PortOpenFailed)
        }

    private fun stepFailure(throwable: Throwable): StarMicronicsPrintResult =
        if (throwable is PrintStepTimeoutException) {
            StarMicronicsPrintResult.PrintTimeout
        } else {
            mapPortException(throwable, StarMicronicsPrintResult.CommunicationError)
        }

    private fun modelIndexOf(deviceData: PrinterDeviceData.Star): Int? {
        PrinterLog.d(
            TAG,
            "Identifying printer modelName='${deviceData.modelName}', portName='${deviceData.portName}'",
        )

        val modelIndex = StarModelCapability.getModelIdx(deviceData.modelName)
            .takeIf { it != StarModelCapability.NONE }
            ?: StarModelCapability.getModelIdxByTitle(deviceData.modelName)

        if (modelIndex == StarModelCapability.NONE) {
            PrinterLog.e(
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

    private fun buildCommands(image: Bitmap, emulation: StarIoExt.Emulation, paperSize: Int): ByteArray {
        val builder = StarIoExt.createCommandBuilder(emulation)
        builder.beginDocument()
        builder.appendBitmap(
            image,
            false,
            paperSize,
            true,
            ICommandBuilder.BitmapConverterRotation.Normal,
        )
        builder.appendCutPaper(CutPaperAction.PartialCutWithFeed)
        builder.endDocument()
        return builder.commands
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
        PrinterLog.e(
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

        // Hard limits for each blocking StarIO call. They are longer than the SDK timeouts above, because a
        // Bluetooth call can block longer than its SDK timeout.
        private val OPEN_PORT_TIMEOUT = 15.seconds
        private val PRINTER_STATUS_TIMEOUT = 10.seconds
        private val SEND_DATA_TIMEOUT = 30.seconds
        private val PRINT_RESULT_TIMEOUT = 35.seconds
        private val RELEASE_PORT_TIMEOUT = 5.seconds

        // StarIOPort.getPort and releasePort are synchronized on the class, so a slow Bluetooth open stops the opens
        // of all Star printers in the app, also after its step timed out. Only one open runs at a time. The lock is
        // released when the getPort call ends, not when its step times out.
        private val portOpenLock = Mutex()

        // A Bluetooth print gets one more attempt on a new connection when no print data was sent.
        private const val BLUETOOTH_ATTEMPTS = 2
        private val BLUETOOTH_RETRY_DELAY = 1.seconds
    }
}

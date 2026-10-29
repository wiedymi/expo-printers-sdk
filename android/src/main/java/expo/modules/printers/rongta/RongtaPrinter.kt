@file:Suppress("DEPRECATION")

package expo.modules.printers.rongta

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.hardware.usb.UsbManager
import android.os.Build
import android.util.Base64
import expo.modules.printers.commons.PrinterLog
import androidx.core.content.ContextCompat
import com.rt.printerlibrary.bean.BluetoothEdrConfigBean
import com.rt.printerlibrary.bean.UsbConfigBean
import com.rt.printerlibrary.bean.WiFiConfigBean
import com.rt.printerlibrary.cmd.EscFactory
import com.rt.printerlibrary.connect.PrinterInterface
import com.rt.printerlibrary.enumerate.BmpPrintMode
import com.rt.printerlibrary.enumerate.CommonEnum
import com.rt.printerlibrary.enumerate.ConnectStateEnum
import com.rt.printerlibrary.enumerate.PrinterAskStatusEnum
import com.rt.printerlibrary.factory.connect.BluetoothFactory
import com.rt.printerlibrary.factory.connect.UsbFactory
import com.rt.printerlibrary.factory.connect.WiFiFactory
import com.rt.printerlibrary.factory.printer.ThermalPrinterFactory
import com.rt.printerlibrary.observer.PrinterObserver
import com.rt.printerlibrary.observer.PrinterObserverManager
import com.rt.printerlibrary.setting.BitmapSetting
import com.rt.printerlibrary.setting.CommonSetting
import com.rt.printerlibrary.utils.ConnectListener
import expo.modules.printers.commons.PaperStatus
import expo.modules.printers.commons.PaperWidth
import expo.modules.printers.commons.PrintReport
import expo.modules.printers.commons.PrintStepTimeoutException
import expo.modules.printers.commons.PrintSteps
import expo.modules.printers.commons.Printer
import expo.modules.printers.commons.PrinterDeviceData
import expo.modules.printers.commons.PrinterError
import expo.modules.printers.commons.PrinterStatus
import expo.modules.printers.commons.PrinterStatusUnavailableException
import expo.modules.printers.commons.PrinterUnreachableException
import expo.modules.printers.rongta.bluetooth.getBluetoothAdapter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

internal typealias PrinterConfigBean = Any

private sealed class ConfigurationResult {
    data class Success(val configBean: PrinterConfigBean) : ConfigurationResult()
    data class Failure(val error: RongtaPrintResult) : ConfigurationResult()
}

class RongtaPrinter(
    private val appContext: Context,
) : Printer<PrinterDeviceData.Rongta, RongtaPrintResult> {
    private val printerFactory = ThermalPrinterFactory()
    private val printer = printerFactory.create()
    private val printMutex = Mutex()

    override suspend fun printImage(
        base64Image: String,
        deviceData: PrinterDeviceData.Rongta,
        paperWidth: PaperWidth?,
        jobId: String?,
    ): PrintReport<RongtaPrintResult> {
        val steps = PrintSteps(TAG, jobId)
        val waitStartNanos = System.nanoTime()
        val result = printMutex.withLock {
            steps.log("wait_for_printer", "ok", waitStartNanos)
            sendPrintJob(base64Image, deviceData, paperWidth, steps).also { result -> steps.log("result", result.code) }
        }
        return steps.report(result)
    }

    private suspend fun sendPrintJob(
        base64Image: String,
        deviceData: PrinterDeviceData.Rongta,
        paperWidth: PaperWidth?,
        steps: PrintSteps,
    ): RongtaPrintResult {
        PrinterLog.i(TAG, "sendPrintJob: Starting print job for deviceData: $deviceData")
        val configBean = when (val configResult = configurePrinter(deviceData)) {
            is ConfigurationResult.Success -> configResult.configBean
            is ConfigurationResult.Failure -> return configResult.error.also {
                PrinterLog.e(TAG, "failed to configure printer - $deviceData, reason: ${configResult.error}")
            }
        }
        PrinterLog.i(TAG, "sendPrintJob: Printer configured, proceeding to decode image")
        val img: Bitmap = runCatching {
            val decodedString = Base64.decode(base64Image, Base64.DEFAULT)
            val inputStream = ByteArrayInputStream(decodedString)
            BitmapFactory.decodeStream(inputStream)
        }.getOrNull() ?: return RongtaPrintResult.ErrorInvalidImage.also {
            PrinterLog.e(TAG, "failed to decode image")
        }
        PrinterLog.i(TAG, "sendPrintJob: Image decoded, connecting with ${CONNECTION_TIMEOUT.inWholeSeconds}s timeout")
        try {
            val connectStartNanos = System.nanoTime()
            val connectResult = connect(configBean, CONNECTION_TIMEOUT, steps)
            steps.log("connect", connectResult.code, connectStartNanos)
            if (connectResult != RongtaPrintResult.Success) return connectResult

            val printingCommand = createImagePrintCommand(img, paperWidth)
                ?: return RongtaPrintResult.ErrorUnknown.also { PrinterLog.e(TAG, "failed to create printing command") }
            PrinterLog.i(TAG, "printing receipt, command size: ${printingCommand.size}")
            try {
                steps.runWithTimeout("send_data", SEND_DATA_TIMEOUT) { printer.writeMsg(printingCommand) }
            } catch (exception: PrintStepTimeoutException) {
                return RongtaPrintResult.ErrorPrintTimeout
            } catch (exception: Exception) {
                PrinterLog.e(TAG, "failed to print receipt - $exception")
                return RongtaPrintResult.ErrorPrint
            }

            val printStartNanos = System.nanoTime()
            val finished = withTimeoutOrNull(PRINT_RESULT_TIMEOUT) {
                delay(PRINT_POLL_FIRST_DELAY)
                while (printer.isPrinting) delay(PRINT_POLL_INTERVAL)
            } != null
            if (!finished) {
                steps.failed("print_result", PrintStepTimeoutException("print_result", PRINT_RESULT_TIMEOUT), printStartNanos, cleanup = false)
                return RongtaPrintResult.ErrorPrintTimeout
            }
            steps.log("print_result", "ok", printStartNanos)
            return RongtaPrintResult.Success
        } finally {
            disconnect(steps)
        }
    }

    override suspend fun getStatus(deviceData: PrinterDeviceData.Rongta): PrinterStatus = printMutex.withLock {
        val steps = PrintSteps(TAG, jobId = null)
        val configBean = when (val configResult = configurePrinter(deviceData)) {
            is ConfigurationResult.Success -> configResult.configBean
            is ConfigurationResult.Failure -> throw PrinterUnreachableException()
        }
        try {
            val connectStartNanos = System.nanoTime()
            val connectResult = connect(configBean, STATUS_CONNECTION_TIMEOUT, steps)
            steps.log("connect", connectResult.code, connectStartNanos)
            if (connectResult != RongtaPrintResult.Success) throw PrinterUnreachableException()

            val reply = steps.measure("printer_status") { requestRealTimeStatus() }
                ?: throw PrinterStatusUnavailableException()
            reply.toPrinterStatus()
        } finally {
            disconnect(steps)
        }
    }

    // Returns Success when the printer is connected, ErrorConnection or ErrorConnectionTimeout otherwise.
    private suspend fun connect(configBean: PrinterConfigBean, timeout: Duration, steps: PrintSteps): RongtaPrintResult =
        withTimeoutOrNull(timeout) {
            suspendCancellableCoroutine { continuation ->
                val isCompleted = AtomicBoolean(false)
                fun completeOnce(result: RongtaPrintResult) {
                    if (isCompleted.compareAndSet(false, true)) continuation.resume(result)
                }

                printer.setConnectListener(object : ConnectListener {
                    override fun onPrinterConnected(configObj: Any?) {
                        PrinterLog.i(TAG, "onPrinterConnected: $configObj, connectState: ${printer.connectState}, thread: ${Thread.currentThread().name}")
                        completeOnce(RongtaPrintResult.Success)
                    }

                    override fun onPrinterDisconnect(configObj: Any?) {
                        PrinterLog.i(TAG, "onPrinterDisconnect: $configObj, connectState: ${printer.connectState}")
                    }

                    override fun onPrinterWritecompletion(configObj: Any?) {
                        PrinterLog.i(TAG, "onPrinterWritecompletion: $configObj, connectState: ${printer.connectState}, thread: ${Thread.currentThread().name}")
                    }
                })
                // printer.connect can block, so it runs on its own thread and the timeout still applies.
                PrintSteps.blockingCalls.launch {
                    PrinterLog.i(TAG, "About to call printer.connect(configBean) on thread: ${Thread.currentThread().name}")
                    runCatching { printer.connect(configBean) }.onFailure { throwable ->
                        PrinterLog.e(TAG, "Exception during printer.connect:  ${throwable.message}", throwable)
                        steps.recordFailure("connect", throwable)
                        completeOnce(RongtaPrintResult.ErrorConnection)
                    }
                }
            }
        } ?: RongtaPrintResult.ErrorConnectionTimeout.also {
            PrinterLog.e(TAG, "CONNECTION TIMEOUT: onPrinterConnected never fired after ${timeout.inWholeSeconds}s")
            steps.recordFailure("connect", PrintStepTimeoutException("connect", timeout))
        }

    private suspend fun disconnect(steps: PrintSteps) {
        printer.setPrintListener(null)
        printer.setConnectListener(null)
        if (printer.connectState == ConnectStateEnum.Connected) {
            runCatching { steps.runWithTimeout("disconnect", DISCONNECT_TIMEOUT, cleanup = true) { printer.disConnect() } }
                .onFailure { throwable -> PrinterLog.e(TAG, "failed to disconnect printer - $throwable") }
        }
    }

    // Sends the ESC/POS real-time status requests DLE EOT 1-4. Returns one reply byte per request,
    // or null if the printer does not send valid replies in time.
    private suspend fun requestRealTimeStatus(): ByteArray? {
        val received = ByteArrayOutputStream()
        val observerManager = PrinterObserverManager.getInstance()
        var replyObserver: PrinterObserver? = null
        try {
            val reply = withTimeoutOrNull(STATUS_REPLY_TIMEOUT) {
                suspendCancellableCoroutine<ByteArray?> { continuation ->
                    replyObserver = object : PrinterObserver {
                        override fun printerObserverCallback(printerInterface: PrinterInterface<*>?, state: Int) = Unit

                        override fun printerReadMsgCallback(printerInterface: PrinterInterface<*>?, bytes: ByteArray?) {
                            if (bytes == null) return
                            synchronized(received) {
                                received.write(bytes)
                                if (received.size() >= STATUS_REQUESTS.size && continuation.isActive) {
                                    continuation.resume(received.toByteArray().copyOf(STATUS_REQUESTS.size))
                                }
                            }
                        }
                    }.also(observerManager::add)
                    printer.setAlwaysReadInputStream(true)

                    val cmd = EscFactory().create()
                    // writeMsg can block, so it runs on its own thread and the reply timeout still applies.
                    PrintSteps.blockingCalls.launch {
                        runCatching {
                            printer.writeMsg(STATUS_REQUESTS.map(cmd::getPrinterStatus).reduce { all, request -> all + request })
                        }.onFailure { throwable ->
                            PrinterLog.e(TAG, "failed to send status request - $throwable")
                            synchronized(received) { if (continuation.isActive) continuation.resume(null) }
                        }
                    }
                }
            }
            PrinterLog.i(TAG, "status reply: ${synchronized(received) { received.toByteArray() }.toHex()}")
            // Every real-time status byte has bit 1 and bit 4 on, and bit 0 and bit 7 off.
            return reply?.takeIf { bytes -> bytes.all { (it.toInt() and 0x93) == 0x12 } }
        } finally {
            replyObserver?.let(observerManager::remove)
            printer.setAlwaysReadInputStream(false)
        }
    }

    // Bits from the ESC/POS DLE EOT specification.
    private fun ByteArray.toPrinterStatus(): PrinterStatus {
        val (printerStatus, offlineCause, errorCause, paperSensor) = map { it.toInt() and 0xFF }
        return PrinterStatus(
            online = (printerStatus and 0x08) == 0,
            coverOpen = (offlineCause and 0x04) != 0,
            paper = when {
                (paperSensor and 0x60) != 0 || (offlineCause and 0x20) != 0 -> PaperStatus.EMPTY
                (paperSensor and 0x0C) != 0 -> PaperStatus.NEAR_END
                else -> PaperStatus.OK
            },
            errors = buildList {
                // Recoverable (mechanical) error, for example a paper jam. Epson's ePOS SDK reports it as MECHANICAL_ERR.
                if ((errorCause and 0x04) != 0) add(PrinterError.MECHANICAL_ERROR)
                if ((errorCause and 0x08) != 0) add(PrinterError.CUTTER_ERROR)
                if ((errorCause and 0x20) != 0) add(PrinterError.UNRECOVERABLE_ERROR)
                if ((errorCause and 0x40) != 0) add(PrinterError.AUTO_RECOVERABLE_ERROR)
                // The offline cause reports an error, but the error cause gives no kind.
                if (isEmpty() && (offlineCause and 0x40) != 0) add(PrinterError.UNKNOWN_ERROR)
            },
        )
    }

    private fun ByteArray.toHex(): String = joinToString(" ") { "%02X".format(it) }

    private fun createImagePrintCommand(image: Bitmap, paperWidth: PaperWidth?): ByteArray? {
        val cmdFactory = EscFactory()
        val cmd = cmdFactory.create()
        cmd.append(cmd.headerCmd)
        val commonSetting = CommonSetting().apply {
            align = CommonEnum.ALIGN_MIDDLE
        }
        cmd.append(cmd.getCommonSettingCmd(commonSetting))
        // getBitmapCmd scales down images wider than bimtapLimitWidth and keeps the aspect ratio.
        val bitmapSettings = BitmapSetting().apply {
            bmpPrintMode = BmpPrintMode.MODE_MULTI_COLOR
            bimtapLimitWidth = when (paperWidth) {
                PaperWidth.MM58 -> PRINTABLE_WIDTH_58_MM
                PaperWidth.MM80, null -> PRINTABLE_WIDTH_80_MM
            }
        }
        try {
            cmd.append(cmd.getBitmapCmd(bitmapSettings, image))
        } catch (e: Exception) {
            PrinterLog.e(TAG, "Failed to build bitmap command", e)
            return null
        }
        cmd.append(cmd.lfcrCmd)
        cmd.append(cmd.cmdCutNew)
        return cmd.appendCmds
    }

    private suspend fun configureUsbPrinter(deviceData: PrinterDeviceData.Rongta.Type.Usb): ConfigurationResult {
        PrinterLog.i(TAG, "configureUsbPrinter: Looking for USB device with vendorId=${deviceData.vendorId}, productId=${deviceData.productId}")
        val usbManager = appContext.getSystemService(Context.USB_SERVICE) as UsbManager
        val usbDevice = usbManager.deviceList.values.find {
            it.vendorId == deviceData.vendorId && it.productId == deviceData.productId
        } ?: return ConfigurationResult.Failure(RongtaPrintResult.ErrorConnection).also {
            PrinterLog.e(TAG, "configureUsbPrinter: USB device not found")
        }

        PrinterLog.i(TAG, "configureUsbPrinter: Found USB device: $usbDevice")
        val usbPrinterInterface = (0 until usbDevice.interfaceCount)
            .map { usbDevice.getInterface(it) }
            .find { it.interfaceClass == 7 }
            ?: usbDevice.getInterface(0)
        PrinterLog.i(TAG, "Selected USB interface: id=${usbPrinterInterface.id}, class=${usbPrinterInterface.interfaceClass}, endpoints=${usbPrinterInterface.endpointCount}")
        val outEndpoint = (0 until usbPrinterInterface.endpointCount)
            .map { usbPrinterInterface.getEndpoint(it) }
            .find { it.direction == 0 }
        if (outEndpoint == null) {
            PrinterLog.e(TAG, "No OUT endpoint found on selected interface!")
            return ConfigurationResult.Failure(RongtaPrintResult.ErrorConnection)
        }
        PrinterLog.i(TAG, "Selected OUT endpoint: address=${outEndpoint.address}, type=${outEndpoint.type}, direction=${outEndpoint.direction}, maxPacketSize=${outEndpoint.maxPacketSize}")
        for (i in 0 until usbDevice.interfaceCount) {
            val intf = usbDevice.getInterface(i)
            PrinterLog.i(TAG, "USB Interface $i: class=${intf.interfaceClass}, endpoints=${intf.endpointCount}")
            for (j in 0 until intf.endpointCount) {
                val ep = intf.getEndpoint(j)
                PrinterLog.i(TAG, "  Endpoint $j: address=${ep.address}, type=${ep.type}, direction=${ep.direction}, maxPacketSize=${ep.maxPacketSize}")
            }
        }
        if (!usbManager.hasPermission(usbDevice)) {
            PrinterLog.i(TAG, "configureUsbPrinter: No permission for USB device, requesting permission")
            // The print waits under printMutex, so a dialog left open must not block the next prints.
            val permissionResult = withTimeoutOrNull(USB_PERMISSION_TIMEOUT) { requestUsbPermission(usbManager, usbDevice) } ?: false
            PrinterLog.i(TAG, "configureUsbPrinter: Permission result: $permissionResult")
            if (!permissionResult) {
                PrinterLog.e(TAG, "USB permission denied for device: $usbDevice")
                return ConfigurationResult.Failure(RongtaPrintResult.ErrorPermission)
            }
        } else {
            PrinterLog.i(TAG, "configureUsbPrinter: Already have permission for USB device")
        }
        val usbPermissionAction = appContext.applicationInfo.packageName
        val permissionIntent = PendingIntent.getBroadcast(
            appContext,
            0,
            Intent(usbPermissionAction),
            PendingIntent.FLAG_IMMUTABLE
        )
        val configBean = UsbConfigBean(appContext, usbDevice, permissionIntent)
        val usbFactory = UsbFactory()
        val printerInterface = usbFactory.create()
        printerInterface.configObject = configBean
        printer.setPrinterInterface(printerInterface)
        // Disconnect if already connected (after interface is set)
        runCatching {
            if (printer.connectState == ConnectStateEnum.Connected) {
                PrinterLog.i(TAG, "Printer already connected (USB). Disconnecting before reconnecting.")
                runCatching { printer.disConnect() }
                    .onFailure { throwable ->
                        PrinterLog.e(TAG, "Failed to disconnect printer before reconnecting (USB): $throwable")
                    }
            }
        }.onFailure { throwable ->
            PrinterLog.e(TAG, "Failed to check connection state (USB): $throwable")
        }
        PrinterLog.i(TAG, "USB printer configured: $usbDevice, configBean: $configBean, printerInterface: $printerInterface")
        return ConfigurationResult.Success(configBean)
    }

    private suspend fun requestUsbPermission(usbManager: UsbManager, usbDevice: android.hardware.usb.UsbDevice): Boolean = suspendCancellableCoroutine { cont ->
        PrinterLog.i(TAG, "requestUsbPermission: Registering USB permission receiver")
        val usbPermissionAction = appContext.applicationInfo.packageName
        val permissionIntent = PendingIntent.getBroadcast(appContext, 0, Intent(usbPermissionAction), PendingIntent.FLAG_IMMUTABLE)
        var isReceiverUnregistered = false
        var receiver: BroadcastReceiver? = null

        fun unregisterReceiverSafe() {
            if (!isReceiverUnregistered) {
                try { appContext.unregisterReceiver(receiver) } catch (_: Exception) {}
                isReceiverUnregistered = true
                PrinterLog.i(TAG, "requestUsbPermission: Receiver unregistered")
            }
        }

        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                PrinterLog.i(TAG, "requestUsbPermission: Receiver triggered with intent: $intent")
                if (intent?.action != usbPermissionAction) return
                unregisterReceiverSafe()
                // The PendingIntent is immutable, so the system cannot add EXTRA_DEVICE and EXTRA_PERMISSION_GRANTED.
                val granted = usbManager.hasPermission(usbDevice)
                PrinterLog.i(TAG, "requestUsbPermission: Permission granted: $granted")
                if (cont.isActive) {
                    cont.resume(granted)
                }
            }
        }
        val contextToRegister = try {
            val activityField = appContext.javaClass.getDeclaredField("currentActivity")
            activityField.isAccessible = true
            val activity = activityField.get(appContext) as? Context
            if (activity != null) {
                PrinterLog.i(TAG, "requestUsbPermission: Registering receiver with current Activity context")
                activity
            } else {
                PrinterLog.i(TAG, "requestUsbPermission: currentActivity is null, using appContext")
                appContext
            }
        } catch (e: Exception) {
            PrinterLog.i(TAG, "requestUsbPermission: Could not get currentActivity, using appContext. Reason: ${e.message}")
            appContext
        }
        // USB permission broadcasts come from the system, so we need RECEIVER_EXPORTED
        if (Build.VERSION.SDK_INT >= 33) {
            contextToRegister.registerReceiver(receiver, IntentFilter(usbPermissionAction), Context.RECEIVER_EXPORTED)
        } else {
            contextToRegister.registerReceiver(receiver, IntentFilter(usbPermissionAction))
        }
        PrinterLog.i(TAG, "requestUsbPermission: Requesting permission for device: $usbDevice")
        usbManager.requestPermission(usbDevice, permissionIntent)
        cont.invokeOnCancellation {
            PrinterLog.i(TAG, "requestUsbPermission: Coroutine cancelled, unregistering receiver")
            unregisterReceiverSafe()
        }
    }

    private suspend fun configurePrinter(
        deviceData: PrinterDeviceData.Rongta
    ): ConfigurationResult {
        PrinterLog.i(TAG, "configuring printer - $deviceData")
        return when (deviceData.type) {
            is PrinterDeviceData.Rongta.Type.Bluetooth -> configureBTPrinter(deviceData.type)
            is PrinterDeviceData.Rongta.Type.Network -> configureNetworkPrinter(deviceData.type)
            is PrinterDeviceData.Rongta.Type.Usb -> configureUsbPrinter(deviceData.type)
        }
    }

    @SuppressLint("MissingPermission")
    private fun configureBTPrinter(deviceData: PrinterDeviceData.Rongta.Type.Bluetooth): ConfigurationResult {
        val btAdapter = appContext.getBluetoothAdapter() ?: return ConfigurationResult.Failure(RongtaPrintResult.ErrorConnection).also {
            PrinterLog.e(TAG, "failed to get bluetooth adapter")
        }
        val device = btAdapter.getRemoteDevice(deviceData.address)
        val btFactory = BluetoothFactory()
        val configBean = BluetoothEdrConfigBean(device)
        val printerInterface = btFactory.create()
        printerInterface.configObject = configBean
        printer.setPrinterInterface(printerInterface)
        // Disconnect if already connected (after interface is set)
        runCatching {
            if (printer.connectState == ConnectStateEnum.Connected) {
                PrinterLog.i(TAG, "Printer already connected (Bluetooth). Disconnecting before reconnecting.")
                runCatching { printer.disConnect() }
                    .onFailure { throwable ->
                        PrinterLog.e(TAG, "Failed to disconnect printer before reconnecting (Bluetooth): $throwable")
                    }
            }
        }.onFailure { throwable ->
            PrinterLog.e(TAG, "Failed to check connection state (Bluetooth): $throwable")
        }
        return ConfigurationResult.Success(configBean)
    }

    private fun configureNetworkPrinter(deviceData: PrinterDeviceData.Rongta.Type.Network): ConfigurationResult {
        val configBean = WiFiConfigBean(
            deviceData.ipAddress,
            deviceData.port
        )
        val wiFiFactory = WiFiFactory()
        val printerInterface = wiFiFactory.create()
        printerInterface.configObject = configBean
        printer.setPrinterInterface(printerInterface)
        // Disconnect if already connected (after interface is set)
        runCatching {
            if (printer.connectState == ConnectStateEnum.Connected) {
                PrinterLog.i(TAG, "Printer already connected (Network). Disconnecting before reconnecting.")
                runCatching { printer.disConnect() }
                    .onFailure { throwable ->
                        PrinterLog.e(TAG, "Failed to disconnect printer before reconnecting (Network): $throwable")
                    }
            }
        }.onFailure { throwable ->
            PrinterLog.e(TAG, "Failed to check connection state (Network): $throwable")
        }
        return ConfigurationResult.Success(configBean)
    }

    companion object {
        private const val TAG = "RongtaPrinter"
        // The SDK cannot report the paper width. 203 dpi: 48 mm printable on 58 mm paper, 72 mm on 80 mm paper.
        private const val PRINTABLE_WIDTH_58_MM = 384
        private const val PRINTABLE_WIDTH_80_MM = 576
        private val CONNECTION_TIMEOUT = 30.seconds
        private val USB_PERMISSION_TIMEOUT = 60.seconds
        private val PRINT_POLL_FIRST_DELAY = 2.seconds
        private val PRINT_POLL_INTERVAL = 500.milliseconds
        private val STATUS_CONNECTION_TIMEOUT = 10.seconds
        private val STATUS_REPLY_TIMEOUT = 3.seconds
        private val SEND_DATA_TIMEOUT = 30.seconds
        private val PRINT_RESULT_TIMEOUT = 60.seconds
        private val DISCONNECT_TIMEOUT = 5.seconds

        // DLE EOT 1 to 4: printer status, offline cause, error cause, paper sensor.
        private val STATUS_REQUESTS = listOf(
            PrinterAskStatusEnum.Printer_Status,
            PrinterAskStatusEnum.Offline_status,
            PrinterAskStatusEnum.Trans_status,
            PrinterAskStatusEnum.Paper_status,
        )
    }
}
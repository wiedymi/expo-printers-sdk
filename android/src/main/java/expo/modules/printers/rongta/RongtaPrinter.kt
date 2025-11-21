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
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import androidx.core.content.ContextCompat
import com.rt.printerlibrary.bean.BluetoothEdrConfigBean
import com.rt.printerlibrary.bean.UsbConfigBean
import com.rt.printerlibrary.bean.WiFiConfigBean
import com.rt.printerlibrary.cmd.EscFactory
import com.rt.printerlibrary.enumerate.BmpPrintMode
import com.rt.printerlibrary.enumerate.CommonEnum
import com.rt.printerlibrary.enumerate.ConnectStateEnum
import com.rt.printerlibrary.factory.connect.BluetoothFactory
import com.rt.printerlibrary.factory.connect.UsbFactory
import com.rt.printerlibrary.factory.connect.WiFiFactory
import com.rt.printerlibrary.factory.printer.ThermalPrinterFactory
import com.rt.printerlibrary.setting.BitmapSetting
import com.rt.printerlibrary.setting.CommonSetting
import com.rt.printerlibrary.utils.ConnectListener
import expo.modules.printers.commons.Printer
import expo.modules.printers.commons.PrinterDeviceData
import expo.modules.printers.rongta.bluetooth.getBluetoothAdapter
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.ByteArrayInputStream
import kotlin.coroutines.resume
import androidx.core.graphics.scale

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
    private val handler: Handler = Handler(Looper.getMainLooper())
    private val printMutex = Mutex()

    override suspend fun printImage(
        base64Image: String,
        deviceData: PrinterDeviceData.Rongta
    ): RongtaPrintResult = printMutex.withLock {
        return@withLock sendPrintJob(base64Image, deviceData)
    }

    private suspend fun sendPrintJob(
        base64Image: String,
        deviceData: PrinterDeviceData.Rongta
    ): RongtaPrintResult {
        Log.i(TAG, "sendPrintJob: Starting print job for deviceData: $deviceData")
        val configBean = when (val configResult = configurePrinter(deviceData)) {
            is ConfigurationResult.Success -> configResult.configBean
            is ConfigurationResult.Failure -> return configResult.error.also {
                Log.e(TAG, "failed to configure printer - $deviceData, reason: ${configResult.error}")
            }
        }
        Log.i(TAG, "sendPrintJob: Printer configured, proceeding to decode image")
        val img: Bitmap = runCatching {
            val decodedString = Base64.decode(base64Image, Base64.DEFAULT)
            val inputStream = ByteArrayInputStream(decodedString)
            BitmapFactory.decodeStream(inputStream)
        }.getOrNull() ?: return RongtaPrintResult.ErrorInvalidImage.also {
            Log.e(TAG, "failed to decode image")
        }
        Log.i(TAG, "sendPrintJob: Image decoded, starting print coroutine")
        val result = suspendCancellableCoroutine { continuation ->
            val isCompleted = java.util.concurrent.atomic.AtomicBoolean(false)
            fun completeOnce(result: RongtaPrintResult) {
                if (isCompleted.compareAndSet(false, true)) {
                    printer.setPrintListener(null)
                    printer.setConnectListener(null)
                    if (printer.connectState == ConnectStateEnum.Connected) {
                        runCatching { printer.disConnect() }
                            .onFailure { throwable ->
                                Log.e(TAG, "failed to disconnect printer - $throwable")
                            }
                    }
                    Log.i(TAG, "sendPrintJob: Completing print job with result: $result")
                    continuation.resumeWith(Result.success(result))
                }
            }
            printer.setConnectListener(object : ConnectListener {
                override fun onPrinterConnected(configObj: Any?) {
                    Log.i(TAG, "onPrinterConnected: $configObj, connectState: ${printer.connectState}, thread: ${Thread.currentThread().name}")
                    val printingCommand = createImagePrintCommand(img)
                    if (printingCommand == null) {
                        Log.e(TAG, "failed to create printing command")
                        completeOnce(RongtaPrintResult.ErrorUnknown)
                    } else {
                        Log.i(TAG, "printing receipt, command size: ${printingCommand.size}")
                        runCatching {
                            printer.writeMsg(printingCommand)
                            Log.i(TAG, "printer.writeMsg called successfully")

                            fun pollPrintCompletion() {
                                if (!printer.isPrinting) {
                                    Log.i(TAG, "Polling detected print job is finished.")
                                    completeOnce(RongtaPrintResult.Success)
                                } else {
                                    handler.postDelayed({ pollPrintCompletion() }, 500)
                                }
                            }
                            handler.postDelayed({ pollPrintCompletion() }, 2000)
                            Log.i(TAG, "pollPrintCompletion called")
                        }.onFailure { throwable ->
                            Log.e(TAG, "failed to print receipt - $throwable")
                            completeOnce(RongtaPrintResult.ErrorPrint)
                        }
                    }
                }
                override fun onPrinterDisconnect(configObj: Any?) {
                    Log.i(TAG, "onPrinterDisconnect: $configObj, connectState: ${printer.connectState}, thread: ${printer.connectState}")
                }
                
                override fun onPrinterWritecompletion(configObj: Any?) {
                    Log.i(TAG, "onPrinterWritecompletion: $configObj, connectState: ${printer.connectState}, thread: ${Thread.currentThread().name}")
                
                }
            })
            Log.i(TAG, "About to call printer.connect(configBean) on thread: ${Thread.currentThread().name}")
            runCatching {
                printer.connect(configBean)
                Log.i(TAG, "printer.connect(configBean) called successfully")
            }.onFailure { throwable ->
                Log.e(TAG, "Exception during printer.connect:  ${throwable.message}", throwable)
                completeOnce(RongtaPrintResult.ErrorConnection)
            }
            continuation.invokeOnCancellation {
                Log.i(TAG, "canceling printing - connection state ${printer.connectState}")
                if (printer.connectState == ConnectStateEnum.Connected) {
                    runCatching { printer.disConnect() }
                        .onFailure { throwable ->
                            Log.e(TAG, "failed to disconnect printer - $throwable")
                        }
                        .getOrNull()
                }
            }
        }
        Log.i(TAG, "sendPrintJob: Print coroutine finished with result: $result")
        return result
    }

    private fun createImagePrintCommand(image: Bitmap): ByteArray? {
        val scaledImage = scaleBitmap(image)
        val cmdFactory = EscFactory()
        val cmd = cmdFactory.create()
        cmd.append(cmd.headerCmd)
        val commonSetting = CommonSetting().apply {
            align = CommonEnum.ALIGN_MIDDLE
        }
        cmd.append(cmd.getCommonSettingCmd(commonSetting))
        val bitmapSettings = BitmapSetting().apply {
            bmpPrintMode = BmpPrintMode.MODE_MULTI_COLOR
            bimtapLimitWidth = scaledImage.width
        }
        try {
            cmd.append(cmd.getBitmapCmd(bitmapSettings, scaledImage))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to build bitmap command", e)
            return null
        }
        cmd.append(cmd.lfcrCmd)
        cmd.append(cmd.cmdCutNew)
        return cmd.appendCmds
    }

    private fun scaleBitmap(bitmap: Bitmap): Bitmap {
        val newWidth = MAX_PRINTER_WIDTH
        if (bitmap.width <= newWidth) {
            return bitmap
        }
        val newHeight = (bitmap.height.toFloat() * newWidth / bitmap.width.toFloat()).toInt()
        return bitmap.scale(newWidth, newHeight)
    }

    private suspend fun configureUsbPrinter(deviceData: PrinterDeviceData.Rongta.Type.Usb): ConfigurationResult {
        Log.i(TAG, "configureUsbPrinter: Looking for USB device with vendorId=${deviceData.vendorId}, productId=${deviceData.productId}")
        val usbManager = appContext.getSystemService(Context.USB_SERVICE) as UsbManager
        val usbDevice = usbManager.deviceList.values.find {
            it.vendorId == deviceData.vendorId && it.productId == deviceData.productId
        } ?: return ConfigurationResult.Failure(RongtaPrintResult.ErrorConnection).also {
            Log.e(TAG, "configureUsbPrinter: USB device not found")
        }

        Log.i(TAG, "configureUsbPrinter: Found USB device: $usbDevice")
        val usbPrinterInterface = (0 until usbDevice.interfaceCount)
            .map { usbDevice.getInterface(it) }
            .find { it.interfaceClass == 7 }
            ?: usbDevice.getInterface(0)
        Log.i(TAG, "Selected USB interface: id=${usbPrinterInterface.id}, class=${usbPrinterInterface.interfaceClass}, endpoints=${usbPrinterInterface.endpointCount}")
        val outEndpoint = (0 until usbPrinterInterface.endpointCount)
            .map { usbPrinterInterface.getEndpoint(it) }
            .find { it.direction == 0 }
        if (outEndpoint == null) {
            Log.e(TAG, "No OUT endpoint found on selected interface!")
            return ConfigurationResult.Failure(RongtaPrintResult.ErrorConnection)
        }
        Log.i(TAG, "Selected OUT endpoint: address=${outEndpoint.address}, type=${outEndpoint.type}, direction=${outEndpoint.direction}, maxPacketSize=${outEndpoint.maxPacketSize}")
        for (i in 0 until usbDevice.interfaceCount) {
            val intf = usbDevice.getInterface(i)
            Log.i(TAG, "USB Interface $i: class=${intf.interfaceClass}, endpoints=${intf.endpointCount}")
            for (j in 0 until intf.endpointCount) {
                val ep = intf.getEndpoint(j)
                Log.i(TAG, "  Endpoint $j: address=${ep.address}, type=${ep.type}, direction=${ep.direction}, maxPacketSize=${ep.maxPacketSize}")
            }
        }
        if (!usbManager.hasPermission(usbDevice)) {
            Log.i(TAG, "configureUsbPrinter: No permission for USB device, requesting permission")
            val permissionResult = requestUsbPermission(usbManager, usbDevice)
            Log.i(TAG, "configureUsbPrinter: Permission result: $permissionResult")
            if (!permissionResult) {
                Log.e(TAG, "USB permission denied for device: $usbDevice")
                return ConfigurationResult.Failure(RongtaPrintResult.ErrorConnection)
            }
        } else {
            Log.i(TAG, "configureUsbPrinter: Already have permission for USB device")
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
        // (No disconnect logic for USB, as connectState is not safe to access)
        Log.i(TAG, "USB printer configured: $usbDevice, configBean: $configBean, printerInterface: $printerInterface")
        return ConfigurationResult.Success(configBean)
    }

    private suspend fun requestUsbPermission(usbManager: UsbManager, usbDevice: android.hardware.usb.UsbDevice): Boolean = suspendCancellableCoroutine { cont ->
        Log.i(TAG, "requestUsbPermission: Registering USB permission receiver")
        val usbPermissionAction = appContext.applicationInfo.packageName
        val permissionIntent = PendingIntent.getBroadcast(appContext, 0, Intent(usbPermissionAction), PendingIntent.FLAG_IMMUTABLE)
        var isReceiverUnregistered = false
        var receiver: BroadcastReceiver? = null

        fun unregisterReceiverSafe() {
            if (!isReceiverUnregistered) {
                try { appContext.unregisterReceiver(receiver) } catch (_: Exception) {}
                isReceiverUnregistered = true
                Log.i(TAG, "requestUsbPermission: Receiver unregistered")
            }
        }

        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                Log.i(TAG, "requestUsbPermission: Receiver triggered with intent: $intent")
                if (intent?.action == usbPermissionAction) {
                    val device: android.hardware.usb.UsbDevice? = intent?.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                    Log.i(TAG, "requestUsbPermission: Received permission result for device: $device")
                    if (device != null && device == usbDevice) {
                        unregisterReceiverSafe()
                        val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                        Log.i(TAG, "requestUsbPermission: Permission granted: $granted")
                        if (cont.isActive) {
                            Log.i(TAG, "requestUsbPermission: Resuming coroutine with granted=$granted")
                            cont.resume(granted)
                        }
                    }
                }
            }
        }
        val contextToRegister = try {
            val activityField = appContext.javaClass.getDeclaredField("currentActivity")
            activityField.isAccessible = true
            val activity = activityField.get(appContext) as? Context
            if (activity != null) {
                Log.i(TAG, "requestUsbPermission: Registering receiver with current Activity context")
                activity
            } else {
                Log.i(TAG, "requestUsbPermission: currentActivity is null, using appContext")
                appContext
            }
        } catch (e: Exception) {
            Log.i(TAG, "requestUsbPermission: Could not get currentActivity, using appContext. Reason: ${e.message}")
            appContext
        }
        if (Build.VERSION.SDK_INT >= 33) {
            contextToRegister.registerReceiver(receiver, IntentFilter(usbPermissionAction), Context.RECEIVER_NOT_EXPORTED)
        } else {
            ContextCompat.registerReceiver(
                contextToRegister,
                receiver,
                IntentFilter(usbPermissionAction),
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
        }
        Log.i(TAG, "requestUsbPermission: Requesting permission for device: $usbDevice")
        usbManager.requestPermission(usbDevice, permissionIntent)
        cont.invokeOnCancellation {
            Log.i(TAG, "requestUsbPermission: Coroutine cancelled, unregistering receiver")
            unregisterReceiverSafe()
        }
    }

    private suspend fun configurePrinter(
        deviceData: PrinterDeviceData.Rongta
    ): ConfigurationResult {
        Log.i(TAG, "configuring printer - $deviceData")
        return when (deviceData.type) {
            is PrinterDeviceData.Rongta.Type.Bluetooth -> configureBTPrinter(deviceData.type)
            is PrinterDeviceData.Rongta.Type.Network -> configureNetworkPrinter(deviceData.type)
            is PrinterDeviceData.Rongta.Type.Usb -> configureUsbPrinter(deviceData.type)
        }
    }

    @SuppressLint("MissingPermission")
    private fun configureBTPrinter(deviceData: PrinterDeviceData.Rongta.Type.Bluetooth): ConfigurationResult {
        val btAdapter = appContext.getBluetoothAdapter() ?: return ConfigurationResult.Failure(RongtaPrintResult.ErrorConnection).also {
            Log.e(TAG, "failed to get bluetooth adapter")
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
                Log.i(TAG, "Printer already connected (Bluetooth). Disconnecting before reconnecting.")
                runCatching { printer.disConnect() }
                    .onFailure { throwable ->
                        Log.e(TAG, "Failed to disconnect printer before reconnecting (Bluetooth): $throwable")
                    }
            }
        }.onFailure { throwable ->
            Log.e(TAG, "Failed to check connection state (Bluetooth): $throwable")
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
                Log.i(TAG, "Printer already connected (Network). Disconnecting before reconnecting.")
                runCatching { printer.disConnect() }
                    .onFailure { throwable ->
                        Log.e(TAG, "Failed to disconnect printer before reconnecting (Network): $throwable")
                    }
            }
        }.onFailure { throwable ->
            Log.e(TAG, "Failed to check connection state (Network): $throwable")
        }
        return ConfigurationResult.Success(configBean)
    }

    companion object {
        private const val TAG = "RongtaPrinter"
        private const val MAX_PRINTER_WIDTH = 576
    }
}
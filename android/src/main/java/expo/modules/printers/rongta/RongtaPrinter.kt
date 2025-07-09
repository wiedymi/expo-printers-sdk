package expo.modules.printers.rongta

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import com.rt.printerlibrary.bean.BluetoothEdrConfigBean
import com.rt.printerlibrary.bean.UsbConfigBean
import com.rt.printerlibrary.bean.WiFiConfigBean
import com.rt.printerlibrary.cmd.EscFactory
import com.rt.printerlibrary.enumerate.BmpPrintMode
import com.rt.printerlibrary.enumerate.BluetoothType
import com.rt.printerlibrary.enumerate.CommonEnum
import com.rt.printerlibrary.enumerate.ConnectStateEnum
import com.rt.printerlibrary.factory.connect.BluetoothFactory
import com.rt.printerlibrary.factory.connect.UsbFactory
import com.rt.printerlibrary.factory.connect.WiFiFactory
import com.rt.printerlibrary.factory.printer.ThermalPrinterFactory
import com.rt.printerlibrary.setting.BitmapSetting
import com.rt.printerlibrary.setting.CommonSetting
import com.rt.printerlibrary.utils.ConnectListener
import com.rt.printerlibrary.utils.PrintListener
import com.rt.printerlibrary.utils.PrintStatusCmd
import expo.modules.printers.commons.Printer
import expo.modules.printers.commons.PrinterDeviceData
import expo.modules.printers.rongta.bluetooth.getBluetoothAdapter
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.ByteArrayInputStream
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

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

        val configResult = configurePrinter(deviceData)
        val configBean = when (configResult) {
            is ConfigurationResult.Success -> configResult.configBean
            is ConfigurationResult.Failure -> return configResult.error.also {
                Log.e(TAG, "failed to configure printer - $deviceData, reason: ${configResult.error}")
            }
        }

        val img: Bitmap = runCatching {
            val decodedString = Base64.decode(base64Image, Base64.DEFAULT)
            val inputStream = ByteArrayInputStream(decodedString)
            BitmapFactory.decodeStream(inputStream)
        }.getOrNull() ?: return RongtaPrintResult.ErrorInvalidImage.also {
            Log.e(TAG, "failed to decode image")
        }

        val result = suspendCancellableCoroutine<RongtaPrintResult> { continuation ->
            val isCompleted = java.util.concurrent.atomic.AtomicBoolean(false)

            fun completeOnce(result: RongtaPrintResult) {
                if (isCompleted.compareAndSet(false, true)) {
                    printer.setPrintListener(null)
                    continuation.resumeWith(Result.success(result))
                }
            }

            printer.setConnectListener(object : ConnectListener {
                override fun onPrinterConnected(configObj: Any?) {
                    Log.i(TAG, "printer connected - $configObj")
                    val printingCommand = createImagePrintCommand(img)
                    if (printingCommand == null) {
                        Log.e(TAG, "failed to create printing command")
                        completeOnce(RongtaPrintResult.ErrorUnknown)
                    } else {
                        Log.i(TAG, "printing receipt")
                        runCatching {
                            printer.writeMsg(printingCommand)
                        }.onFailure { throwable ->
                            Log.e(TAG, "failed to print receipt - $throwable")
                            completeOnce(RongtaPrintResult.ErrorConnection)
                        }
                    }
                }

                override fun onPrinterDisconnect(configObj: Any?) {
                    Log.i(TAG, "printer disconnected - $configObj")
                    completeOnce(RongtaPrintResult.Success)
                }

                override fun onPrinterWritecompletion(configObj: Any?) {
                    Log.i(TAG, "printer write completion")
                    val cmdFactory = EscFactory()
                    val cmd = cmdFactory.create()
                    val statusCmd = cmd.getPrintStausCmd(PrintStatusCmd.cmd_PrintFinish)
                    printer.writeMsg(statusCmd)
                }
            })

            printer.setPrintListener(object : PrintListener {
                override fun onPrinterStatus(statusBean: com.rt.printerlibrary.bean.PrinterStatusBean?) {
                    if (statusBean == null) {
                        return
                    }

                    if (!statusBean.blPrinting) {
                        Log.i(TAG, "Printer is no longer busy, disconnecting.")
                        runCatching { printer.disConnect() }
                    }
                }
            })

            runCatching {
                Log.i(TAG, "connecting to printer - $configBean")
                printer.connect(configBean)
            }.onFailure { throwable ->
                Log.e(TAG, "failed to connect to printer - $throwable")
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

        return result
    }

    private fun createImagePrintCommand(image: Bitmap): ByteArray? {
        val scaledImage = scaleBitmap(image)
        // Build the ESC/POS command following Rongta's official example
        val cmdFactory = EscFactory()
        val cmd = cmdFactory.create()

        // Header (initialization)
        cmd.append(cmd.headerCmd)

        // Center align the content just like the sample implementation
        val commonSetting = CommonSetting().apply {
            align = CommonEnum.ALIGN_MIDDLE
        }
        cmd.append(cmd.getCommonSettingCmd(commonSetting))

        // Bitmap settings – multi-color mode with width limited to the bitmap's width
        val bitmapSettings = BitmapSetting().apply {
            bmpPrintMode = BmpPrintMode.MODE_MULTI_COLOR
            // Library expects dot width, so we clamp to the bitmap's width
            bimtapLimitWidth = scaledImage.width
        }

        try {
            cmd.append(cmd.getBitmapCmd(bitmapSettings, scaledImage))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to build bitmap command", e)
            return null
        }

        // Line-feed and final cut
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
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    private fun configurePrinter(
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

        return ConfigurationResult.Success(configBean)
    }

    private fun configureUsbPrinter(deviceData: PrinterDeviceData.Rongta.Type.Usb): ConfigurationResult {
        val usbManager = appContext.getSystemService(Context.USB_SERVICE) as UsbManager
        val usbDevice = usbManager.deviceList.values.find {
            it.vendorId == deviceData.vendorId && it.productId == deviceData.productId
        } ?: return ConfigurationResult.Failure(RongtaPrintResult.ErrorConnection).also {
            Log.e(TAG, "USB device not found for VID: ${deviceData.vendorId} PID: ${deviceData.productId}")
        }

        if (!usbManager.hasPermission(usbDevice)) {
            return ConfigurationResult.Failure(RongtaPrintResult.ErrorPermission).also {
                Log.e(TAG, "No permission for USB device ${deviceData.name}")
            }
        }

        val configBean = UsbConfigBean(appContext, usbDevice, null)
        val usbFactory = UsbFactory()
        val printerInterface = usbFactory.create()
        printerInterface.configObject = configBean
        printer.setPrinterInterface(printerInterface)

        return ConfigurationResult.Success(configBean)
    }

    companion object {
        private const val TAG = "RongtaPrinter"
        private const val MAX_PRINTER_WIDTH = 576 // 80mm printer
    }
}
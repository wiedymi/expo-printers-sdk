package expo.modules.printers.rongta

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import expo.modules.printers.commons.Printer
import expo.modules.printers.commons.PrinterConnectionType
import expo.modules.printers.commons.PrinterDeviceData
import expo.modules.printers.rongta.bluetooth.getBluetoothAdapter
import com.rt.printerlibrary.bean.BluetoothEdrConfigBean
import com.rt.printerlibrary.bean.WiFiConfigBean
import com.rt.printerlibrary.cmd.EscFactory
import com.rt.printerlibrary.enumerate.BmpPrintMode
import com.rt.printerlibrary.enumerate.CommonEnum
import com.rt.printerlibrary.enumerate.ConnectStateEnum
import com.rt.printerlibrary.enumerate.PageLengthEnum
import com.rt.printerlibrary.factory.connect.BluetoothFactory
import com.rt.printerlibrary.factory.connect.WiFiFactory
import com.rt.printerlibrary.factory.printer.ThermalPrinterFactory
import com.rt.printerlibrary.setting.BitmapSetting
import com.rt.printerlibrary.setting.CommonSetting
import com.rt.printerlibrary.utils.ConnectListener
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.ByteArrayInputStream
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal typealias PrinterConfigBean = Any

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

        val configBean =
            configurePrinter(deviceData) ?: return RongtaPrintResult.ErrorConnection.also {
                Log.e(TAG, "failed to configure printer - $deviceData")
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
                    // After write completion ask the library to close the connection; we'll mark success in onPrinterDisconnect
                    handler.postDelayed({
                        runCatching { printer.disConnect() }
                    }, 500) // small delay to ensure internal buffer is flushed
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
        val cmdFactory = EscFactory()
        val cmd = cmdFactory.create()

        cmd.append(byteArrayOf(0x1B, 0x40)) // reset printer
        cmd.append(cmd.headerCmd)

        val commonSetting = CommonSetting().apply {
            align = CommonEnum.ALIGN_LEFT
        }
        cmd.append(cmd.getCommonSettingCmd(commonSetting))


        val targetWidth = 510 

        val bitmapSettings = BitmapSetting().apply {
            bmpPrintMode = BmpPrintMode.MODE_DITHER_DOUBLE_DENSITY // Buffer-friendly
            bitmapLimitWidth = targetWidth
        }

        try {
            cmd.append(cmd.getBitmapCmd(bitmapSettings, image))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to build bitmap command", e)
            return null
        }

        cmd.append(cmd.lfcrCmd)
        cmd.append(cmd.cmdCutNew)

        return cmd.appendCmds
    }

    // Removed calculateTargetWidth – not needed when letting printer handle scaling.

    private fun configurePrinter(
        deviceData: PrinterDeviceData.Rongta
    ): PrinterConfigBean? {
        Log.i(TAG, "configuring printer - $deviceData")
        return when (deviceData.type) {
            is PrinterDeviceData.Rongta.Type.Bluetooth -> configureBTPrinter(deviceData.type)
            is PrinterDeviceData.Rongta.Type.Network -> configureNetworkPrinter(deviceData.type)
        }
    }

    private fun configureBTPrinter(deviceData: PrinterDeviceData.Rongta.Type.Bluetooth): PrinterConfigBean? {
        val btAdapter = appContext.getBluetoothAdapter() ?: return null.also {
            Log.e(TAG, "failed to get bluetooth adapter")
        }

        val device = btAdapter.getRemoteDevice(deviceData.address)
        val configBean = BluetoothEdrConfigBean(device)
        val btFactory = BluetoothFactory()
        val printerInterface = btFactory.create()
        printerInterface.configObject = configBean
        printer.setPrinterInterface(printerInterface)

        return configBean
    }

    private fun configureNetworkPrinter(deviceData: PrinterDeviceData.Rongta.Type.Network): PrinterConfigBean? {
        val configBean = WiFiConfigBean(
            deviceData.ipAddress,
            deviceData.port
        )
        val wiFiFactory = WiFiFactory()
        val printerInterface = wiFiFactory.create()
        printerInterface.configObject = configBean
        printer.setPrinterInterface(printerInterface)

        return configBean
    }

    companion object {
        private const val TAG = "RongtaPrinter"
    }
}
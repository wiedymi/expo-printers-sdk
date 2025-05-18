package expo.modules.printers.rongta

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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

internal typealias PrinterConfigBean = Any

class RongtaPrinter(
    private val appContext: Context,
) : Printer<PrinterDeviceData.Rongta, RongtaPrintResult> {

    private val printerFactory = ThermalPrinterFactory()
    private val printer = printerFactory.create()

    override suspend fun printImage(
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

        val result = suspendCancellableCoroutine { continuation ->
            printer.setConnectListener(object : ConnectListener {
                override fun onPrinterConnected(configObj: Any?) {
                    Log.i(TAG, "printer connected - $configObj")
                    val printingCommand = createImagePrintCommand(img)
                    if (printingCommand == null) {
                        Log.e(TAG, "failed to create printing command")
                        continuation.resumeWith(Result.success(RongtaPrintResult.ErrorUnknown))
                    } else {
                        Log.i(TAG, "printing receipt")
                        runCatching {
                            printer.writeMsg(printingCommand)
                        }.onFailure { throwable ->
                            Log.e(TAG, "failed to print receipt - $throwable")
                            continuation.resumeWith(Result.success(RongtaPrintResult.ErrorUnknown))
                        }
                    }
                }

                override fun onPrinterDisconnect(configObj: Any?) {
                    Log.i(TAG, "printer disconnected - $configObj")
                }

                override fun onPrinterWritecompletion(configObj: Any?) {
                    Log.i(TAG, "printer write completion")
                    runCatching { printer.disConnect() }
                        .onFailure { throwable ->
                            Log.e(TAG, "failed to disconnect printer - $throwable")
                        }
                        .getOrNull()
                    continuation.resumeWith(Result.success(RongtaPrintResult.Success))
                }
            })

            runCatching {
                Log.i(TAG, "connecting to printer - $configBean")
                printer.connect(configBean)
            }.onFailure { throwable ->
                Log.e(TAG, "failed to connect to printer - $throwable")
                continuation.resumeWith(Result.success(RongtaPrintResult.ErrorUnknown))
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
        cmd.chartsetName = "UTF-8"
        cmd.append(cmd.headerCmd)

        val commonSetting = CommonSetting()
        commonSetting.align = CommonEnum.ALIGN_LEFT
        commonSetting.pageLengthEnum = PageLengthEnum.INCH_3
        cmd.append(cmd.getCommonSettingCmd(commonSetting))

        val bitmapSettings = BitmapSetting()
        bitmapSettings.bmpPrintMode = BmpPrintMode.MODE_SINGLE_FAST
        bitmapSettings.bimtapLimitWidth = 510

        runCatching {
            cmd.append(cmd.getBitmapCmd(bitmapSettings, image))
        }

        cmd.append(cmd.lfcrCmd)
        cmd.append(cmd.cmdCutNew)

        return cmd.appendCmds
    }

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
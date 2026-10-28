package expo.modules.printers.rongta.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import expo.modules.printers.commons.PrinterLog
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlin.time.Duration.Companion.seconds

class RongtaBluetoothScanner(
    private val appContext: Context,
) {

    companion object {
        const val TAG = "RongtaBluetoothScanner"
    }

    private val bluetoothAdapter = appContext.getBluetoothAdapter()

    private val btIntentFilter = IntentFilter().apply {
        addAction(BluetoothDevice.ACTION_FOUND)
        addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
    }

    @SuppressLint("MissingPermission")
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun scan(): Flow<BluetoothDevice> {
        PrinterLog.i(TAG, "started: bluetoothAdapter available - ${bluetoothAdapter != null}")
        return callbackFlow {
            val pairedDevices = runCatching { bluetoothAdapter?.bondedDevices }.getOrNull()
                ?.filter { device ->
                    val deviceType = device.bluetoothClass.majorDeviceClass
                    deviceType == BluetoothClass.Device.Major.IMAGING
                }
                .orEmpty()

            PrinterLog.i(TAG, "already paired devices: $pairedDevices")

            pairedDevices.forEach { send(it) }

            val bluetoothDeviceReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    val action = intent?.action
                    PrinterLog.i(TAG, "bluetoothDeviceReceiver action - $action")
                    if (BluetoothDevice.ACTION_FOUND == action) {
                        val device =
                            intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                                ?: return
                        val deviceType = device.bluetoothClass.majorDeviceClass
                        if (deviceType != BluetoothClass.Device.Major.IMAGING) return

                        PrinterLog.i(TAG, "found device - $device")
                        trySend(device)
                    } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED == action) {
                        PrinterLog.i(TAG, "discovery finished")
                        if (this@callbackFlow.isActive) close()
                    }
                }
            }

            runCatching {
                PrinterLog.i(TAG, "registering bluetoothDeviceReceiver")
                appContext.registerReceiver(bluetoothDeviceReceiver, btIntentFilter)
            }
                .onFailure { throwable ->
                    PrinterLog.e(TAG, "failed to register bluetoothDeviceReceiver: $throwable")
                    close(throwable)
                    return@callbackFlow
                }

            runCatching {
                PrinterLog.i(TAG, "starting discovery")
                bluetoothAdapter?.startDiscovery()
            }
                .onFailure { throwable ->
                    PrinterLog.e(TAG, "failed to start discovery: $throwable")
                    close(throwable)
                    return@callbackFlow
                }

            delay(30.seconds)
            PrinterLog.i(TAG, "discovery finished after 10 seconds timeout")
            close()

            awaitClose {
                PrinterLog.i(TAG, "closing bluetooth scanner")
                runCatching {
                    PrinterLog.i(TAG, "unregistering bluetoothDeviceReceiver")
                    appContext.unregisterReceiver(bluetoothDeviceReceiver)
                }
                    .onFailure { throwable ->
                        PrinterLog.e(TAG, "failed to unregister bluetoothDeviceReceiver: $throwable")
                    }

                runCatching { bluetoothAdapter?.cancelDiscovery() }
                    .onFailure { throwable ->
                        PrinterLog.e(TAG, "failed to cancel discovery: $throwable")
                    }
            }
        }
    }
}
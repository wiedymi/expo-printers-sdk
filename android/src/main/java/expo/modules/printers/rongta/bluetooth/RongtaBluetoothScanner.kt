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
import android.util.Log
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
        Log.i(TAG, "started: bluetoothAdapter available - ${bluetoothAdapter != null}")
        return callbackFlow {
            val pairedDevices = runCatching { bluetoothAdapter?.bondedDevices }.getOrNull()
                ?.filter { device ->
                    val deviceType = device.bluetoothClass.majorDeviceClass
                    deviceType == BluetoothClass.Device.Major.IMAGING
                }
                .orEmpty()

            Log.i(TAG, "already paired devices: $pairedDevices")

            pairedDevices.forEach { send(it) }

            val bluetoothDeviceReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    val action = intent?.action
                    Log.i(TAG, "bluetoothDeviceReceiver action - $action")
                    if (BluetoothDevice.ACTION_FOUND == action) {
                        val device =
                            intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                                ?: return
                        val deviceType = device.bluetoothClass.majorDeviceClass
                        if (deviceType != BluetoothClass.Device.Major.IMAGING) return

                        Log.i(TAG, "found device - $device")
                        trySend(device)
                    } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED == action) {
                        Log.i(TAG, "discovery finished")
                        if (this@callbackFlow.isActive) close()
                    }
                }
            }

            runCatching {
                Log.i(TAG, "registering bluetoothDeviceReceiver")
                appContext.registerReceiver(bluetoothDeviceReceiver, btIntentFilter)
            }
                .onFailure { throwable ->
                    Log.e(TAG, "failed to register bluetoothDeviceReceiver: $throwable")
                    close(throwable)
                    return@callbackFlow
                }

            runCatching {
                Log.i(TAG, "starting discovery")
                bluetoothAdapter?.startDiscovery()
            }
                .onFailure { throwable ->
                    Log.e(TAG, "failed to start discovery: $throwable")
                    close(throwable)
                    return@callbackFlow
                }

            delay(30.seconds)
            Log.i(TAG, "discovery finished after 10 seconds timeout")
            close()

            awaitClose {
                Log.i(TAG, "closing bluetooth scanner")
                runCatching {
                    Log.i(TAG, "unregistering bluetoothDeviceReceiver")
                    appContext.unregisterReceiver(bluetoothDeviceReceiver)
                }
                    .onFailure { throwable ->
                        Log.e(TAG, "failed to unregister bluetoothDeviceReceiver: $throwable")
                    }

                runCatching { bluetoothAdapter?.cancelDiscovery() }
                    .onFailure { throwable ->
                        Log.e(TAG, "failed to cancel discovery: $throwable")
                    }
            }
        }
    }
}
package expo.modules.printers.rongta.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context

internal fun Context.getBluetoothAdapter(): BluetoothAdapter? = runCatching {
    getSystemService(BluetoothManager::class.java)
}.getOrNull()?.adapter
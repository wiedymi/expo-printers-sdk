package expo.modules.printers.commons

sealed class PrinterDeviceData {

    abstract val connectionType: PrinterConnectionType

    data class EPSON(
        override val connectionType: PrinterConnectionType,
        val deviceType: Int,
        val target: String,
        val deviceName: String,
        val ipAddress: String,
        val macAddress: String,
        val bdAddress: String,
        val isSupported: Boolean = true,
        val unsupportedReason: String? = null,
    ) : PrinterDeviceData()

    data class Star(
        override val connectionType: PrinterConnectionType,
        val modelName: String,
        val portName: String,
        val macAddress: String,
        val usbSerialNumber: String,
        val isSupported: Boolean = true,
        val unsupportedReason: String? = null,
    ) : PrinterDeviceData()

    data class Rongta(
        override val connectionType: PrinterConnectionType,
        val type: Type,
        val isSupported: Boolean = true,
        val unsupportedReason: String? = null,
    ) : PrinterDeviceData() {

        sealed class Type {
            data class Bluetooth(
                val alias: String,
                val name: String,
                val address: String,
            ) : Type()

            data class Network(
                val ipAddress: String,
                val port: Int,
            ) : Type()

            data class Usb(
                val name: String,
                val vendorId: Int,
                val productId: Int,
            ) : Type()
        }
    }
}
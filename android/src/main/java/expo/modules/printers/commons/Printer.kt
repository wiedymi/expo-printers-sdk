package expo.modules.printers.commons

interface Printer<D : PrinterDeviceData, R> {

    suspend fun printImage(
        base64Image: String,
        deviceData: D,
    ): R
}
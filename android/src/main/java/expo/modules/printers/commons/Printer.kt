package expo.modules.printers.commons

interface Printer<D : PrinterDeviceData, R> {

    // paperWidth is null when the app did not set it. Then the printer uses its standard or reported paper width.
    suspend fun printImage(
        base64Image: String,
        deviceData: D,
        paperWidth: PaperWidth?,
    ): R
}
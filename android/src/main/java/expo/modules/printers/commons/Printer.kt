package expo.modules.printers.commons

interface Printer<D : PrinterDeviceData, R> {

    // paperWidth is null when the app did not set it. Then the printer uses its standard or reported paper width.
    // jobId is only used in the logs.
    suspend fun printImage(
        base64Image: String,
        deviceData: D,
        paperWidth: PaperWidth?,
        jobId: String?,
    ): R

    // Throws PrinterUnreachableException, PrinterBusyException or PrinterStatusUnavailableException.
    suspend fun getStatus(deviceData: D): PrinterStatus
}
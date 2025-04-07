package expo.modules.printers.commons

interface PrinterFinder<T : PrinterDeviceData> {

    suspend fun search(connectionType: PrinterConnectionType): List<T>
}
package expo.modules.printers.epson

enum class EpsonPrintResult {
    Success,
    ErrorInvalidImage,
    ErrorPrinterOffline,
    ErrorTimeout,
    ErrorUnknown,
}
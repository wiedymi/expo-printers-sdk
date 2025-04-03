package expo.modules.printers.starmicronics

enum class StarMicronicsPrintResult {
    Success,
    ErrorOpenPort,
    ErrorPrinterOffline,
    ErrorCoverOpened,
    ErrorPaperEmpty,
    ErrorPaperJam,
    ErrorUnknown,
    ErrorInvalidImage,
}
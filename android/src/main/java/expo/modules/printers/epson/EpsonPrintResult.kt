package expo.modules.printers.epson

import expo.modules.printers.commons.PrintResultCode

enum class EpsonPrintResult(
    override val code: String,
    override val message: String?,
) : PrintResultCode {
    Success(PrintResultCode.SUCCESS, null),
    ErrorInvalidImage("INVALID_IMAGE", "Invalid image data"),
    ErrorUnsupportedModel("UNSUPPORTED_MODEL", "Printer model is not supported"),
    ErrorPrinterOffline("PRINTER_OFFLINE", "Printer is offline"),
    ErrorTimeout("PRINT_TIMEOUT", "Printer did not answer in time"),
    ErrorUnknown("UNKNOWN_ERROR", "Unknown error occurred"),
}

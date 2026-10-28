package expo.modules.printers.rongta

import expo.modules.printers.commons.PrintResultCode

enum class RongtaPrintResult(
    override val code: String,
    override val message: String?,
) : PrintResultCode {
    Success(PrintResultCode.SUCCESS, null),
    ErrorInvalidImage("INVALID_IMAGE", "Invalid image data"),
    ErrorConnection("CONNECTION_FAILED", "Failed to connect to printer"),
    ErrorPermission("PERMISSION_DENIED", "No permission to access printer"),
    ErrorConnectionTimeout("CONNECTION_TIMEOUT", "Printer connection timed out"),
    ErrorPrint("PRINT_FAILED", "Failed to print receipt"),
    ErrorPrintTimeout("PRINT_TIMEOUT", "Printer did not answer in time during the print"),
    ErrorUnknown("UNKNOWN_ERROR", "Unknown error occurred"),
}

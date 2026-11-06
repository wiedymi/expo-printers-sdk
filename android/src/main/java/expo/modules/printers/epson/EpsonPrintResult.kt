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
    ErrorCoverOpen("COVER_OPEN", "Printer cover is open"),
    ErrorPaperEmpty("PAPER_EMPTY", "Printer is out of paper"),
    ErrorPaperJam("PAPER_JAM", "Printer has a paper jam"),
    ErrorCutter("CUTTER_ERROR", "Printer cutter error. Clear any paper jam and restart the printer"),
    ErrorHardware(
        "HARDWARE_ERROR",
        "Printer hardware error. Restart the printer and check the cutter, print head, and power supply",
    ),
    ErrorUnknown("UNKNOWN_ERROR", "Unknown error occurred"),
}

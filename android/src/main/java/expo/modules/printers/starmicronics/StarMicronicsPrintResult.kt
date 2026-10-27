package expo.modules.printers.starmicronics

enum class StarMicronicsPrintResult(
    val code: String,
    val message: String?,
) {
    Success("SUCCESS", null),
    InvalidImage("INVALID_IMAGE", "Invalid image data"),
    UnsupportedModel("UNSUPPORTED_MODEL", "Printer model is not supported"),
    PortOpenFailed("PORT_OPEN_FAILED", "Unable to connect to the printer"),
    PrinterInUse("PRINTER_IN_USE", "Printer is being used by another device"),
    ConnectionTimeout("CONNECTION_TIMEOUT", "Printer connection timed out"),
    PrintTimeout("PRINT_TIMEOUT", "The printer did not answer in time during the print"),
    CommunicationError("COMMUNICATION_ERROR", "Communication with the printer failed"),
    PrinterOffline("PRINTER_OFFLINE", "Printer is offline"),
    CoverOpen("COVER_OPEN", "Printer cover is open"),
    PaperEmpty("PAPER_EMPTY", "Printer is out of paper"),
    PaperJam("PAPER_JAM", "Printer has a paper jam"),
    CutterError("CUTTER_ERROR", "Printer cutter error. Clear any paper jam and restart the printer"),
    HeadThermistorError(
        "HEAD_THERMISTOR_ERROR",
        "Printer head sensor error. Restart the printer and contact support if the problem continues",
    ),
    VoltageError("VOLTAGE_ERROR", "Printer power supply error. Check the power adapter and restart the printer"),
    OverTemperature("OVER_TEMPERATURE", "Printer is too hot. Wait for it to cool down"),
    ReceiveBufferOverflow("RECEIVE_BUFFER_OVERFLOW", "Printer receive buffer is full. Retry the print"),
    HardwareError(
        "HARDWARE_ERROR",
        "Printer hardware error. Restart the printer and check the cutter, print head, and power supply",
    ),
}

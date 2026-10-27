package expo.modules.printers.commons

import expo.modules.kotlin.exception.CodedException

enum class PaperStatus {
    OK,
    NEAR_END,
    EMPTY,
    UNKNOWN,
}

enum class PrinterError {
    CUTTER_ERROR,
    MECHANICAL_ERROR,
    PAPER_JAM,
    OVER_TEMPERATURE,
    VOLTAGE_ERROR,
    AUTO_RECOVERABLE_ERROR,
    UNRECOVERABLE_ERROR,
}

data class PrinterStatus(
    val online: Boolean,
    val coverOpen: Boolean,
    val paper: PaperStatus,
    val errors: List<PrinterError>,
) {
    fun toMap(): Map<String, Any> = mapOf(
        "online" to online,
        "coverOpen" to coverOpen,
        "paper" to paper.name,
        "errors" to errors.map { it.name },
    )
}

// JS error codes: ERR_PRINTER_UNREACHABLE, ERR_PRINTER_BUSY, ERR_PRINTER_STATUS_UNAVAILABLE.
class PrinterUnreachableException(cause: Throwable? = null) :
    CodedException("Cannot connect to the printer", cause)

class PrinterBusyException(cause: Throwable? = null) :
    CodedException("The printer is in use by another connection", cause)

class PrinterStatusUnavailableException(cause: Throwable? = null) :
    CodedException("The printer did not report its status", cause)

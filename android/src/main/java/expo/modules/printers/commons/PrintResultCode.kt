package expo.modules.printers.commons

// A print result of a vendor: a stable code for the app and a message for people.
interface PrintResultCode {
    val code: String
    val message: String?

    companion object {
        const val SUCCESS = "SUCCESS"
    }
}

// The payload of the onPrintImage event.
fun PrintReport<out PrintResultCode>.toPrintImageEvent(jobId: String?): Map<String, Any?> {
    val success = result.code == PrintResultCode.SUCCESS
    return mapOf(
        "jobId" to jobId,
        "success" to success,
        "errorCode" to if (success) null else result.code,
        "error" to result.message,
        "cause" to if (success) null else cause?.toMap(),
        "steps" to steps.map { it.toMap() },
    )
}

// The payload of the onPrintImage event when the print could not start.
fun printNotStartedEvent(jobId: String?, errorCode: String, throwable: Throwable?): Map<String, Any?> = mapOf(
    "jobId" to jobId,
    "success" to false,
    "errorCode" to errorCode,
    "error" to (throwable?.localizedMessage ?: "Printer not initialized"),
    "cause" to throwable?.let { PrintFailureCause("request", it::class.java.simpleName, it.message, null).toMap() },
    "steps" to emptyList<Map<String, Any>>(),
)

package expo.modules.printers.commons

import kotlinx.coroutines.sync.Mutex
import java.util.concurrent.ConcurrentHashMap

// One lock for each printer, by its connection target. Prints to one printer run one after another, and a status
// check does not run during a print.
class PrinterLocks {
    private val locks = ConcurrentHashMap<String, Mutex>()

    fun of(target: String): Mutex = locks.computeIfAbsent(target) { Mutex() }
}

interface Printer<D : PrinterDeviceData, R : PrintResultCode> {

    // paperWidth is null when the app did not set it. Then the printer uses its standard or reported paper width.
    // jobId is written in the logs. The report has the steps and the native cause of a failure.
    suspend fun printImage(
        base64Image: String,
        deviceData: D,
        paperWidth: PaperWidth?,
        jobId: String?,
    ): PrintReport<R>

    // Throws PrinterUnreachableException, PrinterBusyException or PrinterStatusUnavailableException.
    suspend fun getStatus(deviceData: D): PrinterStatus
}
package expo.modules.printers.commons

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.time.Duration

class PrintStepTimeoutException(val step: String, timeout: Duration) :
    Exception("Print step '$step' did not finish in $timeout")

data class PrintStepRecord(val step: String, val outcome: String, val durationMs: Long) {
    fun toMap(): Map<String, Any> = mapOf("step" to step, "outcome" to outcome, "durationMs" to durationMs)
}

// The native error that ended a print. vendorCode is the error code of the vendor SDK, if it has one.
data class PrintFailureCause(val step: String, val type: String, val message: String?, val vendorCode: Int?) {
    fun toMap(): Map<String, Any?> = mapOf("step" to step, "type" to type, "message" to message, "vendorCode" to vendorCode)
}

class PrintReport<R>(val result: R, val steps: List<PrintStepRecord>, val cause: PrintFailureCause?)

// Logs the steps of one print job with the job ID and the duration of each step, and keeps them for the
// print result. vendorCodeOf reads the error code from an exception of the vendor SDK.
class PrintSteps(
    private val tag: String,
    private val jobId: String?,
    private val vendorCodeOf: (Throwable) -> Int? = { null },
) {
    private val jobStartNanos = System.nanoTime()
    private val records = CopyOnWriteArrayList<PrintStepRecord>()

    @Volatile
    private var cause: PrintFailureCause? = null

    fun log(step: String, outcome: String, stepStartNanos: Long = System.nanoTime()) {
        val durationMs = elapsedMillis(stepStartNanos)
        records.add(PrintStepRecord(step, outcome, durationMs))
        PrinterLog.step(tag, jobId, step, outcome, durationMs, elapsedMillis(jobStartNanos))
    }

    // Records the error that ends the print. A later error replaces an earlier one, for example after a retry.
    fun recordFailure(step: String, throwable: Throwable) {
        cause = PrintFailureCause(step, throwable::class.java.simpleName, throwable.message, vendorCodeOf(throwable))
    }

    fun clearFailure() {
        cause = null
    }

    fun recordFailureIfNone(step: String, throwable: Throwable) {
        if (cause == null) recordFailure(step, throwable)
    }

    fun <R> report(result: R): PrintReport<R> = PrintReport(result, records.toList(), cause)

    inline fun <T> measure(step: String, block: () -> T): T {
        val stepStartNanos = System.nanoTime()
        return try {
            block().also { log(step, "ok", stepStartNanos) }
        } catch (throwable: Throwable) {
            failed(step, throwable, stepStartNanos, cleanup = false)
            throw throwable
        }
    }

    // Runs a blocking SDK call on its own thread. If the call does not finish in time, this throws
    // PrintStepTimeoutException and the call continues in the background. onLateResult receives the result
    // of a call that finishes after the timeout, so the caller can release what it opened. Errors of cleanup
    // steps (release, disconnect) do not replace the cause of the print result.
    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun <T> runWithTimeout(
        step: String,
        timeout: Duration,
        cleanup: Boolean = false,
        onLateResult: (T) -> Unit = {},
        block: () -> T,
    ): T {
        val stepStartNanos = System.nanoTime()
        val call = blockingCalls.async { block() }
        if (withTimeoutOrNull(timeout) { call.join() } == null) {
            val timeoutException = PrintStepTimeoutException(step, timeout)
            failed(step, timeoutException, stepStartNanos, cleanup)
            call.invokeOnCompletion { lateCause ->
                if (lateCause == null) {
                    PrinterLog.i(tag, "jobId=$jobId step=$step finished after the timeout")
                    onLateResult(call.getCompleted())
                } else {
                    PrinterLog.w(tag, "jobId=$jobId step=$step failed after the timeout", lateCause)
                }
            }
            throw timeoutException
        }
        return try {
            call.await().also { log(step, "ok", stepStartNanos) }
        } catch (throwable: Throwable) {
            failed(step, throwable, stepStartNanos, cleanup)
            throw throwable
        }
    }

    fun failed(step: String, throwable: Throwable, stepStartNanos: Long, cleanup: Boolean) {
        log(step, "error: ${throwable::class.java.simpleName}: ${throwable.message}", stepStartNanos)
        if (!cleanup) recordFailure(step, throwable)
    }

    companion object {
        // Blocking SDK calls run here, so a call that never returns cannot block the print job.
        internal val blockingCalls = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        private fun elapsedMillis(startNanos: Long): Long = (System.nanoTime() - startNanos) / 1_000_000
    }
}

package expo.modules.printers.commons

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration

class PrintStepTimeoutException(val step: String, timeout: Duration) :
    Exception("Print step '$step' did not finish in $timeout")

// Logs the steps of one print job with the job ID and the duration of each step.
class PrintSteps(private val tag: String, private val jobId: String?) {
    private val jobStartNanos = System.nanoTime()

    fun log(step: String, outcome: String, stepStartNanos: Long = System.nanoTime()) {
        Log.i(
            tag,
            "jobId=$jobId step=$step outcome=$outcome stepMs=${elapsedMillis(stepStartNanos)} jobMs=${elapsedMillis(jobStartNanos)}",
        )
    }

    inline fun <T> measure(step: String, block: () -> T): T {
        val stepStartNanos = System.nanoTime()
        return try {
            block().also { log(step, "ok", stepStartNanos) }
        } catch (throwable: Throwable) {
            log(step, "error: ${throwable.describe()}", stepStartNanos)
            throw throwable
        }
    }

    // Runs a blocking SDK call on its own thread. If the call does not finish in time, this throws
    // PrintStepTimeoutException and the call continues in the background. onLateResult receives the result
    // of a call that finishes after the timeout, so the caller can release what it opened.
    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun <T> runWithTimeout(
        step: String,
        timeout: Duration,
        onLateResult: (T) -> Unit = {},
        block: () -> T,
    ): T {
        val stepStartNanos = System.nanoTime()
        val call = blockingCalls.async { block() }
        if (withTimeoutOrNull(timeout) { call.join() } == null) {
            log(step, "timeout after $timeout", stepStartNanos)
            call.invokeOnCompletion { cause ->
                if (cause == null) {
                    log(step, "finished after the timeout", stepStartNanos)
                    onLateResult(call.getCompleted())
                } else {
                    log(step, "failed after the timeout: ${cause.describe()}", stepStartNanos)
                }
            }
            throw PrintStepTimeoutException(step, timeout)
        }
        return measureCompleted(step, stepStartNanos) { call.await() }
    }

    private suspend fun <T> measureCompleted(step: String, stepStartNanos: Long, await: suspend () -> T): T =
        try {
            await().also { log(step, "ok", stepStartNanos) }
        } catch (throwable: Throwable) {
            log(step, "error: ${throwable.describe()}", stepStartNanos)
            throw throwable
        }

    companion object {
        // Blocking SDK calls run here, so a call that never returns cannot block the print job.
        internal val blockingCalls = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        private fun elapsedMillis(startNanos: Long): Long = (System.nanoTime() - startNanos) / 1_000_000

        fun Throwable.describe(): String = "${this::class.java.simpleName}: $message"
    }
}

package expo.modules.printers.commons

import android.util.Log
import java.util.concurrent.CopyOnWriteArrayList

// Writes to logcat and sends each entry to the printer module of the same vendor, which emits it as the
// onLog event. A module receives the entries whose tag starts with its vendor prefix, for example "Rongta".
object PrinterLog {
    enum class Level { DEBUG, INFO, WARN, ERROR }

    class Sink(val tagPrefix: String, val emit: (Map<String, Any?>) -> Unit)

    private val sinks = CopyOnWriteArrayList<Sink>()

    fun addSink(sink: Sink) {
        sinks.add(sink)
    }

    fun removeSink(sink: Sink) {
        sinks.remove(sink)
    }

    fun d(tag: String, message: String) = write(Level.DEBUG, tag, message)

    fun i(tag: String, message: String) = write(Level.INFO, tag, message)

    fun w(tag: String, message: String, throwable: Throwable? = null) = write(Level.WARN, tag, message, throwable)

    fun e(tag: String, message: String, throwable: Throwable? = null) = write(Level.ERROR, tag, message, throwable)

    fun step(tag: String, jobId: String?, step: String, outcome: String, durationMs: Long, jobDurationMs: Long) =
        write(
            Level.INFO,
            tag,
            "jobId=$jobId step=$step outcome=$outcome stepMs=$durationMs jobMs=$jobDurationMs",
            fields = mapOf(
                "jobId" to jobId,
                "step" to step,
                "outcome" to outcome,
                "durationMs" to durationMs,
                "jobDurationMs" to jobDurationMs,
            ),
        )

    private fun write(
        level: Level,
        tag: String,
        message: String,
        throwable: Throwable? = null,
        fields: Map<String, Any?> = emptyMap(),
    ) {
        when (level) {
            Level.DEBUG -> Log.d(tag, message, throwable)
            Level.INFO -> Log.i(tag, message, throwable)
            Level.WARN -> Log.w(tag, message, throwable)
            Level.ERROR -> Log.e(tag, message, throwable)
        }

        val entry = mapOf(
            "level" to level.name.lowercase(),
            "tag" to tag,
            "message" to message,
            "timestamp" to System.currentTimeMillis(),
            "error" to throwable?.let { mapOf("type" to it::class.java.simpleName, "message" to it.message) },
        ) + fields
        sinks.filter { tag.startsWith(it.tagPrefix) }.forEach { sink ->
            // A failed event must never stop a print.
            runCatching { sink.emit(entry) }
                .onFailure { failure -> Log.w(tag, "Failed to send the onLog event", failure) }
        }
    }
}

package expo.modules.printers.commons

import expo.modules.kotlin.records.Field
import expo.modules.kotlin.records.Record
import expo.modules.kotlin.types.Enumerable

// Paper loaded in the printer, in millimeters. Most printers cannot report it, so the app can set it.
enum class PaperWidth(val value: Int) : Enumerable {
    MM58(58),
    MM80(80),
}

class PrintOptions : Record {
    @Field
    val paperWidth: PaperWidth? = null

    // Returned in the onPrintImage event and written in the logs, so the app can match a result to its job.
    @Field
    val jobId: String? = null
}

package expo.modules.printers.commons

import expo.modules.kotlin.records.Field
import expo.modules.kotlin.records.Record
import expo.modules.kotlin.types.Enumerable

// Paper loaded in the printer, in millimeters. Most printers cannot report it, so the app can set it.
enum class PaperWidth(val value: Int) : Enumerable {
    MM58(58),
    MM80(80),
}

// How an Epson print makes the image black and white. AREA_AVERAGE, the default, uses toMonochrome. SDK scales the
// image with Bitmap.createScaledBitmap, and the Epson SDK makes it black and white, as in version 0.11.0.
enum class ImageConversion(val value: String) : Enumerable {
    AREA_AVERAGE("areaAverage"),
    SDK("sdk"),
}

class PrintOptions : Record {
    @Field
    val paperWidth: PaperWidth? = null

    // Returned in the onPrintImage event and written in the logs, so the app can match a result to its job.
    @Field
    val jobId: String? = null

    // Epson only.
    @Field
    val imageConversion: ImageConversion? = null
}

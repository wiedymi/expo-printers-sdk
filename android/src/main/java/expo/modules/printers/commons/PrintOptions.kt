package expo.modules.printers.commons

import expo.modules.kotlin.records.Field
import expo.modules.kotlin.records.Record
import expo.modules.kotlin.types.Enumerable

// Paper loaded in the printer, in millimeters. Most printers cannot report it, so the app can set it.
enum class PaperWidth(val value: Int) : Enumerable {
    MM58(58),
    MM80(80),
}

// How an Epson print makes the image black and white. AUTO uses AREA_AVERAGE on the impact printers and SDK on the
// other printers. SDK scales the image with Bitmap.createScaledBitmap, and the Epson SDK makes it black and white.
// AREA_AVERAGE uses toMonochrome.
enum class ImageConversion(val value: String) : Enumerable {
    AUTO("auto"),
    SDK("sdk"),
    AREA_AVERAGE("areaAverage"),
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

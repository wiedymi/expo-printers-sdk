package expo.modules.printers.epson.internals

import com.epson.epos2.printer.Printer
import expo.modules.printers.commons.PaperWidth

object EpsonModelCapability {

    const val UNKNOWN = Printer.UNKNOWN

    private val modelNameToSeries = mapOf(
        // TM-m series
        "TM-m10" to Printer.TM_M10,
        "TM-m30" to Printer.TM_M30,
        "TM-m30II" to Printer.TM_M30II,
        "TM-m30II-H" to Printer.TM_M30II,
        "TM-m30II-NT" to Printer.TM_M30II,
        "TM-m30II-S" to Printer.TM_M30II,
        "TM-m30II-SL" to Printer.TM_M30II,
        "TM-m30III" to Printer.TM_M30III,
        "TM-m30III-H" to Printer.TM_M30III,
        "TM-m50" to Printer.TM_M50,
        "TM-m50II" to Printer.TM_M50II,
        "TM-m50II-H" to Printer.TM_M50II,
        "TM-m55" to Printer.TM_M55,

        // TM-T20 series
        "TM-T20" to Printer.TM_T20,
        "TM-T20II" to Printer.TM_T20,
        "TM-T20III" to Printer.TM_T20,
        "TM-T20IIIL" to Printer.TM_T20,
        "TM-T20X" to Printer.TM_T20,
        "TM-T20II-i" to Printer.TM_T20,

        // TM-T60/T70 series
        "TM-T60" to Printer.TM_T60,
        "TM-T70" to Printer.TM_T70,
        "TM-T70II" to Printer.TM_T70,
        "TM-T70-i" to Printer.TM_T70,
        "TM-T70II-DT" to Printer.TM_T70,
        "TM-T70II-DT2" to Printer.TM_T70,

        // TM-T81 series
        "TM-T81II" to Printer.TM_T81,
        "TM-T81III" to Printer.TM_T81,

        // TM-T82 series
        "TM-T82" to Printer.TM_T82,
        "TM-T82II" to Printer.TM_T82,
        "TM-T82III" to Printer.TM_T82,
        "TM-T82IIIL" to Printer.TM_T82,
        "TM-T82X" to Printer.TM_T82,
        "TM-T82II-i" to Printer.TM_T82,

        // TM-T83 series
        "TM-T83II" to Printer.TM_T83,
        "TM-T83II-i" to Printer.TM_T83,
        "TM-T83III" to Printer.TM_T83III,

        // TM-T88 series
        "TM-T88IV" to Printer.TM_T88,
        "TM-T88V" to Printer.TM_T88,
        "TM-T88VI" to Printer.TM_T88,
        "TM-T88VII" to Printer.TM_T88VII,
        "TM-T88V-i" to Printer.TM_T88,
        "TM-T88VI-iHUB" to Printer.TM_T88,
        "TM-T88V-DT" to Printer.TM_T88,
        "TM-T88VI-DT2" to Printer.TM_T88,

        // TM-T90 series
        "TM-T90" to Printer.TM_T90,
        "TM-T90KP" to Printer.TM_T90KP,

        // TM-T100
        "TM-T100" to Printer.TM_T100,

        // TM-L series (label printers)
        "TM-L90" to Printer.TM_L90,
        "TM-L90LFC" to Printer.TM_L90LFC,
        "TM-L100" to Printer.TM_L100,

        // TM-U series (impact printers)
        "TM-U220" to Printer.TM_U220,
        "TM-U220-i" to Printer.TM_U220,
        "TM-U220II" to Printer.TM_U220II,
        "TM-U330" to Printer.TM_U330,

        // TM-P series (portable)
        "TM-P20" to Printer.TM_P20,
        "TM-P20II" to Printer.TM_P20II,
        "TM-P60" to Printer.TM_P60,
        "TM-P60II" to Printer.TM_P60II,
        "TM-P80" to Printer.TM_P80,
        "TM-P80II" to Printer.TM_P80II,

        // TM-H series (hybrid)
        "TM-H6000IV" to Printer.TM_H6000,
        "TM-H6000IV-DT" to Printer.TM_H6000,
        "TM-H6000V" to Printer.TM_H6000,

        // EU series
        "EU-m30" to Printer.EU_M30,

        // TS series
        "TS-100" to Printer.TS_100
    )

    // Models that report the loaded paper width through getPrinterSetting (ePOS SDK user's manual, "List of Supported APIs").
    private val paperWidthQueryModels = setOf(
        "TM-m30", "TM-m30II", "TM-m30II-H", "TM-T20II", "TM-T88VI", "TM-P20", "TM-U220-i"
    )

    // Printable raster width in dots: one value for the standard paper, and values for other paper widths
    // (keys are Printer.SETTING_PAPERWIDTH_*). Sources: ePOS SDK user's manual "Printer-specific Support
    // Information" and the Technical Reference Guide of each model. If sources disagree, the smaller value is used,
    // because an image wider than the print area can print nothing.
    private class PrintableWidth(val standardDots: Int, val dotsByPaperWidth: Map<Int, Int> = emptyMap())

    private val thermal203Dpi = PrintableWidth(
        576, mapOf(Printer.SETTING_PAPERWIDTH_58_0 to 420, Printer.SETTING_PAPERWIDTH_80_0 to 576)
    )
    private val thermal180Dpi = PrintableWidth(
        512, mapOf(Printer.SETTING_PAPERWIDTH_58_0 to 360, Printer.SETTING_PAPERWIDTH_80_0 to 512)
    )
    // TM-U220 printable area is 385 or 400 half dots on 76 mm paper (DIP SW2-1), and one image dot is two half dots.
    // These values fit both DIP switch settings.
    private val impact = PrintableWidth(
        192,
        mapOf(
            Printer.SETTING_PAPERWIDTH_58_0 to 148,
            Printer.SETTING_PAPERWIDTH_70_0 to 180,
            Printer.SETTING_PAPERWIDTH_76_0 to 192
        )
    )

    private val printableWidthBySeries = mapOf(
        Printer.TM_M10 to PrintableWidth(420),
        Printer.TM_M30 to thermal203Dpi,
        Printer.TM_M30II to thermal203Dpi,
        Printer.TM_M30III to thermal203Dpi,
        Printer.TM_M50 to thermal203Dpi,
        Printer.TM_M50II to thermal180Dpi,
        Printer.TM_T20 to thermal203Dpi,
        Printer.TM_T60 to PrintableWidth(576),
        Printer.TM_T70 to PrintableWidth(
            512, mapOf(Printer.SETTING_PAPERWIDTH_58_0 to 416, Printer.SETTING_PAPERWIDTH_80_0 to 512)
        ),
        Printer.TM_T81 to PrintableWidth(576),
        Printer.TM_T82 to thermal203Dpi,
        Printer.TM_T83 to PrintableWidth(512),
        Printer.TM_T83III to PrintableWidth(546),
        Printer.TM_T88 to thermal180Dpi,
        Printer.TM_T88VII to thermal180Dpi,
        Printer.TM_T90 to PrintableWidth(
            576,
            mapOf(
                Printer.SETTING_PAPERWIDTH_58_0 to 420,
                Printer.SETTING_PAPERWIDTH_60_0 to 384,
                Printer.SETTING_PAPERWIDTH_80_0 to 576
            )
        ),
        Printer.TM_T100 to PrintableWidth(576),
        Printer.TM_L90 to PrintableWidth(560),
        Printer.TM_L100 to thermal203Dpi,
        Printer.TM_U220 to impact,
        Printer.TM_U220II to impact,
        Printer.TM_U330 to impact,
        Printer.TM_P20 to PrintableWidth(384),
        Printer.TM_P20II to PrintableWidth(384),
        Printer.TM_P60 to PrintableWidth(
            420, mapOf(Printer.SETTING_PAPERWIDTH_58_0 to 420, Printer.SETTING_PAPERWIDTH_60_0 to 432)
        ),
        Printer.TM_P60II to PrintableWidth(400),
        Printer.TM_P80 to PrintableWidth(576),
        Printer.TM_P80II to PrintableWidth(576),
        Printer.TM_H6000 to PrintableWidth(512),
        Printer.EU_M30 to thermal203Dpi
    )

    // Smallest standard 80 mm width, for series without official data (TM-m55, TM-T90KP, TM-L90LFC, TS-100).
    private const val FALLBACK_PRINTABLE_WIDTH = 512

    fun getSupportedModels(): List<String> {
        return modelNameToSeries.keys.toList()
    }

    // The longest known name in the provided name, so "TM-m50II-H" does not match "TM-m50".
    private fun modelNameFor(providedPrinterName: String): String? =
        modelNameToSeries.keys
            .filter { printerName -> providedPrinterName.contains(printerName) }
            .maxByOrNull { printerName -> printerName.length }

    fun printerSeriesByName(providedPrinterName: String): Int =
        modelNameFor(providedPrinterName)?.let(modelNameToSeries::getValue) ?: UNKNOWN

    fun supportsPaperWidthQuery(providedPrinterName: String): Boolean =
        modelNameFor(providedPrinterName) in paperWidthQueryModels

    fun paperWidthSetting(paperWidth: PaperWidth): Int = when (paperWidth) {
        PaperWidth.MM58 -> Printer.SETTING_PAPERWIDTH_58_0
        PaperWidth.MM80 -> Printer.SETTING_PAPERWIDTH_80_0
    }

    // paperWidthSetting is a Printer.SETTING_PAPERWIDTH_* value, or null if it is not known. A paper width that the
    // series does not support gives the standard width.
    fun printableWidth(printerSeries: Int, paperWidthSetting: Int?): Int {
        val printableWidth = printableWidthBySeries[printerSeries] ?: return FALLBACK_PRINTABLE_WIDTH
        return paperWidthSetting?.let(printableWidth.dotsByPaperWidth::get) ?: printableWidth.standardDots
    }
}

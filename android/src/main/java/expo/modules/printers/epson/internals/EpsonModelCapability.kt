package expo.modules.printers.epson.internals

import com.epson.epos2.printer.Printer

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

    fun getSupportedModels(): List<String> {
        return modelNameToSeries.keys.toList()
    }

    fun printerSeriesByName(providedPrinterName: String): Int {
        val printerNames = modelNameToSeries.keys

        val found = printerNames.firstOrNull { printerName -> printerName == providedPrinterName }
            ?: printerNames.firstOrNull { printerName -> providedPrinterName.contains(printerName) }

        return if (found != null) {
            modelNameToSeries[found]!!
        } else UNKNOWN
    }
}

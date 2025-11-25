package expo.modules.printers.starmicronics.internals

import android.util.SparseArray
import com.starmicronics.starioextension.StarIoExt.Emulation

/**
 * Minimal printer settings needed for bitmap printing.
 * Only contains: model identification, emulation type, port settings, and paper size.
 */
object StarModelCapability {

    const val NONE = -1
    const val MPOP = 0
    const val FVP10 = 1
    const val TSP100 = 2
    const val TSP650II = 3
    const val TSP700II = 4
    const val TSP800II = 5
    const val SP700 = 6
    const val SM_S210I = 7
    const val SM_S220I = 8
    const val SM_S230I = 9
    const val SM_T300I_T300 = 10
    const val SM_T400I = 11
    const val SM_L200 = 12
    const val BSC10 = 13
    const val SM_S210I_StarPRNT = 14
    const val SM_S220I_StarPRNT = 15
    const val SM_S230I_StarPRNT = 16
    const val SM_T300I_T300_StarPRNT = 17
    const val SM_T400I_StarPRNT = 18
    const val SM_L300 = 19
    const val MC_PRINT2 = 20
    const val MC_PRINT3 = 21
    const val TUP500 = 22
    const val SK1_211_221_V211 = 23
    const val SK1_211_221_V211_Presenter = 24
    const val SK1_311_321_V311 = 25
    const val SK1_311_V311_Presenter = 26
    // New models
    const val MC_LABEL2 = 27
    const val MC_LABEL3 = 28
    const val TSP100IV = 29
    const val TSP100IV_SK = 30
    const val TSP650IISK = 31
    const val BSC10II = 32

    // Paper sizes in dots
    private const val PAPER_SIZE_TWO_INCH = 384
    private const val PAPER_SIZE_TWO_INCH_300DPI = 576  // mC-Label2 at 300dpi
    private const val PAPER_SIZE_THREE_INCH = 576
    private const val PAPER_SIZE_FOUR_INCH = 832
    private const val PAPER_SIZE_ESCPOS_THREE_INCH = 512
    private const val PAPER_SIZE_DOT_THREE_INCH = 210
    private const val PAPER_SIZE_SK1_TWO_INCH = 432

    private val mModelCapabilityMap: SparseArray<ModelInfo> = SparseArray<ModelInfo>().apply {
        // mC-Print series
        put(MC_PRINT2, ModelInfo(
            "mC-Print2",
            arrayOf("MCP20 (STR-001)", "MCP21 (STR-001)", "mC-Print2-", "mC-Print2"),
            Emulation.StarPRNT, "", PAPER_SIZE_TWO_INCH
        ))
        put(MC_PRINT3, ModelInfo(
            "mC-Print3",
            arrayOf("MCP30 (STR-001)", "MCP31 (STR-001)", "mC-Print3-", "mC-Print3"),
            Emulation.StarPRNT, "", PAPER_SIZE_THREE_INCH
        ))

        // mC-Label series
        put(MC_LABEL2, ModelInfo(
            "mC-Label2",
            arrayOf("MCL20 (STR-001)", "MCL21 (STR-001)", "mC-Label2-", "mC-Label2"),
            Emulation.StarPRNT, "", PAPER_SIZE_TWO_INCH_300DPI
        ))
        put(MC_LABEL3, ModelInfo(
            "mC-Label3",
            arrayOf("MCL30 (STR-001)", "MCL31 (STR-001)", "mC-Label3-", "mC-Label3"),
            Emulation.StarPRNT, "", PAPER_SIZE_THREE_INCH
        ))

        // mPOP
        put(MPOP, ModelInfo(
            "mPOP",
            arrayOf("STAR mPOP-", "mPOP"),
            Emulation.StarPRNT, "", PAPER_SIZE_TWO_INCH
        ))

        // FVP10
        put(FVP10, ModelInfo(
            "FVP10",
            arrayOf("FVP10 (STR_T-001)", "Star FVP10"),
            Emulation.StarLine, "", PAPER_SIZE_THREE_INCH
        ))

        // TSP100 series (StarGraphic)
        put(TSP100, ModelInfo(
            "TSP100",
            arrayOf("TSP113", "TSP143", "TSP100-", "Star TSP113", "Star TSP143",
                "TSP100IIIW", "TSP100IIILAN", "TSP100IIIBI", "TSP100IIIU",
                "TSP100IIU+", "TSP100ECO", "TSP100U", "TSP100GT", "TSP100LAN"),
            Emulation.StarGraphic, "", PAPER_SIZE_THREE_INCH
        ))

        // TSP100IV series (StarPRNT - newer)
        put(TSP100IV, ModelInfo(
            "TSP100IV",
            arrayOf("TSP143IV (STR_T-001)", "TSP143IV-", "Star TSP143IV"),
            Emulation.StarPRNT, "", PAPER_SIZE_THREE_INCH
        ))
        put(TSP100IV_SK, ModelInfo(
            "TSP100IV SK",
            arrayOf("TSP143IV SK", "TSP143IVSK"),
            Emulation.StarPRNT, "", PAPER_SIZE_THREE_INCH
        ))

        // TSP650II series
        put(TSP650II, ModelInfo(
            "TSP650II",
            arrayOf("TSP654II (STR_T-001)", "TSP654 (STR_T-001)", "TSP651 (STR_T-001)"),
            Emulation.StarLine, "", PAPER_SIZE_THREE_INCH
        ))
        put(TSP650IISK, ModelInfo(
            "TSP650IISK",
            arrayOf("TSP654IISK", "TSP651IISK"),
            Emulation.StarLine, "", PAPER_SIZE_THREE_INCH
        ))

        // TSP700II series
        put(TSP700II, ModelInfo(
            "TSP700II",
            arrayOf("TSP743II (STR_T-001)", "TSP743 (STR_T-001)"),
            Emulation.StarLine, "", PAPER_SIZE_THREE_INCH
        ))

        // TSP800II series
        put(TSP800II, ModelInfo(
            "TSP800II",
            arrayOf("TSP847II (STR_T-001)", "TSP847 (STR_T-001)"),
            Emulation.StarLine, "", PAPER_SIZE_FOUR_INCH
        ))

        // TUP500
        put(TUP500, ModelInfo(
            "TUP500",
            arrayOf("TUP592 (STR_T-001)", "TUP542 (STR_T-001)"),
            Emulation.StarLine, "", PAPER_SIZE_THREE_INCH
        ))

        // SP700 (dot impact)
        put(SP700, ModelInfo(
            "SP700",
            arrayOf("SP712 (STR-001)", "SP717 (STR-001)", "SP742 (STR-001)", "SP747 (STR-001)"),
            Emulation.StarDotImpact, "", PAPER_SIZE_DOT_THREE_INCH
        ))

        // SM-S series (EscPosMobile)
        put(SM_S210I, ModelInfo(
            "SM-S210i",
            arrayOf("SM-S210i-"),
            Emulation.EscPosMobile, "mini", PAPER_SIZE_TWO_INCH
        ))
        put(SM_S220I, ModelInfo(
            "SM-S220i",
            arrayOf("SM-S220i-"),
            Emulation.EscPosMobile, "mini", PAPER_SIZE_TWO_INCH
        ))
        put(SM_S230I, ModelInfo(
            "SM-S230i",
            arrayOf("SM-S230i-"),
            Emulation.EscPosMobile, "mini", PAPER_SIZE_TWO_INCH
        ))

        // SM-T series (EscPosMobile)
        put(SM_T300I_T300, ModelInfo(
            "SM-T300i/T300",
            arrayOf("SM-T300i-", "SM-T300-"),
            Emulation.EscPosMobile, "mini", PAPER_SIZE_THREE_INCH
        ))
        put(SM_T400I, ModelInfo(
            "SM-T400i",
            arrayOf("SM-T400i-"),
            Emulation.EscPosMobile, "mini", PAPER_SIZE_FOUR_INCH
        ))

        // SM-L series
        put(SM_L200, ModelInfo(
            "SM-L200",
            arrayOf("STAR L200-", "STAR L204-"),
            Emulation.StarPRNT, "Portable", PAPER_SIZE_TWO_INCH
        ))
        put(SM_L300, ModelInfo(
            "SM-L300",
            arrayOf("STAR L300-", "STAR L304-"),
            Emulation.StarPRNTL, "Portable", PAPER_SIZE_THREE_INCH
        ))

        // BSC10 series
        put(BSC10, ModelInfo(
            "BSC10",
            arrayOf("BSC10 (STR_T-001)", "BSC10", "Star BSC10"),
            Emulation.EscPos, "escpos", PAPER_SIZE_ESCPOS_THREE_INCH
        ))
        put(BSC10II, ModelInfo(
            "BSC10II",
            arrayOf("BSC10II (STR_T-001)", "BSC10II", "Star BSC10II"),
            Emulation.StarPRNT, "", PAPER_SIZE_ESCPOS_THREE_INCH
        ))

        // SM-S series (StarPRNT mode)
        put(SM_S210I_StarPRNT, ModelInfo(
            "SM-S210i StarPRNT",
            arrayOf(),
            Emulation.StarPRNT, "Portable", PAPER_SIZE_TWO_INCH
        ))
        put(SM_S220I_StarPRNT, ModelInfo(
            "SM-S220i StarPRNT",
            arrayOf(),
            Emulation.StarPRNT, "Portable", PAPER_SIZE_TWO_INCH
        ))
        put(SM_S230I_StarPRNT, ModelInfo(
            "SM-S230i StarPRNT",
            arrayOf(),
            Emulation.StarPRNT, "Portable", PAPER_SIZE_TWO_INCH
        ))

        // SM-T series (StarPRNT mode)
        put(SM_T300I_T300_StarPRNT, ModelInfo(
            "SM-T300i StarPRNT",
            arrayOf(),
            Emulation.StarPRNT, "Portable", PAPER_SIZE_THREE_INCH
        ))
        put(SM_T400I_StarPRNT, ModelInfo(
            "SM-T400i StarPRNT",
            arrayOf(),
            Emulation.StarPRNT, "Portable", PAPER_SIZE_FOUR_INCH
        ))

        // SK1 series
        put(SK1_211_221_V211, ModelInfo(
            "SK1-211/221/V211",
            arrayOf("SK1-211_221", "SK1-211", "SK1-221", "SK1-V211"),
            Emulation.StarPRNT, "", PAPER_SIZE_SK1_TWO_INCH
        ))
        put(SK1_211_221_V211_Presenter, ModelInfo(
            "SK1-211/221/V211 Presenter",
            arrayOf("SK1-211_221 Presenter", "SK1-211 Presenter", "SK1-221 Presenter"),
            Emulation.StarPRNT, "", PAPER_SIZE_SK1_TWO_INCH
        ))
        put(SK1_311_321_V311, ModelInfo(
            "SK1-311/321/V311",
            arrayOf("SK1-311_321", "SK1-311", "SK1-321", "SK1-V311"),
            Emulation.StarPRNT, "", PAPER_SIZE_THREE_INCH
        ))
        put(SK1_311_V311_Presenter, ModelInfo(
            "SK1-311/V311 Presenter",
            arrayOf("SK1-311 Presenter", "SK1-V311 Presenter"),
            Emulation.StarPRNT, "", PAPER_SIZE_THREE_INCH
        ))
    }

    fun getModelTitle(model: Int): String = mModelCapabilityMap[model].modelTitle

    fun getEmulation(model: Int): Emulation = mModelCapabilityMap[model].emulation

    fun getPortSettings(model: Int): String = mModelCapabilityMap[model].defaultPortSettings

    fun getPaperSize(model: Int): Int = mModelCapabilityMap[model].defaultPaperSize

    fun getModelIdx(modelNameSrc: String): Int {
        // Perfect match
        for (i in 0 until mModelCapabilityMap.size()) {
            for (modelName in mModelCapabilityMap.valueAt(i).modelNameArray) {
                if (modelNameSrc.equals(modelName, true)) {
                    return mModelCapabilityMap.keyAt(i)
                }
            }
        }
        // Partial match from the head
        for (i in 0 until mModelCapabilityMap.size()) {
            for (modelName in mModelCapabilityMap.valueAt(i).modelNameArray) {
                if (modelNameSrc.startsWith(modelName, true)) {
                    return mModelCapabilityMap.keyAt(i)
                }
            }
        }
        return NONE
    }

    fun getModelIdxByTitle(modelTitleIn: String): Int {
        for (i in 0 until mModelCapabilityMap.size()) {
            if (mModelCapabilityMap.valueAt(i).modelTitle.equals(modelTitleIn, true)) {
                return mModelCapabilityMap.keyAt(i)
            }
        }
        // Partial match
        for (i in 0 until mModelCapabilityMap.size()) {
            if (mModelCapabilityMap.valueAt(i).modelTitle.contains(modelTitleIn, true)) {
                return mModelCapabilityMap.keyAt(i)
            }
        }
        return NONE
    }

    fun getSupportedModels(): List<String> {
        val modelTitles = mutableListOf<String>()
        for (i in 0 until mModelCapabilityMap.size()) {
            modelTitles.add(mModelCapabilityMap.valueAt(i).modelTitle)
        }
        return modelTitles
    }

    internal class ModelInfo(
        var modelTitle: String,
        var modelNameArray: Array<String>,
        var emulation: Emulation,
        var defaultPortSettings: String,
        var defaultPaperSize: Int
    )
}

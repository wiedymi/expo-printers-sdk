package expo.modules.printers.starmicronics.internals

import android.util.SparseArray
import com.starmicronics.starioextension.StarIoExt.Emulation
import com.starmicronics.starioextension.StarIoExt.LedModel

/**
 * This class expresses printer settings that should use on each model
 * and which printer function each model can use.
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

    // V5.3.0
    const val SM_L300 = 19

    // V5.6.0
    const val MC_PRINT2 = 20
    const val MC_PRINT3 = 21

    // V5.11.0
    const val TUP500 = 22

    // V5.12.0
    const val SK1_211_221_V211 = 23
    const val SK1_211_221_V211_Presenter = 24
    const val SK1_311_321_V311 = 25
    const val SK1_311_V311_Presenter = 26

    private val mModelCapabilityMap: SparseArray<ModelInfo> = object : SparseArray<ModelInfo>() {
        init {
            put(
                MC_PRINT2, ModelInfo(
                    "mC-Print2", arrayOf( // modelNameArray
                        "MCP20 (STR-001)",  // <-LAN interface
                        "MCP21 (STR-001)",
                        "mC-Print2-",  // <-Bluetooth interface
                        "mC-Print2"
                    ),  // <-USB interface
                    Emulation.StarPRNT,  // Emulation
                    "",  // Default portSettings
                    PAPER_SIZE_TWO_INCH,  // Default paper size
                    true,  // canSetDrawerOpenStatus
                    true,  // canPrintTextReceiptSample
                    true,  // canPrintUtf8EncodedText
                    true,  // canPrintRasterReceiptSample
                    true,  // canPrintCjk
                    false,  // canUseBlackMark
                    false,  // canUseBlackMarkDetection
                    true,  // canUsePageMode
                    true,  // canUseCashDrawer
                    true,  // canUseBarcodeReader
                    true,  // canUseCustomerDisplay
                    false,  // canUsePresenter
                    false,  // canUseLed
                    LedModel.None,  // ledModel
                    false,  // canUseBlinkLed
                    false,  // canUsePaperPresentStatus
                    true,  // canUseAllReceipt
                    true,  // canGetProductSerialNumber
                    16,  // settableUsbSerialNumberLength
                    true,  // isUsbSerialNumberEnabledByDefault
                    false,  // canUseMelodySpeaker
                    -1,  // defaultSoundNumber
                    -1,  // defaultVolume
                    -1,  // volumeMax
                    -1 // volumeMin
                )
            )
            put(
                MC_PRINT3, ModelInfo(
                    "mC-Print3", arrayOf( // modelNameArray
                        "MCP30 (STR-001)",  // <-LAN interface
                        "MCP31 (STR-001)",
                        "mC-Print3-",  // <-Bluetooth interface
                        "mC-Print3"
                    ),  // <-USB interface
                    Emulation.StarPRNT,  // Emulation
                    "",  // Default portSettings
                    PAPER_SIZE_THREE_INCH,  // Default paper size
                    true,  // canSetDrawerOpenStatus
                    true,  // canPrintTextReceiptSample
                    true,  // canPrintUtf8EncodedText
                    true,  // canPrintRasterReceiptSample
                    true,  // canPrintCjk
                    false,  // canUseBlackMark
                    false,  // canUseBlackMarkDetection
                    true,  // canUsePageMode
                    true,  // canUseCashDrawer
                    true,  // canUseBarcodeReader
                    true,  // canUseCustomerDisplay
                    false,  // canUsePresenter
                    false,  // canUseLed
                    LedModel.None,  // ledModel
                    false,  // canUseBlinkLed
                    false,  // canUsePaperPresentStatus
                    true,  // canUseAllReceipt
                    true,  // canGetProductSerialNumber
                    16,  // settableUsbSerialNumberLength
                    true,  // isUsbSerialNumberEnabledByDefault
                    true,  // canUseMelodySpeaker
                    0,  // defaultSoundNumber
                    12,  // defaultVolume
                    15,  // volumeMax
                    0 // volumeMin
                )
            )
            put(
                MPOP, ModelInfo(
                    "mPOP", arrayOf( // modelNameArray
                        "STAR mPOP-",  // <-Bluetooth interface
                        "mPOP"
                    ),  // <-USB interface
                    Emulation.StarPRNT,  // Emulation
                    "",  // Default portSettings
                    PAPER_SIZE_TWO_INCH,  // Default paper size
                    false,  // canSetDrawerOpenStatus
                    true,  // canPrintTextReceiptSample
                    true,  // canPrintUtf8EncodedText
                    true,  // canPrintRasterReceiptSample
                    false,  // canPrintCjk
                    false,  // canUseBlackMark
                    false,  // canUseBlackMarkDetection
                    true,  // canUsePageMode
                    true,  // canUseCashDrawer
                    true,  // canUseBarcodeReader
                    true,  // canUseCustomerDisplay
                    false,  // canUsePresenter
                    false,  // canUseLed
                    LedModel.None,  // ledModel
                    false,  // canUseBlinkLed
                    false,  // canUsePaperPresentStatus
                    true,  // canUseAllReceipt
                    true,  // canGetProductSerialNumber
                    8,  // settableUsbSerialNumberLength
                    false,  // isUsbSerialNumberEnabledByDefault
                    false,  // canUseMelodySpeaker
                    -1,  // defaultSoundNumber
                    -1,  // defaultVolume
                    -1,  // volumeMax
                    -1 // volumeMin
                )
            )
            put(
                FVP10, ModelInfo(
                    "FVP10", arrayOf( // modelNameArray
                        "FVP10 (STR_T-001)",  // <-LAN interface
                        "Star FVP10"
                    ),  // <-USB interface
                    Emulation.StarLine,  // Emulation
                    "",  // Default portSettings
                    PAPER_SIZE_THREE_INCH,  // Default paper size
                    true,  // canSetDrawerOpenStatus
                    true,  // canPrintTextReceiptSample
                    true,  // canPrintUtf8EncodedText
                    true,  // canPrintRasterReceiptSample
                    false,  // canPrintCjk
                    true,  // canUseBlackMark
                    true,  // canUseBlackMarkDetection
                    true,  // canUsePageMode
                    true,  // canUseCashDrawer
                    false,  // canUseBarcodeReader
                    false,  // canUseCustomerDisplay
                    false,  // canUsePresenter
                    false,  // canUseLed
                    LedModel.None,  // ledModel
                    false,  // canUseBlinkLed
                    false,  // canUsePaperPresentStatus
                    true,  // canUseAllReceipt
                    false,  // canGetProductSerialNumber
                    8,  // settableUsbSerialNumberLength
                    false,  // isUsbSerialNumberEnabledByDefault
                    true,  // canUseMelodySpeaker
                    1,  // defaultSoundNumber
                    -1,  // defaultVolume
                    -1,  // volumeMax
                    -1 // volumeMin
                )
            )
            put(
                TSP100, ModelInfo(
                    "TSP100", arrayOf( // modelNameArray
                        "TSP113", "TSP143",  // <-LAN model
                        "TSP100-",  // <-Bluetooth model
                        "Star TSP113", "Star TSP143"
                    ),  // <-USB model
                    Emulation.StarGraphic,  // Emulation
                    "",  // Default portSettings
                    PAPER_SIZE_THREE_INCH,  // Default paper size
                    true,  // canSetDrawerOpenStatus
                    false,  // canPrintTextReceiptSample
                    false,  // canPrintUtf8EncodedText
                    true,  // canPrintRasterReceiptSample
                    false,  // canPrintCjk
                    false,  // canUseBlackMark
                    false,  // canUseBlackMarkDetection
                    false,  // canUsePageMode
                    true,  // canUseCashDrawer
                    false,  // canUseBarcodeReader
                    true,  // canUseCustomerDisplay
                    false,  // canUsePresenter
                    false,  // canUseLed
                    LedModel.None,  // ledModel
                    false,  // canUseBlinkLed
                    false,  // canUsePaperPresentStatus
                    true,  // canUseAllReceipt
                    true,  // canGetProductSerialNumber
                    8,  // settableUsbSerialNumberLength
                    false,  // isUsbSerialNumberEnabledByDefault
                    false,  // canUseMelodySpeaker
                    -1,  // defaultSoundNumber
                    -1,  // defaultVolume
                    -1,  // volumeMax
                    -1 // volumeMin
                )
            )
            put(
                TSP650II, ModelInfo(
                    "TSP650II", arrayOf( // modelNameArray
                        "TSP654II (STR_T-001)",  // Only LAN model->
                        "TSP654 (STR_T-001)",
                        "TSP651 (STR_T-001)"
                    ),
                    Emulation.StarLine,  // Emulation
                    "",  // Default portSettings
                    PAPER_SIZE_THREE_INCH,  // Default paper size
                    true,  // canSetDrawerOpenStatus
                    true,  // canPrintTextReceiptSample
                    true,  // canPrintUtf8EncodedText
                    true,  // canPrintRasterReceiptSample
                    true,  // canPrintCjk
                    false,  // canUseBlackMark
                    false,  // canUseBlackMarkDetection
                    true,  // canUsePageMode
                    true,  // canUseCashDrawer
                    false,  // canUseBarcodeReader
                    false,  // canUseCustomerDisplay
                    false,  // canUsePresenter
                    false,  // canUseLed
                    LedModel.None,  // ledModel
                    false,  // canUseBlinkLed
                    false,  // canUsePaperPresentStatus
                    true,  // canUseAllReceipt
                    false,  // canGetProductSerialNumber
                    8,  // settableUsbSerialNumberLength
                    false,  // isUsbSerialNumberEnabledByDefault
                    false,  // canUseMelodySpeaker
                    -1,  // defaultSoundNumber
                    -1,  // defaultVolume
                    -1,  // volumeMax
                    -1 // volumeMin
                )
            )
            put(
                TSP700II, ModelInfo(
                    "TSP700II", arrayOf( // modelNameArray
                        "TSP743II (STR_T-001)",
                        "TSP743 (STR_T-001)"
                    ),
                    Emulation.StarLine,  // Emulation
                    "",  // Default portSettings
                    PAPER_SIZE_THREE_INCH,  // Default paper size
                    true,  // canSetDrawerOpenStatus
                    true,  // canPrintTextReceiptSample
                    true,  // canPrintUtf8EncodedText
                    true,  // canPrintRasterReceiptSample
                    false,  // canPrintCjk
                    true,  // canUseBlackMark
                    true,  // canUseBlackMarkDetection
                    false,  // canUsePageMode
                    true,  // canUseCashDrawer
                    false,  // canUseBarcodeReader
                    false,  // canUseCustomerDisplay
                    false,  // canUsePresenter
                    false,  // canUseLed
                    LedModel.None,  // ledModel
                    false,  // canUseBlinkLed
                    false,  // canUsePaperPresentStatus
                    true,  // canUseAllReceipt
                    false,  // canGetProductSerialNumber
                    8,  // settableUsbSerialNumberLength
                    false,  // isUsbSerialNumberEnabledByDefault
                    false,  // canUseMelodySpeaker
                    -1,  // defaultSoundNumber
                    -1,  // defaultVolume
                    -1,  // volumeMax
                    -1 // volumeMin
                )
            )
            put(
                TSP800II, ModelInfo(
                    "TSP800II", arrayOf( // modelNameArray
                        "TSP847II (STR_T-001)",
                        "TSP847 (STR_T-001)"
                    ),  // <-Only LAN model
                    Emulation.StarLine,  // Emulation
                    "",  // Default portSettings
                    PAPER_SIZE_FOUR_INCH,  // Default paper size
                    true,  // canSetDrawerOpenStatus
                    true,  // canPrintTextReceiptSample
                    true,  // canPrintUtf8EncodedText
                    true,  // canPrintRasterReceiptSample
                    false,  // canPrintCjk
                    true,  // canUseBlackMark
                    true,  // canUseBlackMarkDetection
                    false,  // canUsePageMode
                    true,  // canUseCashDrawer
                    false,  // canUseBarcodeReader
                    false,  // canUseCustomerDisplay
                    false,  // canUsePresenter
                    false,  // canUseLed
                    LedModel.None,  // ledModel
                    false,  // canUseBlinkLed
                    false,  // canUsePaperPresentStatus
                    true,  // canUseAllReceipt
                    false,  // canGetProductSerialNumber
                    8,  // settableUsbSerialNumberLength
                    false,  // isUsbSerialNumberEnabledByDefault
                    false,  // canUseMelodySpeaker
                    -1,  // defaultSoundNumber
                    -1,  // defaultVolume
                    -1,  // volumeMax
                    -1 // volumeMin
                )
            )
            put(
                TUP500, ModelInfo(
                    "TUP500", arrayOf( // modelNameArray
                        "TUP592 (STR_T-001)",
                        "TUP542 (STR_T-001)"
                    ),
                    Emulation.StarLine,  // Emulation
                    "",  // Default portSettings
                    PAPER_SIZE_THREE_INCH,  // Default paper size
                    false,  // canSetDrawerOpenStatus
                    true,  // canPrintTextReceiptSample
                    false,  // canPrintUtf8EncodedText
                    true,  // canPrintRasterReceiptSample
                    false,  // canPrintCjk
                    true,  // canUseBlackMark
                    true,  // canUseBlackMarkDetection
                    false,  // canUsePageMode
                    false,  // canUseCashDrawer
                    false,  // canUseBarcodeReader
                    false,  // canUseCustomerDisplay
                    true,  // canUsePresenter
                    true,  // canUseLed
                    LedModel.Star,  // ledModel
                    true,  // canUseBlinkLed
                    false,  // canUsePaperPresentStatus
                    true,  // canUseAllReceipt
                    false,  // canGetProductSerialNumber
                    8,  // settableUsbSerialNumberLength
                    false,  // isUsbSerialNumberEnabledByDefault
                    false,  // canUseMelodySpeaker
                    1,  // defaultSoundNumber
                    -1,  // defaultVolume
                    -1,  // volumeMax
                    -1 // volumeMin
                )
            )
            put(
                SP700, ModelInfo(
                    "SP700", arrayOf( // modelNameArray
                        "SP712 (STR-001)",  // Only LAN model
                        "SP717 (STR-001)",
                        "SP742 (STR-001)",
                        "SP747 (STR-001)"
                    ),
                    Emulation.StarDotImpact,  // Emulation
                    "",  // Default portSettings
                    PAPER_SIZE_DOT_THREE_INCH,  // Default paper size
                    true,  // canSetDrawerOpenStatus
                    true,  // canPrintTextReceiptSample
                    true,  // canPrintUtf8EncodedText
                    false,  // canPrintRasterReceiptSample
                    false,  // canPrintCjk
                    true,  // canUseBlackMark
                    true,  // canUseBlackMarkDetection
                    false,  // canUsePageMode
                    true,  // canUseCashDrawer
                    false,  // canUseBarcodeReader
                    false,  // canUseCustomerDisplay
                    false,  // canUsePresenter
                    false,  // canUseLed
                    LedModel.None,  // ledModel
                    false,  // canUseBlinkLed
                    false,  // canUsePaperPresentStatus
                    false,  // canUseAllReceipt
                    false,  // canGetProductSerialNumber
                    8,  // settableUsbSerialNumberLength
                    false,  // isUsbSerialNumberEnabledByDefault
                    false,  // canUseMelodySpeaker
                    -1,  // defaultSoundNumber
                    -1,  // defaultVolume
                    -1,  // volumeMax
                    -1 // volumeMin
                )
            )
            put(
                SM_S210I, ModelInfo(
                    "SM-S210i", arrayOf(),  // modelNameArray
                    Emulation.EscPosMobile,  // Emulation
                    "mini",  // Default portSettings
                    PAPER_SIZE_TWO_INCH,  // Default paper size
                    false,  // canSetDrawerOpenStatus
                    true,  // canPrintTextReceiptSample
                    false,  // canPrintUtf8EncodedText
                    true,  // canPrintRasterReceiptSample
                    false,  // canPrintCjk
                    false,  // canUseBlackMark
                    false,  // canUseBlackMarkDetection
                    true,  // canUsePageMode
                    false,  // canUseCashDrawer
                    false,  // canUseBarcodeReader
                    false,  // canUseCustomerDisplay
                    false,  // canUsePresenter
                    false,  // canUseLed
                    LedModel.None,  // ledModel
                    false,  // canUseBlinkLed
                    false,  // canUsePaperPresentStatus
                    true,  // canUseAllReceipt
                    false,  // canGetProductSerialNumber
                    0,  // settableUsbSerialNumberLength
                    false,  // isUsbSerialNumberEnabledByDefault
                    false,  // canUseMelodySpeaker
                    -1,  // defaultSoundNumber
                    -1,  // defaultVolume
                    -1,  // volumeMax
                    -1 // volumeMin
                )
            )
            put(
                SM_S220I, ModelInfo(
                    "SM-S220i", arrayOf(),  // modelNameArray
                    Emulation.EscPosMobile,  // Emulation
                    "mini",  // Default portSettings
                    PAPER_SIZE_TWO_INCH,  // Default paper size
                    false,  // canSetDrawerOpenStatus
                    true,  // canPrintTextReceiptSample
                    false,  // canPrintUtf8EncodedText
                    true,  // canPrintRasterReceiptSample
                    false,  // canPrintCjk
                    false,  // canUseBlackMark
                    false,  // canUseBlackMarkDetection
                    true,  // canUsePageMode
                    false,  // canUseCashDrawer
                    false,  // canUseBarcodeReader
                    false,  // canUseCustomerDisplay
                    false,  // canUsePresenter
                    false,  // canUseLed
                    LedModel.None,  // ledModel
                    false,  // canUseBlinkLed
                    false,  // canUsePaperPresentStatus
                    true,  // canUseAllReceipt
                    false,  // canGetProductSerialNumber
                    0,  // settableUsbSerialNumberLength
                    false,  // isUsbSerialNumberEnabledByDefault
                    false,  // canUseMelodySpeaker
                    -1,  // defaultSoundNumber
                    -1,  // defaultVolume
                    -1,  // volumeMax
                    -1 // volumeMin
                )
            )
            put(
                SM_S230I, ModelInfo(
                    "SM-S230i", arrayOf(),  // modelNameArray
                    Emulation.EscPosMobile,  // Emulation
                    "mini",  // Default portSettings
                    PAPER_SIZE_TWO_INCH,  // Default paper size
                    false,  // canSetDrawerOpenStatus
                    true,  // canPrintTextReceiptSample
                    false,  // canPrintUtf8EncodedText
                    true,  // canPrintRasterReceiptSample
                    false,  // canPrintCjk
                    false,  // canUseBlackMark
                    false,  // canUseBlackMarkDetection
                    true,  // canUsePageMode
                    false,  // canUseCashDrawer
                    false,  // canUseBarcodeReader
                    false,  // canUseCustomerDisplay
                    false,  // canUsePresenter
                    false,  // canUseLed
                    LedModel.None,  // ledModel
                    false,  // canUseBlinkLed
                    false,  // canUsePaperPresentStatus
                    true,  // canUseAllReceipt
                    false,  // canGetProductSerialNumber
                    0,  // settableUsbSerialNumberLength
                    false,  // isUsbSerialNumberEnabledByDefault
                    false,  // canUseMelodySpeaker
                    -1,  // defaultSoundNumber
                    -1,  // defaultVolume
                    -1,  // volumeMax
                    -1 // volumeMin
                )
            )
            put(
                SM_T300I_T300, ModelInfo(
                    "SM-T300i/T300", arrayOf(),  // modelNameArray
                    Emulation.EscPosMobile,  // Emulation
                    "mini",  // Default portSettings
                    PAPER_SIZE_THREE_INCH,  // Default paper size
                    false,  // canSetDrawerOpenStatus
                    true,  // canPrintTextReceiptSample
                    false,  // canPrintUtf8EncodedText
                    true,  // canPrintRasterReceiptSample
                    false,  // canPrintCjk
                    true,  // canUseBlackMark
                    false,  // canUseBlackMarkDetection
                    true,  // canUsePageMode
                    false,  // canUseCashDrawer
                    false,  // canUseBarcodeReader
                    false,  // canUseCustomerDisplay
                    false,  // canUsePresenter
                    false,  // canUseLed
                    LedModel.None,  // ledModel
                    false,  // canUseBlinkLed
                    false,  // canUsePaperPresentStatus
                    true,  // canUseAllReceipt
                    false,  // canGetProductSerialNumber
                    0,  // settableUsbSerialNumberLength
                    false,  // isUsbSerialNumberEnabledByDefault
                    false,  // canUseMelodySpeaker
                    -1,  // defaultSoundNumber
                    -1,  // defaultVolume
                    -1,  // volumeMax
                    -1 // volumeMin
                )
            )
            put(
                SM_T400I, ModelInfo(
                    "SM-T400i", arrayOf(),  // modelNameArray
                    Emulation.EscPosMobile,  // Emulation
                    "mini",  // Default portSettings
                    PAPER_SIZE_FOUR_INCH,  // Default paper size
                    false,  // canSetDrawerOpenStatus
                    true,  // canPrintTextReceiptSample
                    false,  // canPrintUtf8EncodedText
                    true,  // canPrintRasterReceiptSample
                    false,  // canPrintCjk
                    true,  // canUseBlackMark
                    false,  // canUseBlackMarkDetection
                    true,  // canUsePageMode
                    false,  // canUseCashDrawer
                    false,  // canUseBarcodeReader
                    false,  // canUseCustomerDisplay
                    false,  // canUsePresenter
                    false,  // canUseLed
                    LedModel.None,  // ledModel
                    false,  // canUseBlinkLed
                    false,  // canUsePaperPresentStatus
                    true,  // canUseAllReceipt
                    false,  // canGetProductSerialNumber
                    0,  // settableUsbSerialNumberLength
                    false,  // isUsbSerialNumberEnabledByDefault
                    false,  // canUseMelodySpeaker
                    -1,  // defaultSoundNumber
                    -1,  // defaultVolume
                    -1,  // volumeMax
                    -1 // volumeMin
                )
            )
            put(
                SM_L200, ModelInfo(
                    "SM-L200", arrayOf( // modelNameArray
                        "STAR L200-",
                        "STAR L204-"
                    ),  // <-Bluetooth interface
                    Emulation.StarPRNT,  // Emulation
                    "Portable",  // Default portSettings
                    PAPER_SIZE_TWO_INCH,  // Default paper size
                    false,  // canSetDrawerOpenStatus
                    true,  // canPrintTextReceiptSample
                    false,  // canPrintUtf8EncodedText
                    true,  // canPrintRasterReceiptSample
                    false,  // canPrintCjk
                    true,  // canUseBlackMark
                    true,  // canUseBlackMarkDetection
                    true,  // canUsePageMode
                    false,  // canUseCashDrawer
                    false,  // canUseBarcodeReader
                    false,  // canUseCustomerDisplay
                    false,  // canUsePresenter
                    false,  // canUseLed
                    LedModel.None,  // ledModel
                    false,  // canUseBlinkLed
                    false,  // canUsePaperPresentStatus
                    true,  // canUseAllReceipt
                    false,  // canGetProductSerialNumber
                    0,  // settableUsbSerialNumberLength
                    false,  // isUsbSerialNumberEnabledByDefault
                    false,  // canUseMelodySpeaker
                    -1,  // defaultSoundNumber
                    -1,  // defaultVolume
                    -1,  // volumeMax
                    -1 // volumeMin
                )
            )
            put(
                SM_L300, ModelInfo(
                    "SM-L300", arrayOf( // modelNameArray
                        "STAR L300-",
                        "STAR L304-"
                    ),  // <-Bluetooth interface
                    Emulation.StarPRNTL,  // Emulation
                    "Portable",  // Default portSettings
                    PAPER_SIZE_THREE_INCH,  // Default paper size
                    false,  // canSetDrawerOpenStatus
                    true,  // canPrintTextReceiptSample
                    false,  // canPrintUtf8EncodedText
                    true,  // canPrintRasterReceiptSample
                    false,  // canPrintCjk
                    true,  // canUseBlackMark
                    false,  // canUseBlackMarkDetection
                    true,  // canUsePageMode
                    false,  // canUseCashDrawer
                    false,  // canUseBarcodeReader
                    false,  // canUseCustomerDisplay
                    false,  // canUsePresenter
                    false,  // canUseLed
                    LedModel.None,  // ledModel
                    false,  // canUseBlinkLed
                    false,  // canUsePaperPresentStatus
                    true,  // canUseAllReceipt
                    false,  // canGetProductSerialNumber
                    0,  // settableUsbSerialNumberLength
                    false,  // isUsbSerialNumberEnabledByDefault
                    false,  // canUseMelodySpeaker
                    -1,  // defaultSoundNumber
                    -1,  // defaultVolume
                    -1,  // volumeMax
                    -1 // volumeMin
                )
            )
            put(
                BSC10, ModelInfo(
                    "BSC10", arrayOf( // modelNameArray
                        "BSC10",  // <-LAN model
                        "Star BSC10"
                    ),  // <-USB model
                    Emulation.EscPos,  // Emulation
                    "escpos",  // Default portSettings
                    PAPER_SIZE_ESCPOS_THREE_INCH,  // Default paper size
                    true,  // canSetDrawerOpenStatus
                    true,  // canPrintTextReceiptSample
                    false,  // canPrintUtf8EncodedText
                    true,  // canPrintRasterReceiptSample
                    false,  // canPrintCjk
                    false,  // canUseBlackMark
                    false,  // canUseBlackMarkDetection
                    true,  // canUsePageMode
                    true,  // canUseCashDrawer
                    false,  // canUseBarcodeReader
                    false,  // canUseCustomerDisplay
                    false,  // canUsePresenter
                    false,  // canUseLed
                    LedModel.None,  // ledModel
                    false,  // canUseBlinkLed
                    false,  // canUsePaperPresentStatus
                    true,  // canUseAllReceipt
                    false,  // canGetProductSerialNumber
                    8,  // settableUsbSerialNumberLength
                    false,  // isUsbSerialNumberEnabledByDefault
                    false,  // canUseMelodySpeaker
                    -1,  // defaultSoundNumber
                    -1,  // defaultVolume
                    -1,  // volumeMax
                    -1 // volumeMin
                )
            )
            put(
                SM_S210I_StarPRNT, ModelInfo(
                    "SM-S210i StarPRNT", arrayOf(),  // modelNameArray
                    Emulation.StarPRNT,  // Emulation
                    "Portable",  // Default portSettings
                    PAPER_SIZE_TWO_INCH,  // Default paper size
                    false,  // canSetDrawerOpenStatus
                    true,  // canPrintTextReceiptSample
                    false,  // canPrintUtf8EncodedText
                    true,  // canPrintRasterReceiptSample
                    false,  // canPrintCjk
                    false,  // canUseBlackMark
                    false,  // canUseBlackMarkDetection
                    true,  // canUsePageMode
                    false,  // canUseCashDrawer
                    false,  // canUseBarcodeReader
                    false,  // canUseCustomerDisplay
                    false,  // canUsePresenter
                    false,  // canUseLed
                    LedModel.None,  // ledModel
                    false,  // canUseBlinkLed
                    false,  // canUsePaperPresentStatus
                    true,  // canUseAllReceipt
                    false,  // canGetProductSerialNumber
                    0,  // settableUsbSerialNumberLength
                    false,  // isUsbSerialNumberEnabledByDefault
                    false,  // canUseMelodySpeaker
                    -1,  // defaultSoundNumber
                    -1,  // defaultVolume
                    -1,  // volumeMax
                    -1 // volumeMin
                )
            )
            put(
                SM_S220I_StarPRNT, ModelInfo(
                    "SM-S220i StarPRNT", arrayOf(),  // modelNameArray
                    Emulation.StarPRNT,  // Emulation
                    "Portable",  // Default portSettings
                    PAPER_SIZE_TWO_INCH,  // Default paper size
                    false,  // canSetDrawerOpenStatus
                    true,  // canPrintTextReceiptSample
                    false,  // canPrintUtf8EncodedText
                    true,  // canPrintRasterReceiptSample
                    false,  // canPrintCjk
                    false,  // canUseBlackMark
                    false,  // canUseBlackMarkDetection
                    true,  // canUsePageMode
                    false,  // canUseCashDrawer
                    false,  // canUseBarcodeReader
                    false,  // canUseCustomerDisplay
                    false,  // canUsePresenter
                    false,  // canUseLed
                    LedModel.None,  // ledModel
                    false,  // canUseBlinkLed
                    false,  // canUsePaperPresentStatus
                    true,  // canUseAllReceipt
                    false,  // canGetProductSerialNumber
                    0,  // settableUsbSerialNumberLength
                    false,  // isUsbSerialNumberEnabledByDefault
                    false,  // canUseMelodySpeaker
                    -1,  // defaultSoundNumber
                    -1,  // defaultVolume
                    -1,  // volumeMax
                    -1 // volumeMin
                )
            )
            put(
                SM_S230I_StarPRNT, ModelInfo(
                    "SM-S230i StarPRNT", arrayOf(),  // modelNameArray
                    Emulation.StarPRNT,  // Emulation
                    "Portable",  // Default portSettings
                    PAPER_SIZE_TWO_INCH,  // Default paper size
                    false,  // canSetDrawerOpenStatus
                    true,  // canPrintTextReceiptSample
                    false,  // canPrintUtf8EncodedText
                    true,  // canPrintRasterReceiptSample
                    false,  // canPrintCjk
                    false,  // canUseBlackMark
                    false,  // canUseBlackMarkDetection
                    true,  // canUsePageMode
                    false,  // canUseCashDrawer
                    false,  // canUseBarcodeReader
                    false,  // canUseCustomerDisplay
                    false,  // canUsePresenter
                    false,  // canUseLed
                    LedModel.None,  // ledModel
                    false,  // canUseBlinkLed
                    false,  // canUsePaperPresentStatus
                    true,  // canUseAllReceipt
                    false,  // canGetProductSerialNumber
                    8,  // settableUsbSerialNumberLength
                    false,  // isUsbSerialNumberEnabledByDefault
                    false,  // canUseMelodySpeaker
                    -1,  // defaultSoundNumber
                    -1,  // defaultVolume
                    -1,  // volumeMax
                    -1 // volumeMin
                )
            )
            put(
                SM_T300I_T300_StarPRNT, ModelInfo(
                    "SM-T300i StarPRNT", arrayOf(),  // modelNameArray
                    Emulation.StarPRNT,  // Emulation
                    "Portable",  // Default portSettings
                    PAPER_SIZE_THREE_INCH,  // Default paper size
                    false,  // canSetDrawerOpenStatus
                    true,  // canPrintTextReceiptSample
                    false,  // canPrintUtf8EncodedText
                    true,  // canPrintRasterReceiptSample
                    false,  // canPrintCjk
                    true,  // canUseBlackMark
                    true,  // canUseBlackMarkDetection
                    true,  // canUsePageMode
                    false,  // canUseCashDrawer
                    false,  // canUseBarcodeReader
                    false,  // canUseCustomerDisplay
                    false,  // canUsePresenter
                    false,  // canUseLed
                    LedModel.None,  // ledModel
                    false,  // canUseBlinkLed
                    false,  // canUsePaperPresentStatus
                    true,  // canUseAllReceipt
                    false,  // canGetProductSerialNumber
                    0,  // settableUsbSerialNumberLength
                    false,  // isUsbSerialNumberEnabledByDefault
                    false,  // canUseMelodySpeaker
                    -1,  // defaultSoundNumber
                    -1,  // defaultVolume
                    -1,  // volumeMax
                    -1 // volumeMin
                )
            )
            put(
                SM_T400I_StarPRNT, ModelInfo(
                    "SM-T400i StarPRNT", arrayOf(),  // modelNameArray
                    Emulation.StarPRNT,  // Emulation
                    "Portable",  // Default portSettings
                    PAPER_SIZE_FOUR_INCH,  // Default paper size
                    false,  // canSetDrawerOpenStatus
                    true,  // canPrintTextReceiptSample
                    false,  // canPrintUtf8EncodedText
                    true,  // canPrintRasterReceiptSample
                    false,  // canPrintCjk
                    true,  // canUseBlackMark
                    true,  // canUseBlackMarkDetection
                    true,  // canUsePageMode
                    false,  // canUseCashDrawer
                    false,  // canUseBarcodeReader
                    false,  // canUseCustomerDisplay
                    false,  // canUsePresenter
                    false,  // canUseLed
                    LedModel.None,  // ledModel
                    false,  // canUseBlinkLed
                    false,  // canUsePaperPresentStatus
                    true,  // canUseAllReceipt
                    false,  // canGetProductSerialNumber
                    0,  // settableUsbSerialNumberLength
                    false,  // isUsbSerialNumberEnabledByDefault
                    false,  // canUseMelodySpeaker
                    -1,  // defaultSoundNumber
                    -1,  // defaultVolume
                    -1,  // volumeMax
                    -1 // volumeMin
                )
            )
            put(
                SK1_211_221_V211, ModelInfo(
                    "SK1-211/221/V211", arrayOf( // modelNameArray
                        "SK1-211_221"
                    ),  // <-USB interface
                    Emulation.StarPRNT,  // Emulation
                    "",  // Default portSettings
                    PAPER_SIZE_SK1_TWO_INCH,  // Default paper size
                    false,  // canSetDrawerOpenStatus
                    true,  // canPrintTextReceiptSample
                    true,  // canPrintUtf8EncodedText
                    true,  // canPrintRasterReceiptSample
                    false,  // canPrintCjk
                    true,  // canUseBlackMark
                    true,  // canUseBlackMarkDetection
                    true,  // canUsePageMode
                    false,  // canUseCashDrawer
                    false,  // canUseBarcodeReader
                    false,  // canUseCustomerDisplay
                    false,  // canUsePresenter
                    true,  // canUseLed
                    LedModel.SK,  // ledModel
                    false,  // canUseBlinkLed
                    true,  // canUsePaperPresentStatus
                    true,  // canUseAllReceipt
                    false,  // canGetProductSerialNumber
                    16,  // settableUsbSerialNumberLength
                    true,  // isUsbSerialNumberEnabledByDefault
                    false,  // canUseMelodySpeaker
                    -1,  // defaultSoundNumber
                    -1,  // defaultVolume
                    -1,  // volumeMax
                    -1 // volumeMin
                )
            )
            put(
                SK1_211_221_V211_Presenter, ModelInfo(
                    "SK1-211/221/V211 Presenter", arrayOf( // modelNameArray
                        "SK1-211_221 Presenter"
                    ),  // <-USB interface
                    Emulation.StarPRNT,  // Emulation
                    "",  // Default portSettings
                    PAPER_SIZE_SK1_TWO_INCH,  // Default paper size
                    false,  // canSetDrawerOpenStatus
                    true,  // canPrintTextReceiptSample
                    true,  // canPrintUtf8EncodedText
                    true,  // canPrintRasterReceiptSample
                    false,  // canPrintCjk
                    true,  // canUseBlackMark
                    true,  // canUseBlackMarkDetection
                    true,  // canUsePageMode
                    false,  // canUseCashDrawer
                    false,  // canUseBarcodeReader
                    false,  // canUseCustomerDisplay
                    true,  // canUsePresenter
                    true,  // canUseLed
                    LedModel.SK,  // ledModel
                    true,  // canUseBlinkLed
                    false,  // canUsePaperPresentStatus
                    true,  // canUseAllReceipt
                    false,  // canGetProductSerialNumber
                    16,  // settableUsbSerialNumberLength
                    true,  // isUsbSerialNumberEnabledByDefault
                    false,  // canUseMelodySpeaker
                    -1,  // defaultSoundNumber
                    -1,  // defaultVolume
                    -1,  // volumeMax
                    -1 // volumeMin
                )
            )
            put(
                SK1_311_321_V311, ModelInfo(
                    "SK1-311/321/V311", arrayOf( // modelNameArray
                        "SK1-311_321"
                    ),  // <-USB interface
                    Emulation.StarPRNT,  // Emulation
                    "",  // Default portSettings
                    PAPER_SIZE_THREE_INCH,  // Default paper size
                    false,  // canSetDrawerOpenStatus
                    true,  // canPrintTextReceiptSample
                    true,  // canPrintUtf8EncodedText
                    true,  // canPrintRasterReceiptSample
                    false,  // canPrintCjk
                    true,  // canUseBlackMark
                    true,  // canUseBlackMarkDetection
                    true,  // canUsePageMode
                    false,  // canUseCashDrawer
                    false,  // canUseBarcodeReader
                    false,  // canUseCustomerDisplay
                    false,  // canUsePresenter
                    true,  // canUseLed
                    LedModel.SK,  // ledModel
                    false,  // canUseBlinkLed
                    true,  // canUsePaperPresentStatus
                    true,  // canUseAllReceipt
                    false,  // canGetProductSerialNumber
                    16,  // settableUsbSerialNumberLength
                    true,  // isUsbSerialNumberEnabledByDefault
                    false,  // canUseMelodySpeaker
                    -1,  // defaultSoundNumber
                    -1,  // defaultVolume
                    -1,  // volumeMax
                    -1 // volumeMin
                )
            )
            put(
                SK1_311_V311_Presenter, ModelInfo(
                    "SK1-311/V311 Presenter", arrayOf( // modelNameArray
                        "SK1-311 Presenter"
                    ),  // <-USB interface
                    Emulation.StarPRNT,  // Emulation
                    "",  // Default portSettings
                    PAPER_SIZE_THREE_INCH,  // Default paper size
                    false,  // canSetDrawerOpenStatus
                    true,  // canPrintTextReceiptSample
                    true,  // canPrintUtf8EncodedText
                    true,  // canPrintRasterReceiptSample
                    false,  // canPrintCjk
                    true,  // canUseBlackMark
                    true,  // canUseBlackMarkDetection
                    true,  // canUsePageMode
                    false,  // canUseCashDrawer
                    false,  // canUseBarcodeReader
                    false,  // canUseCustomerDisplay
                    true,  // canUsePresenter
                    true,  // canUseLed
                    LedModel.SK,  // ledModel
                    true,  // canUseBlinkLed
                    false,  // canUsePaperPresentStatus
                    true,  // canUseAllReceipt
                    false,  // canGetProductSerialNumber
                    16,  // settableUsbSerialNumberLength
                    true,  // isUsbSerialNumberEnabledByDefault
                    false,  // canUseMelodySpeaker
                    -1,  // defaultSoundNumber
                    -1,  // defaultVolume
                    -1,  // volumeMax
                    -1 // volumeMin
                )
            )
        }
    }

    fun getModelTitle(model: Int): String {
        return mModelCapabilityMap[model].modelTitle
    }

    fun getEmulation(model: Int): Emulation {
        return mModelCapabilityMap[model].emulation
    }

    fun getPortSettings(model: Int): String {
        return mModelCapabilityMap[model].defaultPortSettings
    }

    fun canSetDrawerOpenStatus(model: Int): Boolean {
        return mModelCapabilityMap[model].canSetDrawerOpenStatus
    }

    fun canPrintTextReceiptSample(model: Int): Boolean {
        return mModelCapabilityMap[model].canPrintTextReceiptSample
    }

    fun canPrintUtf8EncodedText(model: Int): Boolean {
        return mModelCapabilityMap[model].canPrintUtf8EncodedText
    }

    fun canPrintRasterReceiptSample(model: Int): Boolean {
        return mModelCapabilityMap[model].canPrintRasterReceiptSample
    }

    fun canPrintCjk(model: Int): Boolean {
        return mModelCapabilityMap[model].canPrintCjk
    }

    fun canUseBlackMark(model: Int): Boolean {
        return mModelCapabilityMap[model].canUseBlackMark
    }

    fun canUseBlackMarkDetection(model: Int): Boolean {
        return mModelCapabilityMap[model].canUseBlackMarkDetection
    }

    fun canUsePageMode(model: Int): Boolean {
        return mModelCapabilityMap[model].canUsePageMode
    }

    fun canUseCashDrawer(model: Int): Boolean {
        return mModelCapabilityMap[model].canUseCashDrawer
    }

    fun canUseBarcodeReader(model: Int): Boolean {
        return mModelCapabilityMap[model].canUseBarcodeReader
    }

    fun canUseCustomerDisplay(model: Int, modelName: String): Boolean {
        var canUse =
            mModelCapabilityMap[model].canUseCustomerDisplay
        if (model == TSP100) {
            canUse =
                modelName == mModelCapabilityMap[TSP100].modelTitle || modelName == "Star TSP143IIIU" // Support TSP100IIIU.
            // Not support TSP100LAN, TSP100U, TSP100GT, TSP100IIU, TSP100IIILAN, TSP100IIIW and TSP100IIIBI.
        }
        return canUse
    }

    fun canUsePresenter(model: Int): Boolean {
        return mModelCapabilityMap[model].canUsePresenter
    }

    fun canUseLed(model: Int): Boolean {
        return mModelCapabilityMap[model].canUseLed
    }

    fun getLedModel(model: Int): LedModel {
        return mModelCapabilityMap[model].ledModel
    }

    fun canUseBlinkLed(model: Int): Boolean {
        return mModelCapabilityMap[model].canUseBlinkLed
    }

    fun canUsePaperPresentStatus(model: Int): Boolean {
        return mModelCapabilityMap[model].canUsePaperPresentStatus
    }

    fun canUseAllReceipt(model: Int): Boolean {
        return mModelCapabilityMap[model].canUseAllReceipt
    }

    fun canGetProductSerialNumber(
        model: Int,
        modelName: String,
        isBluetoothInterface: Boolean
    ): Boolean {
        var canGet =
            mModelCapabilityMap[model].canGetProductSerialNumber
        if (model == TSP100) {
            canGet =
                modelName == mModelCapabilityMap[TSP100].modelTitle || modelName == "TSP143IIILAN (STR_T-001)" || modelName == "TSP143IIIW (STR_T-001)" ||  // Support TSP100IIIW.
                        isBluetoothInterface || modelName == "Star TSP143IIIU" // Support TSP100IIIU.
            // Not support TSP100LAN, TSP100U, TSP100GT and TSP100IIU.
        }
        return canGet
    }

    fun settableUsbSerialNumberLength(
        model: Int,
        modelName: String,
        isUsbInterface: Boolean
    ): Int {
        var length =
            mModelCapabilityMap[model].settableUsbSerialNumberLength
        if (model == TSP100) {
            if (!isUsbInterface) {
                return 0
            }
            length =
                if (modelName == mModelCapabilityMap[TSP100].modelTitle || modelName == "Star TSP143IIIU") { // TSP100IIIU supports 16digits USB-ID.
                    16
                } else { // TSP100U, TSP100GT and TSP100IIU support 8digits USB-ID.
                    8
                }
        }
        if (model == BSC10 && !isUsbInterface) { // It is useless to set a USB serial number to BSC10LAN.
            length = 0
        }
        return length
    }

    fun isUsbSerialNumberEnabledByDefault(model: Int): Boolean {
        return mModelCapabilityMap[model].isUsbSerialNumberEnabledByDefault
    }

    fun canUseMelodySpeaker(model: Int): Boolean {
        return mModelCapabilityMap[model].canUseMelodySpeaker
    }

    fun getDefaultSoundNumber(model: Int): Int {
        return mModelCapabilityMap[model].defaultSoundNumber
    }

    fun getDefaultVolume(model: Int): Int {
        return mModelCapabilityMap[model].defaultVolume
    }

    fun getVolumeMax(model: Int): Int {
        return mModelCapabilityMap[model].volumeMax
    }

    fun getVolumeMin(model: Int): Int {
        return mModelCapabilityMap[model].volumeMin
    }

    /**
     * Get a model index from model name string that can be got by
     * PortInfo.getModelName() or PortInfo.getPortName();
     */
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
        // Partial match from the head
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
            modelTitles.add(mModelCapabilityMap[i].modelTitle)
        }
        return modelTitles
    }

    internal class ModelInfo(
        var modelTitle: String,
        var modelNameArray: Array<String>,
        var emulation: Emulation,
        var defaultPortSettings: String,
        var defaultPaperSize: Int,
        var canSetDrawerOpenStatus: Boolean,
        var canPrintTextReceiptSample: Boolean,
        var canPrintUtf8EncodedText: Boolean,
        var canPrintRasterReceiptSample: Boolean,
        var canPrintCjk: Boolean,
        var canUseBlackMark: Boolean,
        var canUseBlackMarkDetection: Boolean,
        var canUsePageMode: Boolean,
        var canUseCashDrawer: Boolean,
        var canUseBarcodeReader: Boolean,
        var canUseCustomerDisplay: Boolean,
        var canUsePresenter: Boolean,
        var canUseLed: Boolean,
        var ledModel: LedModel,
        var canUseBlinkLed: Boolean,
        var canUsePaperPresentStatus: Boolean,
        var canUseAllReceipt: Boolean,
        var canGetProductSerialNumber: Boolean,
        var settableUsbSerialNumberLength: Int,
        var isUsbSerialNumberEnabledByDefault: Boolean,
        var canUseMelodySpeaker: Boolean,
        var defaultSoundNumber: Int,
        var defaultVolume: Int,
        var volumeMax: Int,
        var volumeMin: Int
    )
}
import { NativeModule, requireNativeModule } from "expo";
import type { EpsonPrintersModuleEvents, EpsonPrinterInfo } from "./EpsonPrinters.types";
import type { PrinterConnectionType } from "./commons";

declare class EpsonPrintersModule extends NativeModule<EpsonPrintersModuleEvents> {
  findPrinters(connectionType: PrinterConnectionType): Promise<boolean>;
  printImage(base64Image: string, deviceData: EpsonPrinterInfo): Promise<boolean>;
}

// This call loads the native module object from the JSI.
export default requireNativeModule<EpsonPrintersModule>("EpsonPrintersModule");

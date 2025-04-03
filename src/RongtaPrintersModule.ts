import { NativeModule, requireNativeModule } from "expo";
import type { RongtaPrintersModuleEvents, RongtaPrinterInfo } from "./RongtaPrinters.types";
import type { PrinterConnectionType } from "./commons";

declare class RongtaPrintersModule extends NativeModule<RongtaPrintersModuleEvents> {
  findPrinters(connectionType: PrinterConnectionType): Promise<boolean>;
  printImage(base64Image: string, deviceData: RongtaPrinterInfo): Promise<boolean>;
}

// This call loads the native module object from the JSI.
export default requireNativeModule<RongtaPrintersModule>("RongtaPrintersModule"); 
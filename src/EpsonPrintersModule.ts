import { type NativeModule, requireNativeModule } from "expo";

import type {
  EpsonPrintersModuleEvents,
  EpsonPrinterInfo,
} from "./EpsonPrinters.types";
import type {
  PrinterConnectionType,
  PrinterStatus,
  PrintOptions,
} from "./commons";

declare class EpsonPrintersModule extends NativeModule<EpsonPrintersModuleEvents> {
  findPrinters(connectionType: PrinterConnectionType): Promise<boolean>;
  connectManually(
    connectionType: PrinterConnectionType,
    connectionDetails: {
      ipAddress: string;
      port?: number;
      modelName?: string; // required for Epson
    },
  ): Promise<EpsonPrinterInfo>;
  getSupportedModels(): Promise<string[]>;
  printImage(
    base64Image: string,
    deviceData: EpsonPrinterInfo,
    options?: PrintOptions,
  ): Promise<boolean>;
  getStatus(deviceData: EpsonPrinterInfo): Promise<PrinterStatus>;
}

// This call loads the native module object from the JSI.
export default requireNativeModule<EpsonPrintersModule>("EpsonPrintersModule");

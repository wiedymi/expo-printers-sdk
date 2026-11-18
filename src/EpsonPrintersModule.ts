import { type NativeModule, requireNativeModule } from "expo";

import type {
  EpsonImagePreview,
  EpsonPrintersModuleEvents,
  EpsonPrinterInfo,
  EpsonPrintOptions,
} from "./EpsonPrinters.types";
import type { PrinterConnectionType, PrinterStatus } from "./commons";

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
    options?: EpsonPrintOptions,
  ): Promise<boolean>;
  getStatus(deviceData: EpsonPrinterInfo): Promise<PrinterStatus>;
  // Makes the image that printImage sends to the printer, without a connection. The printable width comes from
  // options.paperWidth or the standard paper of the model. With "sdk", the Epson SDK makes this image black and
  // white when it prints.
  previewImage(
    base64Image: string,
    deviceData: EpsonPrinterInfo,
    options?: EpsonPrintOptions,
  ): Promise<EpsonImagePreview>;
}

// This call loads the native module object from the JSI.
export default requireNativeModule<EpsonPrintersModule>("EpsonPrintersModule");

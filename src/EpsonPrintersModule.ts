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
  // options.paperWidth or the standard paper of the model. With "sdk", the Epson SDK makes the image black and
  // white when it prints, and the preview uses the same threshold, so the preview has the same dots.
  // React Native smooths an image that it shows larger, so set targetWidth to the width in pixels of the view
  // that shows the preview, and show the PNG at its own size: each dot becomes a sharp square of `scale` pixels.
  previewImage(
    base64Image: string,
    deviceData: EpsonPrinterInfo,
    options?: EpsonPrintOptions,
    targetWidth?: number,
  ): Promise<EpsonImagePreview>;
}

// This call loads the native module object from the JSI.
export default requireNativeModule<EpsonPrintersModule>("EpsonPrintersModule");

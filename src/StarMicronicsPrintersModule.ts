import { type NativeModule, requireNativeModule } from "expo";

import type {
  StarMicronicsPrintersModuleEvents,
  StarMicronicsPrinterInfo,
} from "./StarMicronicsPrinters.types";
import type {
  PrinterConnectionType,
  PrinterStatus,
  PrintOptions,
} from "./commons";

declare class StarMicronicsPrintersModule extends NativeModule<StarMicronicsPrintersModuleEvents> {
  findPrinters(connectionType: PrinterConnectionType): Promise<boolean>;
  connectManually(
    connectionType: PrinterConnectionType,
    connectionDetails: {
      ipAddress: string;
      port?: number;
      modelName: string;
    },
  ): Promise<StarMicronicsPrinterInfo>;
  getSupportedModels(): Promise<string[]>;
  printImage(
    base64Image: string,
    deviceData: StarMicronicsPrinterInfo,
    options?: PrintOptions,
  ): Promise<boolean>;
  getStatus(deviceData: StarMicronicsPrinterInfo): Promise<PrinterStatus>;
}

export default requireNativeModule<StarMicronicsPrintersModule>(
  "StarMicronicsPrintersModule",
);

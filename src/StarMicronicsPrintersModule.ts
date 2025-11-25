import { type NativeModule, requireNativeModule } from "expo";

import type {
  StarMicronicsPrintersModuleEvents,
  StarMicronicsPrinterInfo,
} from "./StarMicronicsPrinters.types";
import type { PrinterConnectionType } from "./commons";

declare class StarMicronicsPrintersModule extends NativeModule<StarMicronicsPrintersModuleEvents> {
  findPrinters(connectionType: PrinterConnectionType): Promise<boolean>;
  connectManually(
    connectionType: PrinterConnectionType,
    connectionDetails: {
      ipAddress: string;
      port?: number;
      modelName?: string; // required for Star Micronics
    },
  ): Promise<StarMicronicsPrinterInfo>;
  getSupportedModels(): Promise<string[]>;
  printImage(
    base64Image: string,
    deviceData: StarMicronicsPrinterInfo,
  ): Promise<boolean>;
}

// This call loads the native module object from the JSI.
export default requireNativeModule<StarMicronicsPrintersModule>(
  "StarMicronicsPrintersModule",
);

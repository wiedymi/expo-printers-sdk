import { type NativeModule, requireNativeModule } from "expo";

import type {
  StarMicronicsPrintersModuleEvents,
  StarMicronicsPrinterInfo,
} from "./StarMicronicsPrinters.types";
import type { PrinterConnectionType } from "./commons";

declare class StarMicronicsPrintersModule extends NativeModule<StarMicronicsPrintersModuleEvents> {
  findPrinters(connectionType: PrinterConnectionType): Promise<boolean>;
  connectManually(ipAddress: string, port?: number): Promise<StarMicronicsPrinterInfo>;
  printImage(
    base64Image: string,
    deviceData: StarMicronicsPrinterInfo,
  ): Promise<boolean>;
}

// This call loads the native module object from the JSI.
export default requireNativeModule<StarMicronicsPrintersModule>(
  "StarMicronicsPrintersModule",
);

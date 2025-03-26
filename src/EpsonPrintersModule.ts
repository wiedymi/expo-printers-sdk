import { NativeModule, requireNativeModule } from "expo";

import type {
  EpsonPrintersModuleEvents,
  EpsonPrintOptions,
  EpsonPrinterStatus,
} from "./EpsonPrinters.types";

declare class EpsonPrintersModule extends NativeModule<EpsonPrintersModuleEvents> {
  startDiscovery(type: number): Promise<boolean>;
  stopDiscovery(): Promise<boolean>;
  printImage(base64Image: string, options: EpsonPrintOptions): Promise<boolean>;
  getPrinterStatus(): Promise<EpsonPrinterStatus>;
  connectPrinter(ip: string): Promise<boolean>;
  disconnectPrinter(): Promise<boolean>;
}

// This call loads the native module object from the JSI.
export default requireNativeModule<EpsonPrintersModule>("EpsonPrintersModule");

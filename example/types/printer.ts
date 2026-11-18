import type {
  EpsonPrinterInfo,
  EpsonPrintResult,
  PrinterStatus,
  RongtaPrinterInfo,
  RongtaPrintResult,
  StarMicronicsPrinterInfo,
  StarMicronicsPrintResult,
} from "expo-printers-sdk";

export type Vendor = "EPSON" | "STAR" | "RONGTA";

export type Printer =
  | { vendor: "EPSON"; info: EpsonPrinterInfo }
  | { vendor: "STAR"; info: StarMicronicsPrinterInfo }
  | { vendor: "RONGTA"; info: RongtaPrinterInfo };

export type PrintResult =
  EpsonPrintResult | StarMicronicsPrintResult | RongtaPrintResult;

// One print job or status check in the results list. result and status are null while the call runs.
export type ResultEntry =
  | {
      kind: "print";
      id: string;
      printer: string;
      startedAt: number;
      finishedAt: number | null;
      result: PrintResult | null;
    }
  | {
      kind: "status";
      id: string;
      printer: string;
      startedAt: number;
      finishedAt: number | null;
      status: PrinterStatus | null;
      errorCode: string | null;
    };

export type LogLevel = "debug" | "info" | "warn" | "error";

export type LogEntry = {
  id: number;
  time: string;
  level: LogLevel;
  tag: string;
  message: string;
};

import type {
  PrinterConnectionType,
  PrinterLogEvent,
  PrintFailureCause,
  PrintNotStartedErrorCode,
  PrintStep,
} from "./commons";

export type EpsonPrinterInfo = {
  deviceName: string;
  target: string;
  ipAddress: string;
  macAddress: string;
  bdAddress: string;
  connectionType: PrinterConnectionType;
  deviceType: number;
  isSupported: boolean;
  unsupportedReason?: string;
};

export type EpsonPrintErrorCode =
  | "INVALID_IMAGE"
  | "UNSUPPORTED_MODEL"
  | "PRINTER_OFFLINE"
  | "PRINT_TIMEOUT"
  | "UNKNOWN_ERROR"
  | PrintNotStartedErrorCode;

export type EpsonPrintResult = {
  jobId: string | null;
  success: boolean;
  errorCode: EpsonPrintErrorCode | null;
  error: string | null;
  cause: PrintFailureCause | null;
  steps: PrintStep[];
};

export type EpsonPrintersModuleEvents = {
  onPrintersFound: (data: { printers: EpsonPrinterInfo[] }) => void;
  onPrintImage: (result: EpsonPrintResult) => void;
  onLog: (event: PrinterLogEvent) => void;
};

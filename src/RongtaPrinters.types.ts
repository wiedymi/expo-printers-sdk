import type {
  PrinterConnectionType,
  PrinterLogEvent,
  PrintFailureCause,
  PrintNotStartedErrorCode,
  PrintStep,
} from "./commons";

export type RongtaPrinterInfo = {
  connectionType: PrinterConnectionType;
  type: RongtaPrinterType;
  isSupported: boolean;
  unsupportedReason?: string;
};

export type RongtaPrinterType =
  | {
      type: "BLUETOOTH";
      alias: string;
      name: string;
      address: string;
    }
  | {
      type: "NETWORK";
      ipAddress: string;
      port: number;
    }
  | {
      type: "USB";
      name: string;
      vendorId: number;
      productId: number;
    };

export type RongtaPrintErrorCode =
  | "INVALID_IMAGE"
  | "CONNECTION_FAILED"
  | "PERMISSION_DENIED"
  | "CONNECTION_TIMEOUT"
  | "PRINT_FAILED"
  | "PRINT_TIMEOUT"
  | "UNKNOWN_ERROR"
  | PrintNotStartedErrorCode;

export type RongtaPrintResult = {
  jobId: string | null;
  success: boolean;
  errorCode: RongtaPrintErrorCode | null;
  error: string | null;
  cause: PrintFailureCause | null;
  steps: PrintStep[];
};

export type RongtaPrintersModuleEvents = {
  onPrintersFound: (data: { printers: RongtaPrinterInfo[] }) => void;
  onPrintImage: (result: RongtaPrintResult) => void;
  onLog: (event: PrinterLogEvent) => void;
};

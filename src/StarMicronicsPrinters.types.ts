import type {
  PrinterConnectionType,
  PrinterLogEvent,
  PrintFailureCause,
  PrintNotStartedErrorCode,
  PrintStep,
} from "./commons";

export type StarMicronicsPrinterInfo = {
  deviceName: string;
  portName: string;
  macAddress: string;
  usbSerialNumber: string;
  connectionType: PrinterConnectionType;
  isSupported: boolean;
  unsupportedReason?: string;
};

export type StarMicronicsPrintErrorCode =
  | "INVALID_IMAGE"
  | "UNSUPPORTED_MODEL"
  | "PORT_OPEN_FAILED"
  | "PRINTER_IN_USE"
  | "CONNECTION_TIMEOUT"
  | "PRINT_TIMEOUT"
  | "COMMUNICATION_ERROR"
  | "PRINTER_OFFLINE"
  | "COVER_OPEN"
  | "PAPER_EMPTY"
  | "PAPER_JAM"
  | "CUTTER_ERROR"
  | "HEAD_THERMISTOR_ERROR"
  | "VOLTAGE_ERROR"
  | "OVER_TEMPERATURE"
  | "RECEIVE_BUFFER_OVERFLOW"
  | "HARDWARE_ERROR"
  | PrintNotStartedErrorCode;

export type StarMicronicsPrintResult = {
  jobId: string | null;
  success: boolean;
  errorCode: StarMicronicsPrintErrorCode | null;
  error: string | null;
  cause: PrintFailureCause | null;
  steps: PrintStep[];
};

export type StarMicronicsPrintersModuleEvents = {
  onPrintersFound: (data: { printers: StarMicronicsPrinterInfo[] }) => void;
  onPrintImage: (result: StarMicronicsPrintResult) => void;
  onLog: (event: PrinterLogEvent) => void;
};

import type {
  PrinterConnectionType,
  PrinterLogEvent,
  PrintFailureCause,
  PrintNotStartedErrorCode,
  PrintOptions,
  PrintStep,
} from "./commons";

// How the image becomes black and white.
// "areaAverage" (default): each dot gets the average of the image pixels that it covers, and is black or white by
// comparison with the dots around it, so gray text and thin lines stay visible.
// "sdk": the image is scaled to the printable width, and the Epson SDK makes it black and white with a fixed
// threshold, as in version 0.11.0.
export type EpsonImageConversion = "areaAverage" | "sdk";

export type EpsonPrintOptions = PrintOptions & {
  imageConversion?: EpsonImageConversion;
};

// The image that printImage sends to the printer, as a base64 PNG. width and height are in printer dots. Each dot
// is a square of scale x scale pixels in the PNG, so the PNG is width * scale pixels wide.
export type EpsonImagePreview = {
  base64: string;
  width: number;
  height: number;
  scale: number;
};

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
  | "COVER_OPEN"
  | "PAPER_EMPTY"
  | "PAPER_JAM"
  | "CUTTER_ERROR"
  | "HARDWARE_ERROR"
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

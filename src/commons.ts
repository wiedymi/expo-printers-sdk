export type PrinterConnectionType = "Bluetooth" | "Network" | "USB";

// Paper loaded in the printer, in millimeters. Most printers cannot report it,
// so set it when it is not the printer's standard paper.
export type PaperWidth = 58 | 80;

export type PrintOptions = {
  paperWidth?: PaperWidth;
  // Returned in the onPrintImage event and written in the native logs,
  // so the app can match a result to its job.
  jobId?: string;
};

export type PaperStatus = "OK" | "NEAR_END" | "EMPTY" | "UNKNOWN";

export type PrinterError =
  | "CUTTER_ERROR"
  | "MECHANICAL_ERROR"
  | "PAPER_JAM"
  | "OVER_TEMPERATURE"
  | "VOLTAGE_ERROR"
  | "AUTO_RECOVERABLE_ERROR"
  | "UNRECOVERABLE_ERROR"
  | "UNKNOWN_ERROR"; // The printer reports an error, but not which kind.

export type PrinterStatus = {
  // The printer reports that it is ready to print.
  online: boolean;
  coverOpen: boolean;
  paper: PaperStatus;
  // Empty when the printer reports no error.
  errors: PrinterError[];
};

// `getStatus` rejects with one of these codes when it cannot read the status.
export type PrinterStatusErrorCode =
  | "ERR_PRINTER_UNREACHABLE"
  | "ERR_PRINTER_BUSY"
  | "ERR_PRINTER_STATUS_UNAVAILABLE";

// One step of a print, in the order the library ran it.
export type PrintStep = {
  step: string;
  outcome: string;
  durationMs: number;
};

// The native error that ended a print. vendorCode is the error code of the vendor SDK, if it has one.
export type PrintFailureCause = {
  step: string;
  type: string;
  message: string | null;
  vendorCode: number | null;
};

// Error codes of every vendor when the print could not start.
export type PrintNotStartedErrorCode = "NOT_INITIALIZED" | "INVALID_REQUEST";

export type PrinterLogLevel = "debug" | "info" | "warn" | "error";

// One native log line, sent by the onLog event.
export type PrinterLogEvent = {
  level: PrinterLogLevel;
  tag: string;
  message: string;
  // Milliseconds since the Unix epoch.
  timestamp: number;
  error: { type: string; message: string | null } | null;
  // Set for the step lines of a print or status check.
  jobId?: string | null;
  step?: string;
  outcome?: string;
  durationMs?: number;
  jobDurationMs?: number;
};

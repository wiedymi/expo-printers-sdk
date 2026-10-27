export type PrinterConnectionType = "Bluetooth" | "Network" | "USB";

// Paper loaded in the printer, in millimeters. Most printers cannot report it,
// so set it when it is not the printer's standard paper.
export type PaperWidth = 58 | 80;

export type PrintOptions = {
  paperWidth?: PaperWidth;
};

export type PaperStatus = "OK" | "NEAR_END" | "EMPTY" | "UNKNOWN";

export type PrinterError =
  | "CUTTER_ERROR"
  | "MECHANICAL_ERROR"
  | "PAPER_JAM"
  | "OVER_TEMPERATURE"
  | "VOLTAGE_ERROR"
  | "AUTO_RECOVERABLE_ERROR"
  | "UNRECOVERABLE_ERROR";

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

import type { PrinterConnectionType } from "./commons";

export type EpsonPrinterInfo = {
  deviceName: string;
  target: string;
  ipAddress: string;
  macAddress: string;
  bdAddress: string;
  connectionType: PrinterConnectionType;
  deviceType: number;
};

export type EpsonPrintResult = {
  success: boolean;
  error?: string;
};

export type EpsonPrintersModuleEvents = {
  onPrintersFound: (data: { printers: EpsonPrinterInfo[] }) => void;
  onPrintImage: (result: EpsonPrintResult) => void;
};

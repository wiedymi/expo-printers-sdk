import type { PrinterConnectionType } from "./commons";

export type StarMicronicsPrinterInfo = {
  deviceName: string;
  portName: string;
  macAddress: string;
  usbSerialNumber: string;
  connectionType: PrinterConnectionType;
};

export type StarMicronicsPrintResult = {
  success: boolean;
  error?: string;
};

export type StarMicronicsPrintersModuleEvents = {
  onPrintersFound: (data: { printers: StarMicronicsPrinterInfo[] }) => void;
  onPrintImage: (result: StarMicronicsPrintResult) => void;
};

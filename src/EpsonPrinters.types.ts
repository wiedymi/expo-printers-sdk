import { PrinterConnectionType } from './commons';

export type EpsonPrinterInfo = {
  deviceName: string;
  target: string;
  ip: string;
  mac: string;
  bdAddress: string;
  connectionType: PrinterConnectionType;
};

export type EpsonPrintResult = {
  success: boolean;
  error?: string;
};

export type EpsonPrintersModuleEvents = {
  onPrintersFound: (data: { printers: EpsonPrinterInfo[] }) => void;
  onPrintImage: (result: EpsonPrintResult) => void;
};

import { PrinterConnectionType } from './commons';

export type RongtaPrinterInfo = {
  deviceName: string;
  alias: string;
  address: string;
  connectionType: PrinterConnectionType;
};

export type RongtaPrintResult = {
  success: boolean;
  error?: string;
};

export type RongtaPrintersModuleEvents = {
  onPrintersFound: (data: { printers: RongtaPrinterInfo[] }) => void;
  onPrintImage: (result: RongtaPrintResult) => void;
}; 
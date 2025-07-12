import type { PrinterConnectionType } from "./commons";

export type RongtaPrinterInfo = {
  connectionType: PrinterConnectionType;
  type: RongtaPrinterType;
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

export type RongtaPrintResult = {
  success: boolean;
  error?: string;
};

export type RongtaPrintersModuleEvents = {
  onPrintersFound: (data: { printers: RongtaPrinterInfo[] }) => void;
  onPrintImage: (result: RongtaPrintResult) => void;
};

export type EpsonPrinterInfo = {
  deviceName: string;
  target: string;
  ip: string;
  mac: string;
  bdAddress: string;
};

export type EpsonPrinterStatus = {
  connection: number;
  online: number;
  coverOpen: number;
  paper: number;
  paperFeed: number;
  panelSwitch: number;
};

export type EpsonPrintOptions = {
  alignment?: number;
  font?: number;
  lang?: number;
};

export type EpsonErrorPayload = {
  error: string;
};

export type EpsonPrintersModuleEvents = {
  onPrinterStatusChange: (status: EpsonPrinterStatus) => void;
  onPrintSuccess: () => void;
  onPrintError: (error: EpsonErrorPayload) => void;
  onDiscovery: (printerInfo: EpsonPrinterInfo) => void;
};

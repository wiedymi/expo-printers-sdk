import {
  EpsonPrinters,
  RongtaPrinters,
  StarMicronicsPrinters,
} from "expo-printers-sdk";
import type {
  EpsonImagePreview,
  EpsonPrintOptions,
  PrinterConnectionType,
  PrinterStatus,
} from "expo-printers-sdk";
import type { Printer, Vendor } from "../types/printer";

export const VENDORS: Vendor[] = ["EPSON", "STAR", "RONGTA"];

export const VENDOR_LABELS: Record<Vendor, string> = {
  EPSON: "Epson",
  STAR: "Star",
  RONGTA: "Rongta",
};

export const CONNECTION_TYPES: PrinterConnectionType[] = [
  "Network",
  "Bluetooth",
  "USB",
];

export const printerId = (printer: Printer): string => {
  switch (printer.vendor) {
    case "EPSON":
      return `EPSON:${printer.info.target}`;
    case "STAR":
      return `STAR:${printer.info.macAddress || printer.info.usbSerialNumber || printer.info.portName}`;
    case "RONGTA": {
      const type = printer.info.type;
      if (type.type === "BLUETOOTH") return `RONGTA:BT:${type.address}`;
      if (type.type === "NETWORK")
        return `RONGTA:TCP:${type.ipAddress}:${type.port}`;
      return `RONGTA:USB:${type.vendorId}:${type.productId}`;
    }
  }
};

export const printerName = (printer: Printer): string => {
  switch (printer.vendor) {
    case "EPSON":
    case "STAR":
      return `${VENDOR_LABELS[printer.vendor]} ${printer.info.deviceName}`;
    case "RONGTA": {
      const type = printer.info.type;
      if (type.type === "BLUETOOTH") return `Rongta ${type.alias || type.name}`;
      if (type.type === "USB") return `Rongta ${type.name}`;
      return "Rongta";
    }
  }
};

// The connection and address, for example "Network TCP:192.168.1.20".
export const printerAddress = (printer: Printer): string => {
  switch (printer.vendor) {
    case "EPSON":
      return `${printer.info.connectionType} ${printer.info.target}`;
    case "STAR":
      return `${printer.info.connectionType} ${printer.info.portName}`;
    case "RONGTA": {
      const type = printer.info.type;
      if (type.type === "BLUETOOTH") return `Bluetooth ${type.address}`;
      if (type.type === "NETWORK")
        return `Network ${type.ipAddress}:${type.port}`;
      return `USB ${type.vendorId}:${type.productId}`;
    }
  }
};

export const findPrinters = (
  vendor: Vendor,
  connectionType: PrinterConnectionType,
): Promise<boolean> => {
  switch (vendor) {
    case "EPSON":
      return EpsonPrinters.findPrinters(connectionType);
    case "STAR":
      return StarMicronicsPrinters.findPrinters(connectionType);
    case "RONGTA":
      return RongtaPrinters.findPrinters(connectionType);
  }
};

// Epson and Star need a model name. Rongta printers cannot report their model.
export const needsModelName = (vendor: Vendor): boolean => vendor !== "RONGTA";

export const getSupportedModels = (vendor: Vendor): Promise<string[]> => {
  switch (vendor) {
    case "EPSON":
      return EpsonPrinters.getSupportedModels();
    case "STAR":
      return StarMicronicsPrinters.getSupportedModels();
    case "RONGTA":
      return Promise.resolve([]);
  }
};

export const connectManually = async (
  vendor: Vendor,
  ipAddress: string,
  port: number,
  modelName: string,
): Promise<Printer> => {
  switch (vendor) {
    case "EPSON":
      return {
        vendor,
        info: await EpsonPrinters.connectManually("Network", {
          ipAddress,
          port,
          modelName,
        }),
      };
    case "STAR":
      return {
        vendor,
        info: await StarMicronicsPrinters.connectManually("Network", {
          ipAddress,
          port,
          modelName,
        }),
      };
    case "RONGTA":
      return {
        vendor,
        info: await RongtaPrinters.connectManually("Network", {
          ipAddress,
          port,
        }),
      };
  }
};

// Returns true when the print job started. The result comes in the onPrintImage event.
export const printImage = (
  printer: Printer,
  base64Image: string,
  { imageConversion, ...options }: EpsonPrintOptions,
): Promise<boolean> => {
  switch (printer.vendor) {
    case "EPSON":
      return EpsonPrinters.printImage(base64Image, printer.info, {
        ...options,
        imageConversion,
      });
    case "STAR":
      return StarMicronicsPrinters.printImage(
        base64Image,
        printer.info,
        options,
      );
    case "RONGTA":
      return RongtaPrinters.printImage(base64Image, printer.info, options);
  }
};

export const getStatus = (printer: Printer): Promise<PrinterStatus> => {
  switch (printer.vendor) {
    case "EPSON":
      return EpsonPrinters.getStatus(printer.info);
    case "STAR":
      return StarMicronicsPrinters.getStatus(printer.info);
    case "RONGTA":
      return RongtaPrinters.getStatus(printer.info);
  }
};

// Only Epson can show the image that the printer gets.
export const canPreview = (printer: Printer | null): boolean =>
  printer?.vendor === "EPSON";

export const previewImage = (
  printer: Printer,
  base64Image: string,
  options: EpsonPrintOptions,
): Promise<EpsonImagePreview> => {
  if (printer.vendor !== "EPSON") {
    throw new Error("Preview is available for Epson printers only");
  }
  return EpsonPrinters.previewImage(base64Image, printer.info, options);
};

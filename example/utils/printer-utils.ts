import type { Manufacturer, PrinterInfo } from "../types/printer";

export const fetchImageAsBase64 = async (imageUrl: string): Promise<string> => {
  const response = await fetch(imageUrl);
  if (!response.ok) {
    throw new Error(`Failed to fetch image: ${response.statusText}`);
  }
  const blob = await response.blob();
  return await new Promise<string>((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => {
      const fullResult = reader.result as string;
      const base64Data = fullResult.split(",")[1] || "";
      resolve(base64Data);
    };
    reader.onerror = (error) => reject(error);
    reader.readAsDataURL(blob);
  });
};

export const getUniquePrinterId = (printer: PrinterInfo): string => {
  switch (printer.type) {
    case "EPSON": {
      const info = printer.info as any;
      return `EPSON-${info.macAddress || info.ipAddress || info.deviceName}`;
    }
    case "RONGTA": {
      const info = printer.info as any;
      if (info.type.type === "BLUETOOTH") {
        return `RONGTA-BT-${info.type.address}`;
      } else {
        return `RONGTA-NET-${info.type.ipAddress}:${info.type.port}`;
      }
    }
    case "STAR": {
      const info = printer.info as any;
      return `STAR-${info.macAddress || info.usbSerialNumber || info.portName}`;
    }
    default:
      return `${printer.type}-unknown`;
  }
};

export const deduplicatePrinters = (printers: PrinterInfo[]): PrinterInfo[] => {
  const seen = new Set<string>();
  return printers.filter((printer) => {
    const id = getUniquePrinterId(printer);
    if (seen.has(id)) return false;
    seen.add(id);
    return true;
  });
};

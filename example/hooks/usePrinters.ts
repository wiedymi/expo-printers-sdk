import { useState, useEffect, useCallback } from "react";
import {
  EpsonPrinters,
  RongtaPrinters,
  StarMicronicsPrinters,
} from "expo-printers-sdk";
import type {
  EpsonPrinterInfo,
  RongtaPrinterInfo,
  StarMicronicsPrinterInfo,
  EpsonPrintResult,
  RongtaPrintResult,
  StarMicronicsPrintResult,
  PrinterConnectionType,
} from "expo-printers-sdk";
import type { Manufacturer, PrinterInfo } from "../types/printer";
import { deduplicatePrinters } from "../utils/printer-utils";

export const usePrinters = (manufacturer: Manufacturer) => {
  const [printers, setPrinters] = useState<PrinterInfo[]>([]);
  const [isSearching, setIsSearching] = useState(false);
  const [printingStates, setPrintingStates] = useState<{
    [key: string]: boolean;
  }>({});

  const handlePrintersFound = useCallback(
    (data: { printers: any[] }) => {
      setPrinters((prev) =>
        deduplicatePrinters([
          ...prev,
          ...data.printers.map((printer) => ({
            type: manufacturer,
            info: printer,
          })),
        ])
      );
      setIsSearching(false);
    },
    [manufacturer]
  );

  const handlePrintResult = useCallback(
    (result: EpsonPrintResult | RongtaPrintResult | StarMicronicsPrintResult) => {
      setPrintingStates((prev) => {
        const newState = { ...prev };
        Object.keys(newState).forEach((key) => {
          newState[key] = false;
        });
        return newState;
      });
      return result;
    },
    []
  );

  useEffect(() => {
    let foundListener: { remove: () => void } | null = null;
    let printListener: { remove: () => void } | null = null;
    let mod: any;

    switch (manufacturer) {
      case "EPSON":
        mod = EpsonPrinters;
        break;
      case "RONGTA":
        mod = RongtaPrinters;
        break;
      case "STAR":
        mod = StarMicronicsPrinters;
        break;
    }

    if (mod) {
      foundListener = mod.addListener("onPrintersFound", handlePrintersFound);
      printListener = mod.addListener("onPrintImage", handlePrintResult);
    }

    return () => {
      foundListener?.remove();
      printListener?.remove();
    };
  }, [manufacturer, handlePrintersFound, handlePrintResult]);

  const searchPrinters = useCallback(
    async (connectionType: PrinterConnectionType): Promise<boolean> => {
      setPrinters([]);
      setIsSearching(true);

      try {
        let result: boolean;
        switch (manufacturer) {
          case "EPSON":
            result = await EpsonPrinters.findPrinters(connectionType);
            break;
          case "RONGTA":
            result = await RongtaPrinters.findPrinters(connectionType);
            break;
          case "STAR":
            result = await StarMicronicsPrinters.findPrinters(connectionType);
            break;
        }
        return result;
      } catch (error) {
        setIsSearching(false);
        throw error;
      }
    },
    [manufacturer]
  );

  const connectManually = useCallback(
    async (ipAddress: string, port: number) => {
      let printerInfo: any;
      switch (manufacturer) {
        case "EPSON":
          printerInfo = await EpsonPrinters.connectManually(ipAddress, port);
          break;
        case "RONGTA":
          printerInfo = await RongtaPrinters.connectManually(ipAddress, port);
          break;
        case "STAR":
          printerInfo = await StarMicronicsPrinters.connectManually(
            ipAddress,
            port
          );
          break;
      }

      if (printerInfo) {
        setPrinters((prev) =>
          deduplicatePrinters([
            ...prev,
            { type: manufacturer, info: printerInfo },
          ])
        );
      }

      return printerInfo;
    },
    [manufacturer]
  );

  const printImage = useCallback(
    async (
      base64Image: string,
      printer: PrinterInfo,
      printerId: string
    ): Promise<boolean> => {
      setPrintingStates((prev) => ({ ...prev, [printerId]: true }));

      try {
        let result: boolean;
        switch (printer.type) {
          case "EPSON":
            result = await EpsonPrinters.printImage(
              base64Image,
              printer.info as EpsonPrinterInfo
            );
            break;
          case "RONGTA":
            result = await RongtaPrinters.printImage(
              base64Image,
              printer.info as RongtaPrinterInfo
            );
            break;
          case "STAR":
            result = await StarMicronicsPrinters.printImage(
              base64Image,
              printer.info as StarMicronicsPrinterInfo
            );
            break;
          default:
            throw new Error("Unknown printer type");
        }
        return result;
      } catch (error) {
        setPrintingStates((prev) => ({ ...prev, [printerId]: false }));
        throw error;
      }
    },
    []
  );

  return {
    printers,
    isSearching,
    printingStates,
    searchPrinters,
    connectManually,
    printImage,
  };
};

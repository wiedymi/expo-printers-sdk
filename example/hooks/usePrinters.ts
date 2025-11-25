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

type PrinterModule =
  | typeof EpsonPrinters
  | typeof RongtaPrinters
  | typeof StarMicronicsPrinters;

export const usePrinters = (manufacturer: Manufacturer) => {
  const [printers, setPrinters] = useState<PrinterInfo[]>([]);
  const [isSearching, setIsSearching] = useState(false);
  const [supportedModels, setSupportedModels] = useState<string[]>([]);
  const modelsCacheRef = useState<Record<Manufacturer, string[]>>({
    EPSON: [],
    STAR: [],
    RONGTA: [],
  })[0];
  const [printingStates, setPrintingStates] = useState<{
    [key: string]: boolean;
  }>({});

  const handlePrintersFound = useCallback(
    (data: { printers: any[] }) => {
      console.log(`[Event] onPrintersFound: ${data.printers.length} printer(s)`);
      data.printers.forEach((p, i) => {
        console.log(`[Event]   [${i}] ${JSON.stringify(p)}`);
      });
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
      if (result.success) {
        console.log("[Event] onPrintImage: SUCCESS");
      } else {
        console.error(`[Event] onPrintImage: FAILED - ${result.error || "Unknown error"}`);
      }
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

  const getPrinterModule = (mfr: Manufacturer): PrinterModule => {
    switch (mfr) {
      case "EPSON":
        return EpsonPrinters;
      case "RONGTA":
        return RongtaPrinters;
      case "STAR":
        return StarMicronicsPrinters;
    }
  };

  const refreshSupportedModels = useCallback(
    async (forceRefresh = false): Promise<void> => {
      try {
        const cached = modelsCacheRef[manufacturer];
        if (!forceRefresh && cached.length > 0) {
          setSupportedModels(cached);
          return;
        }

        const mod = getPrinterModule(manufacturer) as any;
        if (typeof mod.getSupportedModels === "function") {
          const models: string[] = (await mod.getSupportedModels()) ?? [];
          modelsCacheRef[manufacturer] = models;
          setSupportedModels(models);
          return;
        } else {
          setSupportedModels([]);
          return;
        }
      } catch (error) {
        console.warn("Failed to load supported models", error);
        setSupportedModels([]);
        return;
      }
    },
    [manufacturer, modelsCacheRef]
  );

  useEffect(() => {
    refreshSupportedModels();
  }, [refreshSupportedModels]);

  useEffect(() => {
    let foundListener: { remove: () => void } | null = null;
    let printListener: { remove: () => void } | null = null;

    const mod = getPrinterModule(manufacturer);

    foundListener = mod.addListener("onPrintersFound", handlePrintersFound);
    printListener = mod.addListener("onPrintImage", handlePrintResult);

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
        const mod = getPrinterModule(manufacturer);
        const result = await mod.findPrinters(connectionType);
        if (!result) {
          setIsSearching(false);
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
    async (ipAddress: string, modelName: string, port: number) => {
      const mod = getPrinterModule(manufacturer);
      const printerInfo = await mod.connectManually("Network", {
        ipAddress,
        port,
        modelName,
      });

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
        switch (printer.type) {
          case "EPSON":
            return await EpsonPrinters.printImage(
              base64Image,
              printer.info as EpsonPrinterInfo
            );
          case "RONGTA":
            return await RongtaPrinters.printImage(
              base64Image,
              printer.info as RongtaPrinterInfo
            );
          case "STAR":
            return await StarMicronicsPrinters.printImage(
              base64Image,
              printer.info as StarMicronicsPrinterInfo
            );
          default:
            throw new Error("Unknown printer type");
        }
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
    supportedModels,
    printingStates,
    searchPrinters,
    connectManually,
    printImage,
    refreshSupportedModels,
  };
};

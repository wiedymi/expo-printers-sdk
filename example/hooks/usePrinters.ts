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

      // JS-side timeout tracker (45s to allow native 30s timeout to fire first)
      const JS_TIMEOUT_MS = 45000;
      let timeoutId: NodeJS.Timeout | null = null;
      const startTime = Date.now();

      const logElapsed = () => {
        const elapsed = Math.floor((Date.now() - startTime) / 1000);
        console.log(`[Print] Waiting for onPrintImage event... (${elapsed}s elapsed)`);
      };

      // Log progress every 5 seconds
      const progressInterval = setInterval(logElapsed, 5000);

      const timeoutPromise = new Promise<never>((_, reject) => {
        timeoutId = setTimeout(() => {
          console.error(`[Print] JS TIMEOUT: No onPrintImage event after ${JS_TIMEOUT_MS / 1000}s`);
          reject(new Error("Print timeout - no response from native layer"));
        }, JS_TIMEOUT_MS);
      });

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
      } finally {
        if (timeoutId) clearTimeout(timeoutId);
        clearInterval(progressInterval);
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

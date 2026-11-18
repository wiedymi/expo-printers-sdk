import { useCallback, useEffect, useMemo, useState } from "react";
import {
  EpsonPrinters,
  RongtaPrinters,
  StarMicronicsPrinters,
} from "expo-printers-sdk";
import type { PrinterConnectionType } from "expo-printers-sdk";
import type { Printer, Vendor } from "../types/printer";
import { findPrinters, printerId } from "../utils/printers";

// The printers that the app found or added, and the selected printer.
export const usePrinters = (log: (message: string) => void) => {
  const [printers, setPrinters] = useState<Printer[]>([]);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [searching, setSearching] = useState<Vendor | null>(null);

  const addPrinters = useCallback((found: Printer[]) => {
    setPrinters((current) => {
      const known = new Set(current.map(printerId));
      return [
        ...current,
        ...found.filter((printer) => !known.has(printerId(printer))),
      ];
    });
    setSelectedId(
      (current) => current ?? (found[0] ? printerId(found[0]) : null),
    );
  }, []);

  useEffect(() => {
    const subscriptions = [
      EpsonPrinters.addListener("onPrintersFound", ({ printers: found }) => {
        log(`Epson search found ${found.length} printer(s)`);
        addPrinters(found.map((info) => ({ vendor: "EPSON", info })));
        setSearching(null);
      }),
      StarMicronicsPrinters.addListener(
        "onPrintersFound",
        ({ printers: found }) => {
          log(`Star search found ${found.length} printer(s)`);
          addPrinters(found.map((info) => ({ vendor: "STAR", info })));
          setSearching(null);
        },
      ),
      RongtaPrinters.addListener("onPrintersFound", ({ printers: found }) => {
        log(`Rongta search found ${found.length} printer(s)`);
        addPrinters(found.map((info) => ({ vendor: "RONGTA", info })));
        setSearching(null);
      }),
    ];
    return () => subscriptions.forEach((subscription) => subscription.remove());
  }, [addPrinters, log]);

  const search = useCallback(
    async (vendor: Vendor, connectionType: PrinterConnectionType) => {
      setSearching(vendor);
      log(`Searching ${vendor} ${connectionType} printers...`);
      try {
        if (!(await findPrinters(vendor, connectionType))) {
          log(`${vendor} search did not start`);
          setSearching(null);
        }
      } catch (error) {
        setSearching(null);
        throw error;
      }
    },
    [log],
  );

  const add = useCallback(
    (printer: Printer) => {
      addPrinters([printer]);
      setSelectedId(printerId(printer));
    },
    [addPrinters],
  );

  const selected = useMemo(
    () => printers.find((printer) => printerId(printer) === selectedId) ?? null,
    [printers, selectedId],
  );

  return {
    printers,
    selected,
    selectedId,
    select: setSelectedId,
    searching,
    search,
    add,
  };
};

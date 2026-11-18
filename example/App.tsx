import React, { useState } from "react";
import {
  SafeAreaView,
  View,
  Text,
  Alert,
  StyleSheet,
  Platform,
} from "react-native";
import { EpsonPrinters } from "expo-printers-sdk";
import type {
  EpsonImagePreview,
  EpsonPrinterInfo,
  EpsonPrintOptions,
  PaperWidth,
  PrinterConnectionType,
} from "expo-printers-sdk";
import type { Manufacturer, PrinterInfo } from "./types/printer";
import { usePrinters } from "./hooks/usePrinters";
import { usePermissions } from "./hooks/usePermissions";
import { useLogs } from "./hooks/useLogs";
import { getUniquePrinterId } from "./utils/printer-utils";
import { loadReceiptBase64 } from "./utils/receipts";
import CompactPrinterSelector from "./components/compact-printer-selector";
import PrinterList from "./components/printer-list";
import LogViewer from "./components/log-viewer";
import PrintOptions, { type PrintSettings } from "./components/print-options";
import ImagePreview from "./components/image-preview";

const DEFAULT_SETTINGS: PrintSettings = {
  receipt: "SHORT",
  imageUrl: "https://placehold.co/600x400/000000/FFFFFF.png",
  paperWidth: "AUTO",
  imageConversion: "auto",
  jobCount: "1",
};

const toPrintOptions = (settings: PrintSettings): EpsonPrintOptions => ({
  paperWidth:
    settings.paperWidth === "AUTO"
      ? undefined
      : (Number(settings.paperWidth) as PaperWidth),
  imageConversion: settings.imageConversion,
});

const MONO_FONT = Platform.select({
  ios: "Menlo",
  android: "monospace",
  default: "monospace",
});

export default function App() {
  const [selectedConnectionType, setSelectedConnectionType] =
    useState<PrinterConnectionType>("Network");
  const [selectedManufacturer, setSelectedManufacturer] =
    useState<Manufacturer>("RONGTA");

  const [settings, setSettings] = useState<PrintSettings>(DEFAULT_SETTINGS);
  const [preview, setPreview] = useState<EpsonImagePreview | null>(null);

  const { logs, clearLogs } = useLogs();
  const { isRequestingPermissions, requestPermissions } = usePermissions();
  const {
    printers,
    isSearching,
    supportedModels,
    printingStates,
    searchPrinters,
    connectManually,
    printImage,
    getStatus,
    refreshSupportedModels,
  } = usePrinters(selectedManufacturer);

  const handleSearch = async () => {
    try {
      console.log(
        `[Search] Starting ${selectedManufacturer} ${selectedConnectionType} search...`,
      );
      const hasPermissions = await requestPermissions();
      if (!hasPermissions) {
        console.warn("[Search] Permissions not granted");
        return;
      }

      await searchPrinters(selectedConnectionType);
      console.log(`[Search] Search initiated for ${selectedConnectionType}`);
    } catch (error) {
      console.error("[Search] Failed:", error);
      Alert.alert("Error", "Failed to find printers. Please try again.");
    }
  };

  const handleManualConnect = async (
    ipAddress: string,
    modelName: string,
    port: number,
  ) => {
    try {
      console.log(
        `[Manual] Connecting to ${selectedManufacturer} @ ${ipAddress}:${port}...`,
      );
      const printerInfo = await connectManually(ipAddress, modelName, port);
      if (printerInfo) {
        console.log("[Manual] Connected successfully:", printerInfo);
        Alert.alert(
          "Connected",
          `${selectedManufacturer} ${modelName} @ ${ipAddress}:${port}`,
        );
      }
    } catch (error) {
      console.error("[Manual] Connection failed:", error);
      Alert.alert("Error", "Failed to connect to printer");
    }
  };

  const handlePrint = async (printer: PrinterInfo) => {
    const printerId = getUniquePrinterId(printer);
    console.log(`[Print] Starting print job for ${printerId}...`);
    console.log("[Print] Printer info:", printer);

    try {
      console.log(`[Print] Loading the ${settings.receipt} image...`);
      const base64Image = await loadReceiptBase64(
        settings.receipt,
        settings.imageUrl,
      );
      console.log(
        `[Print] Image loaded (${base64Image.length} chars), sending ${settings.jobCount} job(s)...`,
      );

      for (let job = 0; job < Number(settings.jobCount); job++) {
        const result = await printImage(
          base64Image,
          printer,
          printerId,
          toPrintOptions(settings),
        );
        console.log(`[Print] printImage returned: ${result}`);

        if (!result) {
          console.error("[Print] Failed to initiate print job");
          Alert.alert("Print Failed", "Failed to initiate print job");
          return;
        }
      }
    } catch (error) {
      console.error("[Print] Error:", error);
      Alert.alert(
        "Print Failed",
        error instanceof Error ? error.message : "Unknown error occurred",
      );
    }
  };

  const handlePreview = async (printer: PrinterInfo) => {
    console.log(`[Preview] Making the ${settings.receipt} image...`);
    try {
      const base64Image = await loadReceiptBase64(
        settings.receipt,
        settings.imageUrl,
      );
      const result = await EpsonPrinters.previewImage(
        base64Image,
        printer.info as EpsonPrinterInfo,
        toPrintOptions(settings),
      );
      console.log(`[Preview] ${result.width} x ${result.height} dots`);
      setPreview(result);
    } catch (error) {
      console.error("[Preview] Failed:", error);
      Alert.alert(
        "Preview Failed",
        error instanceof Error ? error.message : "Unknown error occurred",
      );
    }
  };

  const handleStatus = async (printer: PrinterInfo) => {
    console.log(`[Status] Reading status of ${getUniquePrinterId(printer)}...`);
    try {
      const status = await getStatus(printer);
      console.log("[Status] Result:", status);
      Alert.alert(
        status.online ? "Online" : "Offline",
        `Cover open: ${status.coverOpen}\nPaper: ${status.paper}\nErrors: ${
          status.errors.join(", ") || "none"
        }`,
      );
    } catch (error) {
      const code = (error as { code?: string }).code ?? "UNKNOWN";
      console.error(`[Status] Failed (${code}):`, error);
      Alert.alert("Status Failed", code);
    }
  };

  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.content}>
        <Text style={styles.title}>Printers SDK</Text>

        <CompactPrinterSelector
          connectionType={selectedConnectionType}
          manufacturer={selectedManufacturer}
          isSearching={isSearching}
          isRequestingPermissions={isRequestingPermissions}
          supportedModels={supportedModels}
          onConnectionTypeChange={setSelectedConnectionType}
          onManufacturerChange={setSelectedManufacturer}
          onSearch={handleSearch}
          onRefreshModels={refreshSupportedModels}
          onManualConnect={handleManualConnect}
        />

        <PrintOptions settings={settings} onChange={setSettings} />

        <PrinterList
          printers={printers}
          isSearching={isSearching}
          printingStates={printingStates}
          onPrint={handlePrint}
          onStatus={handleStatus}
          onPreview={handlePreview}
        />

        <LogViewer logs={logs} onClear={clearLogs} maxHeight={180} />
      </View>
      <ImagePreview
        preview={preview}
        title={`${settings.receipt}, ${settings.imageConversion}`}
        onClose={() => setPreview(null)}
      />
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: "#fff",
  },
  content: {
    flex: 1,
    padding: 16,
    gap: 16,
  },
  title: {
    fontSize: 20,
    fontWeight: "700",
    textAlign: "center",
    color: "#111",
    fontFamily: MONO_FONT,
  },
});

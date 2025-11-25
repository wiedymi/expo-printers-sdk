import React, { useState } from "react";
import {
  SafeAreaView,
  View,
  Text,
  Alert,
  StyleSheet,
  Platform,
} from "react-native";
import type { PrinterConnectionType } from "expo-printers-sdk";
import type { Manufacturer, PrinterInfo } from "./types/printer";
import { usePrinters } from "./hooks/usePrinters";
import { usePermissions } from "./hooks/usePermissions";
import { useLogs } from "./hooks/useLogs";
import { fetchImageAsBase64, getUniquePrinterId } from "./utils/printer-utils";
import CompactPrinterSelector from "./components/compact-printer-selector";
import PrinterList from "./components/printer-list";
import LogViewer from "./components/log-viewer";

const TEST_IMAGE_URL = "https://placehold.co/600x400/000000/FFFFFF.png";

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
    refreshSupportedModels,
  } = usePrinters(selectedManufacturer);

  const handleSearch = async () => {
    try {
      console.log(`[Search] Starting ${selectedManufacturer} ${selectedConnectionType} search...`);
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
    port: number
  ) => {
    try {
      console.log(`[Manual] Connecting to ${selectedManufacturer} @ ${ipAddress}:${port}...`);
      const printerInfo = await connectManually(ipAddress, modelName, port);
      if (printerInfo) {
        console.log("[Manual] Connected successfully:", printerInfo);
        Alert.alert(
          "Connected",
          `${selectedManufacturer} ${modelName} @ ${ipAddress}:${port}`
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
      console.log("[Print] Fetching test image...");
      const base64Image = await fetchImageAsBase64(TEST_IMAGE_URL);
      console.log(`[Print] Image fetched (${base64Image.length} chars), sending to printer...`);

      const result = await printImage(base64Image, printer, printerId);
      console.log(`[Print] printImage returned: ${result}`);

      if (!result) {
        console.error("[Print] Failed to initiate print job");
        Alert.alert("Print Failed", "Failed to initiate print job");
      }
    } catch (error) {
      console.error("[Print] Error:", error);
      Alert.alert(
        "Print Failed",
        error instanceof Error ? error.message : "Unknown error occurred"
      );
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

        <PrinterList
          printers={printers}
          isSearching={isSearching}
          printingStates={printingStates}
          onPrint={handlePrint}
        />

        <LogViewer logs={logs} onClear={clearLogs} maxHeight={180} />
      </View>
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

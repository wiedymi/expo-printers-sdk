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
import { fetchImageAsBase64, getUniquePrinterId } from "./utils/printer-utils";
import CompactPrinterSelector from "./components/compact-printer-selector";
import PrinterList from "./components/printer-list";

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

  const { isRequestingPermissions, requestPermissions } = usePermissions();
  const {
    printers,
    isSearching,
    printingStates,
    searchPrinters,
    connectManually,
    printImage,
  } = usePrinters(selectedManufacturer);

  const handleSearch = async () => {
    try {
      const hasPermissions = await requestPermissions();
      if (!hasPermissions) return;

      await searchPrinters(selectedConnectionType);
    } catch (error) {
      console.error("Failed to find printers:", error);
      Alert.alert("Error", "Failed to find printers. Please try again.");
    }
  };

  const handleManualConnect = async (ipAddress: string, port: number) => {
    try {
      const printerInfo = await connectManually(ipAddress, port);
      if (printerInfo) {
        Alert.alert("Connected", `${selectedManufacturer} @ ${ipAddress}:${port}`);
      }
    } catch (error) {
      Alert.alert("Error", "Failed to connect to printer");
    }
  };

  const handlePrint = async (printer: PrinterInfo) => {
    const printerId = getUniquePrinterId(printer);

    try {
      const base64Image = await fetchImageAsBase64(TEST_IMAGE_URL);
      const result = await printImage(base64Image, printer, printerId);

      if (!result) {
        Alert.alert("Print Failed", "Failed to initiate print job");
      }
    } catch (error) {
      console.error("Print failed:", error);
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
          onConnectionTypeChange={setSelectedConnectionType}
          onManufacturerChange={setSelectedManufacturer}
          onSearch={handleSearch}
          onManualConnect={handleManualConnect}
        />

        <PrinterList
          printers={printers}
          isSearching={isSearching}
          printingStates={printingStates}
          onPrint={handlePrint}
        />
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

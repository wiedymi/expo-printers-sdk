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
import React, { useEffect, useState } from "react";
import {
  SafeAreaView,
  ScrollView,
  Text,
  View,
  Alert,
  Platform,
  PermissionsAndroid,
  TouchableOpacity,
  ActivityIndicator,
  StyleSheet,
} from "react-native";
import PrinterCard from "./components/printer-card";
import ButtonRow from "./components/button-row";
import {
  fetchImageAsBase64,
  deduplicatePrinters,
  getUniquePrinterId,
} from "./utils/printer-utils";
import type { Manufacturer, PrinterInfo } from "./types/printer";

// Test image URL - a sample receipt-like image for testing
const TEST_IMAGE_URL = "https://placehold.co/600x400/000000/FFFFFF.png";

const MONO_FONT = Platform.select({
  ios: "Menlo",
  android: "monospace",
  default: "monospace",
});

export default function App() {
  const [printers, setPrinters] = useState<PrinterInfo[]>([]);
  const [isSearching, setIsSearching] = useState(false);
  const [selectedConnectionType, setSelectedConnectionType] =
    useState<PrinterConnectionType>("Network");
  const [selectedManufacturer, setSelectedManufacturer] =
    useState<Manufacturer>("EPSON");
  const [isRequestingPermissions, setIsRequestingPermissions] = useState(false);
  const [printingStates, setPrintingStates] = useState<{
    [key: string]: boolean;
  }>({});

  // Unified handler for onPrintersFound (no manufacturer param needed)
  const handlePrintersFound = (data: { printers: any[] }) => {
    setPrinters((prev) =>
      deduplicatePrinters([
        ...prev,
        ...data.printers.map((printer) => ({
          type: selectedManufacturer,
          info: printer,
        })),
      ])
    );
    setIsSearching(false);
  };

  // Unified print result handler (already present)
  const handlePrintResult = (
    result: EpsonPrintResult | RongtaPrintResult | StarMicronicsPrintResult
  ) => {
    console.log("Print result:", result);
    // Try to clear all printing states (since we use random printerId, this is a best-effort)
    setPrintingStates((prev) => {
      // Optionally, you could clear all, or try to match by result/printer info if available
      const newState = { ...prev };
      Object.keys(newState).forEach((key) => {
        newState[key] = false;
      });
      return newState;
    });
    if (result.success) {
      Alert.alert("Print Success", "Image printed successfully!");
    } else {
      Alert.alert("Print Failed", result.error || "Unknown error occurred");
    }
  };

  // Attach only one onPrintersFound and one onPrintImage listener for the selected manufacturer
  useEffect(() => {
    let foundListener: { remove: () => void } | null = null;
    let printListener: { remove: () => void } | null = null;
    let mod: any;
    switch (selectedManufacturer) {
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
  }, [selectedManufacturer]);

  const requestPermissions = async () => {
    console.log("Requesting permissions");
    setIsRequestingPermissions(true);
    try {
      if (Platform.OS === "android") {
        const permissions = [
          PermissionsAndroid.PERMISSIONS.ACCESS_FINE_LOCATION,
          PermissionsAndroid.PERMISSIONS.BLUETOOTH_SCAN,
          PermissionsAndroid.PERMISSIONS.BLUETOOTH_CONNECT,
        ].filter(Boolean);

        if (permissions.length === 0) {
          return true;
        }

        const results = await PermissionsAndroid.requestMultiple(permissions);

        const allGranted = Object.values(results).every(
          (result) => result === PermissionsAndroid.RESULTS.GRANTED
        );

        if (!allGranted) {
          console.log("Permissions not granted");
          Alert.alert(
            "Permission Required",
            "Location and Bluetooth permissions are required to search for printers."
          );
          return false;
        }

        console.log("All permissions granted");
        return true;
      }
      return true;
    } catch (error) {
      console.error("Error requesting permissions:", error);
      return false;
    } finally {
      setIsRequestingPermissions(false);
    }
  };

  const handleSearch = async () => {
    console.log(
      "Starting search for",
      selectedManufacturer,
      "printers over",
      selectedConnectionType
    );
    setPrinters([]);
    setIsSearching(true);

    try {
      const hasPermissions = await requestPermissions();
      if (!hasPermissions) {
        console.log("Permissions denied, stopping search");
        setIsSearching(false);
        return;
      }

      console.log("Searching for printers...");
      let searchPromise: Promise<boolean>;
      switch (selectedManufacturer) {
        case "EPSON":
          searchPromise = EpsonPrinters.findPrinters(selectedConnectionType);
          break;
        case "RONGTA":
          searchPromise = RongtaPrinters.findPrinters(selectedConnectionType);
          break;
        case "STAR":
          searchPromise = StarMicronicsPrinters.findPrinters(
            selectedConnectionType
          );
          break;
        default:
          throw new Error("Unknown manufacturer");
      }
      const result = await searchPromise;
      console.log("Search completed with result:", result);
      // No timeout here; isSearching will be set to false in the listener
    } catch (error) {
      console.error("Failed to find printers:", error);
      Alert.alert("Error", "Failed to find printers. Please try again.");
      setIsSearching(false);
    }
  };

  const handlePrint = async (printer: PrinterInfo) => {
    const printerId = getUniquePrinterId(printer);
    setPrintingStates((prev) => ({ ...prev, [printerId]: true }));

    try {
      console.log(
        "Fetching and printing test image to",
        printer.type,
        "printer"
      );

      // Fetch the image and convert to base64
      const base64Image = await fetchImageAsBase64(TEST_IMAGE_URL);
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

      console.log("Print initiated with result:", result);

      if (!result) {
        setPrintingStates((prev) => ({ ...prev, [printerId]: false }));
        Alert.alert("Print Failed", "Failed to initiate print job");
      }
      // Note: actual result will come via event listener
    } catch (error) {
      console.error("Print failed:", error);
      setPrintingStates((prev) => ({ ...prev, [printerId]: false }));

      // Provide more specific error messages
      let errorMessage = "Unknown error occurred";
      if (error instanceof Error) {
        if (error.message.includes("fetch")) {
          errorMessage =
            "Failed to download test image. Please check your internet connection.";
        } else {
          errorMessage = error.message;
        }
      }

      Alert.alert("Print Failed", errorMessage);
    }
  };

  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.content}>
        <Text style={styles.title}>🖨️ Printers SDK Demo</Text>

        {/* Connection Type Selection */}
        <Text style={styles.sectionTitle}>Connection Type</Text>
        <ButtonRow
          options={["Network", "Bluetooth", "USB"]}
          selected={selectedConnectionType}
          onSelect={setSelectedConnectionType}
          buttonStyle={styles.connectionButton}
          selectedButtonStyle={styles.selectedButton}
          buttonTextStyle={styles.buttonText}
          selectedButtonTextStyle={styles.selectedButtonText}
        />

        {/* Manufacturer Selection */}
        <Text style={styles.sectionTitle}>Manufacturer</Text>
        <ButtonRow
          options={["EPSON", "RONGTA", "STAR"]}
          selected={selectedManufacturer}
          onSelect={setSelectedManufacturer}
          buttonStyle={styles.manufacturerButton}
          selectedButtonStyle={styles.selectedButton}
          buttonTextStyle={styles.buttonText}
          selectedButtonTextStyle={styles.selectedButtonText}
          labelMap={{ STAR: "Star", EPSON: "EPSON", RONGTA: "RONGTA" }}
        />

        {/* Search Button */}
        <TouchableOpacity
          style={[
            styles.searchButton,
            (isSearching || isRequestingPermissions) && styles.disabledButton,
          ]}
          onPress={handleSearch}
          disabled={isSearching || isRequestingPermissions}
          accessibilityLabel="Search for printers"
          accessible={true}
        >
          {isSearching ? (
            <View style={styles.loadingContainer}>
              <ActivityIndicator color="#fff" size="small" />
              <Text style={styles.loadingText}>Searching...</Text>
            </View>
          ) : (
            <Text style={styles.searchButtonText}>🔍 Search Printers</Text>
          )}
        </TouchableOpacity>

        {/* Results */}
        <ScrollView
          style={styles.resultsContainer}
          showsVerticalScrollIndicator={false}
        >
          {!isSearching && printers.length === 0 ? (
            <View style={styles.emptyState}>
              <Text style={styles.emptyStateText}>
                No printers found yet...
              </Text>
              <Text style={styles.emptyStateSubtext}>
                Select connection type and manufacturer, then tap "Search
                Printers"
              </Text>
            </View>
          ) : (
            printers.map((printer, index) => {
              const printerId = getUniquePrinterId(printer);
              return (
                <PrinterCard
                  key={printerId}
                  printer={printer}
                  onPrint={handlePrint}
                  isPrinting={!!printingStates[printerId]}
                  printerId={printerId}
                />
              );
            })
          )}
        </ScrollView>
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
  },
  title: {
    fontSize: 24,
    fontWeight: "700",
    marginBottom: 24,
    textAlign: "center",
    color: "#111",
    fontFamily: MONO_FONT,
  },
  sectionTitle: {
    fontSize: 16,
    fontWeight: "600",
    marginBottom: 8,
    marginTop: 16,
    color: "#222",
    fontFamily: MONO_FONT,
  },
  buttonRow: {
    flexDirection: "row",
    marginBottom: 8,
  },
  connectionButton: {
    flex: 1,
    padding: 10,
    backgroundColor: "#f5f5f5",
    marginHorizontal: 4,
    borderRadius: 0,
    alignItems: "center",
    borderWidth: 1,
    borderColor: "#d1d1d1",
  },
  manufacturerButton: {
    flex: 1,
    padding: 10,
    backgroundColor: "#f5f5f5",
    marginHorizontal: 4,
    borderRadius: 0,
    alignItems: "center",
    borderWidth: 1,
    borderColor: "#d1d1d1",
  },
  selectedButton: {
    backgroundColor: "#222",
    borderColor: "#111",
    borderRadius: 0,
  },
  buttonText: {
    color: "#222",
    fontWeight: "500",
    fontFamily: MONO_FONT,
  },
  selectedButtonText: {
    color: "#fff",
    fontFamily: MONO_FONT,
  },
  searchButton: {
    backgroundColor: "#222",
    padding: 16,
    borderRadius: 0,
    alignItems: "center",
    marginTop: 16,
  },
  disabledButton: {
    backgroundColor: "#bbb",
  },
  searchButtonText: {
    color: "#fff",
    fontSize: 16,
    fontWeight: "600",
    fontFamily: MONO_FONT,
  },
  loadingContainer: {
    flexDirection: "row",
    alignItems: "center",
  },
  loadingText: {
    color: "#fff",
    marginLeft: 8,
    fontSize: 16,
    fontWeight: "600",
    fontFamily: MONO_FONT,
  },
  resultsContainer: {
    flex: 1,
    marginTop: 16,
  },
  emptyState: {
    padding: 40,
    alignItems: "center",
  },
  emptyStateText: {
    fontSize: 16,
    color: "#444",
    textAlign: "center",
    fontFamily: MONO_FONT,
  },
  emptyStateSubtext: {
    fontSize: 14,
    color: "#888",
    textAlign: "center",
    marginTop: 8,
    fontFamily: MONO_FONT,
  },
  printerCard: {
    backgroundColor: "#f5f5f5",
    padding: 16,
    borderRadius: 0,
    marginBottom: 12,
    borderWidth: 1,
    borderColor: "#d1d1d1",
  },
  printerHeader: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
    marginBottom: 12,
  },
  printerType: {
    fontSize: 18,
    fontWeight: "700",
    color: "#111",
    fontFamily: MONO_FONT,
  },
  printButton: {
    backgroundColor: "#444",
    paddingHorizontal: 16,
    paddingVertical: 8,
    borderRadius: 0,
  },
  printButtonText: {
    color: "#fff",
    fontWeight: "600",
    fontFamily: MONO_FONT,
  },
  printerDetails: {
    gap: 4,
  },
  detailText: {
    fontSize: 14,
    color: "#333",
    lineHeight: 20,
    fontFamily: MONO_FONT,
  },
});

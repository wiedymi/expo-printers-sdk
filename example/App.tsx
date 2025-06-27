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

type Manufacturer = "EPSON" | "STAR" | "RONGTA";

type PrinterInfo = {
  type: Manufacturer;
  info: EpsonPrinterInfo | RongtaPrinterInfo | StarMicronicsPrinterInfo;
};

// Test image URL - a sample receipt-like image for testing
const TEST_IMAGE_URL = "https://placehold.co/600x400/000000/FFFFFF.png";

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

  /**
   * Fetches an image from URL and converts it to base64 string
   */
  const fetchImageAsBase64 = async (imageUrl: string): Promise<string> => {
    console.log(`Fetching image from ${imageUrl}`);

    const response = await fetch(imageUrl);
    if (!response.ok) {
      const error = new Error(`Failed to fetch image: ${response.statusText}`);
      console.error("Fetch error:", error);
      throw error;
    }

    const blob = await response.blob();
    console.log(`Successfully retrieved image blob of size ${blob.size} bytes`);

    const base64String = await new Promise<string>((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = () => {
        // Extract the pure base64 content by removing the data URL prefix
        const fullResult = reader.result as string;
        const base64Data = fullResult.split(",")[1] || "";
        resolve(base64Data);
      };
      reader.onerror = (error) => {
        console.error("FileReader error:", error);
        reject(error);
      };
      reader.readAsDataURL(blob);
    });

    console.log(
      `Converted image to base64 string of length ${base64String.length}`
    );
    return base64String;
  };

  useEffect(() => {
    // Set up event listeners for printer discovery
    const epsonListener = EpsonPrinters.addListener(
      "onPrintersFound",
      (data: { printers: EpsonPrinterInfo[] }) => {
        console.log("Epson printers found:", data.printers.length);
        setPrinters((prev) => [
          ...prev,
          ...data.printers.map((printer) => ({
            type: "EPSON" as Manufacturer,
            info: printer,
          })),
        ]);
        setIsSearching(false);
      }
    );

    const rongtaListener = RongtaPrinters.addListener(
      "onPrintersFound",
      (data: { printers: RongtaPrinterInfo[] }) => {
        console.log("Rongta printers found:", data.printers.length);
        setPrinters((prev) => [
          ...prev,
          ...data.printers.map((printer) => ({
            type: "RONGTA" as Manufacturer,
            info: printer,
          })),
        ]);
        setIsSearching(false);
      }
    );

    const starListener = StarMicronicsPrinters.addListener(
      "onPrintersFound",
      (data: { printers: StarMicronicsPrinterInfo[] }) => {
        console.log("Star Micronics printers found:", data.printers.length);
        setPrinters((prev) => [
          ...prev,
          ...data.printers.map((printer) => ({
            type: "STAR" as Manufacturer,
            info: printer,
          })),
        ]);
        setIsSearching(false);
      }
    );

    // Set up event listeners for print results
    const epsonPrintListener = EpsonPrinters.addListener(
      "onPrintImage",
      (result: EpsonPrintResult) => {
        console.log("Epson print result:", result);
        setPrintingStates((prev) => ({ ...prev, epson: false }));
        if (result.success) {
          Alert.alert("Print Success", "Image printed successfully!");
        } else {
          Alert.alert("Print Failed", result.error || "Unknown error occurred");
        }
      }
    );

    const rongtaPrintListener = RongtaPrinters.addListener(
      "onPrintImage",
      (result: RongtaPrintResult) => {
        console.log("Rongta print result:", result);
        setPrintingStates((prev) => ({ ...prev, rongta: false }));
        if (result.success) {
          Alert.alert("Print Success", "Image printed successfully!");
        } else {
          Alert.alert("Print Failed", result.error || "Unknown error occurred");
        }
      }
    );

    const starPrintListener = StarMicronicsPrinters.addListener(
      "onPrintImage",
      (result: StarMicronicsPrintResult) => {
        console.log("Star print result:", result);
        setPrintingStates((prev) => ({ ...prev, star: false }));
        if (result.success) {
          Alert.alert("Print Success", "Image printed successfully!");
        } else {
          Alert.alert("Print Failed", result.error || "Unknown error occurred");
        }
      }
    );

    return () => {
      console.log("Cleaning up listeners");
      epsonListener.remove();
      rongtaListener.remove();
      starListener.remove();
      epsonPrintListener.remove();
      rongtaPrintListener.remove();
      starPrintListener.remove();
    };
  }, []);

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

      if (selectedManufacturer === "EPSON") {
        searchPromise = EpsonPrinters.findPrinters(selectedConnectionType);
      } else if (selectedManufacturer === "RONGTA") {
        searchPromise = RongtaPrinters.findPrinters(selectedConnectionType);
      } else if (selectedManufacturer === "STAR") {
        searchPromise = StarMicronicsPrinters.findPrinters(
          selectedConnectionType
        );
      } else {
        throw new Error("Unknown manufacturer");
      }

      const result = await searchPromise;
      console.log("Search completed with result:", result);

      // Set a timeout to stop searching if no results come back
      setTimeout(() => {
        setIsSearching(false);
      }, 10000); // 10 second timeout
    } catch (error) {
      console.error("Failed to find printers:", error);
      Alert.alert("Error", "Failed to find printers. Please try again.");
      setIsSearching(false);
    }
  };

  const handlePrint = async (printer: PrinterInfo) => {
    const printerId = `${printer.type}-${Math.random()}`;
    setPrintingStates((prev) => ({ ...prev, [printerId]: true }));

    try {
      console.log(
        "Fetching and printing test image to",
        printer.type,
        "printer"
      );

      // Fetch the image and convert to base64
      const base64Image = await fetchImageAsBase64(TEST_IMAGE_URL);

      let printPromise: Promise<boolean>;

      if (printer.type === "EPSON") {
        printPromise = EpsonPrinters.printImage(
          base64Image,
          printer.info as EpsonPrinterInfo
        );
      } else if (printer.type === "RONGTA") {
        printPromise = RongtaPrinters.printImage(
          base64Image,
          printer.info as RongtaPrinterInfo
        );
      } else if (printer.type === "STAR") {
        printPromise = StarMicronicsPrinters.printImage(
          base64Image,
          printer.info as StarMicronicsPrinterInfo
        );
      } else {
        throw new Error("Unknown printer type");
      }

      const result = await printPromise;
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

  const renderPrinterInfo = (printer: PrinterInfo) => {
    switch (printer.type) {
      case "EPSON":
        const epsonInfo = printer.info as EpsonPrinterInfo;
        return (
          <View style={styles.printerDetails}>
            <Text style={styles.detailText}>
              Device: {epsonInfo.deviceName}
            </Text>
            <Text style={styles.detailText}>Target: {epsonInfo.target}</Text>
            <Text style={styles.detailText}>
              IP Address: {epsonInfo.ipAddress}
            </Text>
            <Text style={styles.detailText}>
              MAC Address: {epsonInfo.macAddress}
            </Text>
            <Text style={styles.detailText}>
              BD Address: {epsonInfo.bdAddress}
            </Text>
            <Text style={styles.detailText}>
              Connection: {epsonInfo.connectionType}
            </Text>
            <Text style={styles.detailText}>
              Device Type: {epsonInfo.deviceType}
            </Text>
          </View>
        );
      case "RONGTA":
        const rongtaInfo = printer.info as RongtaPrinterInfo;
        return (
          <View style={styles.printerDetails}>
            <Text style={styles.detailText}>
              Connection: {rongtaInfo.connectionType}
            </Text>
            <Text style={styles.detailText}>Type: {rongtaInfo.type.type}</Text>
            {rongtaInfo.type.type === "BLUETOOTH" ? (
              <>
                <Text style={styles.detailText}>
                  Alias: {rongtaInfo.type.alias}
                </Text>
                <Text style={styles.detailText}>
                  Name: {rongtaInfo.type.name}
                </Text>
                <Text style={styles.detailText}>
                  Address: {rongtaInfo.type.address}
                </Text>
              </>
            ) : (
              <>
                <Text style={styles.detailText}>
                  IP Address: {rongtaInfo.type.ipAddress}
                </Text>
                <Text style={styles.detailText}>
                  Port: {rongtaInfo.type.port}
                </Text>
              </>
            )}
          </View>
        );
      case "STAR":
        const starInfo = printer.info as StarMicronicsPrinterInfo;
        return (
          <View style={styles.printerDetails}>
            <Text style={styles.detailText}>Device: {starInfo.deviceName}</Text>
            <Text style={styles.detailText}>Port: {starInfo.portName}</Text>
            <Text style={styles.detailText}>
              MAC Address: {starInfo.macAddress}
            </Text>
            <Text style={styles.detailText}>
              USB Serial: {starInfo.usbSerialNumber}
            </Text>
            <Text style={styles.detailText}>
              Connection: {starInfo.connectionType}
            </Text>
          </View>
        );
    }
  };

  const getPrinterId = (printer: PrinterInfo, index: number) => {
    return `${printer.type}-${index}`;
  };

  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.content}>
        <Text style={styles.title}>🖨️ Printers SDK Demo</Text>

        {/* Connection Type Selection */}
        <Text style={styles.sectionTitle}>Connection Type</Text>
        <View style={styles.buttonRow}>
          {(["Network", "Bluetooth", "USB"] as PrinterConnectionType[]).map(
            (type) => (
              <TouchableOpacity
                key={type}
                style={[
                  styles.connectionButton,
                  selectedConnectionType === type && styles.selectedButton,
                ]}
                onPress={() => setSelectedConnectionType(type)}
              >
                <Text
                  style={[
                    styles.buttonText,
                    selectedConnectionType === type &&
                      styles.selectedButtonText,
                  ]}
                >
                  {type}
                </Text>
              </TouchableOpacity>
            )
          )}
        </View>

        {/* Manufacturer Selection */}
        <Text style={styles.sectionTitle}>Manufacturer</Text>
        <View style={styles.buttonRow}>
          {(["EPSON", "RONGTA", "STAR"] as Manufacturer[]).map(
            (manufacturer) => (
              <TouchableOpacity
                key={manufacturer}
                style={[
                  styles.manufacturerButton,
                  selectedManufacturer === manufacturer &&
                    styles.selectedButton,
                ]}
                onPress={() => setSelectedManufacturer(manufacturer)}
              >
                <Text
                  style={[
                    styles.buttonText,
                    selectedManufacturer === manufacturer &&
                      styles.selectedButtonText,
                  ]}
                >
                  {manufacturer === "STAR" ? "Star" : manufacturer}
                </Text>
              </TouchableOpacity>
            )
          )}
        </View>

        {/* Search Button */}
        <TouchableOpacity
          style={[
            styles.searchButton,
            (isSearching || isRequestingPermissions) && styles.disabledButton,
          ]}
          onPress={handleSearch}
          disabled={isSearching || isRequestingPermissions}
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
            printers.map((printer, index) => (
              <View key={index} style={styles.printerCard}>
                <View style={styles.printerHeader}>
                  <Text style={styles.printerType}>{printer.type}</Text>
                  <TouchableOpacity
                    style={[
                      styles.printButton,
                      printingStates[getPrinterId(printer, index)] &&
                        styles.disabledButton,
                    ]}
                    onPress={() => handlePrint(printer)}
                    disabled={printingStates[getPrinterId(printer, index)]}
                  >
                    {printingStates[getPrinterId(printer, index)] ? (
                      <ActivityIndicator color="#fff" size="small" />
                    ) : (
                      <Text style={styles.printButtonText}>Print Test</Text>
                    )}
                  </TouchableOpacity>
                </View>
                {renderPrinterInfo(printer)}
              </View>
            ))
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
    color: "#333",
  },
  sectionTitle: {
    fontSize: 16,
    fontWeight: "600",
    marginBottom: 8,
    marginTop: 16,
    color: "#555",
  },
  buttonRow: {
    flexDirection: "row",
    marginBottom: 8,
  },
  connectionButton: {
    flex: 1,
    padding: 10,
    backgroundColor: "#f0f0f0",
    marginHorizontal: 4,
    borderRadius: 8,
    alignItems: "center",
    borderWidth: 1,
    borderColor: "#e0e0e0",
  },
  manufacturerButton: {
    flex: 1,
    padding: 10,
    backgroundColor: "#f0f0f0",
    marginHorizontal: 4,
    borderRadius: 8,
    alignItems: "center",
    borderWidth: 1,
    borderColor: "#e0e0e0",
  },
  selectedButton: {
    backgroundColor: "#007AFF",
    borderColor: "#0056CC",
  },
  buttonText: {
    color: "#333",
    fontWeight: "500",
  },
  selectedButtonText: {
    color: "#fff",
  },
  searchButton: {
    backgroundColor: "#007AFF",
    padding: 16,
    borderRadius: 8,
    alignItems: "center",
    marginTop: 16,
  },
  disabledButton: {
    backgroundColor: "#ccc",
  },
  searchButtonText: {
    color: "#fff",
    fontSize: 16,
    fontWeight: "600",
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
    color: "#666",
    textAlign: "center",
  },
  emptyStateSubtext: {
    fontSize: 14,
    color: "#999",
    textAlign: "center",
    marginTop: 8,
  },
  printerCard: {
    backgroundColor: "#f8f8f8",
    padding: 16,
    borderRadius: 12,
    marginBottom: 12,
    borderWidth: 1,
    borderColor: "#e0e0e0",
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
    color: "#333",
  },
  printButton: {
    backgroundColor: "#34C759",
    paddingHorizontal: 16,
    paddingVertical: 8,
    borderRadius: 6,
  },
  printButtonText: {
    color: "#fff",
    fontWeight: "600",
  },
  printerDetails: {
    gap: 4,
  },
  detailText: {
    fontSize: 14,
    color: "#666",
    lineHeight: 20,
  },
});

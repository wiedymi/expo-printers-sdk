import React, { useEffect, useState } from "react";
import { EpsonPrinters, RongtaPrinters } from "expo-printers-sdk";
import {
  Button,
  SafeAreaView,
  ScrollView,
  Text,
  View,
  Alert,
  Platform,
  PermissionsAndroid,
  TouchableOpacity,
  ActivityIndicator,
} from "react-native";
import type { EpsonPrinterInfo, RongtaPrinterInfo } from "expo-printers-sdk";

type ConnectionType = "USB" | "Bluetooth" | "Network";
type Manufacturer = "EPSON" | "STAR" | "RONGTA";

type PrinterInfo = {
  type: Manufacturer;
  info: EpsonPrinterInfo | RongtaPrinterInfo;
};

export default function App() {
  const [printers, setPrinters] = useState<PrinterInfo[]>([]);
  const [isSearching, setIsSearching] = useState(false);
  const [selectedConnectionType, setSelectedConnectionType] = useState<ConnectionType>("Network");
  const [selectedManufacturer, setSelectedManufacturer] = useState<Manufacturer>("EPSON");
  const [isRequestingPermissions, setIsRequestingPermissions] = useState(false);

  useEffect(() => {
    // Set up event listeners for all printer types
    EpsonPrinters.addListener("onPrintersFound", (data: { printers: EpsonPrinterInfo[] }) => {
      setPrinters(prev => [...prev, ...data.printers.map(printer => ({ type: "EPSON" as Manufacturer, info: printer }))]);
      setIsSearching(false); // Stop searching when we get results
    });

    RongtaPrinters.addListener("onPrintersFound", (data: { printers: RongtaPrinterInfo[] }) => {
      setPrinters(prev => [...prev, ...data.printers.map(printer => ({ type: "RONGTA" as Manufacturer, info: printer }))]);
      setIsSearching(false); // Stop searching when we get results
    });

    return () => {
      // Cleanup listeners
      EpsonPrinters.removeAllListeners("onPrintersFound");
      RongtaPrinters.removeAllListeners("onPrintersFound");
    };
  }, []);

  const requestPermissions = async () => {
    if (Platform.OS !== "android") return true;

    setIsRequestingPermissions(true);
    try {
      const permissions = [
        PermissionsAndroid.PERMISSIONS.ACCESS_FINE_LOCATION,
        PermissionsAndroid.PERMISSIONS.ACCESS_COARSE_LOCATION,
        PermissionsAndroid.PERMISSIONS.BLUETOOTH_SCAN,
        PermissionsAndroid.PERMISSIONS.BLUETOOTH_CONNECT,
      ];

      const results = await PermissionsAndroid.requestMultiple(permissions);
      console.log("Permission results:", results);

      const allGranted = Object.values(results).every(
        (result) => result === PermissionsAndroid.RESULTS.GRANTED
      );

      if (!allGranted) {
        Alert.alert(
          "Permissions Required",
          "Please grant the necessary permissions in your device settings to search for printers."
        );
      }

      return allGranted;
    } catch (err) {
      console.error("Failed to request permissions:", err);
      return false;
    } finally {
      setIsRequestingPermissions(false);
    }
  };

  const handleSearch = async () => {
    console.log("handleSearch started");
    if (isSearching) return;

    const hasPermissions = await requestPermissions();
    console.log("Permissions result:", hasPermissions);
    if (!hasPermissions) return;

    setIsSearching(true);
    setPrinters([]); // Clear previous results

    console.log("Starting search with params:", {
      connectionType: selectedConnectionType,
      manufacturer: selectedManufacturer,
    });

    try {
      if (selectedManufacturer === "EPSON") {
        await EpsonPrinters.findPrinters(selectedConnectionType);
      } else if (selectedManufacturer === "RONGTA") {
        await RongtaPrinters.findPrinters(selectedConnectionType);
      }
      console.log("Search completed successfully");
    } catch (error) {
      console.error("Search failed:", error);
      setIsSearching(false);
      Alert.alert("Error", "Failed to search for printers");
    }
  };

  const handleStopSearch = () => {
    console.log("handleStopSearch called");
    setIsSearching(false);
  };

  const renderPrinterInfo = (printer: PrinterInfo) => {
    switch (printer.type) {
      case "EPSON":
        const epsonInfo = printer.info as EpsonPrinterInfo;
        return (
          <>
            <Text>Device: {epsonInfo.deviceName}</Text>
            <Text>IP: {epsonInfo.ip}</Text>
            <Text>MAC: {epsonInfo.mac}</Text>
          </>
        );
      case "RONGTA":
        const rongtaInfo = printer.info as RongtaPrinterInfo;
        return (
          <>
            <Text>Device: {rongtaInfo.deviceName}</Text>
            <Text>Alias: {rongtaInfo.alias}</Text>
            <Text>Address: {rongtaInfo.address}</Text>
          </>
        );
      default:
        return null;
    }
  };

  return (
    <SafeAreaView style={{ flex: 1, backgroundColor: "#fff" }}>
      <View style={{ padding: 16 }}>
        <Text style={styles.title}>Printers Playground</Text>
        
        <View style={{ flexDirection: "row", marginBottom: 16 }}>
          <TouchableOpacity
            style={[
              styles.connectionButton,
              selectedConnectionType === "Network" && styles.selectedButton,
            ]}
            onPress={() => setSelectedConnectionType("Network")}
          >
            <Text style={styles.buttonText}>Network</Text>
          </TouchableOpacity>
          <TouchableOpacity
            style={[
              styles.connectionButton,
              selectedConnectionType === "Bluetooth" && styles.selectedButton,
            ]}
            onPress={() => setSelectedConnectionType("Bluetooth")}
          >
            <Text style={styles.buttonText}>Bluetooth</Text>
          </TouchableOpacity>
          <TouchableOpacity
            style={[
              styles.connectionButton,
              selectedConnectionType === "USB" && styles.selectedButton,
            ]}
            onPress={() => setSelectedConnectionType("USB")}
          >
            <Text style={styles.buttonText}>USB</Text>
          </TouchableOpacity>
        </View>

        <View style={{ flexDirection: "row", marginBottom: 16 }}>
          <TouchableOpacity
            style={[
              styles.manufacturerButton,
              selectedManufacturer === "EPSON" && styles.selectedButton,
            ]}
            onPress={() => setSelectedManufacturer("EPSON")}
          >
            <Text style={styles.buttonText}>Epson</Text>
          </TouchableOpacity>
          <TouchableOpacity
            style={[
              styles.manufacturerButton,
              selectedManufacturer === "RONGTA" && styles.selectedButton,
            ]}
            onPress={() => setSelectedManufacturer("RONGTA")}
          >
            <Text style={styles.buttonText}>Rongta</Text>
          </TouchableOpacity>
        </View>

        <TouchableOpacity
          style={[
            styles.searchButton,
            (isSearching || isRequestingPermissions) && styles.disabledButton,
          ]}
          onPress={isSearching ? handleStopSearch : handleSearch}
          disabled={isSearching || isRequestingPermissions}
        >
          {isSearching || isRequestingPermissions ? (
            <ActivityIndicator color="#fff" />
          ) : (
            <Text style={styles.buttonText}>
              {isSearching ? "Stop Search" : "Search Printers"}
            </Text>
          )}
        </TouchableOpacity>

        <ScrollView style={{ marginTop: 16 }}>
          {!isSearching && printers.length === 0 ? (
            <View style={styles.emptyState}>
              <Text style={styles.emptyStateText}>No printers found yet...</Text>
            </View>
          ) : (
            printers.map((printer, index) => (
              <View key={index} style={styles.printerCard}>
                <Text style={styles.printerType}>{printer.type}</Text>
                {renderPrinterInfo(printer)}
              </View>
            ))
          )}
        </ScrollView>
      </View>
    </SafeAreaView>
  );
}

const styles = {
  title: {
    fontSize: 24,
    fontWeight: "700" as const,
    marginBottom: 24,
    textAlign: "center" as const,
  },
  emptyState: {
    padding: 24,
    alignItems: "center" as const,
  },
  emptyStateText: {
    fontSize: 16,
    color: "#666",
  },
  connectionButton: {
    flex: 1,
    padding: 8,
    backgroundColor: "#f0f0f0",
    marginHorizontal: 4,
    borderRadius: 8,
    alignItems: "center",
  },
  manufacturerButton: {
    flex: 1,
    padding: 8,
    backgroundColor: "#f0f0f0",
    marginHorizontal: 4,
    borderRadius: 8,
    alignItems: "center",
  },
  selectedButton: {
    backgroundColor: "#007AFF",
  },
  buttonText: {
    color: "#000",
  },
  searchButton: {
    backgroundColor: "#007AFF",
    padding: 12,
    borderRadius: 8,
    alignItems: "center",
  },
  disabledButton: {
    backgroundColor: "#ccc",
  },
  printerCard: {
    backgroundColor: "#f8f8f8",
    padding: 16,
    borderRadius: 8,
    marginBottom: 8,
  },
  printerType: {
    fontSize: 16,
    fontWeight: "700" as const,
    marginBottom: 8,
  },
};

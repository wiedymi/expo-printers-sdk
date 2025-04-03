import React, { useEffect, useState } from "react";
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
import { EpsonPrinters, RongtaPrinters, StarMicronicsPrinters } from "expo-printers-sdk";
import type { EpsonPrinterInfo, RongtaPrinterInfo, StarMicronicsPrinterInfo } from "expo-printers-sdk";

type ConnectionType = "USB" | "Bluetooth" | "Network";
type Manufacturer = "EPSON" | "STAR" | "RONGTA";

type PrinterInfo = {
  type: Manufacturer;
  info: EpsonPrinterInfo | RongtaPrinterInfo | StarMicronicsPrinterInfo;
};

export default function App() {
  const [printers, setPrinters] = useState<PrinterInfo[]>([]);
  const [isSearching, setIsSearching] = useState(false);
  const [selectedConnectionType, setSelectedConnectionType] = useState<ConnectionType>("Network");
  const [selectedManufacturer, setSelectedManufacturer] = useState<Manufacturer>("EPSON");
  const [isRequestingPermissions, setIsRequestingPermissions] = useState(false);

  useEffect(() => {
    // Set up event listeners for all printer types
    const epsonListener = EpsonPrinters.addListener("onPrintersFound", (data: { printers: EpsonPrinterInfo[] }) => {
      console.log("Epson printers found:", data.printers.length);
      setPrinters(prev => [...prev, ...data.printers.map(printer => ({ type: "EPSON" as Manufacturer, info: printer }))]);
      setIsSearching(false);
    });

    const rongtaListener = RongtaPrinters.addListener("onPrintersFound", (data: { printers: RongtaPrinterInfo[] }) => {
      console.log("Rongta printers found:", data.printers.length);
      setPrinters(prev => [...prev, ...data.printers.map(printer => ({ type: "RONGTA" as Manufacturer, info: printer }))]);
      setIsSearching(false);
    });

    const starListener = StarMicronicsPrinters.addListener("onPrintersFound", (data: { printers: StarMicronicsPrinterInfo[] }) => {
      console.log("Star Micronics printers found:", data.printers.length);
      setPrinters(prev => [...prev, ...data.printers.map(printer => ({ type: "STAR" as Manufacturer, info: printer }))]);
      setIsSearching(false);
    });

    return () => {
      console.log("Cleaning up listeners");
      epsonListener.remove();
      rongtaListener.remove();
      starListener.remove();
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
        ].filter(Boolean); // Filter out any undefined permissions

        if (permissions.length === 0) {
          // If no permissions are required (e.g., on older Android versions)
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
    console.log("Starting search for", selectedManufacturer, "printers over", selectedConnectionType);
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
      if (selectedManufacturer === "EPSON") {
        await EpsonPrinters.findPrinters(selectedConnectionType);
      } else if (selectedManufacturer === "RONGTA") {
        await RongtaPrinters.findPrinters(selectedConnectionType);
      } else if (selectedManufacturer === "STAR") {
        await StarMicronicsPrinters.findPrinters(selectedConnectionType);
      }
      console.log("Search completed successfully");
    } catch (error) {
      console.error("Failed to find printers:", error);
      Alert.alert("Error", "Failed to find printers. Please try again.");
      setIsSearching(false);
    }
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
      case "STAR":
        const starInfo = printer.info as StarMicronicsPrinterInfo;
        return (
          <>
            <Text>Device: {starInfo.deviceName}</Text>
            <Text>Port: {starInfo.portName}</Text>
            <Text>MAC: {starInfo.macAddress}</Text>
            <Text>USB Serial: {starInfo.usbSerialNumber}</Text>
          </>
        );
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
          <TouchableOpacity
            style={[
              styles.manufacturerButton,
              selectedManufacturer === "STAR" && styles.selectedButton,
            ]}
            onPress={() => setSelectedManufacturer("STAR")}
          >
            <Text style={styles.buttonText}>Star Micronics</Text>
          </TouchableOpacity>
        </View>

        <TouchableOpacity
          style={[styles.searchButton, isSearching && styles.disabledButton]}
          onPress={handleSearch}
          disabled={isSearching || isRequestingPermissions}
        >
          {isSearching ? (
            <ActivityIndicator color="#fff" />
          ) : (
            <Text style={styles.buttonText}>Search Printers</Text>
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

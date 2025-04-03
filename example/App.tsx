import React, { useEffect, useState } from "react";
import { EpsonPrinters } from "expo-printers-sdk";
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
import type { EpsonPrinterInfo } from "expo-printers-sdk/EpsonPrinters.types";

type ConnectionType = "USB" | "Bluetooth" | "Network";
type Manufacturer = "EPSON" | "STAR" | "RONGTA";

type PrinterInfo = {
  type: Manufacturer;
  info: EpsonPrinterInfo;
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

    return () => {
      // Cleanup listeners
      EpsonPrinters.removeAllListeners("onPrintersFound");
    };
  }, []);

  const requestPermissions = async () => {
    if (Platform.OS === "android") {
      try {
        setIsRequestingPermissions(true);
        const permissions = [
          PermissionsAndroid.PERMISSIONS.ACCESS_FINE_LOCATION,
          PermissionsAndroid.PERMISSIONS.ACCESS_COARSE_LOCATION,
          PermissionsAndroid.PERMISSIONS.BLUETOOTH_SCAN,
          PermissionsAndroid.PERMISSIONS.BLUETOOTH_CONNECT,
          PermissionsAndroid.PERMISSIONS.BLUETOOTH_ADVERTISE,
          PermissionsAndroid.PERMISSIONS.ACCESS_NETWORK_STATE,
          PermissionsAndroid.PERMISSIONS.INTERNET,
        ].filter(Boolean);

        const results = await PermissionsAndroid.requestMultiple(permissions);
        console.log("Permission results:", results);

        const allGranted = Object.values(results).every(result => result === PermissionsAndroid.RESULTS.GRANTED);

        if (!allGranted) {
          Alert.alert(
            "Permissions Required",
            "Please grant all permissions in your device settings to search for printers. You can find them in Settings > Apps > Expo Go > Permissions"
          );
        }

        return allGranted;
      } catch (err) {
        console.error("Error requesting permissions:", err);
        Alert.alert("Error", "Failed to request permissions. Please try again.");
        return false;
      } finally {
        setIsRequestingPermissions(false);
      }
    }
    return true;
  };

  const handleSearch = async () => {
    console.log("handleSearch started");
    try {
      if (isRequestingPermissions) {
        console.log("Already requesting permissions, please wait...");
        return;
      }

      console.log("Requesting permissions...");
      const hasPermissions = await requestPermissions();
      console.log("Permissions result:", hasPermissions);
      if (!hasPermissions) {
        return;
      }

      console.log("Starting search with:", { selectedManufacturer, selectedConnectionType });
      setPrinters([]); // Clear previous results
      setIsSearching(true); // Start searching

      // Call appropriate search method based on manufacturer and connection type
      switch (selectedManufacturer) {
        case "EPSON":
          console.log("Calling EpsonPrinters.findPrinters");
          await EpsonPrinters.findPrinters(selectedConnectionType);
          console.log("EpsonPrinters.findPrinters completed");
          break;
        case "STAR":
          Alert.alert("Not Implemented", "Star Micronics printer discovery is not implemented yet");
          setIsSearching(false);
          break;
        case "RONGTA":
          Alert.alert("Not Implemented", "Rongta printer discovery is not implemented yet");
          setIsSearching(false);
          break;
      }
    } catch (error) {
      console.error("Failed to start discovery:", error);
      Alert.alert("Error", "Failed to start printer discovery");
      setIsSearching(false);
    }
  };

  const handleStopSearch = async () => {
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
      case "STAR":
        const starInfo = printer.info as StarMicronicsPrinterInfo;
        return (
          <>
            <Text>Device: {starInfo.deviceName}</Text>
            <Text>IP: {starInfo.ip}</Text>
            <Text>MAC: {starInfo.mac}</Text>
          </>
        );
      case "RONGTA":
        const rongtaInfo = printer.info as RongtaPrinterInfo;
        return (
          <>
            <Text>Device: {rongtaInfo.deviceName}</Text>
            <Text>IP: {rongtaInfo.ip}</Text>
            <Text>MAC: {rongtaInfo.mac}</Text>
          </>
        );
    }
  };

  return (
    <SafeAreaView style={styles.container}>
      <ScrollView style={styles.container}>
        <Text style={styles.header}>Printer Discovery</Text>

        <View style={styles.group}>
          <Text style={styles.subHeader}>Connection Type</Text>
          <View style={styles.radioGroup}>
            {(["USB", "Bluetooth", "Network"] as ConnectionType[]).map((type) => (
              <TouchableOpacity
                key={type}
                style={[styles.radioButton, isSearching && styles.disabled]}
                onPress={() => !isSearching && setSelectedConnectionType(type)}
                disabled={isSearching}
              >
                <View style={styles.radioCircle}>
                  {selectedConnectionType === type && <View style={styles.selectedRb} />}
                </View>
                <Text style={[styles.radioLabel, isSearching && styles.disabledText]}>{type}</Text>
              </TouchableOpacity>
            ))}
          </View>

          <Text style={styles.subHeader}>Manufacturer</Text>
          <View style={styles.radioGroup}>
            {(["EPSON", "STAR", "RONGTA"] as Manufacturer[]).map((manufacturer) => (
              <TouchableOpacity
                key={manufacturer}
                style={[styles.radioButton, isSearching && styles.disabled]}
                onPress={() => !isSearching && setSelectedManufacturer(manufacturer)}
                disabled={isSearching}
              >
                <View style={styles.radioCircle}>
                  {selectedManufacturer === manufacturer && <View style={styles.selectedRb} />}
                </View>
                <Text style={[styles.radioLabel, isSearching && styles.disabledText]}>{manufacturer}</Text>
              </TouchableOpacity>
            ))}
          </View>

          <Button
            title={isSearching ? "Searching..." : isRequestingPermissions ? "Requesting Permissions..." : "Search Printers"}
            onPress={() => {
              console.log("Button clicked, isSearching:", isSearching, "isRequestingPermissions:", isRequestingPermissions);
              if (isSearching) {
                console.log("Calling handleStopSearch");
                handleStopSearch();
              } else {
                console.log("Calling handleSearch");
                handleSearch();
              }
            }}
            disabled={isSearching || isRequestingPermissions}
            color={isSearching || isRequestingPermissions ? "#FFA500" : "#007AFF"}
          />

          <Text style={styles.groupHeader}>Found Printers</Text>

          {isSearching ? (
            <View style={styles.loadingContainer}>
              <ActivityIndicator size="large" color="#000" />
              <Text style={styles.loadingText}>Searching for printers...</Text>
            </View>
          ) : printers.length === 0 ? (
            <Text>No printers found yet...</Text>
          ) : (
            printers.map((printer, index) => (
              <View key={index} style={styles.printerItem}>
                {renderPrinterInfo(printer)}
              </View>
            ))
          )}
        </View>
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = {
  header: {
    fontSize: 30,
    margin: 20,
  },
  subHeader: {
    fontSize: 16,
    marginBottom: 10,
  },
  groupHeader: {
    fontSize: 20,
    marginTop: 20,
    marginBottom: 20,
  },
  group: {
    margin: 20,
    backgroundColor: "#fff",
    borderRadius: 10,
    padding: 20,
  },
  container: {
    flex: 1,
    backgroundColor: "#eee",
  },
  printerItem: {
    marginBottom: 15,
    paddingBottom: 15,
    borderBottomWidth: 1,
    borderBottomColor: "#eee",
  },
  radioGroup: {
    flexDirection: "row" as const,
    flexWrap: "wrap" as const,
    marginBottom: 20,
  },
  radioButton: {
    flexDirection: "row" as const,
    alignItems: "center" as const,
    marginRight: 20,
    marginBottom: 10,
  },
  radioCircle: {
    height: 20,
    width: 20,
    borderRadius: 10,
    borderWidth: 2,
    borderColor: "#000",
    alignItems: "center" as const,
    justifyContent: "center" as const,
    marginRight: 8,
  },
  selectedRb: {
    width: 12,
    height: 12,
    borderRadius: 6,
    backgroundColor: "#000",
  },
  radioLabel: {
    fontSize: 16,
  },
  loadingContainer: {
    alignItems: "center" as const,
    justifyContent: "center" as const,
    padding: 20,
  },
  loadingText: {
    marginTop: 10,
    fontSize: 16,
    color: "#666",
  },
  disabled: {
    opacity: 0.5,
  },
  disabledText: {
    color: "#999",
  },
};

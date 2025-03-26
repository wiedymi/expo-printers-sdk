import { useEffect, useState } from "react";
import { EpsonPrinters } from "expo-printers-sdk";
import {
  Button,
  SafeAreaView,
  ScrollView,
  Text,
  View,
  Alert,
  Image,
  TextInput,
  Platform,
  PermissionsAndroid,
} from "react-native";
import type { EpsonPrinterInfo } from "expo-printers-sdk/EpsonPrinters.types";

const testImageUrl = "https://placehold.co/250x150.png";

export default function App() {
  const [printers, setPrinters] = useState<EpsonPrinterInfo[]>([]);
  const [isDiscovering, setIsDiscovering] = useState(false);
  const [selectedPrinter, setSelectedPrinter] =
    useState<EpsonPrinterInfo | null>(null);
  const [isConnected, setIsConnected] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [manualIpAddress, setManualIpAddress] = useState("");

  useEffect(() => {
    EpsonPrinters.addListener(
      "onDiscovery",
      (printerInfo: EpsonPrinterInfo) => {
        setPrinters((prev) => [...prev, printerInfo]);
      }
    );

    // @ts-ignore
    EpsonPrinters.addListener("onPrintError", ({ error, code }) => {
      console.error("onPrintError", { error, code });
    });

    EpsonPrinters.addListener("onPrintSuccess", () => {
      console.log("onPrintSuccess");
    });

    return () => {
      EpsonPrinters.stopDiscovery();
      if (isConnected) {
        EpsonPrinters.disconnectPrinter();
      }
    };
  }, [isConnected]);

  const requestPermissions = async () => {
    if (Platform.OS === "android") {
      try {
        const permissions = [
          PermissionsAndroid.PERMISSIONS.ACCESS_FINE_LOCATION,
          PermissionsAndroid.PERMISSIONS.ACCESS_COARSE_LOCATION,
          PermissionsAndroid.PERMISSIONS.BLUETOOTH_SCAN,
          PermissionsAndroid.PERMISSIONS.BLUETOOTH_CONNECT,
          PermissionsAndroid.PERMISSIONS.BLUETOOTH_ADVERTISE,
          PermissionsAndroid.PERMISSIONS.ACCESS_NETWORK_STATE,
          PermissionsAndroid.PERMISSIONS.INTERNET,
        ].filter(Boolean); // Filter out any null permissions

        console.log("permissions", permissions);

        const results = [];
        for (const permission of permissions) {
          const result = await PermissionsAndroid.request(permission);
          results.push(result);
        }

        console.log("results", results);

        const allGranted = results.every((result) => result);

        if (!allGranted) {
          Alert.alert(
            "Permissions Required",
            "Please grant all permissions to use printer features"
          );
        }

        return true;
      } catch (err) {
        return false;
      }
    }
  };

  const handleStartDiscovery = async () => {
    try {
      await requestPermissions();
      setPrinters([]);
      setIsDiscovering(true);
      // Start network printer discovery (type 1)
      await EpsonPrinters.startDiscovery(1);
    } catch (error) {
      console.error("Failed to start discovery:", error);
    }
  };

  const handleStopDiscovery = async () => {
    try {
      setIsDiscovering(false);
      await EpsonPrinters.stopDiscovery();
    } catch (error) {
      console.error("Failed to stop discovery:", error);
    }
  };

  const handleConnectPrinter = async (printer: EpsonPrinterInfo) => {
    try {
      setIsLoading(true);
      const result = await EpsonPrinters.connectPrinter(printer.target);
      setIsLoading(false);
      if (result) {
        setIsConnected(true);
        setSelectedPrinter(printer);
        Alert.alert("Connected", `Connected to ${printer.deviceName}`);
      }
    } catch (error) {
      setIsLoading(false);
      console.error("Failed to connect to printer:", error);
      Alert.alert("Connection Error", "Failed to connect to printer");
    }
  };

  const handleManualConnect = async () => {
    if (!manualIpAddress.trim()) {
      Alert.alert("Error", "Please enter an IP address");
      return;
    }

    try {
      setIsLoading(true);
      // Create a manual printer info object
      const manualPrinter: EpsonPrinterInfo = {
        deviceName: `Printer at ${manualIpAddress}`,
        target: `TCP:${manualIpAddress}`,
        ip: manualIpAddress,
        mac: "",
        bdAddress: "",
      };

      const result = await EpsonPrinters.connectPrinter(manualPrinter.target);
      setIsLoading(false);

      if (result) {
        setIsConnected(true);
        setSelectedPrinter(manualPrinter);
        Alert.alert("Connected", `Connected to printer at ${manualIpAddress}`);
        setManualIpAddress("");
      }
    } catch (error) {
      setIsLoading(false);
      console.error("Failed to connect to printer:", error);
      Alert.alert("Connection Error", "Failed to connect to printer");
    }
  };

  const handleDisconnectPrinter = async () => {
    try {
      setIsLoading(true);
      const result = await EpsonPrinters.disconnectPrinter();
      setIsLoading(false);
      if (result) {
        setIsConnected(false);
        setSelectedPrinter(null);
        Alert.alert("Disconnected", "Printer disconnected successfully");
      }
    } catch (error) {
      setIsLoading(false);
      console.error("Failed to disconnect printer:", error);
      Alert.alert("Disconnection Error", "Failed to disconnect printer");
    }
  };

  const handlePrintTestText = async () => {
    if (!isConnected) {
      Alert.alert("Error", "No printer connected");
      return;
    }

    try {
      setIsLoading(true);
      await EpsonPrinters.printText(
        "Test Print from Expo Printers\n\nHello World!",
        {
          alignment: EpsonPrinters.ALIGN_CENTER,
          font: EpsonPrinters.FONT_A,
          lang: EpsonPrinters.LANG_EN,
        }
      );
    } catch (error) {
      setIsLoading(false);
      console.error("Failed to print test text:", error);
    }
  };

  const handlePrintTestImage = async () => {
    if (!isConnected) {
      Alert.alert("Error", "No printer connected");
      return;
    }

    try {
      setIsLoading(true);
      // Convert image to base64
      const response = await fetch(testImageUrl);
      const blob = await response.blob();
      const base64 = await new Promise<string>((resolve, reject) => {
        const reader = new FileReader();
        reader.onload = () => {
          if (typeof reader.result === "string") {
            // Remove data:image/jpeg;base64, prefix
            const base64Data = reader.result.split(",")[1];
            resolve(base64Data);
          } else {
            reject(new Error("Failed to convert image to base64"));
          }
        };
        reader.onerror = reject;
        reader.readAsDataURL(blob);
      });

      await EpsonPrinters.printImage(base64, {
        alignment: EpsonPrinters.ALIGN_CENTER,
      });
    } catch (error) {
      setIsLoading(false);
      console.error("Failed to print test image:", error);
      Alert.alert("Error", "Failed to print test image");
    }
  };

  return (
    <SafeAreaView style={styles.container}>
      <ScrollView style={styles.container}>
        <Text style={styles.header}>Printer Discovery</Text>

        <View style={styles.group}>
          <View style={styles.manualConnectContainer}>
            <Text style={styles.subHeader}>Manual Connection</Text>
            <TextInput
              style={styles.input}
              placeholder="Enter printer IP address"
              value={manualIpAddress}
              onChangeText={setManualIpAddress}
              keyboardType="numeric"
            />
            <Button
              title="Connect Manually"
              onPress={handleManualConnect}
              disabled={isLoading || isConnected || !manualIpAddress.trim()}
            />
          </View>

          <View style={styles.divider} />

          <Button
            title={isDiscovering ? "Stop Discovery" : "Start Discovery"}
            onPress={isDiscovering ? handleStopDiscovery : handleStartDiscovery}
            disabled={isLoading}
          />

          <Text style={styles.groupHeader}>Found Printers</Text>

          {printers.length === 0 ? (
            <Text>No printers found yet...</Text>
          ) : (
            printers.map((printer) => (
              <View
                key={printer.mac || printer.target}
                style={styles.printerItem}
              >
                <View>
                  <Text>Device: {printer.deviceName}</Text>
                  <Text>IP: {printer.ip}</Text>
                  <Text>MAC: {printer.mac}</Text>
                </View>
                <Button
                  title={
                    selectedPrinter?.target === printer.target
                      ? "Disconnect"
                      : "Connect"
                  }
                  onPress={
                    selectedPrinter?.target === printer.target
                      ? handleDisconnectPrinter
                      : () => handleConnectPrinter(printer)
                  }
                  disabled={isLoading}
                />
              </View>
            ))
          )}
        </View>

        {isConnected && selectedPrinter && (
          <View style={styles.group}>
            <Text style={styles.groupHeader}>Connected Printer</Text>
            <Text>Device: {selectedPrinter.deviceName}</Text>
            <Text>IP: {selectedPrinter.ip}</Text>

            <View style={styles.testImageContainer}>
              <Text style={styles.subHeader}>Test Image:</Text>
            </View>

            <View style={styles.buttonContainer}>
              <Button
                title="Print Test Text"
                onPress={handlePrintTestText}
                disabled={isLoading}
              />
              <Button
                title="Print Test Image"
                onPress={handlePrintTestImage}
                disabled={isLoading}
              />
              <Button
                title="Disconnect"
                onPress={handleDisconnectPrinter}
                disabled={isLoading}
              />
            </View>

            {isLoading && <Text style={styles.loadingText}>Processing...</Text>}
          </View>
        )}
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
    flexDirection: "row" as const,
    justifyContent: "space-between" as const,
    alignItems: "center" as const,
    marginBottom: 15,
    paddingBottom: 15,
    borderBottomWidth: 1,
    borderBottomColor: "#eee",
  },
  buttonContainer: {
    marginTop: 15,
    gap: 10,
  },
  testImageContainer: {
    marginTop: 20,
    marginBottom: 20,
  },
  testImage: {
    width: "100%",
    height: 200,
    backgroundColor: "#f0f0f0",
    borderRadius: 5,
  },
  loadingText: {
    marginTop: 10,
    textAlign: "center" as const,
    color: "#666",
  },
  input: {
    borderWidth: 1,
    borderColor: "#ccc",
    borderRadius: 5,
    padding: 10,
    marginBottom: 10,
  },
  manualConnectContainer: {
    marginBottom: 20,
  },
  divider: {
    height: 1,
    backgroundColor: "#eee",
    marginVertical: 20,
  },
};

import React, { useState } from "react";
import {
  View,
  Text,
  TextInput,
  TouchableOpacity,
  Alert,
  StyleSheet,
  Platform,
} from "react-native";

const MONO_FONT = Platform.select({
  ios: "Menlo",
  android: "monospace",
  default: "monospace",
});

type ManualConnectionProps = {
  onConnect: (ipAddress: string, port: number) => Promise<void>;
};

const ManualConnection: React.FC<ManualConnectionProps> = ({ onConnect }) => {
  const [ipAddress, setIpAddress] = useState("");
  const [port, setPort] = useState("9100");

  const handleConnect = async () => {
    if (!ipAddress.trim()) {
      Alert.alert("Error", "Please enter an IP address");
      return;
    }

    const portNum = parseInt(port, 10);
    if (isNaN(portNum) || portNum < 1 || portNum > 65535) {
      Alert.alert("Error", "Please enter a valid port number (1-65535)");
      return;
    }

    try {
      await onConnect(ipAddress, portNum);
      setIpAddress("");
      setPort("9100");
    } catch (error) {
      console.error("Manual connection failed:", error);
      Alert.alert("Error", "Failed to connect to printer");
    }
  };

  return (
    <View style={styles.container}>
      <Text style={styles.title}>Manual Network Connection</Text>
      <View style={styles.inputRow}>
        <TextInput
          style={[styles.input, styles.ipInput]}
          placeholder="IP Address (e.g., 192.168.1.100)"
          value={ipAddress}
          onChangeText={setIpAddress}
          keyboardType="numeric"
          autoCapitalize="none"
          autoCorrect={false}
        />
        <TextInput
          style={[styles.input, styles.portInput]}
          placeholder="Port"
          value={port}
          onChangeText={setPort}
          keyboardType="numeric"
        />
      </View>
      <TouchableOpacity
        style={styles.connectButton}
        onPress={handleConnect}
        accessibilityLabel="Connect to printer manually"
        accessible={true}
      >
        <Text style={styles.connectButtonText}>→ Connect</Text>
      </TouchableOpacity>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    marginTop: 16,
    padding: 16,
    backgroundColor: "#f9f9f9",
    borderWidth: 1,
    borderColor: "#d1d1d1",
    borderRadius: 0,
  },
  title: {
    fontSize: 16,
    fontWeight: "600",
    marginBottom: 12,
    color: "#222",
    fontFamily: MONO_FONT,
  },
  inputRow: {
    flexDirection: "row",
    gap: 8,
    marginBottom: 12,
  },
  input: {
    backgroundColor: "#fff",
    borderWidth: 1,
    borderColor: "#d1d1d1",
    padding: 12,
    fontSize: 14,
    fontFamily: MONO_FONT,
    borderRadius: 0,
  },
  ipInput: {
    flex: 2,
  },
  portInput: {
    flex: 1,
  },
  connectButton: {
    backgroundColor: "#444",
    padding: 12,
    alignItems: "center",
    borderRadius: 0,
  },
  connectButtonText: {
    color: "#fff",
    fontSize: 14,
    fontWeight: "600",
    fontFamily: MONO_FONT,
  },
});

export default ManualConnection;

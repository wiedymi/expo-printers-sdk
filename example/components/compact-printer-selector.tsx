import React, { useState } from "react";
import {
  View,
  Text,
  TextInput,
  TouchableOpacity,
  ActivityIndicator,
  StyleSheet,
  Platform,
} from "react-native";
import type { PrinterConnectionType } from "expo-printers-sdk";
import type { Manufacturer } from "../types/printer";

const MONO_FONT = Platform.select({
  ios: "Menlo",
  android: "monospace",
  default: "monospace",
});

type CompactPrinterSelectorProps = {
  connectionType: PrinterConnectionType;
  manufacturer: Manufacturer;
  isSearching: boolean;
  isRequestingPermissions: boolean;
  onConnectionTypeChange: (type: PrinterConnectionType) => void;
  onManufacturerChange: (manufacturer: Manufacturer) => void;
  onSearch: () => void;
  onManualConnect: (ipAddress: string, port: number) => Promise<void>;
};

const CompactPrinterSelector: React.FC<CompactPrinterSelectorProps> = ({
  connectionType,
  manufacturer,
  isSearching,
  isRequestingPermissions,
  onConnectionTypeChange,
  onManufacturerChange,
  onSearch,
  onManualConnect,
}) => {
  const [mode, setMode] = useState<"auto" | "manual">("auto");
  const [ipAddress, setIpAddress] = useState("");
  const [port, setPort] = useState("9100");

  const handleManualConnect = async () => {
    if (!ipAddress.trim()) return;
    const portNum = parseInt(port, 10);
    if (isNaN(portNum)) return;

    await onManualConnect(ipAddress, portNum);
    setIpAddress("");
    setPort("9100");
  };

  return (
    <View style={styles.container}>
      {/* Manufacturer Selector */}
      <View style={styles.row}>
        {(["EPSON", "RONGTA", "STAR"] as Manufacturer[]).map((mfr) => (
          <TouchableOpacity
            key={mfr}
            style={[styles.chip, manufacturer === mfr && styles.chipActive]}
            onPress={() => onManufacturerChange(mfr)}
          >
            <Text
              style={[
                styles.chipText,
                manufacturer === mfr && styles.chipTextActive,
              ]}
            >
              {mfr}
            </Text>
          </TouchableOpacity>
        ))}
      </View>

      {/* Connection Type Selector */}
      <View style={styles.row}>
        {(["Network", "Bluetooth", "USB"] as PrinterConnectionType[]).map(
          (type) => (
            <TouchableOpacity
              key={type}
              style={[
                styles.chip,
                connectionType === type && styles.chipActive,
              ]}
              onPress={() => onConnectionTypeChange(type)}
            >
              <Text
                style={[
                  styles.chipText,
                  connectionType === type && styles.chipTextActive,
                ]}
              >
                {type}
              </Text>
            </TouchableOpacity>
          )
        )}
      </View>

      {/* Mode Toggle for Network */}
      {connectionType === "Network" && (
        <View style={styles.modeToggle}>
          <TouchableOpacity
            style={[styles.modeBtn, mode === "auto" && styles.modeBtnActive]}
            onPress={() => setMode("auto")}
          >
            <Text
              style={[
                styles.modeBtnText,
                mode === "auto" && styles.modeBtnTextActive,
              ]}
            >
              Auto Scan
            </Text>
          </TouchableOpacity>
          <TouchableOpacity
            style={[styles.modeBtn, mode === "manual" && styles.modeBtnActive]}
            onPress={() => setMode("manual")}
          >
            <Text
              style={[
                styles.modeBtnText,
                mode === "manual" && styles.modeBtnTextActive,
              ]}
            >
              Manual IP
            </Text>
          </TouchableOpacity>
        </View>
      )}

      {/* Auto Scan Mode */}
      {(connectionType !== "Network" || mode === "auto") && (
        <TouchableOpacity
          style={[
            styles.actionBtn,
            (isSearching || isRequestingPermissions) && styles.actionBtnDisabled,
          ]}
          onPress={onSearch}
          disabled={isSearching || isRequestingPermissions}
        >
          {isSearching ? (
            <View style={styles.loadingRow}>
              <ActivityIndicator color="#fff" size="small" />
              <Text style={styles.actionBtnText}>Searching...</Text>
            </View>
          ) : (
            <Text style={styles.actionBtnText}>🔍 Search</Text>
          )}
        </TouchableOpacity>
      )}

      {/* Manual Connection Mode */}
      {connectionType === "Network" && mode === "manual" && (
        <View style={styles.manualForm}>
          <View style={styles.inputRow}>
            <TextInput
              style={[styles.input, styles.ipInput]}
              placeholder="IP (e.g. 192.168.1.100)"
              placeholderTextColor="#999"
              value={ipAddress}
              onChangeText={setIpAddress}
              keyboardType="numeric"
              autoCapitalize="none"
              autoCorrect={false}
            />
            <TextInput
              style={[styles.input, styles.portInput]}
              placeholder="Port"
              placeholderTextColor="#999"
              value={port}
              onChangeText={setPort}
              keyboardType="numeric"
            />
          </View>
          <TouchableOpacity
            style={styles.actionBtn}
            onPress={handleManualConnect}
          >
            <Text style={styles.actionBtnText}>→ Connect</Text>
          </TouchableOpacity>
        </View>
      )}
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    backgroundColor: "#f5f5f5",
    padding: 12,
    borderRadius: 8,
    gap: 8,
  },
  row: {
    flexDirection: "row",
    gap: 6,
  },
  chip: {
    flex: 1,
    paddingVertical: 8,
    paddingHorizontal: 12,
    backgroundColor: "#fff",
    borderRadius: 6,
    borderWidth: 1,
    borderColor: "#ddd",
    alignItems: "center",
  },
  chipActive: {
    backgroundColor: "#222",
    borderColor: "#222",
  },
  chipText: {
    fontSize: 13,
    fontWeight: "600",
    color: "#666",
    fontFamily: MONO_FONT,
  },
  chipTextActive: {
    color: "#fff",
  },
  modeToggle: {
    flexDirection: "row",
    backgroundColor: "#e0e0e0",
    borderRadius: 6,
    padding: 2,
    gap: 2,
  },
  modeBtn: {
    flex: 1,
    paddingVertical: 6,
    alignItems: "center",
    borderRadius: 4,
  },
  modeBtnActive: {
    backgroundColor: "#fff",
  },
  modeBtnText: {
    fontSize: 12,
    fontWeight: "600",
    color: "#666",
    fontFamily: MONO_FONT,
  },
  modeBtnTextActive: {
    color: "#222",
  },
  actionBtn: {
    backgroundColor: "#222",
    paddingVertical: 12,
    borderRadius: 6,
    alignItems: "center",
  },
  actionBtnDisabled: {
    backgroundColor: "#999",
  },
  actionBtnText: {
    color: "#fff",
    fontSize: 14,
    fontWeight: "600",
    fontFamily: MONO_FONT,
  },
  loadingRow: {
    flexDirection: "row",
    alignItems: "center",
    gap: 8,
  },
  manualForm: {
    gap: 8,
  },
  inputRow: {
    flexDirection: "row",
    gap: 6,
  },
  input: {
    backgroundColor: "#fff",
    borderWidth: 1,
    borderColor: "#ddd",
    borderRadius: 6,
    paddingHorizontal: 12,
    paddingVertical: 10,
    fontSize: 13,
    fontFamily: MONO_FONT,
  },
  ipInput: {
    flex: 2,
  },
  portInput: {
    flex: 1,
  },
});

export default CompactPrinterSelector;

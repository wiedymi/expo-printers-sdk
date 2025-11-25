import React, { useState } from "react";
import {
  View,
  Text,
  TextInput,
  TouchableOpacity,
  ActivityIndicator,
  StyleSheet,
  Platform,
  Modal,
  FlatList,
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
  supportedModels: string[];
  onConnectionTypeChange: (type: PrinterConnectionType) => void;
  onManufacturerChange: (manufacturer: Manufacturer) => void;
  onSearch: () => void;
  onRefreshModels: (force?: boolean) => Promise<void>;
  onManualConnect: (
    ipAddress: string,
    modelName: string,
    port: number
  ) => Promise<void>;
};

const CompactPrinterSelector: React.FC<CompactPrinterSelectorProps> = ({
  connectionType,
  manufacturer,
  isSearching,
  isRequestingPermissions,
  supportedModels,
  onConnectionTypeChange,
  onManufacturerChange,
  onSearch,
  onRefreshModels,
  onManualConnect,
}) => {
  const [mode, setMode] = useState<"auto" | "manual">("auto");
  const [ipAddress, setIpAddress] = useState("");
  const [modelName, setModelName] = useState("");
  const [port, setPort] = useState("9100");
  const [modelPickerVisible, setModelPickerVisible] = useState(false);
  const [modelSearch, setModelSearch] = useState("");

  const handleManualConnect = async () => {
    if (!ipAddress.trim()) return;
    if ((manufacturer === "EPSON" || manufacturer === "STAR") && !modelName.trim()) {
      return;
    }
    const portNum = parseInt(port, 10);
    if (isNaN(portNum)) return;

    await onManualConnect(ipAddress, modelName.trim(), portNum);
    setIpAddress("");
    setModelName("");
    setPort("9100");
  };

  const filteredModels = supportedModels.filter((m) =>
    m.toLowerCase().includes(modelSearch.toLowerCase())
  );

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
          {(manufacturer === "EPSON" || manufacturer === "STAR") && (
            <TextInput
              style={styles.input}
              placeholder={
                manufacturer === "EPSON"
                  ? "Model (e.g. TM-m30II)"
                  : "Model (e.g. mC-Print3)"
              }
              placeholderTextColor="#999"
              value={modelName}
              onChangeText={setModelName}
              autoCapitalize="none"
              autoCorrect={false}
            />
          )}
          {(manufacturer === "EPSON" || manufacturer === "STAR") && (
            <View style={styles.modelsRow}>
              <TouchableOpacity
                onPress={() => onRefreshModels(true)}
                style={styles.refreshModelsBtn}
              >
                <Text style={styles.refreshModelsText}>↻ models</Text>
              </TouchableOpacity>
              <TouchableOpacity
                onPress={() => setModelPickerVisible(true)}
                style={styles.refreshModelsBtn}
              >
                <Text style={styles.refreshModelsText}>Browse</Text>
              </TouchableOpacity>
              <View style={styles.modelChipsWrap}>
                {supportedModels.slice(0, 12).map((model) => (
                  <TouchableOpacity
                    key={model}
                    style={[styles.modelChip, modelName === model && styles.modelChipActive]}
                    onPress={() => setModelName(model)}
                  >
                    <Text
                      style={[
                        styles.modelChipText,
                        modelName === model && styles.modelChipTextActive,
                      ]}
                    >
                      {model}
                    </Text>
                  </TouchableOpacity>
                ))}
              </View>
            </View>
          )}
          <TouchableOpacity
            style={styles.actionBtn}
            onPress={handleManualConnect}
          >
            <Text style={styles.actionBtnText}>→ Connect</Text>
          </TouchableOpacity>
        </View>
      )}

      <Modal
        visible={modelPickerVisible}
        transparent
        animationType="slide"
        onRequestClose={() => setModelPickerVisible(false)}
      >
        <View style={styles.modalOverlay}>
          <View style={styles.modalCard}>
            <Text style={styles.modalTitle}>Select Model</Text>
            <TextInput
              style={styles.modalSearch}
              placeholder="Search models"
              placeholderTextColor="#999"
              value={modelSearch}
              onChangeText={setModelSearch}
              autoCapitalize="none"
              autoCorrect={false}
            />
            <FlatList
              data={filteredModels}
              keyExtractor={(item) => item}
              style={styles.modalList}
              keyboardShouldPersistTaps="handled"
              renderItem={({ item }) => (
                <TouchableOpacity
                  style={styles.modalItem}
                  onPress={() => {
                    setModelName(item);
                    setModelPickerVisible(false);
                  }}
                >
                  <Text
                    style={[
                      styles.modalItemText,
                      item === modelName && styles.modalItemTextActive,
                    ]}
                  >
                    {item}
                  </Text>
                </TouchableOpacity>
              )}
              ListEmptyComponent={
                <Text style={styles.modalEmpty}>No models found</Text>
              }
            />
            <TouchableOpacity
              style={styles.modalCloseBtn}
              onPress={() => setModelPickerVisible(false)}
            >
              <Text style={styles.modalCloseText}>Close</Text>
            </TouchableOpacity>
          </View>
        </View>
      </Modal>
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
  modelsRow: {
    gap: 6,
  },
  refreshModelsBtn: {
    alignSelf: "flex-start",
    paddingVertical: 4,
    paddingHorizontal: 8,
    borderRadius: 4,
    borderWidth: 1,
    borderColor: "#ccc",
    backgroundColor: "#fff",
  },
  refreshModelsText: {
    fontSize: 12,
    color: "#333",
    fontFamily: MONO_FONT,
  },
  modelChipsWrap: {
    flexDirection: "row",
    flexWrap: "wrap",
    gap: 6,
  },
  modelChip: {
    paddingVertical: 6,
    paddingHorizontal: 10,
    borderRadius: 6,
    backgroundColor: "#eee",
  },
  modelChipActive: {
    backgroundColor: "#222",
  },
  modelChipText: {
    fontSize: 12,
    color: "#333",
    fontFamily: MONO_FONT,
  },
  modelChipTextActive: {
    color: "#fff",
  },
  modalOverlay: {
    flex: 1,
    backgroundColor: "rgba(0,0,0,0.35)",
    justifyContent: "center",
    padding: 16,
  },
  modalCard: {
    backgroundColor: "#fff",
    borderRadius: 10,
    padding: 16,
    maxHeight: "80%",
  },
  modalTitle: {
    fontSize: 16,
    fontWeight: "700",
    color: "#111",
    marginBottom: 8,
    fontFamily: MONO_FONT,
  },
  modalSearch: {
    borderWidth: 1,
    borderColor: "#ddd",
    borderRadius: 6,
    paddingHorizontal: 12,
    paddingVertical: 10,
    fontSize: 13,
    marginBottom: 10,
    fontFamily: MONO_FONT,
  },
  modalList: {
    maxHeight: 300,
  },
  modalItem: {
    paddingVertical: 10,
  },
  modalItemText: {
    fontSize: 14,
    color: "#222",
    fontFamily: MONO_FONT,
  },
  modalItemTextActive: {
    fontWeight: "700",
    color: "#000",
  },
  modalEmpty: {
    textAlign: "center",
    color: "#777",
    fontSize: 13,
    paddingVertical: 12,
    fontFamily: MONO_FONT,
  },
  modalCloseBtn: {
    marginTop: 12,
    alignSelf: "flex-end",
    paddingHorizontal: 12,
    paddingVertical: 8,
    borderRadius: 6,
    backgroundColor: "#222",
  },
  modalCloseText: {
    color: "#fff",
    fontWeight: "600",
    fontSize: 13,
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

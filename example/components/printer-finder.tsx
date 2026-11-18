import { useEffect, useState } from "react";
import {
  ActivityIndicator,
  Pressable,
  StyleSheet,
  Text,
  TextInput,
  View,
} from "react-native";
import type { PrinterConnectionType } from "expo-printers-sdk";
import type { Vendor } from "../types/printer";
import {
  CONNECTION_TYPES,
  VENDOR_LABELS,
  VENDORS,
  getSupportedModels,
  needsModelName,
} from "../utils/printers";
import { colors } from "../theme";
import Segmented from "./segmented";
import ModelPicker from "./model-picker";
import { Label } from "./section";

type PrinterFinderProps = {
  vendor: Vendor;
  onVendorChange: (vendor: Vendor) => void;
  searching: boolean;
  onSearch: (connectionType: PrinterConnectionType) => void;
  onAdd: (ipAddress: string, port: number, modelName: string) => void;
};

// Finds printers of one vendor, or adds a network printer by its IP address. A printer added by IP address is
// not checked, so it can also be a printer that is not on the network, for example to test Preview.
export default function PrinterFinder({
  vendor,
  onVendorChange,
  searching,
  onSearch,
  onAdd,
}: PrinterFinderProps) {
  const [connectionType, setConnectionType] =
    useState<PrinterConnectionType>("Network");
  const [ipAddress, setIpAddress] = useState("");
  const [port, setPort] = useState("9100");
  const [modelName, setModelName] = useState("");
  const [models, setModels] = useState<string[]>([]);
  const [pickerVisible, setPickerVisible] = useState(false);

  useEffect(() => {
    setModelName("");
    getSupportedModels(vendor)
      .then(setModels)
      .catch(() => setModels([]));
  }, [vendor]);

  const portNumber = Number(port);
  const canAdd =
    ipAddress.trim().length > 0 &&
    Number.isInteger(portNumber) &&
    (!needsModelName(vendor) || modelName.length > 0);

  return (
    <View style={styles.container}>
      <Segmented
        options={VENDORS}
        value={vendor}
        onChange={onVendorChange}
        labels={VENDOR_LABELS}
      />

      <Label>Search</Label>
      <Segmented
        options={CONNECTION_TYPES}
        value={connectionType}
        onChange={setConnectionType}
      />
      <Pressable
        style={[styles.button, searching && styles.buttonDisabled]}
        onPress={() => onSearch(connectionType)}
        disabled={searching}
      >
        {searching ? (
          <ActivityIndicator color={colors.onPrimary} size="small" />
        ) : (
          <Text style={styles.buttonText}>
            Search {VENDOR_LABELS[vendor]} {connectionType}
          </Text>
        )}
      </Pressable>

      <Label>Or add a network printer</Label>
      <View style={styles.row}>
        <TextInput
          style={[styles.input, styles.grow]}
          placeholder="IP address, e.g. 192.168.1.50"
          placeholderTextColor={colors.muted}
          value={ipAddress}
          onChangeText={setIpAddress}
          keyboardType="decimal-pad"
          autoCorrect={false}
        />
        <TextInput
          style={[styles.input, styles.port]}
          placeholder="Port"
          placeholderTextColor={colors.muted}
          value={port}
          onChangeText={setPort}
          keyboardType="number-pad"
        />
      </View>
      <View style={styles.row}>
        {needsModelName(vendor) && (
          <Pressable
            style={[styles.input, styles.grow]}
            onPress={() => setPickerVisible(true)}
          >
            <Text style={modelName ? styles.value : styles.placeholder}>
              {modelName || "Select the model"}
            </Text>
          </Pressable>
        )}
        <Pressable
          style={[
            styles.button,
            styles.addButton,
            !canAdd && styles.buttonDisabled,
          ]}
          onPress={() => onAdd(ipAddress.trim(), portNumber, modelName)}
          disabled={!canAdd}
        >
          <Text style={styles.buttonText}>Add</Text>
        </Pressable>
      </View>

      <ModelPicker
        key={vendor}
        visible={pickerVisible}
        models={models}
        selected={modelName}
        onSelect={setModelName}
        onClose={() => setPickerVisible(false)}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    gap: 10,
  },
  row: {
    flexDirection: "row",
    gap: 8,
  },
  grow: {
    flex: 1,
  },
  input: {
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: 8,
    paddingHorizontal: 12,
    paddingVertical: 10,
    color: colors.text,
    fontSize: 14,
    justifyContent: "center",
  },
  port: {
    width: 80,
  },
  value: {
    color: colors.text,
    fontSize: 14,
  },
  placeholder: {
    color: colors.muted,
    fontSize: 14,
  },
  button: {
    backgroundColor: colors.primary,
    borderRadius: 8,
    paddingVertical: 12,
    alignItems: "center",
    justifyContent: "center",
  },
  addButton: {
    paddingHorizontal: 22,
  },
  buttonDisabled: {
    opacity: 0.4,
  },
  buttonText: {
    color: colors.onPrimary,
    fontSize: 14,
    fontWeight: "600",
  },
});

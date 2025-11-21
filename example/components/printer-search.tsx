import React from "react";
import {
  View,
  Text,
  TouchableOpacity,
  ActivityIndicator,
  StyleSheet,
  Platform,
} from "react-native";
import type { PrinterConnectionType } from "expo-printers-sdk";
import type { Manufacturer } from "../types/printer";
import ButtonRow from "./button-row";

const MONO_FONT = Platform.select({
  ios: "Menlo",
  android: "monospace",
  default: "monospace",
});

type PrinterSearchProps = {
  connectionType: PrinterConnectionType;
  manufacturer: Manufacturer;
  isSearching: boolean;
  isRequestingPermissions: boolean;
  onConnectionTypeChange: (type: PrinterConnectionType) => void;
  onManufacturerChange: (manufacturer: Manufacturer) => void;
  onSearch: () => void;
};

const PrinterSearch: React.FC<PrinterSearchProps> = ({
  connectionType,
  manufacturer,
  isSearching,
  isRequestingPermissions,
  onConnectionTypeChange,
  onManufacturerChange,
  onSearch,
}) => {
  return (
    <View>
      <Text style={styles.sectionTitle}>Connection Type</Text>
      <ButtonRow
        options={["Network", "Bluetooth", "USB"]}
        selected={connectionType}
        onSelect={onConnectionTypeChange}
        buttonStyle={styles.connectionButton}
        selectedButtonStyle={styles.selectedButton}
        buttonTextStyle={styles.buttonText}
        selectedButtonTextStyle={styles.selectedButtonText}
      />

      <Text style={styles.sectionTitle}>Manufacturer</Text>
      <ButtonRow
        options={["EPSON", "RONGTA", "STAR"]}
        selected={manufacturer}
        onSelect={onManufacturerChange}
        buttonStyle={styles.manufacturerButton}
        selectedButtonStyle={styles.selectedButton}
        buttonTextStyle={styles.buttonText}
        selectedButtonTextStyle={styles.selectedButtonText}
        labelMap={{ STAR: "Star", EPSON: "EPSON", RONGTA: "RONGTA" }}
      />

      <TouchableOpacity
        style={[
          styles.searchButton,
          (isSearching || isRequestingPermissions) && styles.disabledButton,
        ]}
        onPress={onSearch}
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
    </View>
  );
};

const styles = StyleSheet.create({
  sectionTitle: {
    fontSize: 16,
    fontWeight: "600",
    marginBottom: 8,
    marginTop: 16,
    color: "#222",
    fontFamily: MONO_FONT,
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
});

export default PrinterSearch;

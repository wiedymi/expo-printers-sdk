import React from "react";
import { View, Text, ScrollView, StyleSheet, Platform } from "react-native";
import type { PrinterInfo } from "../types/printer";
import PrinterCard from "./printer-card";
import { getUniquePrinterId } from "../utils/printer-utils";

const MONO_FONT = Platform.select({
  ios: "Menlo",
  android: "monospace",
  default: "monospace",
});

type PrinterListProps = {
  printers: PrinterInfo[];
  isSearching: boolean;
  printingStates: { [key: string]: boolean };
  onPrint: (printer: PrinterInfo) => void;
};

const PrinterList: React.FC<PrinterListProps> = ({
  printers,
  isSearching,
  printingStates,
  onPrint,
}) => {
  if (!isSearching && printers.length === 0) {
    return (
      <View style={styles.emptyState}>
        <Text style={styles.emptyStateText}>No printers found yet...</Text>
        <Text style={styles.emptyStateSubtext}>
          Select connection type and manufacturer, then tap "Search Printers"
        </Text>
      </View>
    );
  }

  return (
    <ScrollView style={styles.container} showsVerticalScrollIndicator={false}>
      {printers.map((printer) => {
        const printerId = getUniquePrinterId(printer);
        return (
          <PrinterCard
            key={printerId}
            printer={printer}
            onPrint={onPrint}
            isPrinting={!!printingStates[printerId]}
            printerId={printerId}
          />
        );
      })}
    </ScrollView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    marginTop: 16,
  },
  emptyState: {
    padding: 40,
    alignItems: "center",
  },
  emptyStateText: {
    fontSize: 16,
    color: "#444",
    textAlign: "center",
    fontFamily: MONO_FONT,
  },
  emptyStateSubtext: {
    fontSize: 14,
    color: "#888",
    textAlign: "center",
    marginTop: 8,
    fontFamily: MONO_FONT,
  },
});

export default PrinterList;

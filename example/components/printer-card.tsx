import React from "react";
import {
  View,
  Text,
  TouchableOpacity,
  ActivityIndicator,
  StyleSheet,
} from "react-native";
import type { Manufacturer, PrinterInfo } from "../types/printer";

type PrinterCardProps = {
  printer: PrinterInfo;
  onPrint: (printer: PrinterInfo) => void;
  isPrinting: boolean;
  printerId: string;
};

const PrinterCard: React.FC<PrinterCardProps> = ({
  printer,
  onPrint,
  isPrinting,
  printerId,
}) => {
  const renderPrinterInfo = () => {
    switch (printer.type) {
      case "EPSON":
        const epsonInfo = printer.info as any;
        return (
          <View style={styles.printerDetails}>
            <Text style={styles.detailText}>
              Device: {epsonInfo.deviceName}
            </Text>
            <Text style={styles.detailText}>Target: {epsonInfo.target}</Text>
            <Text style={styles.detailText}>
              IP Address: {epsonInfo.ipAddress}
            </Text>
            <Text style={styles.detailText}>
              MAC Address: {epsonInfo.macAddress}
            </Text>
            <Text style={styles.detailText}>
              BD Address: {epsonInfo.bdAddress}
            </Text>
            <Text style={styles.detailText}>
              Connection: {epsonInfo.connectionType}
            </Text>
            <Text style={styles.detailText}>
              Device Type: {epsonInfo.deviceType}
            </Text>
          </View>
        );
      case "RONGTA":
        const rongtaInfo = printer.info as any;
        return (
          <View style={styles.printerDetails}>
            <Text style={styles.detailText}>
              Connection: {rongtaInfo.connectionType}
            </Text>
            <Text style={styles.detailText}>Type: {rongtaInfo.type.type}</Text>
            {rongtaInfo.type.type === "BLUETOOTH" ? (
              <>
                <Text style={styles.detailText}>
                  Alias: {rongtaInfo.type.alias}
                </Text>
                <Text style={styles.detailText}>
                  Name: {rongtaInfo.type.name}
                </Text>
                <Text style={styles.detailText}>
                  Address: {rongtaInfo.type.address}
                </Text>
              </>
            ) : rongtaInfo.type.type === "NETWORK" ? (
              <>
                <Text style={styles.detailText}>
                  IP Address: {rongtaInfo.type.ipAddress}
                </Text>
                <Text style={styles.detailText}>
                  Port: {rongtaInfo.type.port}
                </Text>
              </>
            ) : (
              <>
                <Text style={styles.detailText}>
                  Name: {rongtaInfo.type.name}
                </Text>
                <Text style={styles.detailText}>
                  Vendor ID: {rongtaInfo.type.vendorId}
                </Text>
                <Text style={styles.detailText}>
                  Product ID: {rongtaInfo.type.productId}
                </Text>
              </>
            )}
          </View>
        );
      case "STAR":
        const starInfo = printer.info as any;
        return (
          <View style={styles.printerDetails}>
            <Text style={styles.detailText}>Device: {starInfo.deviceName}</Text>
            <Text style={styles.detailText}>Port: {starInfo.portName}</Text>
            <Text style={styles.detailText}>
              MAC Address: {starInfo.macAddress}
            </Text>
            <Text style={styles.detailText}>
              USB Serial: {starInfo.usbSerialNumber}
            </Text>
            <Text style={styles.detailText}>
              Connection: {starInfo.connectionType}
            </Text>
          </View>
        );
    }
  };

  return (
    <View style={styles.printerCard}>
      <View style={styles.printerHeader}>
        <Text style={styles.printerType}>{printer.type}</Text>
        <TouchableOpacity
          style={[styles.printButton, isPrinting && styles.disabledButton]}
          onPress={() => onPrint(printer)}
          disabled={isPrinting}
          accessibilityLabel="Print test receipt"
          accessible={true}
        >
          {isPrinting ? (
            <ActivityIndicator color="#fff" size="small" />
          ) : (
            <Text style={styles.printButtonText}>Print Test</Text>
          )}
        </TouchableOpacity>
      </View>
      {renderPrinterInfo()}
    </View>
  );
};

const styles = StyleSheet.create({
  printerCard: {
    backgroundColor: "#f5f5f5",
    padding: 16,
    borderRadius: 0,
    marginBottom: 12,
    borderWidth: 1,
    borderColor: "#d1d1d1",
  },
  printerHeader: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
    marginBottom: 12,
  },
  printerType: {
    fontSize: 18,
    fontWeight: "700",
    color: "#111",
  },
  printButton: {
    backgroundColor: "#444",
    paddingHorizontal: 16,
    paddingVertical: 8,
    borderRadius: 0,
  },
  printButtonText: {
    color: "#fff",
    fontWeight: "600",
  },
  disabledButton: {
    backgroundColor: "#bbb",
  },
  printerDetails: {
    gap: 4,
  },
  detailText: {
    fontSize: 14,
    color: "#333",
    lineHeight: 20,
  },
});

export default PrinterCard;

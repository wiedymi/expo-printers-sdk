import { Pressable, StyleSheet, Text, View } from "react-native";
import type { Printer } from "../types/printer";
import { printerAddress, printerId, printerName } from "../utils/printers";
import { colors, MONO_FONT } from "../theme";

type PrinterPickerProps = {
  printers: Printer[];
  selectedId: string | null;
  onSelect: (id: string) => void;
};

// The found and added printers. The actions at the bottom of the screen use the selected printer.
export default function PrinterPicker({
  printers,
  selectedId,
  onSelect,
}: PrinterPickerProps) {
  if (printers.length === 0) {
    return (
      <Text style={styles.empty}>
        No printers yet. Search, or add a network printer.
      </Text>
    );
  }

  return (
    <View style={styles.list}>
      {printers.map((printer) => {
        const id = printerId(printer);
        const selected = id === selectedId;
        return (
          <Pressable
            key={id}
            style={[styles.row, selected && styles.rowSelected]}
            onPress={() => onSelect(id)}
            accessibilityRole="radio"
            accessibilityState={{ selected }}
          >
            <View style={[styles.radio, selected && styles.radioSelected]} />
            <View style={styles.text}>
              <Text style={styles.name}>{printerName(printer)}</Text>
              <Text style={styles.address}>{printerAddress(printer)}</Text>
            </View>
          </Pressable>
        );
      })}
    </View>
  );
}

const styles = StyleSheet.create({
  list: {
    gap: 6,
  },
  empty: {
    color: colors.muted,
    fontSize: 14,
  },
  row: {
    flexDirection: "row",
    alignItems: "center",
    gap: 12,
    padding: 10,
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: 8,
  },
  rowSelected: {
    borderColor: colors.primary,
    backgroundColor: colors.selected,
  },
  radio: {
    width: 18,
    height: 18,
    borderRadius: 9,
    borderWidth: 2,
    borderColor: colors.muted,
  },
  radioSelected: {
    borderColor: colors.primary,
    borderWidth: 6,
  },
  text: {
    flex: 1,
    gap: 2,
  },
  name: {
    fontSize: 15,
    fontWeight: "600",
    color: colors.text,
  },
  address: {
    fontSize: 12,
    color: colors.muted,
    fontFamily: MONO_FONT,
  },
});

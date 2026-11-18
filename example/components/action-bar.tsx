import { StyleSheet, Text, View } from "react-native";
import { colors } from "../theme";
import Button from "./button";

export type Action = "print" | "status" | "preview";

type ActionBarProps = {
  printerName: string | null;
  canPreview: boolean;
  // The action that loads its image or waits for its answer.
  running: Action | null;
  onAction: (action: Action) => void;
  // Height of the system navigation bar under the action bar.
  bottomInset: number;
};

// The actions for the selected printer, at the bottom of the screen.
export default function ActionBar({
  printerName,
  canPreview,
  running,
  onAction,
  bottomInset,
}: ActionBarProps) {
  const hasPrinter = printerName !== null;
  return (
    <View style={[styles.bar, { paddingBottom: bottomInset + 12 }]}>
      <Text style={styles.printer} numberOfLines={1}>
        {printerName ?? "Select a printer"}
        {hasPrinter && !canPreview ? "  ·  Preview is Epson only" : ""}
      </Text>
      <View style={styles.row}>
        <Button
          label="Print"
          onPress={() => onAction("print")}
          disabled={!hasPrinter}
          loading={running === "print"}
          style={styles.grow}
        />
        <Button
          label="Status"
          variant="outline"
          onPress={() => onAction("status")}
          disabled={!hasPrinter}
          loading={running === "status"}
          style={styles.grow}
        />
        <Button
          label="Preview"
          variant="outline"
          onPress={() => onAction("preview")}
          disabled={!canPreview}
          loading={running === "preview"}
          style={styles.grow}
        />
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  bar: {
    backgroundColor: colors.card,
    borderTopWidth: 1,
    borderTopColor: colors.border,
    paddingHorizontal: 16,
    paddingTop: 10,
    gap: 8,
  },
  printer: {
    fontSize: 12,
    color: colors.muted,
  },
  row: {
    flexDirection: "row",
    gap: 8,
  },
  grow: {
    flex: 1,
  },
});

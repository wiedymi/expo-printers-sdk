import {
  ActivityIndicator,
  Pressable,
  StyleSheet,
  Text,
  View,
} from "react-native";
import { colors } from "../theme";

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
  const button = (action: Action, label: string, enabled: boolean) => (
    <Pressable
      style={[
        styles.button,
        action === "print" && styles.primary,
        !enabled && styles.disabled,
      ]}
      onPress={() => onAction(action)}
      disabled={!enabled}
      accessibilityRole="button"
    >
      {running === action ? (
        <ActivityIndicator
          size="small"
          color={action === "print" ? colors.onPrimary : colors.text}
        />
      ) : (
        <Text style={[styles.text, action === "print" && styles.primaryText]}>
          {label}
        </Text>
      )}
    </Pressable>
  );

  const hasPrinter = printerName !== null;
  return (
    <View style={[styles.bar, { paddingBottom: bottomInset + 12 }]}>
      <Text style={styles.printer} numberOfLines={1}>
        {printerName ?? "Select a printer"}
        {hasPrinter && !canPreview ? "  ·  Preview is Epson only" : ""}
      </Text>
      <View style={styles.row}>
        {button("print", "Print", hasPrinter)}
        {button("status", "Status", hasPrinter)}
        {button("preview", "Preview", canPreview)}
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
  button: {
    flex: 1,
    paddingVertical: 13,
    borderRadius: 8,
    borderWidth: 1,
    borderColor: colors.primary,
    alignItems: "center",
  },
  primary: {
    backgroundColor: colors.primary,
  },
  disabled: {
    opacity: 0.35,
  },
  text: {
    fontSize: 15,
    fontWeight: "600",
    color: colors.text,
  },
  primaryText: {
    color: colors.onPrimary,
  },
});

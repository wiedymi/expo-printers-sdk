import {
  ActivityIndicator,
  Pressable,
  StyleSheet,
  Text,
  type StyleProp,
  type ViewStyle,
} from "react-native";
import { colors } from "../theme";

type ButtonProps = {
  label: string;
  onPress: () => void;
  // "primary" is filled, "outline" has a border only.
  variant?: "primary" | "outline";
  disabled?: boolean;
  // Shows a spinner in place of the label and ignores taps.
  loading?: boolean;
  style?: StyleProp<ViewStyle>;
};

export default function Button({
  label,
  onPress,
  variant = "primary",
  disabled = false,
  loading = false,
  style,
}: ButtonProps) {
  const primary = variant === "primary";
  return (
    <Pressable
      style={[
        styles.button,
        primary && styles.primary,
        disabled && styles.disabled,
        style,
      ]}
      onPress={onPress}
      disabled={disabled || loading}
      accessibilityRole="button"
      accessibilityState={{ disabled, busy: loading }}
    >
      {loading ? (
        <ActivityIndicator
          size="small"
          color={primary ? colors.onPrimary : colors.text}
        />
      ) : (
        <Text style={[styles.text, primary && styles.primaryText]}>
          {label}
        </Text>
      )}
    </Pressable>
  );
}

const styles = StyleSheet.create({
  button: {
    paddingVertical: 12,
    paddingHorizontal: 16,
    borderRadius: 8,
    borderWidth: 1,
    borderColor: colors.primary,
    alignItems: "center",
    justifyContent: "center",
  },
  primary: {
    backgroundColor: colors.primary,
  },
  disabled: {
    opacity: 0.35,
  },
  text: {
    fontSize: 14,
    fontWeight: "600",
    color: colors.text,
  },
  primaryText: {
    color: colors.onPrimary,
  },
});

import { Platform } from "react-native";

export const colors = {
  background: "#f4f4f5",
  card: "#ffffff",
  border: "#e4e4e7",
  text: "#18181b",
  muted: "#71717a",
  primary: "#18181b",
  onPrimary: "#ffffff",
  selected: "#f4f4f5",
  success: "#15803d",
  warning: "#b45309",
  error: "#b91c1c",
  logBackground: "#18181b",
  logText: "#d4d4d8",
};

export const MONO_FONT = Platform.select({
  ios: "Menlo",
  default: "monospace",
});

import type { ReactNode } from "react";
import { Pressable, StyleSheet, Text, View } from "react-native";
import { colors } from "../theme";

type SectionProps = {
  title: string;
  action?: { label: string; onPress: () => void };
  children: ReactNode;
};

// A card with a title and an optional action on the right, for example "Clear".
export default function Section({ title, action, children }: SectionProps) {
  return (
    <View style={styles.card}>
      <View style={styles.header}>
        <Text style={styles.title}>{title}</Text>
        {action && (
          <Pressable onPress={action.onPress} hitSlop={8}>
            <Text style={styles.action}>{action.label}</Text>
          </Pressable>
        )}
      </View>
      {children}
    </View>
  );
}

export function Label({ children }: { children: ReactNode }) {
  return <Text style={styles.label}>{children}</Text>;
}

const styles = StyleSheet.create({
  card: {
    backgroundColor: colors.card,
    borderRadius: 12,
    borderWidth: 1,
    borderColor: colors.border,
    padding: 14,
    gap: 10,
  },
  header: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
  },
  title: {
    fontSize: 16,
    fontWeight: "700",
    color: colors.text,
  },
  action: {
    fontSize: 13,
    fontWeight: "600",
    color: colors.muted,
  },
  label: {
    fontSize: 12,
    fontWeight: "600",
    color: colors.muted,
    marginBottom: -4,
  },
});

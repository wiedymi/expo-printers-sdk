import { Pressable, StyleSheet, Text, View } from "react-native";
import { colors } from "../theme";

type SegmentedProps<T extends string> = {
  options: readonly T[];
  value: T;
  onChange: (value: T) => void;
  labels?: Partial<Record<T, string>>;
};

// A row of options. The selected option is filled.
export default function Segmented<T extends string>({
  options,
  value,
  onChange,
  labels,
}: SegmentedProps<T>) {
  return (
    <View style={styles.row}>
      {options.map((option) => {
        const selected = option === value;
        return (
          <Pressable
            key={option}
            style={[styles.option, selected && styles.selected]}
            onPress={() => onChange(option)}
            accessibilityRole="button"
            accessibilityState={{ selected }}
          >
            <Text
              style={[styles.text, selected && styles.selectedText]}
              numberOfLines={1}
            >
              {labels?.[option] ?? option}
            </Text>
          </Pressable>
        );
      })}
    </View>
  );
}

const styles = StyleSheet.create({
  row: {
    flexDirection: "row",
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: 8,
    overflow: "hidden",
  },
  option: {
    flex: 1,
    paddingVertical: 9,
    alignItems: "center",
    backgroundColor: colors.card,
  },
  selected: {
    backgroundColor: colors.primary,
  },
  text: {
    color: colors.text,
    fontSize: 13,
    fontWeight: "500",
  },
  selectedText: {
    color: colors.onPrimary,
  },
});

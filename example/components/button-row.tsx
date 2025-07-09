import React from "react";
import {
  View,
  Text,
  TouchableOpacity,
  StyleSheet,
  ViewStyle,
  TextStyle,
} from "react-native";

type ButtonRowProps<T extends string> = {
  options: T[];
  selected: T;
  onSelect: (option: T) => void;
  buttonStyle?: ViewStyle;
  selectedButtonStyle?: ViewStyle;
  buttonTextStyle?: TextStyle;
  selectedButtonTextStyle?: TextStyle;
  labelMap?: Record<T, string>;
};

function ButtonRow<T extends string>({
  options,
  selected,
  onSelect,
  buttonStyle,
  selectedButtonStyle,
  buttonTextStyle,
  selectedButtonTextStyle,
  labelMap,
}: ButtonRowProps<T>) {
  return (
    <View style={styles.buttonRow}>
      {options.map((option) => (
        <TouchableOpacity
          key={option}
          style={[
            styles.button,
            buttonStyle,
            selected === option && [styles.selectedButton, selectedButtonStyle],
          ]}
          onPress={() => onSelect(option)}
          accessibilityLabel={`Select ${labelMap?.[option] || option}`}
          accessible
        >
          <Text
            style={[
              styles.buttonText,
              buttonTextStyle,
              selected === option && [
                styles.selectedButtonText,
                selectedButtonTextStyle,
              ],
            ]}
          >
            {labelMap?.[option] || option}
          </Text>
        </TouchableOpacity>
      ))}
    </View>
  );
}

const styles = StyleSheet.create({
  buttonRow: {
    flexDirection: "row",
    marginBottom: 8,
    borderRadius: 0,
  },
  button: {
    flex: 1,
    padding: 10,
    backgroundColor: "#f5f5f5",
    marginHorizontal: 4,
    borderRadius: 0,
    alignItems: "center",
  },
  selectedButton: {
    backgroundColor: "#222",
    borderColor: "#111",
    borderRadius: 0,
  },
  buttonText: {
    color: "#222",
    fontWeight: "500",
    borderRadius: 0,
  },
  selectedButtonText: {
    color: "#fff",
    borderRadius: 0,
  },
});

export default ButtonRow;

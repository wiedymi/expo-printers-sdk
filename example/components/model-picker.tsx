import { useState } from "react";
import {
  FlatList,
  Modal,
  Pressable,
  StyleSheet,
  Text,
  TextInput,
  View,
} from "react-native";
import { useSafeAreaInsets } from "react-native-safe-area-context";
import { colors } from "../theme";

type ModelPickerProps = {
  visible: boolean;
  models: string[];
  selected: string;
  onSelect: (model: string) => void;
  onClose: () => void;
};

// A full-screen list of the supported models with a search field at the top, so the keyboard does not cover
// the results.
export default function ModelPicker({
  visible,
  models,
  selected,
  onSelect,
  onClose,
}: ModelPickerProps) {
  const insets = useSafeAreaInsets();
  const [query, setQuery] = useState("");
  const filtered = models.filter((model) =>
    model.toLowerCase().includes(query.trim().toLowerCase()),
  );

  return (
    <Modal visible={visible} animationType="slide" onRequestClose={onClose}>
      <View style={[styles.screen, { paddingTop: insets.top + 12 }]}>
        <View style={styles.header}>
          <Text style={styles.title}>Printer model</Text>
          <Pressable onPress={onClose} hitSlop={8}>
            <Text style={styles.close}>Close</Text>
          </Pressable>
        </View>
        <TextInput
          style={styles.search}
          placeholder="Search, for example TM-U220"
          placeholderTextColor={colors.muted}
          value={query}
          onChangeText={setQuery}
          autoCapitalize="none"
          autoCorrect={false}
        />
        <FlatList
          data={filtered}
          keyExtractor={(model) => model}
          keyboardShouldPersistTaps="handled"
          renderItem={({ item }) => (
            <Pressable
              style={styles.item}
              onPress={() => {
                onSelect(item);
                onClose();
              }}
            >
              <Text
                style={[
                  styles.itemText,
                  item === selected && styles.itemSelected,
                ]}
              >
                {item}
              </Text>
            </Pressable>
          )}
        />
      </View>
    </Modal>
  );
}

const styles = StyleSheet.create({
  screen: {
    flex: 1,
    backgroundColor: colors.card,
    paddingHorizontal: 16,
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
  close: {
    fontSize: 14,
    fontWeight: "600",
    color: colors.muted,
  },
  search: {
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: 8,
    paddingHorizontal: 12,
    paddingVertical: 10,
    color: colors.text,
  },
  item: {
    paddingVertical: 12,
    borderBottomWidth: 1,
    borderBottomColor: colors.border,
  },
  itemText: {
    fontSize: 15,
    color: colors.text,
  },
  itemSelected: {
    fontWeight: "700",
  },
});

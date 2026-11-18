import {
  Image,
  Modal,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  View,
  useWindowDimensions,
} from "react-native";
import type { EpsonImagePreview } from "expo-printers-sdk";
import { useSafeAreaInsets } from "react-native-safe-area-context";
import { colors, MONO_FONT } from "../theme";

type ImagePreviewProps = {
  preview: EpsonImagePreview | null;
  title: string;
  onClose: () => void;
};

// Shows the image that the printer gets, scaled to the screen width.
export default function ImagePreview({
  preview,
  title,
  onClose,
}: ImagePreviewProps) {
  const insets = useSafeAreaInsets();
  const { width: screenWidth } = useWindowDimensions();
  const imageWidth = screenWidth - 32;

  return (
    <Modal
      visible={preview !== null}
      animationType="slide"
      onRequestClose={onClose}
    >
      <View style={[styles.container, { paddingTop: insets.top + 12 }]}>
        <View style={styles.header}>
          <View style={styles.titles}>
            <Text style={styles.title}>{title}</Text>
            {preview && (
              <Text style={styles.size}>
                {preview.width} x {preview.height} dots
              </Text>
            )}
          </View>
          <Pressable style={styles.close} onPress={onClose}>
            <Text style={styles.closeText}>Close</Text>
          </Pressable>
        </View>
        {preview && (
          <ScrollView
            contentContainerStyle={[
              styles.content,
              { paddingBottom: insets.bottom + 16 },
            ]}
          >
            <Image
              source={{ uri: `data:image/png;base64,${preview.base64}` }}
              style={[
                styles.image,
                {
                  width: imageWidth,
                  height: (imageWidth * preview.height) / preview.width,
                },
              ]}
            />
          </ScrollView>
        )}
      </View>
    </Modal>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: colors.background,
  },
  header: {
    flexDirection: "row",
    alignItems: "center",
    paddingHorizontal: 16,
    paddingBottom: 12,
    gap: 12,
  },
  titles: {
    flex: 1,
    gap: 2,
  },
  title: {
    fontSize: 16,
    fontWeight: "700",
    color: colors.text,
  },
  size: {
    fontSize: 12,
    color: colors.muted,
    fontFamily: MONO_FONT,
  },
  close: {
    backgroundColor: colors.primary,
    borderRadius: 8,
    paddingHorizontal: 16,
    paddingVertical: 10,
  },
  closeText: {
    color: colors.onPrimary,
    fontWeight: "600",
  },
  content: {
    padding: 16,
  },
  image: {
    backgroundColor: "#ffffff",
    borderWidth: 1,
    borderColor: colors.border,
  },
});

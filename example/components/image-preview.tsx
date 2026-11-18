import {
  Image,
  Modal,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  PixelRatio,
  View,
} from "react-native";
import type { EpsonImagePreview } from "expo-printers-sdk";
import { useSafeAreaInsets } from "react-native-safe-area-context";
import { colors, MONO_FONT } from "../theme";

type ImagePreviewProps = {
  preview: EpsonImagePreview | null;
  title: string;
  onClose: () => void;
};

const PADDING = 16;

// The width in pixels for previewImage, so that the preview fits the screen.
export const previewTargetWidth = (screenWidth: number): number =>
  Math.floor((screenWidth - 2 * PADDING) * PixelRatio.get());

// Shows the image that the printer gets. The PNG has whole pixels for each dot and is shown at its own size,
// because React Native smooths an image that it shows larger.
export default function ImagePreview({
  preview,
  title,
  onClose,
}: ImagePreviewProps) {
  const insets = useSafeAreaInsets();
  const pixelRatio = PixelRatio.get();

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
                {preview.width} x {preview.height} dots, {preview.scale} px for
                each dot
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
                  width: (preview.width * preview.scale) / pixelRatio,
                  height: (preview.height * preview.scale) / pixelRatio,
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
    padding: PADDING,
    alignItems: "center",
  },
  // No border: a border takes space from the image, and the image is then scaled and blurred again.
  image: {
    backgroundColor: "#ffffff",
  },
});

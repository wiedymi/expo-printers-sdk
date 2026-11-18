import React from "react";
import {
  Modal,
  View,
  Text,
  Image,
  ScrollView,
  TouchableOpacity,
  StyleSheet,
  useWindowDimensions,
  Platform,
} from "react-native";
import type { EpsonImagePreview } from "expo-printers-sdk";

const MONO_FONT = Platform.select({
  ios: "Menlo",
  android: "monospace",
  default: "monospace",
});

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
  const { width: screenWidth } = useWindowDimensions();
  const imageWidth = screenWidth - 32;

  return (
    <Modal
      visible={preview !== null}
      animationType="slide"
      onRequestClose={onClose}
    >
      <View style={styles.container}>
        <View style={styles.header}>
          <Text style={styles.title}>
            {title}
            {preview ? `\n${preview.width} x ${preview.height} dots` : ""}
          </Text>
          <TouchableOpacity
            style={styles.closeButton}
            onPress={onClose}
            accessibilityLabel="Close preview"
            accessible
          >
            <Text style={styles.closeText}>Close</Text>
          </TouchableOpacity>
        </View>
        {preview && (
          <ScrollView contentContainerStyle={styles.content}>
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
    backgroundColor: "#fff",
    paddingTop: 16,
  },
  header: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
    paddingHorizontal: 16,
    paddingBottom: 12,
  },
  title: {
    fontSize: 14,
    color: "#111",
    fontFamily: MONO_FONT,
    flex: 1,
  },
  closeButton: {
    backgroundColor: "#444",
    paddingHorizontal: 16,
    paddingVertical: 8,
  },
  closeText: {
    color: "#fff",
    fontWeight: "600",
  },
  content: {
    padding: 16,
  },
  image: {
    borderWidth: 1,
    borderColor: "#d1d1d1",
  },
});

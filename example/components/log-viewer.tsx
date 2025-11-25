import React, { useRef, useEffect } from "react";
import {
  View,
  Text,
  ScrollView,
  StyleSheet,
  TouchableOpacity,
  Platform,
} from "react-native";

export type LogEntry = {
  id: number;
  timestamp: string;
  level: "log" | "info" | "warn" | "error";
  message: string;
};

type LogViewerProps = {
  logs: LogEntry[];
  onClear: () => void;
  maxHeight?: number;
};

const MONO_FONT = Platform.select({
  ios: "Menlo",
  android: "monospace",
  default: "monospace",
});

const LOG_COLORS = {
  log: "#ccc",
  info: "#66b3ff",
  warn: "#ffaa33",
  error: "#ff6666",
};

export default function LogViewer({
  logs,
  onClear,
  maxHeight = 200,
}: LogViewerProps) {
  const scrollViewRef = useRef<ScrollView>(null);

  useEffect(() => {
    // Auto-scroll to bottom when new logs arrive
    scrollViewRef.current?.scrollToEnd({ animated: true });
  }, [logs]);

  return (
    <View style={[styles.container, { height: maxHeight }]}>
      <View style={styles.header}>
        <Text style={styles.headerText}>Logs ({logs.length})</Text>
        <TouchableOpacity onPress={onClear} style={styles.clearButton}>
          <Text style={styles.clearButtonText}>Clear</Text>
        </TouchableOpacity>
      </View>
      <ScrollView
        ref={scrollViewRef}
        style={styles.scrollView}
        contentContainerStyle={styles.scrollContent}
      >
        {logs.length === 0 ? (
          <Text style={styles.emptyText}>No logs yet...</Text>
        ) : (
          logs.map((log) => (
            <View key={log.id} style={styles.logEntry}>
              <Text style={styles.timestamp}>{log.timestamp}</Text>
              <Text
                style={[styles.level, { color: LOG_COLORS[log.level] }]}
              >
                [{log.level.toUpperCase()}]
              </Text>
              <Text style={[styles.message, { color: LOG_COLORS[log.level] }]}>
                {log.message}
              </Text>
            </View>
          ))
        )}
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    backgroundColor: "#1a1a1a",
    borderRadius: 8,
    overflow: "hidden",
    borderWidth: 1,
    borderColor: "#333",
  },
  header: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
    paddingHorizontal: 10,
    paddingVertical: 6,
    backgroundColor: "#2a2a2a",
    borderBottomWidth: 1,
    borderBottomColor: "#333",
  },
  headerText: {
    color: "#aaa",
    fontSize: 12,
    fontFamily: MONO_FONT,
    fontWeight: "600",
  },
  clearButton: {
    paddingHorizontal: 8,
    paddingVertical: 2,
    backgroundColor: "#444",
    borderRadius: 4,
  },
  clearButtonText: {
    color: "#ccc",
    fontSize: 10,
    fontFamily: MONO_FONT,
  },
  scrollView: {
    flex: 1,
  },
  scrollContent: {
    padding: 8,
  },
  emptyText: {
    color: "#666",
    fontSize: 11,
    fontFamily: MONO_FONT,
    fontStyle: "italic",
  },
  logEntry: {
    flexDirection: "row",
    flexWrap: "wrap",
    marginBottom: 2,
  },
  timestamp: {
    color: "#888",
    fontSize: 11,
    fontFamily: MONO_FONT,
    marginRight: 6,
  },
  level: {
    fontSize: 11,
    fontFamily: MONO_FONT,
    fontWeight: "600",
    marginRight: 6,
  },
  message: {
    fontSize: 11,
    fontFamily: MONO_FONT,
    flex: 1,
  },
});

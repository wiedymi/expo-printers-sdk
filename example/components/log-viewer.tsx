import React, { memo, useEffect, useMemo, useRef, useState } from "react";
import { ScrollView, StyleSheet, Text, View } from "react-native";
import type { LogEntry, LogLevel } from "../types/printer";
import { colors, MONO_FONT } from "../theme";
import Segmented from "./segmented";

type LogFilter = "ALL" | "INFO" | "WARN";

const FILTER_LABELS: Record<LogFilter, string> = {
  ALL: "All",
  INFO: "No debug",
  WARN: "Warnings",
};

const LEVELS_BY_FILTER: Record<LogFilter, LogLevel[]> = {
  ALL: ["debug", "info", "warn", "error"],
  INFO: ["info", "warn", "error"],
  WARN: ["warn", "error"],
};

const LEVEL_COLORS: Record<LogLevel, string> = {
  debug: "#a1a1aa",
  info: colors.logText,
  warn: "#fbbf24",
  error: "#f87171",
};

// A log line does not change, so it renders only once.
const LogLine = memo(function LogLine({ entry }: { entry: LogEntry }) {
  return (
    <Text
      style={[styles.line, { color: LEVEL_COLORS[entry.level] }]}
      selectable
    >
      {entry.time} {entry.tag}: {entry.message}
    </Text>
  );
});

// App and native log lines, newest at the bottom.
export default function LogViewer({ logs }: { logs: LogEntry[] }) {
  const [filter, setFilter] = useState<LogFilter>("INFO");
  const scrollRef = useRef<React.ComponentRef<typeof ScrollView>>(null);
  const visible = useMemo(
    () =>
      logs.filter((entry) => LEVELS_BY_FILTER[filter].includes(entry.level)),
    [logs, filter],
  );

  useEffect(() => {
    scrollRef.current?.scrollToEnd({ animated: false });
  }, [visible.length]);

  return (
    <View style={styles.container}>
      <Segmented
        options={["ALL", "INFO", "WARN"]}
        value={filter}
        onChange={setFilter}
        labels={FILTER_LABELS}
      />
      <ScrollView
        ref={scrollRef}
        style={styles.scroll}
        contentContainerStyle={styles.content}
        nestedScrollEnabled
      >
        {visible.length === 0 ? (
          <Text style={styles.line}>No log lines.</Text>
        ) : (
          visible.map((entry) => <LogLine key={entry.id} entry={entry} />)
        )}
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    gap: 8,
  },
  scroll: {
    height: 280,
    backgroundColor: colors.logBackground,
    borderRadius: 8,
  },
  content: {
    padding: 10,
    gap: 3,
  },
  line: {
    fontSize: 11,
    color: colors.logText,
    fontFamily: MONO_FONT,
  },
});

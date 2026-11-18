import { useState } from "react";
import { Pressable, StyleSheet, Text, View } from "react-native";
import type { ResultEntry } from "../types/printer";
import { colors, MONO_FONT } from "../theme";

const seconds = (entry: ResultEntry): string =>
  entry.finishedAt === null
    ? ""
    : `${((entry.finishedAt - entry.startedAt) / 1000).toFixed(1)} s`;

// The outcome text and its color.
const outcomeOf = (entry: ResultEntry): { text: string; color: string } => {
  if (entry.finishedAt === null) {
    return {
      text: entry.kind === "print" ? "Printing..." : "Checking...",
      color: colors.muted,
    };
  }
  if (entry.kind === "status") {
    if (entry.errorCode) return { text: entry.errorCode, color: colors.error };
    const status = entry.status!;
    const problems = [
      !status.online && "offline",
      status.coverOpen && "cover open",
      status.paper !== "OK" && `paper ${status.paper}`,
      ...status.errors,
    ].filter(Boolean);
    return problems.length === 0
      ? { text: "Online, no errors", color: colors.success }
      : { text: problems.join(", "), color: colors.warning };
  }
  const result = entry.result!;
  return result.success
    ? { text: "Printed", color: colors.success }
    : {
        text: `${result.errorCode}: ${result.error ?? ""}`,
        color: colors.error,
      };
};

function ResultRow({ entry }: { entry: ResultEntry }) {
  const [open, setOpen] = useState(false);
  const outcome = outcomeOf(entry);
  const result = entry.kind === "print" ? entry.result : null;

  return (
    <Pressable
      style={styles.row}
      onPress={() => setOpen(!open)}
      disabled={!result}
    >
      <View style={styles.header}>
        <Text style={styles.kind}>
          {entry.kind === "print" ? entry.id : "status"}
        </Text>
        <Text style={styles.printer} numberOfLines={1}>
          {entry.printer}
        </Text>
        <Text style={styles.time}>{seconds(entry)}</Text>
      </View>
      <Text style={[styles.outcome, { color: outcome.color }]}>
        {outcome.text}
        {result ? (open ? "  ▲" : "  ▼ steps") : ""}
      </Text>
      {open && result && (
        <View style={styles.details}>
          {result.steps.map((step, index) => (
            <Text key={index} style={styles.step}>
              {step.step.padEnd(17)} {String(step.durationMs).padStart(6)} ms{" "}
              {step.outcome}
            </Text>
          ))}
          {result.cause && (
            <Text style={[styles.step, { color: colors.error }]}>
              cause: {result.cause.step} {result.cause.type}{" "}
              {result.cause.message ?? ""}
              {result.cause.vendorCode !== null
                ? ` (code ${result.cause.vendorCode})`
                : ""}
            </Text>
          )}
        </View>
      )}
    </Pressable>
  );
}

// Print jobs and status checks, newest first. Tap a print job to see its steps.
export default function ResultsList({ results }: { results: ResultEntry[] }) {
  if (results.length === 0) {
    return (
      <Text style={styles.empty}>
        Print or Status results show here, newest first.
      </Text>
    );
  }
  return (
    <View style={styles.list}>
      {results.map((entry) => (
        <ResultRow key={entry.id} entry={entry} />
      ))}
    </View>
  );
}

const styles = StyleSheet.create({
  list: {
    gap: 6,
  },
  empty: {
    color: colors.muted,
    fontSize: 14,
  },
  row: {
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: 8,
    padding: 10,
    gap: 4,
  },
  header: {
    flexDirection: "row",
    alignItems: "center",
    gap: 8,
  },
  kind: {
    fontSize: 12,
    fontWeight: "700",
    color: colors.text,
    fontFamily: MONO_FONT,
  },
  printer: {
    flex: 1,
    fontSize: 12,
    color: colors.muted,
  },
  time: {
    fontSize: 12,
    color: colors.muted,
    fontFamily: MONO_FONT,
  },
  outcome: {
    fontSize: 14,
    fontWeight: "600",
  },
  details: {
    marginTop: 4,
    gap: 2,
  },
  step: {
    fontSize: 11,
    color: colors.text,
    fontFamily: MONO_FONT,
  },
});

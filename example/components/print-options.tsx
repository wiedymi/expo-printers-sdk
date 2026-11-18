import React from "react";
import { View, Text, TextInput, StyleSheet, Platform } from "react-native";
import type { EpsonImageConversion } from "expo-printers-sdk";
import ButtonRow from "./button-row";
import { RECEIPT_LABELS, type ReceiptOption } from "../utils/receipts";

export type PaperWidthOption = "AUTO" | "58" | "80";
export type JobCountOption = "1" | "3";

export type PrintSettings = {
  receipt: ReceiptOption;
  imageUrl: string;
  paperWidth: PaperWidthOption;
  imageConversion: EpsonImageConversion;
  jobCount: JobCountOption;
};

const PAPER_WIDTH_LABELS: Record<PaperWidthOption, string> = {
  AUTO: "Auto",
  "58": "58 mm",
  "80": "80 mm",
};

const IMAGE_CONVERSION_LABELS: Record<EpsonImageConversion, string> = {
  auto: "Auto",
  sdk: "SDK",
  areaAverage: "Area avg",
};

const JOB_COUNT_LABELS: Record<JobCountOption, string> = {
  "1": "1 job",
  "3": "3 jobs",
};

const MONO_FONT = Platform.select({
  ios: "Menlo",
  android: "monospace",
  default: "monospace",
});

type PrintOptionsProps = {
  settings: PrintSettings;
  onChange: (settings: PrintSettings) => void;
};

export default function PrintOptions({
  settings,
  onChange,
}: PrintOptionsProps) {
  const update = (change: Partial<PrintSettings>) =>
    onChange({ ...settings, ...change });

  return (
    <View style={styles.container}>
      <Text style={styles.label}>Image</Text>
      <ButtonRow
        options={["SHORT", "LONG", "CHART", "URL"]}
        selected={settings.receipt}
        onSelect={(receipt) => update({ receipt })}
        labelMap={RECEIPT_LABELS}
      />
      {settings.receipt === "URL" && (
        <TextInput
          style={styles.input}
          value={settings.imageUrl}
          onChangeText={(imageUrl) => update({ imageUrl })}
          autoCapitalize="none"
          autoCorrect={false}
          keyboardType="url"
          placeholder="https://example.com/receipt.png"
        />
      )}
      <Text style={styles.label}>Paper width</Text>
      <ButtonRow
        options={["AUTO", "58", "80"]}
        selected={settings.paperWidth}
        onSelect={(paperWidth) => update({ paperWidth })}
        labelMap={PAPER_WIDTH_LABELS}
      />
      <Text style={styles.label}>Epson image conversion</Text>
      <ButtonRow
        options={["auto", "sdk", "areaAverage"]}
        selected={settings.imageConversion}
        onSelect={(imageConversion) => update({ imageConversion })}
        labelMap={IMAGE_CONVERSION_LABELS}
      />
      <Text style={styles.label}>Jobs per tap</Text>
      <ButtonRow
        options={["1", "3"]}
        selected={settings.jobCount}
        onSelect={(jobCount) => update({ jobCount })}
        labelMap={JOB_COUNT_LABELS}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    gap: 2,
  },
  label: {
    fontSize: 12,
    color: "#666",
    marginHorizontal: 4,
    fontFamily: MONO_FONT,
  },
  input: {
    borderWidth: 1,
    borderColor: "#d1d1d1",
    marginHorizontal: 4,
    marginBottom: 8,
    paddingHorizontal: 10,
    paddingVertical: 8,
    color: "#111",
    fontFamily: MONO_FONT,
  },
});

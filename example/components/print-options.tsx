import { StyleSheet, TextInput, View } from "react-native";
import type {
  EpsonImageConversion,
  EpsonPrintOptions,
  PaperWidth,
} from "expo-printers-sdk";
import { RECEIPT_LABELS, type ReceiptOption } from "../utils/receipts";
import { colors } from "../theme";
import Segmented from "./segmented";
import { Label } from "./section";

export type PaperWidthOption = "AUTO" | "58" | "80";
export type JobCountOption = "1" | "3";

export type PrintSettings = {
  receipt: ReceiptOption;
  imageUrl: string;
  paperWidth: PaperWidthOption;
  imageConversion: EpsonImageConversion;
  jobCount: JobCountOption;
};

export const DEFAULT_PRINT_SETTINGS: PrintSettings = {
  receipt: "SHORT",
  imageUrl: "https://placehold.co/600x400/000000/FFFFFF.png",
  paperWidth: "AUTO",
  imageConversion: "areaAverage",
  jobCount: "1",
};

export const toPrintOptions = (settings: PrintSettings): EpsonPrintOptions => ({
  paperWidth:
    settings.paperWidth === "AUTO"
      ? undefined
      : (Number(settings.paperWidth) as PaperWidth),
  imageConversion: settings.imageConversion,
});

const PAPER_WIDTH_LABELS: Record<PaperWidthOption, string> = {
  AUTO: "Auto",
  "58": "58 mm",
  "80": "80 mm",
};

const IMAGE_CONVERSION_LABELS: Record<EpsonImageConversion, string> = {
  areaAverage: "Area average",
  sdk: "SDK (0.11.0)",
};

const JOB_COUNT_LABELS: Record<JobCountOption, string> = {
  "1": "1 job",
  "3": "3 jobs",
};

type PrintOptionsProps = {
  settings: PrintSettings;
  onChange: (settings: PrintSettings) => void;
  showImageConversion: boolean;
};

export default function PrintOptions({
  settings,
  onChange,
  showImageConversion,
}: PrintOptionsProps) {
  const update = (change: Partial<PrintSettings>) =>
    onChange({ ...settings, ...change });

  return (
    <View style={styles.container}>
      <Label>Image</Label>
      <Segmented
        options={["SHORT", "LONG", "CHART", "URL"]}
        value={settings.receipt}
        onChange={(receipt) => update({ receipt })}
        labels={RECEIPT_LABELS}
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
          placeholderTextColor={colors.muted}
        />
      )}

      <Label>Paper width</Label>
      <Segmented
        options={["AUTO", "58", "80"]}
        value={settings.paperWidth}
        onChange={(paperWidth) => update({ paperWidth })}
        labels={PAPER_WIDTH_LABELS}
      />

      {showImageConversion && (
        <>
          <Label>Image conversion (Epson)</Label>
          <Segmented
            options={["areaAverage", "sdk"]}
            value={settings.imageConversion}
            onChange={(imageConversion) => update({ imageConversion })}
            labels={IMAGE_CONVERSION_LABELS}
          />
        </>
      )}

      <Label>Jobs for each tap on Print</Label>
      <Segmented
        options={["1", "3"]}
        value={settings.jobCount}
        onChange={(jobCount) => update({ jobCount })}
        labels={JOB_COUNT_LABELS}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    gap: 10,
  },
  input: {
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: 8,
    paddingHorizontal: 12,
    paddingVertical: 10,
    color: colors.text,
    fontSize: 14,
  },
});

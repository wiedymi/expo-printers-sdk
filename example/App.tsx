import { useState } from "react";
import {
  Alert,
  ScrollView,
  StatusBar,
  StyleSheet,
  Text,
  View,
  useWindowDimensions,
} from "react-native";
import type {
  EpsonImagePreview,
  PrinterConnectionType,
} from "expo-printers-sdk";
import type { Vendor } from "./types/printer";
import { usePrinters } from "./hooks/usePrinters";
import { usePermissions } from "./hooks/usePermissions";
import { useActivity } from "./hooks/useActivity";
import {
  canPreview,
  connectManually,
  getStatus,
  previewImage,
  printImage,
  printerName,
} from "./utils/printers";
import { loadReceiptBase64, RECEIPT_LABELS } from "./utils/receipts";
import {
  SafeAreaProvider,
  useSafeAreaInsets,
} from "react-native-safe-area-context";
import { colors } from "./theme";
import Section from "./components/section";
import PrinterFinder from "./components/printer-finder";
import PrinterPicker from "./components/printer-picker";
import PrintOptions, {
  DEFAULT_PRINT_SETTINGS,
  toPrintOptions,
  type PrintSettings,
} from "./components/print-options";
import ActionBar, { type Action } from "./components/action-bar";
import ResultsList from "./components/results-list";
import LogViewer from "./components/log-viewer";
import ImagePreview, { previewTargetWidth } from "./components/image-preview";

const errorMessage = (error: unknown): string =>
  error instanceof Error ? error.message : String(error);

// Each print gets its own job ID, also when one tap sends several jobs.
let nextJobNumber = 1;

export default function App() {
  return (
    <SafeAreaProvider>
      <PrinterTester />
    </SafeAreaProvider>
  );
}

function PrinterTester() {
  const insets = useSafeAreaInsets();
  const { width: screenWidth } = useWindowDimensions();
  const [vendor, setVendor] = useState<Vendor>("EPSON");
  const [settings, setSettings] = useState<PrintSettings>(
    DEFAULT_PRINT_SETTINGS,
  );
  const [running, setRunning] = useState<Action | null>(null);
  const [preview, setPreview] = useState<EpsonImagePreview | null>(null);

  const activity = useActivity();
  const { log, logError } = activity;
  const printers = usePrinters(log);
  const { requestPermissions } = usePermissions();
  const selected = printers.selected;

  const handleSearch = async (connectionType: PrinterConnectionType) => {
    try {
      if (!(await requestPermissions())) return;
      await printers.search(vendor, connectionType);
    } catch (error) {
      logError(`Search failed: ${errorMessage(error)}`);
    }
  };

  const handleAdd = async (
    ipAddress: string,
    port: number,
    modelName: string,
  ) => {
    try {
      const printer = await connectManually(vendor, ipAddress, port, modelName);
      printers.add(printer);
      log(`Added ${printerName(printer)} at ${ipAddress}:${port}`);
    } catch (error) {
      logError(`Add failed: ${errorMessage(error)}`);
      Alert.alert("Cannot add the printer", errorMessage(error));
    }
  };

  const handlePrint = async () => {
    if (!selected) return;
    const name = printerName(selected);
    const base64Image = await loadReceiptBase64(
      settings.receipt,
      settings.imageUrl,
    );
    for (let job = 0; job < Number(settings.jobCount); job++) {
      const jobId = `job-${nextJobNumber++}`;
      activity.startPrint(jobId, name);
      log(`${jobId}: ${RECEIPT_LABELS[settings.receipt]} to ${name}`);
      const started = await printImage(selected, base64Image, {
        ...toPrintOptions(settings),
        jobId,
      });
      if (!started) {
        activity.finishPrint({
          jobId,
          success: false,
          errorCode: "NOT_INITIALIZED",
          error: "The print did not start",
          cause: null,
          steps: [],
        });
      }
    }
  };

  const handleStatus = async () => {
    if (!selected) return;
    const id = activity.startStatus(printerName(selected));
    try {
      activity.finishStatus(id, { status: await getStatus(selected) });
    } catch (error) {
      const code = (error as { code?: string }).code ?? errorMessage(error);
      activity.finishStatus(id, { errorCode: code });
    }
  };

  const handlePreview = async () => {
    if (!selected) return;
    const base64Image = await loadReceiptBase64(
      settings.receipt,
      settings.imageUrl,
    );
    setPreview(
      await previewImage(
        selected,
        base64Image,
        toPrintOptions(settings),
        previewTargetWidth(screenWidth),
      ),
    );
  };

  const handleAction = async (action: Action) => {
    setRunning(action);
    try {
      if (action === "print") await handlePrint();
      if (action === "status") await handleStatus();
      if (action === "preview") await handlePreview();
    } catch (error) {
      logError(`${action} failed: ${errorMessage(error)}`);
      Alert.alert(`${action} failed`, errorMessage(error));
    } finally {
      setRunning(null);
    }
  };

  return (
    <View style={styles.screen}>
      <StatusBar barStyle="dark-content" />
      <ScrollView
        contentContainerStyle={[
          styles.content,
          { paddingTop: insets.top + 12 },
        ]}
        keyboardShouldPersistTaps="handled"
      >
        <Text style={styles.title}>Printers SDK</Text>

        <Section title="1. Find a printer">
          <PrinterFinder
            vendor={vendor}
            onVendorChange={setVendor}
            searching={printers.searching === vendor}
            onSearch={handleSearch}
            onAdd={handleAdd}
          />
        </Section>

        <Section title="2. Select a printer">
          <PrinterPicker
            printers={printers.printers}
            selectedId={printers.selectedId}
            onSelect={printers.select}
          />
        </Section>

        <Section title="3. What to print">
          <PrintOptions
            settings={settings}
            onChange={setSettings}
            showImageConversion={selected?.vendor === "EPSON"}
          />
        </Section>

        <Section
          title="Results"
          action={{ label: "Clear", onPress: activity.clearResults }}
        >
          <ResultsList results={activity.results} />
        </Section>

        <Section
          title="Logs"
          action={{ label: "Clear", onPress: activity.clearLogs }}
        >
          <LogViewer logs={activity.logs} />
        </Section>
      </ScrollView>

      <View style={[styles.statusBar, { height: insets.top }]} />

      <ActionBar
        printerName={selected ? printerName(selected) : null}
        canPreview={canPreview(selected)}
        running={running}
        onAction={handleAction}
        bottomInset={insets.bottom}
      />

      <ImagePreview
        preview={preview}
        title={`${selected ? printerName(selected) : ""}: ${RECEIPT_LABELS[settings.receipt]}, ${settings.imageConversion}`}
        onClose={() => setPreview(null)}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  screen: {
    flex: 1,
    backgroundColor: colors.background,
  },
  content: {
    padding: 16,
    gap: 14,
  },
  // The content scrolls under the transparent system status bar, so this keeps the bar readable.
  statusBar: {
    position: "absolute",
    top: 0,
    left: 0,
    right: 0,
    backgroundColor: colors.background,
  },
  title: {
    fontSize: 22,
    fontWeight: "800",
    color: colors.text,
  },
});

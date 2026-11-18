import { useCallback, useEffect, useRef, useState } from "react";
import {
  EpsonPrinters,
  RongtaPrinters,
  StarMicronicsPrinters,
} from "expo-printers-sdk";
import type { PrinterLogEvent, PrinterStatus } from "expo-printers-sdk";
import type {
  LogEntry,
  LogLevel,
  PrintResult,
  ResultEntry,
} from "../types/printer";

const MAX_RESULTS = 50;
const MAX_LOGS = 500;

const clockTime = (timestamp: number): string => {
  const date = new Date(timestamp);
  const pad = (value: number, length = 2) =>
    value.toString().padStart(length, "0");
  return `${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}.${pad(date.getMilliseconds(), 3)}`;
};

// The results of print jobs and status checks, and the log lines of the app and of the native modules.
export const useActivity = () => {
  const [results, setResults] = useState<ResultEntry[]>([]);
  const [logs, setLogs] = useState<LogEntry[]>([]);
  const nextLogId = useRef(0);
  const nextStatusId = useRef(0);

  const addLog = useCallback(
    (level: LogLevel, tag: string, message: string, timestamp = Date.now()) => {
      const entry = {
        id: nextLogId.current++,
        time: clockTime(timestamp),
        level,
        tag,
        message,
      };
      setLogs((current) => [...current, entry].slice(-MAX_LOGS));
    },
    [],
  );

  const log = useCallback(
    (message: string) => addLog("info", "App", message),
    [addLog],
  );
  const logError = useCallback(
    (message: string) => addLog("error", "App", message),
    [addLog],
  );

  const addResult = useCallback((entry: ResultEntry) => {
    setResults((current) => [entry, ...current].slice(0, MAX_RESULTS));
  }, []);

  const updateResult = useCallback(
    (id: string, update: (entry: ResultEntry) => ResultEntry) => {
      setResults((current) =>
        current.map((entry) => (entry.id === id ? update(entry) : entry)),
      );
    },
    [],
  );

  const startPrint = useCallback(
    (jobId: string, printer: string) =>
      addResult({
        kind: "print",
        id: jobId,
        printer,
        startedAt: Date.now(),
        finishedAt: null,
        result: null,
      }),
    [addResult],
  );

  const finishPrint = useCallback(
    (result: PrintResult) => {
      if (!result.jobId) return;
      updateResult(result.jobId, (entry) =>
        entry.kind === "print"
          ? { ...entry, finishedAt: Date.now(), result }
          : entry,
      );
    },
    [updateResult],
  );

  // Returns the ID of the new entry for finishStatus.
  const startStatus = useCallback(
    (printer: string): string => {
      const id = `status-${++nextStatusId.current}`;
      addResult({
        kind: "status",
        id,
        printer,
        startedAt: Date.now(),
        finishedAt: null,
        status: null,
        errorCode: null,
      });
      return id;
    },
    [addResult],
  );

  const finishStatus = useCallback(
    (
      id: string,
      outcome: { status: PrinterStatus } | { errorCode: string },
    ) => {
      updateResult(id, (entry) =>
        entry.kind === "status"
          ? { ...entry, ...outcome, finishedAt: Date.now() }
          : entry,
      );
    },
    [updateResult],
  );

  useEffect(() => {
    const onLog = (event: PrinterLogEvent) => {
      const error = event.error
        ? ` (${event.error.type}: ${event.error.message})`
        : "";
      addLog(event.level, event.tag, event.message + error, event.timestamp);
    };
    const subscriptions = [
      EpsonPrinters.addListener("onLog", onLog),
      StarMicronicsPrinters.addListener("onLog", onLog),
      RongtaPrinters.addListener("onLog", onLog),
      EpsonPrinters.addListener("onPrintImage", finishPrint),
      StarMicronicsPrinters.addListener("onPrintImage", finishPrint),
      RongtaPrinters.addListener("onPrintImage", finishPrint),
    ];
    return () => subscriptions.forEach((subscription) => subscription.remove());
  }, [addLog, finishPrint]);

  return {
    results,
    logs,
    log,
    logError,
    startPrint,
    finishPrint,
    startStatus,
    finishStatus,
    clearResults: useCallback(() => setResults([]), []),
    clearLogs: useCallback(() => setLogs([]), []),
  };
};

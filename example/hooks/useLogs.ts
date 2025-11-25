import { useState, useEffect, useCallback, useRef } from "react";
import type { LogEntry } from "../components/log-viewer";

const MAX_LOGS = 200;

export function useLogs() {
  const [logs, setLogs] = useState<LogEntry[]>([]);
  const logIdRef = useRef(0);
  const originalConsoleRef = useRef<{
    log: typeof console.log;
    info: typeof console.info;
    warn: typeof console.warn;
    error: typeof console.error;
  } | null>(null);

  const addLog = useCallback(
    (level: LogEntry["level"], ...args: unknown[]) => {
      const message = args
        .map((arg) => {
          if (typeof arg === "object") {
            try {
              return JSON.stringify(arg, null, 0);
            } catch {
              return String(arg);
            }
          }
          return String(arg);
        })
        .join(" ");

      const now = new Date();
      const timestamp = `${now.getHours().toString().padStart(2, "0")}:${now.getMinutes().toString().padStart(2, "0")}:${now.getSeconds().toString().padStart(2, "0")}.${now.getMilliseconds().toString().padStart(3, "0")}`;

      const entry: LogEntry = {
        id: logIdRef.current++,
        timestamp,
        level,
        message,
      };

      setLogs((prev) => {
        const newLogs = [...prev, entry];
        // Keep only last MAX_LOGS entries
        if (newLogs.length > MAX_LOGS) {
          return newLogs.slice(-MAX_LOGS);
        }
        return newLogs;
      });
    },
    []
  );

  const clearLogs = useCallback(() => {
    setLogs([]);
  }, []);

  useEffect(() => {
    // Store original console methods
    originalConsoleRef.current = {
      log: console.log,
      info: console.info,
      warn: console.warn,
      error: console.error,
    };

    // Override console methods
    console.log = (...args: unknown[]) => {
      originalConsoleRef.current?.log(...args);
      addLog("log", ...args);
    };

    console.info = (...args: unknown[]) => {
      originalConsoleRef.current?.info(...args);
      addLog("info", ...args);
    };

    console.warn = (...args: unknown[]) => {
      originalConsoleRef.current?.warn(...args);
      addLog("warn", ...args);
    };

    console.error = (...args: unknown[]) => {
      originalConsoleRef.current?.error(...args);
      addLog("error", ...args);
    };

    // Cleanup: restore original console methods
    return () => {
      if (originalConsoleRef.current) {
        console.log = originalConsoleRef.current.log;
        console.info = originalConsoleRef.current.info;
        console.warn = originalConsoleRef.current.warn;
        console.error = originalConsoleRef.current.error;
      }
    };
  }, [addLog]);

  return { logs, clearLogs, addLog };
}

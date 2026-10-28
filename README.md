# expo-printers-sdk

A robust Expo (React Native) module for integrating thermal printers from multiple manufacturers into your Expo/React Native applications. Built with enterprise-grade error handling and production-ready reliability.

## Features

- **Multi-Manufacturer Support**:

  - **Epson** (TM series, compatible models)
  - **Rongta** (RPP series, Bluetooth & Network)
  - **Star Micronics** (TSP series, mPOP, etc.)

- **Multiple Connection Types**:

  - **Bluetooth** - Wireless printing
  - **Network** - TCP/IP connection with auto-discovery
  - **USB** - Direct USB connection
  - **Manual Network** - Direct IP/port connection (NEW!)

- **Enterprise Features**:
  - **Smart Discovery** - Automatic printer detection
  - **Manual Connection** - Connect directly to known IP addresses
  - **Image Printing** - High-quality thermal printing
  - **Event-Driven** - Real-time status updates
  - **Robust Error Handling** - Production-ready reliability
  - **Full Example App** - Complete implementation reference

## Installation

#### Add the package to your dependencies

```bash
bunx expo install expo-printers-sdk
```

## In bare React Native projects

For bare React Native projects, you must ensure that you have [installed and configured the `expo` package](https://docs.expo.dev/bare/installing-expo-modules/) before continuing.

## Platform Support

- **Android** - Fully supported with native printer SDKs
- **iOS** - Maybe someday

## Setup & Configuration

### Android Configuration

No additional configuration needed! The module includes all necessary native dependencies and permissions.

### Required Permissions

The following permissions are automatically included but need runtime requests:

```xml
<!-- Bluetooth -->
<uses-permission android:name="android.permission.BLUETOOTH" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />

<!-- Location (required for Bluetooth discovery) -->
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />

<!-- Network -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
<uses-permission android:name="android.permission.CHANGE_WIFI_MULTICAST_STATE" />
```

## Quick Start Guide

### 1. Import the modules

```typescript
import {
  EpsonPrinters,
  RongtaPrinters,
  StarMicronicsPrinters,
  type PrinterConnectionType,
  type EpsonPrinterInfo,
  type RongtaPrinterInfo,
  type StarMicronicsPrinterInfo,
} from "expo-printers-sdk";
```

### 2. Request permissions (Android)

```typescript
import { PermissionsAndroid, Platform } from "react-native";

const requestPermissions = async () => {
  if (Platform.OS === "android") {
    const permissions = [
      PermissionsAndroid.PERMISSIONS.ACCESS_FINE_LOCATION,
      PermissionsAndroid.PERMISSIONS.BLUETOOTH_SCAN,
      PermissionsAndroid.PERMISSIONS.BLUETOOTH_CONNECT,
    ];

    const results = await PermissionsAndroid.requestMultiple(permissions);
    return Object.values(results).every(
      (result) => result === PermissionsAndroid.RESULTS.GRANTED,
    );
  }
  return true;
};
```

### 3. Discover printers (Auto-Discovery)

```typescript
import { useEffect, useState } from "react";

const [printers, setPrinters] = useState<EpsonPrinterInfo[]>([]);

useEffect(() => {
  // Listen for discovered printers
  const listener = EpsonPrinters.addListener("onPrintersFound", (data) => {
    console.log("Found printers:", data.printers);
    setPrinters(data.printers);
  });

  return () => listener.remove();
}, []);

// Start discovery
const discoverPrinters = async () => {
  const hasPermissions = await requestPermissions();
  if (hasPermissions) {
    await EpsonPrinters.findPrinters("Network");
  }
};
```

### 3b. Manual Connection (NEW!)

If you already know your printer's IP address and port, you can connect directly without discovery:

```typescript
// Rongta
const connectToPrinter = async () => {
  try {
    const printer = await RongtaPrinters.connectManually("Network", {
      ipAddress: "192.168.1.100",
      port: 9100,
    });
    console.log("Connected to printer:", printer);
  } catch (error) {
    console.error("Failed to connect:", error);
  }
};

// Epson & Star require a model identifier so the SDK can select capabilities
const epsonPrinter = await EpsonPrinters.connectManually("Network", {
  ipAddress: "192.168.1.101",
  port: 9100,
  modelName: "TM-m30II",
});
const starPrinter = await StarMicronicsPrinters.connectManually("Network", {
  ipAddress: "192.168.1.102",
  port: 9100,
  modelName: "mC-Print3",
});
```

**When to use manual connection:**

- You know the printer's IP address and port
- The printer is on a different subnet
- The printer doesn't respond to network discovery
- You want faster connection without scanning
- For **Epson** and **Star Micronics**, supply an exact supported model name (e.g. `TM-m30II`, `mC-Print3`) so the SDK can resolve capabilities; call `getSupportedModels()` to list valid values

### 4. Print images

```typescript
import { Alert } from "react-native";

// Function to fetch and convert image to base64
const fetchImageAsBase64 = async (imageUrl: string): Promise<string> => {
  const response = await fetch(imageUrl);
  if (!response.ok) {
    throw new Error(`Failed to fetch image: ${response.statusText}`);
  }

  const blob = await response.blob();
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => {
      const result = reader.result as string;
      const base64Data = result.split(",")[1] || "";
      resolve(base64Data);
    };
    reader.onerror = reject;
    reader.readAsDataURL(blob);
  });
};

// Print function with error handling
const printImage = async (printer: EpsonPrinterInfo, imageUrl: string) => {
  try {
    // A result that arrives late belongs to an earlier job, so match the job ID
    const jobId = `receipt-${Date.now()}`;

    // Listen for print results
    const printListener = EpsonPrinters.addListener(
      "onPrintImage",
      (result) => {
        if (result.jobId !== jobId) return;
        if (result.success) {
          Alert.alert("Success", "Image printed successfully!");
        } else {
          Alert.alert("Error", result.error || "Print failed");
        }
        printListener.remove();
      },
    );

    // Fetch and convert image
    const base64Image = await fetchImageAsBase64(imageUrl);

    // Initiate printing
    const success = await EpsonPrinters.printImage(base64Image, printer, {
      jobId,
    });
    if (!success) {
      Alert.alert("Error", "Failed to initiate print job");
    }
  } catch (error) {
    console.error("Print error:", error);
    Alert.alert("Error", "Failed to print image");
  }
};
```

## Complete Example

Check out our comprehensive example app in the `/example` folder that demonstrates:

- **Multi-manufacturer printer discovery**
- **Manual IP/port connection**
- **Real image fetching and printing**
- **Compact, modern UI**
- **Comprehensive error handling**
- **Modern React Native best practices**

### Running the Example

```bash
cd example
bun install
bun run android
```

## API Reference

### Connection Types

```typescript
type PrinterConnectionType = "Bluetooth" | "Network" | "USB";
```

### Paper Width

`printImage` scales the image to the printable width of the printer. The width comes from, in this order:

1. `options.paperWidth`, when the app sets it.
2. The paper width that the printer reports. Only the Epson TM-m30, TM-m30II, TM-m30II-H, TM-T20II, TM-T88VI, TM-P20 and TM-U220-i report it.
3. The standard paper of the model (80 mm for most models). Rongta printers cannot report their model, so they use 80 mm.

Set `paperWidth` for printers that have 58 mm paper and cannot report it, for example a 58 mm Rongta or a Star TSP100IV with the 58 mm paper guide.

```typescript
type PaperWidth = 58 | 80;

type PrintOptions = {
  paperWidth?: PaperWidth;
  jobId?: string; // Returned in the onPrintImage result and written in the native logs
};

await RongtaPrinters.printImage(base64Image, printer, { paperWidth: 58 });
```

### Printer Status

`getStatus` connects to the printer, reads its status, and disconnects. Use it before you print, for example to show "Paper is empty" to the user.

```typescript
type PaperStatus = "OK" | "NEAR_END" | "EMPTY" | "UNKNOWN";

type PrinterError =
  | "CUTTER_ERROR"
  | "MECHANICAL_ERROR"
  | "PAPER_JAM"
  | "OVER_TEMPERATURE"
  | "VOLTAGE_ERROR"
  | "AUTO_RECOVERABLE_ERROR"
  | "UNRECOVERABLE_ERROR"
  | "UNKNOWN_ERROR"; // The printer reports an error, but not which kind.

type PrinterStatus = {
  online: boolean; // The printer reports that it is ready to print.
  coverOpen: boolean;
  paper: PaperStatus;
  errors: PrinterError[]; // Empty when the printer reports no error.
};

try {
  const status = await EpsonPrinters.getStatus(printer);
  if (!status.online) {
    // Show status.coverOpen, status.paper and status.errors to the user.
  }
} catch (error) {
  // error.code: "ERR_PRINTER_UNREACHABLE", "ERR_PRINTER_BUSY" or "ERR_PRINTER_STATUS_UNAVAILABLE"
}
```

- Epson uses `Printer.getStatus` of the ePOS SDK.
- Star uses `StarIOPort.retreiveStatus`.
- Rongta sends the ESC/POS real-time status requests `DLE EOT 1-4`. A printer that does not answer them gives `ERR_PRINTER_STATUS_UNAVAILABLE`.

### Timeouts and Retries

Every blocking call to a printer SDK has a time limit. If a call hangs, for example in the Android Bluetooth stack, the print ends with a timeout error. The stuck call stays on its own thread, and a connection that it opens later is closed.

| Step                        | Epson | Star | Rongta                      |
| --------------------------- | ----- | ---- | --------------------------- |
| Connect or open the port    | 20 s  | 15 s | 30 s (10 s for `getStatus`) |
| Printer status before print | -     | 10 s | -                           |
| Send the data               | 20 s  | 30 s | 30 s                        |
| Print result                | -     | 35 s | 60 s                        |
| Disconnect or release       | 5 s   | 5 s  | 5 s                         |

- Star over Bluetooth: if the port does not open or the status check before the print fails, the print starts again once on a new connection after 1 second. There is no retry after print data was sent, so a receipt is not printed twice.
- A print sends its `onPrintImage` result within about 2 minutes after it starts. Rongta prints one job at a time, so a Rongta job can first wait for the job before it.
- Timeouts give the error code `CONNECTION_TIMEOUT` or `PRINT_TIMEOUT`.
- Set `jobId` in `PrintOptions` and compare it with `result.jobId`, so a late result is not counted for the next job.
- The native logs show every step with its duration, for example `jobId=receipt-1 step=open_port outcome=ok stepMs=812 jobMs=830`. The log tags are `EpsonPrinter`, `StarMicronicsPrinter` and `RongtaPrinter`.

### Print Results and Native Logs

Every `onPrintImage` result has a stable `errorCode`, the native `cause` of a failure and the `steps` of the print, so the app can send them to its own logging or analytics.

```typescript
type PrintStep = { step: string; outcome: string; durationMs: number };

type PrintFailureCause = {
  step: string; // The step that failed, for example "send_data"
  type: string; // The exception type, for example "IOException"
  message: string | null;
  vendorCode: number | null; // Epson errorStatus or Star errorCode
};

// Fields of EpsonPrintResult, StarMicronicsPrintResult and RongtaPrintResult
type PrintResult = {
  jobId: string | null;
  success: boolean;
  errorCode: string | null; // See the error code type of each vendor
  error: string | null;
  cause: PrintFailureCause | null;
  steps: PrintStep[];
};
```

The `onLog` event sends every native log line of a vendor to the app, also during discovery and status checks. The same lines go to logcat.

```typescript
type PrinterLogEvent = {
  level: "debug" | "info" | "warn" | "error";
  tag: string; // For example "StarMicronicsPrinter"
  message: string;
  timestamp: number; // Milliseconds since the Unix epoch
  error: { type: string; message: string | null } | null;
  jobId?: string | null; // Set for the step lines of a print or status check
  step?: string;
  outcome?: string;
  durationMs?: number;
  jobDurationMs?: number;
};

const subscription = StarMicronicsPrinters.addListener("onLog", (event) => {
  if (event.level === "warn" || event.level === "error" || event.step) {
    sendToAnalytics(event);
  }
});
```

### Epson Printers

#### Types

```typescript
type EpsonPrinterInfo = {
  deviceName: string; // e.g., "TM-T88V"
  target: string; // Connection target (BT:MAC or TCP:IP)
  ipAddress: string; // IP address for network printers
  macAddress: string; // MAC address for Bluetooth
  bdAddress: string; // Bluetooth device address
  connectionType: PrinterConnectionType;
  deviceType: number; // Epson device type identifier
};

type EpsonPrintErrorCode =
  | "INVALID_IMAGE"
  | "UNSUPPORTED_MODEL"
  | "PRINTER_OFFLINE"
  | "PRINT_TIMEOUT"
  | "UNKNOWN_ERROR"
  | "NOT_INITIALIZED"
  | "INVALID_REQUEST";

type EpsonPrintResult = {
  jobId: string | null; // jobId from PrintOptions
  success: boolean;
  errorCode: EpsonPrintErrorCode | null;
  error: string | null;
  cause: PrintFailureCause | null;
  steps: PrintStep[];
};

type EpsonPrintersModuleEvents = {
  onPrintersFound: (data: { printers: EpsonPrinterInfo[] }) => void;
  onPrintImage: (result: EpsonPrintResult) => void;
};
```

#### Methods

```typescript
// Discover available printers
EpsonPrinters.findPrinters(connectionType: PrinterConnectionType): Promise<boolean>

// Connect manually to a network printer
EpsonPrinters.connectManually(
  connectionType: PrinterConnectionType,
  connectionDetails: { ipAddress: string; port?: number; modelName?: string }
): Promise<EpsonPrinterInfo>

// Get list of supported printer models
EpsonPrinters.getSupportedModels(): Promise<string[]>

// Print base64 image to printer
EpsonPrinters.printImage(base64Image: string, deviceData: EpsonPrinterInfo, options?: PrintOptions): Promise<boolean>

// Read printer status (see Printer Status)
EpsonPrinters.getStatus(deviceData: EpsonPrinterInfo): Promise<PrinterStatus>

// Event listener management
EpsonPrinters.addListener(eventName: string, listener: Function): EventSubscription
```

### Rongta Printers

#### Types

```typescript
type RongtaPrinterInfo = {
  connectionType: PrinterConnectionType;
  type: RongtaPrinterType;
  isSupported: boolean;
  unsupportedReason?: string;
};

type RongtaPrinterType =
  | {
      type: "BLUETOOTH";
      alias: string; // Display name
      name: string; // Device name
      address: string; // Bluetooth MAC address
    }
  | {
      type: "NETWORK";
      ipAddress: string; // IP address
      port: number; // TCP port (default 9100)
    }
  | {
      type: "USB";
      name: string; // Device name
      vendorId: number; // USB vendor ID
      productId: number; // USB product ID
    };

type RongtaPrintErrorCode =
  | "INVALID_IMAGE"
  | "CONNECTION_FAILED"
  | "PERMISSION_DENIED"
  | "CONNECTION_TIMEOUT"
  | "PRINT_FAILED"
  | "PRINT_TIMEOUT"
  | "UNKNOWN_ERROR"
  | "NOT_INITIALIZED"
  | "INVALID_REQUEST";

type RongtaPrintResult = {
  jobId: string | null; // jobId from PrintOptions
  success: boolean;
  errorCode: RongtaPrintErrorCode | null;
  error: string | null;
  cause: PrintFailureCause | null;
  steps: PrintStep[];
};
```

#### Methods

```typescript
// Discover available printers
RongtaPrinters.findPrinters(connectionType: PrinterConnectionType): Promise<boolean>

// Connect manually to a network printer
RongtaPrinters.connectManually(
  connectionType: PrinterConnectionType,
  connectionDetails: { ipAddress: string; port?: number }
): Promise<RongtaPrinterInfo>

// Print base64 image to printer
RongtaPrinters.printImage(base64Image: string, deviceData: RongtaPrinterInfo, options?: PrintOptions): Promise<boolean>

// Read printer status (see Printer Status)
RongtaPrinters.getStatus(deviceData: RongtaPrinterInfo): Promise<PrinterStatus>
```

### Star Micronics Printers

#### Types

```typescript
type StarMicronicsPrinterInfo = {
  deviceName: string; // e.g., "TSP100"
  portName: string; // Port identifier (TCP:IP or BT:MAC)
  macAddress: string; // MAC address
  usbSerialNumber: string; // USB serial (if applicable)
  connectionType: PrinterConnectionType;
};

type StarMicronicsPrintResult = {
  jobId: string | null; // jobId from PrintOptions
  success: boolean;
  errorCode: StarMicronicsPrintErrorCode | null;
  error: string | null;
  cause: PrintFailureCause | null;
  steps: PrintStep[];
};
```

#### Methods

```typescript
// Discover available printers
StarMicronicsPrinters.findPrinters(connectionType: PrinterConnectionType): Promise<boolean>

// Connect manually to a network printer
StarMicronicsPrinters.connectManually(
  connectionType: PrinterConnectionType,
  connectionDetails: { ipAddress: string; port?: number; modelName: string }
): Promise<StarMicronicsPrinterInfo>

// Get list of supported printer models
StarMicronicsPrinters.getSupportedModels(): Promise<string[]>

// Print base64 image to printer
StarMicronicsPrinters.printImage(base64Image: string, deviceData: StarMicronicsPrinterInfo, options?: PrintOptions): Promise<boolean>

// Read printer status (see Printer Status)
StarMicronicsPrinters.getStatus(deviceData: StarMicronicsPrinterInfo): Promise<PrinterStatus>
```

## Error Handling Best Practices

The SDK includes comprehensive error handling with specific error messages:

```typescript
try {
  const success = await EpsonPrinters.findPrinters("Bluetooth");
  if (!success) {
    console.log("No printers found or search failed");
  }
} catch (error) {
  if (error.message.includes("Invalid connection type")) {
    // Handle invalid connection type
  } else if (error.message.includes("Permission")) {
    // Handle permission errors
  } else {
    // Handle other errors
  }
}
```

### Common Error Scenarios

- **Permission Denied**: Request Bluetooth/Location permissions
- **Network Unavailable**: Check WiFi connection for network printers
- **Printer Offline**: Ensure printer is powered on and discoverable
- **Invalid Image**: Verify base64 image format and size
- **Connection Failed**: Check printer pairing or network settings

## Troubleshooting

### Bluetooth Issues

- Ensure location permissions are granted
- Check if Bluetooth is enabled
- Make sure printer is in pairing mode
- Clear Bluetooth cache if needed

### Network Issues

- **Auto-Discovery not finding printers?**
  - Ensure printer and device are on same subnet
  - Check if printer supports network discovery
  - Try using `connectManually()` with known IP address
  - Verify multicast is enabled on your network

- **Manual connection issues:**
  - Verify printer IP address is correct (use `ping` to test)
  - Check port number (default is 9100 for most printers)
  - Ensure printer is powered on and connected to network
  - Test printer via web interface if available

### Rongta Network Discovery

Rongta network discovery requires:

- Device and printer on the **same subnet** (e.g., both on 192.168.1.x)
- WiFi **multicast** enabled (automatically handled by SDK)
- Printer must respond to UDP broadcast on port 1460

**If auto-discovery doesn't work:**

1. Use `connectManually()` with the printer's IP address
2. Verify printer is network-accessible: `ping <printer-ip>`
3. Check printer port: `telnet <printer-ip> 9100`

### Image Printing Issues

- Verify image is valid base64 format
- If the right side of the receipt is cut off, set `paperWidth` to the paper in the printer (see [Paper Width](#paper-width))
- Ensure image file size is reasonable
- Test with simple black and white images first

## Changelog

See [CHANGELOG.md](CHANGELOG.md).

## Contributing

Contributions are welcome. Open an issue or a pull request.

### Development Setup

```bash
git clone https://github.com/wiedymi/expo-printers-sdk.git
cd expo-printers-sdk
bun install

# Run the example
cd example
bun install
bun run android
```

## License

The source code is licensed under the [MIT License](LICENSE).

This package also contains the Epson ePOS SDK, the Rongta printer library and android-serialport-api, and the app build downloads the Star Micronics SDK. These components keep their own licenses. See [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).

---

**Built with love for the React Native community**

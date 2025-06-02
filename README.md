# expo-printers-sdk

An Expo (React Native) module for integrating thermal printers from multiple manufacturers into your Expo/React Native applications.

## Features

- Support for multiple printer manufacturers:
  - Epson
  - Rongta
  - Star Micronics
- Connect via Bluetooth, Network, or USB
- Discover available printers
- Print images to connected printers
- Event-based communication for printer discovery and print results

## Installation

#### Add the package to your dependencies

```bash
bunx expo install expo-printers-sdk
```

## In bare React Native projects

For bare React Native projects, you must ensure that you have [installed and configured the `expo` package](https://docs.expo.dev/bare/installing-expo-modules/) before continuing.

## Platform Support
- [x] Android
- [ ] iOS

Note: iOS is not supported yet.

## Configure for Android

No additional configuration is needed for Android.

## Usage

### Importing the modules

```typescript
import {
  EpsonPrinters,
  RongtaPrinters,
  StarMicronicsPrinters,
  PrinterConnectionType
} from "expo-printers-sdk";
```

### Discovering printers

#### Epson printers

```typescript
import { EpsonPrinters, PrinterConnectionType, EpsonPrinterInfo } from 'expo-printers-sdk';

// Add event listener for when printers are found
EpsonPrinters.addListener("onPrintersFound", (data: { printers: EpsonPrinterInfo[] }) => {
  console.log("Found Epson printers:", data.printers);
  // Store or use the printers as needed
});

// Search for available printers
const searchResult = await EpsonPrinters.findPrinters("Bluetooth" as PrinterConnectionType);
```

#### Rongta printers

```typescript
import { RongtaPrinters, PrinterConnectionType, RongtaPrinterInfo } from 'expo-printers-sdk';

// Add event listener for when printers are found
RongtaPrinters.addListener("onPrintersFound", (data: { printers: RongtaPrinterInfo[] }) => {
  console.log("Found Rongta printers:", data.printers);
  // Store or use the printers as needed
});

// Search for available printers
const searchResult = await RongtaPrinters.findPrinters("Bluetooth" as PrinterConnectionType);
```

#### Star Micronics printers

```typescript
import { StarMicronicsPrinters, PrinterConnectionType, StarMicronicsPrinterInfo } from 'expo-printers-sdk';

// Add event listener for when printers are found
StarMicronicsPrinters.addListener("onPrintersFound", (data: { printers: StarMicronicsPrinterInfo[] }) => {
  console.log("Found Star Micronics printers:", data.printers);
  // Store or use the printers as needed
});

// Search for available printers
const searchResult = await StarMicronicsPrinters.findPrinters("Network" as PrinterConnectionType);
```

### Printing images

#### Epson printers

```typescript
import { EpsonPrinters, EpsonPrinterInfo, EpsonPrintResult } from 'expo-printers-sdk';

// Add event listener for print results
EpsonPrinters.addListener("onPrintImage", (result: EpsonPrintResult) => {
  if (result.success) {
    console.log("Print succeeded!");
  } else {
    console.error("Print failed:", result.error);
  }
});

// Print an image to a printer
const printerInfo: EpsonPrinterInfo = {
  deviceName: "TM-T88V",
  target: "BT:00:11:22:33:44:55",
  ipAddress: "",
  macAddress: "00:11:22:33:44:55",
  bdAddress: "00:11:22:33:44:55",
  connectionType: "Bluetooth",
  deviceType: 0
};

// Convert your image to base64
const base64Image = "..."; // your image in base64 format

const printResult = await EpsonPrinters.printImage(base64Image, printerInfo);
```

#### Rongta printers

```typescript
import { RongtaPrinters, RongtaPrinterInfo, RongtaPrintResult } from 'expo-printers-sdk';

// Add event listener for print results
RongtaPrinters.addListener("onPrintImage", (result: RongtaPrintResult) => {
  if (result.success) {
    console.log("Print succeeded!");
  } else {
    console.error("Print failed:", result.error);
  }
});

// Print an image to a Bluetooth printer
const bluetoothPrinterInfo: RongtaPrinterInfo = {
  connectionType: "Bluetooth",
  type: {
    type: "BLUETOOTH",
    alias: "Thermal Printer",
    name: "RPP300",
    address: "00:11:22:33:44:55"
  }
};

// Print an image to a Network printer
const networkPrinterInfo: RongtaPrinterInfo = {
  connectionType: "Network",
  type: {
    type: "NETWORK",
    ipAddress: "192.168.1.100",
    port: 9100
  }
};

// Convert your image to base64
const base64Image = '...'; // your image in base64 format

const printResult = await RongtaPrinters.printImage(base64Image, bluetoothPrinterInfo);
```

#### Star Micronics printers

```typescript
import { StarMicronicsPrinters, StarMicronicsPrinterInfo, StarMicronicsPrintResult } from 'expo-printers-sdk';

// Add event listener for print results
StarMicronicsPrinters.addListener("onPrintImage", (result: StarMicronicsPrintResult) => {
  if (result.success) {
    console.log("Print succeeded!");
  } else {
    console.error("Print failed:", result.error);
  }
});

// Print an image to a printer
const printerInfo: StarMicronicsPrinterInfo = {
  deviceName: "TSP100",
  portName: "TCP:192.168.1.100",
  macAddress: "00:11:22:33:44:55",
  usbSerialNumber: "",
  connectionType: "Network"
};

// Convert your image to base64
const base64Image = '...'; // your image in base64 format

const printResult = await StarMicronicsPrinters.printImage(base64Image, printerInfo);
```

## API Reference

### Connection Types

```typescript
type PrinterConnectionType = "Bluetooth" | "Network" | "USB";
```

### Epson Printers

#### Types

```typescript
type EpsonPrinterInfo = {
  deviceName: string;
  target: string;
  ipAddress: string;
  macAddress: string;
  bdAddress: string;
  connectionType: PrinterConnectionType;
  deviceType: number;
};

type EpsonPrintResult = {
  success: boolean;
  error?: string;
};
```

#### Methods

- `findPrinters(connectionType: PrinterConnectionType): Promise<boolean>`
- `printImage(base64Image: string, deviceData: EpsonPrinterInfo): Promise<boolean>`

#### Events

- `onPrintersFound`: Triggered when printers are discovered
- `onPrintImage`: Triggered when a print job completes

### Rongta Printers

#### Types

```typescript
type RongtaPrinterInfo = {
  connectionType: PrinterConnectionType;
  type: RongtaPrinterType;
};

type RongtaPrinterType = 
  | {
      type: "BLUETOOTH";
      alias: string;
      name: string;
      address: string;
    }
  | {
      type: "NETWORK";
      ipAddress: string;
      port: number;
    };

type RongtaPrintResult = {
  success: boolean;
  error?: string;
};
```

#### Methods

- `findPrinters(connectionType: PrinterConnectionType): Promise<boolean>`
- `printImage(base64Image: string, deviceData: RongtaPrinterInfo): Promise<boolean>`

#### Events

- `onPrintersFound`: Triggered when printers are discovered
- `onPrintImage`: Triggered when a print job completes

### Star Micronics Printers

#### Types

```typescript
type StarMicronicsPrinterInfo = {
  deviceName: string;
  portName: string;
  macAddress: string;
  usbSerialNumber: string;
  connectionType: PrinterConnectionType;
};

type StarMicronicsPrintResult = {
  success: boolean;
  error?: string;
};
```

#### Methods

- `findPrinters(connectionType: PrinterConnectionType): Promise<boolean>`
- `printImage(base64Image: string, deviceData: StarMicronicsPrinterInfo): Promise<boolean>`

#### Events

- `onPrintersFound`: Triggered when printers are discovered
- `onPrintImage`: Triggered when a print job completes

## Permission Requirements

For Android, your app will need the following permissions in the `AndroidManifest.xml`:

- For Bluetooth: `BLUETOOTH`, `BLUETOOTH_ADMIN`, `BLUETOOTH_CONNECT` (for Android 12+)
- For Network: `INTERNET`, `ACCESS_NETWORK_STATE`
- For USB: `USB_PERMISSION`

Make sure to request these permissions at runtime as needed.

## Contributing

Contributions are very welcome!

## License

MIT
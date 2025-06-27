# expo-printers-sdk

A robust Expo (React Native) module for integrating thermal printers from multiple manufacturers into your Expo/React Native applications. Built with enterprise-grade error handling and production-ready reliability.

## ✨ Features

- **Multi-Manufacturer Support**:

  - ✅ **Epson** (TM series, compatible models)
  - ✅ **Rongta** (RPP series, Bluetooth & Network)
  - ✅ **Star Micronics** (TSP series, mPOP, etc.)

- **Multiple Connection Types**:

  - 🔵 **Bluetooth** - Wireless printing
  - 🌐 **Network** - TCP/IP connection
  - 🔌 **USB** - Direct USB connection

- **Enterprise Features**:
  - 🔍 **Smart Discovery** - Automatic printer detection
  - 🖼️ **Image Printing** - High-quality thermal printing
  - ⚡ **Event-Driven** - Real-time status updates
  - 🛡️ **Robust Error Handling** - Production-ready reliability
  - 📱 **Full Example App** - Complete implementation reference

## 🚀 Installation

#### Add the package to your dependencies

```bash
bunx expo install expo-printers-sdk
```

## In bare React Native projects

For bare React Native projects, you must ensure that you have [installed and configured the `expo` package](https://docs.expo.dev/bare/installing-expo-modules/) before continuing.

## 📱 Platform Support

- ✅ **Android** - Fully supported with native printer SDKs
- ❌ **iOS** - Coming soon

## 🔧 Setup & Configuration

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
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

## 📖 Quick Start Guide

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
      (result) => result === PermissionsAndroid.RESULTS.GRANTED
    );
  }
  return true;
};
```

### 3. Discover printers

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
    // Listen for print results
    const printListener = EpsonPrinters.addListener(
      "onPrintImage",
      (result) => {
        if (result.success) {
          Alert.alert("Success", "Image printed successfully!");
        } else {
          Alert.alert("Error", result.error || "Print failed");
        }
        printListener.remove();
      }
    );

    // Fetch and convert image
    const base64Image = await fetchImageAsBase64(imageUrl);

    // Initiate printing
    const success = await EpsonPrinters.printImage(base64Image, printer);
    if (!success) {
      Alert.alert("Error", "Failed to initiate print job");
    }
  } catch (error) {
    console.error("Print error:", error);
    Alert.alert("Error", "Failed to print image");
  }
};
```

## 🎯 Complete Example

Check out our comprehensive example app in the `/example` folder that demonstrates:

- 🔍 **Multi-manufacturer printer discovery**
- 🖼️ **Real image fetching and printing**
- 📱 **Professional UI with loading states**
- ⚠️ **Comprehensive error handling**
- 🎨 **Modern React Native best practices**

### Running the Example

```bash
cd example
npm install
npm run android
```

## 📚 API Reference

### Connection Types

```typescript
type PrinterConnectionType = "Bluetooth" | "Network" | "USB";
```

### 🖨️ Epson Printers

#### Types

```typescript
type EpsonPrinterInfo = {
  deviceName: string; // e.g., "TM-T88V"
  target: string; // Connection target (BT:MAC or IP:PORT)
  ipAddress: string; // IP address for network printers
  macAddress: string; // MAC address for Bluetooth
  bdAddress: string; // Bluetooth device address
  connectionType: PrinterConnectionType;
  deviceType: number; // Epson device type identifier
};

type EpsonPrintResult = {
  success: boolean;
  error?: string; // Detailed error message if failed
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

// Print base64 image to printer
EpsonPrinters.printImage(base64Image: string, deviceData: EpsonPrinterInfo): Promise<boolean>

// Event listener management
EpsonPrinters.addListener(eventName: string, listener: Function): EventSubscription
```

### 🖨️ Rongta Printers

#### Types

```typescript
type RongtaPrinterInfo = {
  connectionType: PrinterConnectionType;
  type: RongtaPrinterType;
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
      port: number; // TCP port (usually 9100)
    };

type RongtaPrintResult = {
  success: boolean;
  error?: string;
};
```

#### Methods

```typescript
// Discover available printers
RongtaPrinters.findPrinters(connectionType: PrinterConnectionType): Promise<boolean>

// Print base64 image to printer
RongtaPrinters.printImage(base64Image: string, deviceData: RongtaPrinterInfo): Promise<boolean>
```

### 🖨️ Star Micronics Printers

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
  success: boolean;
  error?: string;
};
```

#### Methods

```typescript
// Discover available printers
StarMicronicsPrinters.findPrinters(connectionType: PrinterConnectionType): Promise<boolean>

// Print base64 image to printer
StarMicronicsPrinters.printImage(base64Image: string, deviceData: StarMicronicsPrinterInfo): Promise<boolean>
```

## 🛡️ Error Handling Best Practices

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

## 🔧 Troubleshooting

### Bluetooth Issues

- Ensure location permissions are granted
- Check if Bluetooth is enabled
- Make sure printer is in pairing mode
- Clear Bluetooth cache if needed

### Network Issues

- Verify printer IP address and port
- Check network connectivity
- Ensure printer and device are on same network
- Test printer via web interface if available

### Image Printing Issues

- Verify image is valid base64 format
- Check image dimensions (thermal printers have width limits)
- Ensure image file size is reasonable
- Test with simple black and white images first

## 🤝 Contributing

We welcome contributions! Please see our [Contributing Guide](CONTRIBUTING.md) for details.

### Development Setup

```bash
git clone https://github.com/your-org/expo-printers-sdk.git
cd expo-printers-sdk
npm install

# Run the example
cd example
npm install
npm run android
```

## 📋 Changelog

### Latest Version

- ✅ **Enhanced Error Handling** - Comprehensive error messages and recovery
- ✅ **Image Fetching** - Built-in URL to base64 conversion
- ✅ **Type Safety** - Full TypeScript support with strict typing
- ✅ **Production Ready** - Extensive testing and validation
- ✅ **Modern Example** - Complete demo app with best practices


**Built with ❤️ for the React Native community**

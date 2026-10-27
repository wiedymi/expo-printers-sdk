# Changelog

All notable changes to this project are documented in this file.
The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and the project uses [Semantic Versioning](https://semver.org/).

## [Unreleased]

### Added

- `getStatus(deviceData)` on all printer modules. It returns `online`, `coverOpen`, `paper` (`OK`, `NEAR_END`, `EMPTY`, `UNKNOWN`) and `errors`. It rejects with `ERR_PRINTER_UNREACHABLE`, `ERR_PRINTER_BUSY` or `ERR_PRINTER_STATUS_UNAVAILABLE` when it cannot read the status.
- Rongta status through the ESC/POS real-time status requests `DLE EOT 1-4`.
- Optional `paperWidth` print option (`58` or `80`) on `printImage`, for printers that cannot report their paper width.
- Epson paper width detection with `getPrinterSetting` on the TM-m30, TM-m30II, TM-m30II-H, TM-T20II, TM-T88VI, TM-P20 and TM-U220-i.
- "Status" button in the example app.

### Changed

- Images are scaled to the printable width of each printer model. Epson used a fixed width of 560 dots, which is wider than the print area of the TM-T88 family (512 dots), the TM-U220 (192 dots) and 58 mm paper.
- Star and Rongta images are scaled by the vendor SDK.
- Upgraded the development and example app dependencies to Expo SDK 58, React 19.3 and React Native 0.88. Dependency versions are pinned.
- Upgraded Star `stario` to 2.14.2, `starioextension` to 1.17.0 and `androidx.core:core-ktx` to 1.18.0.
- The Android build uses `expo-module-gradle-plugin`. The library uses the `minSdkVersion` of the app.
- Removed `expo-module-scripts`. The package builds with `tsc`.
- Bun is the package manager. `bun fmt` and `bun lint` replace Prettier and ESLint.

### Fixed

- The Star BSC10II uses a paper width of 576 dots (was 512).
- An Epson model name matches its longest known name, so "TM-m50II-H" no longer selects the TM-m50.
- The Epson printer disconnects after a failed print.

## [0.9.8] - 2026-07-22

### Added

- Stable `errorCode` values in Star print results for connection, status and hardware failures.

### Fixed

- Star network connection and print-completion timeouts.
- Port names of manually connected Star network printers.
- Star printer ports are released after a failed print.

## [0.9.7] - 2025-12-15

### Changed

- The `react` peer dependency is `>=18.0.0`.
- Package description and keywords.

### Fixed

- The Rongta Bluetooth scanner no longer blocks a thread for each found device (`trySend` replaces `runBlocking`).

## [0.9.6] - 2025-11-25

### Added

- `connectManually(connectionType, connectionDetails)` on all printer modules, to connect to a network printer by IP address and port without discovery.
- `getSupportedModels()` on the Epson and Star modules.
- `isSupported` and `unsupportedReason` on discovered printers.
- Input validation for manual connection parameters.
- Multicast lock for UDP broadcast discovery.
- More logging for discovery and connection.
- Rongta connection timeout result.

### Fixed

- Rongta network scanner startup.
- Crashes during print configuration.
- Connection state checks before a printer reconnects.
- Star Micronics discovery and printing.

### Changed

- New example app UI with auto-discovery, manual connection and a log viewer.

## [0.9.5] - 2025-10-16

### Added

- Rongta serial port library `libserial_port.so` with 16 KB page size support for Android 15 and later.

## [0.9.4] - 2025-10-15

### Changed

- Epson ePOS SDK 2.33.2, with 16 KB page size support for Android 15 and later.

## Earlier versions

- Error messages for print failures.
- Image fetching from a URL to base64 in the example app.
- TypeScript types for all modules.
- Example app.

[Unreleased]: https://github.com/wiedymi/expo-printers-sdk/compare/01fd498...main
[0.9.8]: https://www.npmjs.com/package/expo-printers-sdk/v/0.9.8
[0.9.7]: https://www.npmjs.com/package/expo-printers-sdk/v/0.9.7
[0.9.6]: https://www.npmjs.com/package/expo-printers-sdk/v/0.9.6
[0.9.5]: https://www.npmjs.com/package/expo-printers-sdk/v/0.9.5
[0.9.4]: https://www.npmjs.com/package/expo-printers-sdk/v/0.9.4

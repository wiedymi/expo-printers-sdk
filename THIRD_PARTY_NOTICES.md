# Third-Party Notices

The source code of expo-printers-sdk is licensed under the MIT License (see [LICENSE](LICENSE)).
The MIT License does not cover the third-party components below. Each component keeps its own license.
The full license texts are in the [licenses](licenses) folder.

## Components included in this package

These files are in this repository and in the npm package.

### Epson ePOS SDK for Android 2.33.2

- Files: `android/libs/ePOS2.jar`, `android/src/main/jniLibs/*/libepos2.so`
- Copyright: Seiko Epson Corporation
- License: Epson End User Software License Agreement, see [licenses/EPSON-EULA.en.txt](licenses/EPSON-EULA.en.txt)
- Section 2.2 of the agreement requires that the agreement is distributed with the software. The agreement permits use only with Epson hardware (section 2.1) and does not permit reverse engineering (section 2.5).

`libepos2.so` contains these open-source components:

- **OpenSSL 1.0.2u**: OpenSSL License and Original SSLeay License, see [licenses/OpenSSL-1.0.2.txt](licenses/OpenSSL-1.0.2.txt)

  This product includes software developed by the OpenSSL Project for use in the OpenSSL Toolkit (http://www.openssl.org/).

  This product includes cryptographic software written by Eric Young (eay@cryptsoft.com).

  This product includes software written by Tim Hudson (tjh@cryptsoft.com).

- **zlib 1.2.8**: Copyright (C) 1995-2013 Jean-loup Gailly and Mark Adler, zlib License, see [licenses/zlib.txt](licenses/zlib.txt)

### Rongta printer library

- File: `android/libs/rongta_library.jar` (package `com.rt.printerlibrary`)
- Copyright: Rongta Technology (Xiamen) Co., Ltd.
- License: proprietary. Rongta supplies the library in its Android SDK (https://www.rongtatech.com/sdk/) and does not publish license terms for it.

`rongta_library.jar` contains these open-source components:

- **FastBle** (package `com.clj.fastble`): Copyright 2016 chenlijian, Apache License 2.0, see [licenses/Apache-2.0.txt](licenses/Apache-2.0.txt). Source: https://github.com/Jasonchenlijian/FastBle
- **android-serialport-api** (package `android_serialport_api01`): Copyright 2009-2011 Cedric Priscal, Apache License 2.0, see [licenses/Apache-2.0.txt](licenses/Apache-2.0.txt). Source: https://github.com/cepr/android-serialport-api

### android-serialport-api native library

- Files: `android/src/main/jniLibs/*/libserial_port.so`
- Copyright: 2009-2011 Cedric Priscal
- License: Apache License 2.0, see [licenses/Apache-2.0.txt](licenses/Apache-2.0.txt)
- Source: https://github.com/cepr/android-serialport-api (`jni/SerialPort.c`), built with 16 KB page alignment

## Dependencies downloaded by the app build

These libraries are not in this package. The Android build of the app downloads them from Maven repositories, and they are part of the app.

| Library                                                 | Version      | License                                                                                                                           |
| ------------------------------------------------------- | ------------ | --------------------------------------------------------------------------------------------------------------------------------- |
| `com.starmicronics:stario`                              | 2.14.2       | [Star Micronics Software License Agreement](https://www.starmicronics.com/support/Mannualfolder/SoftwareLicenseAgreement_web.pdf) |
| `com.starmicronics:starioextension`                     | 1.17.0       | [Star Micronics Software License Agreement](https://www.starmicronics.com/support/Mannualfolder/SoftwareLicenseAgreement_web.pdf) |
| `androidx.core:core-ktx`                                | 1.18.0       | Apache License 2.0                                                                                                                |
| `androidx.appcompat:appcompat` (from `starioextension`) | 1.6.1        | Apache License 2.0                                                                                                                |
| `com.google.zxing:core` (from `starioextension`)        | 3.2.1        | Apache License 2.0                                                                                                                |
| `org.jetbrains.kotlin:kotlin-stdlib`                    | from the app | Apache License 2.0                                                                                                                |

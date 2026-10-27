# Keep rules that apps apply automatically (consumerProguardFiles in build.gradle).
# The vendor SDKs find their classes, fields and methods by name from native code and reflection,
# so R8 must not rename or remove them. The Epson rules are the ones in the ePOS SDK documentation.

-keep class com.epson.** { *; }
-dontwarn com.epson.**

-keep class com.starmicronics.** { *; }
-dontwarn com.starmicronics.**

-keep class com.rt.printerlibrary.** { *; }
-dontwarn com.rt.printerlibrary.**

-keep class com.clj.fastble.** { *; }
-dontwarn com.clj.fastble.**

-keep class android_serialport_api01.** { *; }
-dontwarn android_serialport_api01.**

# Volume Widget

A native Android application providing a persistent floating volume control widget to replace broken physical volume buttons.

## Features
- Draggable floating button that persists across reboots.
- Expands to a vertical volume slider when tapped.
- Material Design 3 UI with blue accents.
- Supports Android 8.0 (Oreo) and above.

## Build Instructions (Command Line / Termux)

Since this project uses the standard Gradle build system, you can build it on any system with Java/Gradle installed, or directly on an Android device using Termux.

### Prerequisites (Termux)
1. Install Termux from F-Droid.
2. Install necessary packages:
   ```bash
   pkg update
   pkg install openjdk-17 gradle
   ```
3. Building an Android app directly in Termux can sometimes require specialized setups for `aapt2` and `android.jar`. If native Termux building fails, you may need a minimal PC environment or an IDE-like app (e.g., AIDE or Termux:Arch).

### Prerequisites (PC/Mac/Linux)
1. Install Java JDK 17.
2. Ensure you have the Android SDK installed and `ANDROID_HOME` or `ANDROID_SDK_ROOT` configured in your environment variables.

### Building
Open a terminal in the project root and run:
#### Windows:
```cmd
gradlew.bat assembleDebug
```
#### Linux/macOS/Termux:
```bash
chmod +x gradlew
./gradlew assembleDebug
```

_(Note: A Gradle Wrapper `gradlew` should typically be initialized. If the wrapper files are missing from your directory, simply run `gradle wrapper` first if you have a local Gradle installation.)_

### Installation
The output APK will be located at:
```
app/build/outputs/apk/debug/app-debug.apk
```
Install it via adb:
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Usage
1. Open the app from your launcher.
2. It will prompt you for the **"Display over other apps"** permission.
3. Find "Volume Widget" in the list and enable it.
4. The floating widget will appear on your screen.
5. Drag it around to reposition it, or tap it to open the volume slider.
6. The widget will automatically restart when you reboot the device.

## Permissions Requested
- `SYSTEM_ALERT_WINDOW`: To draw the floating widget over other applications.
- `FOREGROUND_SERVICE` & `FOREGROUND_SERVICE_SPECIAL_USE`: To keep the widget running reliably in the background without being killed by the Android system.
- `RECEIVE_BOOT_COMPLETED`: To automatically start the service on device reboot to remember your widget state.

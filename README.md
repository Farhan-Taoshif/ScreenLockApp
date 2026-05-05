# ScreenLockApp

A sophisticated Android application that provides advanced screen locking functionality with automatic triggers and a persistent floating interface. The app enables users to lock their device screen using an overlay-based system with automatic activation on device boot and power connection events.

## Features

### Core Functionality

- **Floating Lock Button**: A draggable, persistent floating button that remains accessible over other applications, allowing users to manually toggle the lock state at any time.
- **Automatic Screen Lock**: The app automatically activates screen locking when the device boots or when the power adapter is connected.
- **Automatic Screen Unlock**: The app automatically deactivates screen locking when the power adapter is disconnected.
- **Notification Panel Blocking**: When the screen is locked, the notification panel is blocked to prevent unauthorized access or accidental interactions.
- **Persistent Foreground Service**: A robust background service ensures the application's locking logic remains active even when the main interface is closed.

### System Integration

- **Boot Receiver**: Automatically starts the locking service when the device boots up.
- **Charging Receiver**: Monitors power connection and disconnection events to trigger automatic locking/unlocking.
- **Overlay Permission**: Requests and manages the "Display over other apps" permission required for the floating button and blocking overlays.

## Technical Specifications

### Platform & Requirements

- **Platform**: Android
- **Minimum SDK**: 23 (Android 6.0)
- **Target SDK**: 34 (Android 14)
- **Build System**: Gradle 8.2
- **Java Version**: Java 8+

### Key Permissions

The application requires the following permissions to function:

| Permission | Purpose |
|-----------|---------|
| `SYSTEM_ALERT_WINDOW` | Display the floating overlay button on top of other applications |
| `FOREGROUND_SERVICE` | Run the locking service in the foreground |
| `FOREGROUND_SERVICE_SPECIAL_USE` | Special use case for the charging screen touch lock overlay |
| `RECEIVE_BOOT_COMPLETED` | Start the service automatically after device boot |
| `POST_NOTIFICATIONS` | Display notifications and status updates |

### Project Structure

```
ScreenLockApp/
├── app/
│   ├── src/main/
│   │   ├── java/com/screenlock/app/
│   │   │   ├── MainActivity.java              # Main activity with UI controls
│   │   │   ├── FloatingLockService.java       # Core locking service
│   │   │   ├── BootReceiver.java              # Boot event receiver
│   │   │   └── ChargingReceiver.java          # Power connection receiver
│   │   ├── res/
│   │   │   ├── layout/
│   │   │   │   ├── activity_main.xml          # Main UI layout
│   │   │   │   └── floating_lock_button.xml   # Floating button layout
│   │   │   ├── drawable/
│   │   │   │   ├── ic_lock_closed.xml         # Locked state icon
│   │   │   │   ├── ic_lock_open.xml           # Unlocked state icon
│   │   │   │   └── floating_button_bg.xml     # Button background
│   │   │   └── values/
│   │   │       ├── strings.xml                # String resources
│   │   │       └── styles.xml                 # Theme and styles
│   │   └── AndroidManifest.xml                # App manifest
│   ├── build.gradle                           # App-level build configuration
│   └── proguard-rules.pro                     # ProGuard rules
├── build.gradle                               # Project-level build configuration
├── settings.gradle                            # Gradle settings
├── gradle.properties                          # Gradle properties
├── gradlew                                    # Gradle wrapper (Linux/macOS)
├── gradlew.bat                                # Gradle wrapper (Windows)
└── .github/workflows/build.yml                # GitHub Actions CI/CD

```

## Component Overview

### MainActivity.java

The entry point of the application. It provides a user interface with two buttons to start and stop the locking service. The activity:

- Checks for the "Display over other apps" permission and requests it if necessary.
- Requests notification permission on Android 13+.
- Displays the current charging state and locking status.
- Allows users to manually start and stop the locking service.

### FloatingLockService.java

The core service that implements the locking functionality. It:

- Creates a persistent notification to keep the service alive in the foreground.
- Displays a draggable floating button over other applications.
- Manages lock and unlock states by showing/hiding overlay blocking views.
- Listens to charging events via an internal broadcast receiver.
- Automatically locks when power is connected and unlocks when disconnected.

### BootReceiver.java

A broadcast receiver that listens for device boot events. It:

- Automatically starts the `FloatingLockService` when the device boots.
- Checks for the overlay permission before starting the service.

### ChargingReceiver.java

A broadcast receiver that listens for power connection events. It:

- Starts the `FloatingLockService` when the power adapter is connected (if the service is not already running).
- Allows the service's internal receiver to handle disconnection events.

## Installation

### Prerequisites

- Android device running Android 6.0 (API 23) or higher.
- Developer Options enabled on the device.

### Installation Steps

1. **Download the APK**: Get the latest `app-debug.apk` from the [GitHub Releases](https://github.com/Farhan-Taoshif/ScreenLockApp/releases) or build it yourself.

2. **Enable Unknown Sources**:
   - Go to **Settings** → **Security** → **Unknown sources** → **Allow**.

3. **Enable Special App Access**:
   - Go to **Settings** → **Apps** → **Special app access** → **Install unknown apps** → **Browser** → **Allow**.

4. **Install the APK**:
   - Download the APK file and tap it to install.

5. **Grant Permissions**:
   - When prompted, grant the app the "Display over other apps" permission.
   - Grant notification permission if requested.

6. **Start the Service**:
   - Open the ScreenLockApp and tap the **Start** button to activate the locking service.

## Building from Source

### Prerequisites

- Java Development Kit (JDK) 17 or higher.
- Android SDK with API 34 installed.
- Gradle 8.2 or higher.

### Build Steps

1. **Clone the Repository**:
   ```bash
   git clone https://github.com/Farhan-Taoshif/ScreenLockApp.git
   cd ScreenLockApp
   ```

2. **Build the Debug APK**:
   ```bash
   ./gradlew assembleDebug
   ```

3. **Build the Release APK** (optional):
   ```bash
   ./gradlew assembleRelease
   ```

4. **Locate the APK**:
   - Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
   - Release APK: `app/build/outputs/apk/release/app-release.apk`

## Continuous Integration

This project uses **GitHub Actions** to automatically build the APK on every push to the `main` or `master` branch. The workflow:

1. Checks out the code.
2. Sets up JDK 17.
3. Configures Gradle 8.2.
4. Builds the debug APK.
5. Uploads the APK as an artifact with a 30-day retention period.

### Accessing Build Artifacts

1. Go to the [GitHub Actions tab](https://github.com/Farhan-Taoshif/ScreenLockApp/actions).
2. Select the latest successful build.
3. Download the `ScreenLock-debug-apk` artifact.

## Known Limitations

### Overlay-Based Locking

The app uses overlay-based touch blocking rather than true device screen locking. This means:

- The screen remains visible but unresponsive to touch input.
- The notification panel is blocked by an overlay, not by system-level restrictions.
- Some manufacturer-specific features (like Samsung's Knox or Pixel's security features) may interact with the overlay system.

### Notification Panel Blocking

On some devices (Samsung, Pixel), the notification panel blocking may not work perfectly due to Android limitations. This is a known limitation of the overlay-based approach.

### Background Execution

The app requires the "Display over other apps" permission and foreground service permissions to function. Some devices with aggressive battery optimization may kill the service. Users may need to:

- Add the app to the device's battery optimization whitelist.
- Disable battery saver mode while using the app.

### Device-Specific Issues

- **Samsung Devices**: May require additional permissions or battery optimization settings.
- **Pixel Devices**: May have different notification panel behavior.
- **Custom ROMs**: May have different permission handling or overlay behavior.

## Troubleshooting

### App Crashes on Startup

**Issue**: The app crashes immediately after launching.

**Solution**:
- Ensure you have granted the "Display over other apps" permission.
- Go to **Settings** → **Apps** → **Special app access** → **Display over other apps** → Enable for ScreenLockApp.

### Floating Button Not Appearing

**Issue**: The floating button doesn't appear on the screen.

**Solution**:
- Verify that the "Display over other apps" permission is granted.
- Restart the app and tap the **Start** button again.
- Restart the device.

### Automatic Locking Not Working

**Issue**: The app doesn't automatically lock when the device boots or power is connected.

**Solution**:
- Ensure the app is not in the device's battery optimization list.
- Go to **Settings** → **Battery** → **Battery optimization** → Find ScreenLockApp and select **Don't optimize**.
- Restart the device to trigger the boot receiver.

### Notification Panel Still Accessible

**Issue**: The notification panel can still be accessed when the screen is locked.

**Solution**:
- This is a known limitation on some devices due to Android's overlay system.
- The app blocks the notification panel using overlays, which may not work on all devices.

## Contributing

Contributions are welcome! If you find a bug or have a feature request, please open an issue on the [GitHub repository](https://github.com/Farhan-Taoshif/ScreenLockApp/issues).

## License

This project is provided as-is for educational and personal use. Please refer to the LICENSE file for more details.

## Support

For issues, questions, or suggestions, please visit the [GitHub Issues page](https://github.com/Farhan-Taoshif/ScreenLockApp/issues).

## Version History

### Version 2.0
- Improved floating button dragging functionality.
- Enhanced charging state detection.
- Better permission handling for Android 13+.
- Improved notification panel blocking.

### Version 1.0
- Initial release with basic screen locking functionality.
- Floating button with manual lock/unlock control.
- Automatic locking on device boot and power connection.
- Automatic unlocking on power disconnection.

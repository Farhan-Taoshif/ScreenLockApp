# ScreenLockApp - Complete Project Documentation

## Project Overview

**ScreenLockApp** is a sophisticated Android application designed to provide users with advanced screen locking functionality. The application combines manual controls via a persistent floating interface with automated triggers based on system events such as device booting or power state changes.

The app implements an overlay-based locking system that prevents touch input to the screen while displaying a visual indicator of the lock state. It includes automatic activation on device boot and power connection events, along with automatic deactivation on power disconnection.

## Architecture Overview

### Component Diagram

The ScreenLockApp consists of four main components that work together to provide the locking functionality:

| Component | Role | Lifecycle |
|-----------|------|-----------|
| **MainActivity** | User interface and permission management | Launched on app startup |
| **FloatingLockService** | Core locking engine and overlay management | Foreground service (persistent) |
| **BootReceiver** | Device boot event handler | Broadcast receiver (manifest-registered) |
| **ChargingReceiver** | Power connection event handler | Broadcast receiver (manifest-registered) |

### Data Flow

```
Device Boot / Power Connected
         ↓
BootReceiver / ChargingReceiver
         ↓
Start FloatingLockService
         ↓
Create Notification & Overlays
         ↓
Display Floating Button
         ↓
Listen for Charging Events
         ↓
Lock/Unlock Based on Power State
```

## Component Details

### 1. MainActivity.java

**Purpose**: Provides the user interface and handles initial setup and permission management.

**Key Responsibilities**:
- Display the main UI with Start/Stop buttons.
- Check and request the "Display over other apps" permission.
- Request notification permission on Android 13+.
- Display the current charging state and lock status.
- Start and stop the `FloatingLockService`.

**Key Methods**:
- `onCreate()`: Initialize the UI and check permissions.
- `startLockService()`: Start the foreground service.
- `stopLockService()`: Stop the foreground service.
- `updateStatus()`: Update the UI with current charging state.
- `onActivityResult()`: Handle permission request results.

**Permissions Checked**:
- `SYSTEM_ALERT_WINDOW`: Required to display overlays.
- `POST_NOTIFICATIONS`: Required to show notifications (Android 13+).

### 2. FloatingLockService.java

**Purpose**: The core engine of the application, managing the overlay window and state transitions.

**Key Responsibilities**:
- Create and manage the floating lock button.
- Manage lock and unlock states.
- Block the notification panel when locked.
- Listen to charging events and automatically lock/unlock.
- Maintain a persistent foreground notification.

**Key Methods**:
- `onCreate()`: Initialize the service, create notification channel, and register receivers.
- `onStartCommand()`: Handle service start commands.
- `enableLock()`: Activate the locking overlays and change the icon to "locked".
- `disableLock()`: Deactivate the locking overlays and change the icon to "unlocked".
- `onDestroy()`: Clean up resources and unregister receivers.

**Overlay System**:
The service creates multiple overlay windows:
1. **Floating Button**: The draggable lock/unlock button.
2. **Full-Screen Blocker**: Blocks all touch input to the screen when locked.
3. **Status Bar Blocker**: Blocks swipe-down gestures to prevent notification panel access.

**Charging Event Handling**:
The service registers an internal `BroadcastReceiver` to listen for:
- `ACTION_POWER_CONNECTED`: Automatically locks the screen.
- `ACTION_POWER_DISCONNECTED`: Automatically unlocks the screen.

### 3. BootReceiver.java

**Purpose**: Automatically starts the locking service when the device boots.

**Key Responsibilities**:
- Listen for device boot events.
- Check if the overlay permission is granted.
- Start the `FloatingLockService` on boot.

**Key Methods**:
- `onReceive()`: Handle boot completed events.

**Manifest Registration**:
```xml
<receiver android:name=".BootReceiver" android:enabled="true" android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.BOOT_COMPLETED" />
        <action android:name="android.intent.action.LOCKED_BOOT_COMPLETED" />
    </intent-filter>
</receiver>
```

### 4. ChargingReceiver.java

**Purpose**: Starts the locking service when the power adapter is connected.

**Key Responsibilities**:
- Listen for power connection events.
- Start the `FloatingLockService` if not already running.

**Key Methods**:
- `onReceive()`: Handle power connection events.

**Manifest Registration**:
```xml
<receiver android:name=".ChargingReceiver" android:enabled="true" android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.ACTION_POWER_CONNECTED" />
        <action android:name="android.intent.action.ACTION_POWER_DISCONNECTED" />
    </intent-filter>
</receiver>
```

## Permission Model

The application requires several permissions to function correctly:

### Required Permissions

| Permission | Purpose | Android Version |
|-----------|---------|-----------------|
| `SYSTEM_ALERT_WINDOW` | Display overlays on top of other apps | All |
| `FOREGROUND_SERVICE` | Run service in foreground | Android 8.0+ |
| `FOREGROUND_SERVICE_SPECIAL_USE` | Special use case for charging overlay | Android 12.0+ |
| `RECEIVE_BOOT_COMPLETED` | Start service on device boot | All |
| `POST_NOTIFICATIONS` | Display notifications | Android 13.0+ |

### Permission Flow

1. **On App Launch**: MainActivity checks for `SYSTEM_ALERT_WINDOW` permission.
2. **Permission Request**: If not granted, the app requests the permission via Settings.
3. **Service Start**: Once permission is granted, the user can start the service.
4. **Notification Permission**: On Android 13+, the app requests notification permission.

## Lifecycle Management

### Service Lifecycle

```
App Launch
    ↓
MainActivity Created
    ↓
User Taps "Start"
    ↓
startForegroundService() Called
    ↓
FloatingLockService.onCreate()
    ├─ Create Notification Channel
    ├─ Call startForeground()
    ├─ Register Charging Receiver
    └─ Inflate Floating Button Layout
    ↓
FloatingLockService.onStartCommand()
    ├─ Check for ACTION_STOP_SERVICE
    └─ Return START_STICKY
    ↓
Service Running (Persistent)
    ├─ Listen for Charging Events
    ├─ Update Lock State
    └─ Maintain Overlays
    ↓
User Taps "Stop" or App Closed
    ↓
FloatingLockService.onDestroy()
    ├─ Unregister Charging Receiver
    ├─ Remove Overlays
    └─ Clean Up Resources
```

### Boot Sequence

```
Device Boot
    ↓
BootReceiver.onReceive()
    ├─ Check Overlay Permission
    └─ Start FloatingLockService
    ↓
FloatingLockService Starts
    ├─ Create Overlays
    └─ Listen for Events
    ↓
Service Running (Auto-Start)
```

### Charging Sequence

```
Power Adapter Connected
    ↓
ChargingReceiver.onReceive() (if service not running)
    └─ Start FloatingLockService
    ↓
FloatingLockService Internal Receiver
    ├─ Detect ACTION_POWER_CONNECTED
    └─ Call enableLock()
    ↓
Lock Activated
    ├─ Change Icon to Locked
    ├─ Show Blocking Overlays
    └─ Block Touch Input
    ↓
Power Adapter Disconnected
    ↓
FloatingLockService Internal Receiver
    ├─ Detect ACTION_POWER_DISCONNECTED
    └─ Call disableLock()
    ↓
Lock Deactivated
    ├─ Change Icon to Unlocked
    ├─ Hide Blocking Overlays
    └─ Allow Touch Input
```

## UI Components

### Main Activity Layout (activity_main.xml)

The main activity displays:
- **Title**: "🔒 Screen Lock"
- **Start Button**: Green button to activate the locking service.
- **Stop Button**: Red button to deactivate the locking service.
- **Status Text**: Displays current charging state and lock status.

### Floating Button Layout (floating_lock_button.xml)

The floating button is a simple `FrameLayout` containing:
- **ImageView**: Displays the lock icon (locked or unlocked).
- **Size**: 56dp (standard floating action button size).
- **Icon**: Changes between `ic_lock_closed` and `ic_lock_open`.

### Icons

- **ic_lock_closed.xml**: Vector drawable representing a locked state.
- **ic_lock_open.xml**: Vector drawable representing an unlocked state.
- **floating_button_bg.xml**: Background shape for the floating button (oval with shadow).

## Build Configuration

### Gradle Configuration

**Project-Level (build.gradle)**:
- Defines repositories (Google, Maven Central).
- Specifies Android Gradle Plugin version (8.2.2).

**App-Level (app/build.gradle)**:
- **Namespace**: `com.screenlock.app`
- **Compile SDK**: 34 (Android 14)
- **Min SDK**: 23 (Android 6.0)
- **Target SDK**: 34 (Android 14)
- **Version Code**: 2
- **Version Name**: "2.0"
- **Java Compatibility**: Java 8

**Dependencies**:
- `androidx.appcompat:appcompat:1.6.1`: AppCompat library.
- `com.google.android.material:material:1.11.0`: Material Design components.
- `androidx.core:core:1.12.0`: Core Android library.

## GitHub Actions CI/CD

### Workflow File (.github/workflows/build.yml)

The GitHub Actions workflow automatically builds the APK on every push to the `main` or `master` branch.

**Workflow Steps**:
1. Checkout code from the repository.
2. Set up JDK 17.
3. Configure Gradle 8.2.
4. Grant execute permissions to `gradlew`.
5. Build the debug APK using `gradle assembleDebug`.
6. Upload the APK as an artifact with a 30-day retention period.

**Triggering Events**:
- Push to `main` or `master` branch.
- Pull request to `main` or `master` branch.
- Manual trigger via `workflow_dispatch`.

**Artifacts**:
- **Name**: `ScreenLock-debug-apk`
- **Path**: `app/build/outputs/apk/debug/app-debug.apk`
- **Retention**: 30 days

## Known Limitations and Workarounds

### Limitation 1: Overlay-Based Locking

**Issue**: The app uses overlay-based touch blocking rather than true device screen locking.

**Impact**:
- The screen remains visible but unresponsive.
- Some system-level features may bypass the overlay.

**Workaround**: None - this is by design due to Android's security model.

### Limitation 2: Notification Panel Blocking

**Issue**: On some devices (Samsung, Pixel), the notification panel blocking may not work perfectly.

**Impact**:
- Users may be able to access the notification panel by swiping down.

**Workaround**: Use a larger overlay to cover more of the screen.

### Limitation 3: Background Service Termination

**Issue**: Some devices with aggressive battery optimization may kill the service.

**Impact**:
- The locking service may stop running in the background.

**Workaround**:
- Add the app to the device's battery optimization whitelist.
- Disable battery saver mode while using the app.

### Limitation 4: Device-Specific Behavior

**Issue**: Different manufacturers implement Android differently.

**Impact**:
- Samsung devices may have different permission handling.
- Pixel devices may have different notification panel behavior.

**Workaround**: Test on multiple devices and adjust the overlay system as needed.

## Development Guidelines

### Code Style

- Follow Android coding conventions.
- Use meaningful variable and method names.
- Add comments for complex logic.
- Use proper exception handling.

### Testing

- Test on multiple Android versions (API 23+).
- Test on different device manufacturers (Samsung, Pixel, etc.).
- Test permission handling on different Android versions.
- Test background service behavior with battery optimization enabled.

### Building

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Clean build
./gradlew clean assembleDebug
```

### Debugging

- Use Android Studio's built-in debugger.
- Enable logcat to view application logs.
- Use Android Device Monitor to inspect running processes.

## Future Enhancements

Potential improvements for future versions:

1. **Settings Screen**: Allow users to customize lock behavior.
2. **Lock Patterns**: Implement pattern-based unlocking.
3. **Time-Based Locking**: Lock the screen at specific times.
4. **App-Specific Locking**: Lock specific apps instead of the entire screen.
5. **Biometric Unlock**: Use fingerprint or face recognition to unlock.
6. **Custom Notifications**: Display custom messages on the lock screen.
7. **Lock History**: Track when the screen was locked/unlocked.
8. **Remote Control**: Control the app from another device.

## Troubleshooting Guide

### Issue: App Crashes on Startup

**Symptoms**: The app crashes immediately after launching.

**Causes**:
- Missing "Display over other apps" permission.
- Incompatible Android version.
- Corrupted installation.

**Solutions**:
1. Grant the "Display over other apps" permission.
2. Uninstall and reinstall the app.
3. Update Android to the latest version.

### Issue: Floating Button Not Appearing

**Symptoms**: The floating button doesn't appear on the screen after tapping Start.

**Causes**:
- "Display over other apps" permission not granted.
- Service not started correctly.
- Device in battery saver mode.

**Solutions**:
1. Verify the "Display over other apps" permission is granted.
2. Restart the app and tap Start again.
3. Disable battery saver mode.
4. Restart the device.

### Issue: Automatic Locking Not Working

**Symptoms**: The app doesn't automatically lock when the device boots or power is connected.

**Causes**:
- BootReceiver or ChargingReceiver not triggered.
- Service killed by battery optimization.
- Permission not granted.

**Solutions**:
1. Add the app to the battery optimization whitelist.
2. Restart the device to trigger BootReceiver.
3. Manually start the service via the app.

### Issue: Notification Panel Still Accessible

**Symptoms**: The notification panel can still be accessed when the screen is locked.

**Causes**:
- Overlay system not working on this device.
- Notification panel blocker overlay not created.

**Solutions**:
1. This is a known limitation on some devices.
2. Try restarting the app.
3. Try a different device to test.

## Support and Contribution

For issues, questions, or contributions, please visit the [GitHub repository](https://github.com/Farhan-Taoshif/ScreenLockApp).

## License

This project is provided as-is for educational and personal use.

## Version History

### Version 2.0
- Improved floating button dragging functionality.
- Enhanced charging state detection.
- Better permission handling for Android 13+.
- Improved notification panel blocking.

### Version 1.0
- Initial release with basic screen locking functionality.

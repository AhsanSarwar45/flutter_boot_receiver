# flutter_boot_listener

A Flutter plugin for registering Dart callbacks when the Android device boots up.

## Contents

- [Platform Support](#platform-support)
- [Installation](#installation)
- [Usage](#usage)

## Platform Support

| Platform | Supported |
| -------- | --------- |
| Android  | ✔️ Yes    |
| iOS      | ❌ No     |
| Web      | ❌ No     |
| Windows  | ❌ No     |
| Linux    | ❌ No     |
| MacOS    | ❌ No     |

## Installation

In your `AndroidManifest.xml`:

1. Add `android:installLocation="internalOnly"` in your `manifest` tag. Only devices installed in internal storage can receive the `BOOT_COMPLETE` broadcast in Android.

```xml
<manifest
    android:installLocation="internalOnly"
    >
```

2. Add the following permission:

```xml
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED"/>
```

3. Add the following service and receiver inside your `application` tag:

```xml
<service
    android:name="com.flux.flutter_boot_receiver.BootHandlerService"
    android:exported="false"
    android:permission="android.permission.BIND_JOB_SERVICE"
    />
<receiver
    android:enabled="true"
    android:exported="true"
    android:name="com.flux.flutter_boot_receiver.BootBroadcastReceiver"
    android:permission="android.permission.RECEIVE_BOOT_COMPLETED">
    <intent-filter>
        <action android:name="android.intent.action.BOOT_COMPLETED" />
        <category android:name="android.intent.category.DEFAULT" />
    </intent-filter>
</receiver>
```

Your `AndroidManifest.xml` should now have a structure similar to this:

```xml
<manifest
    android:installLocation="internalOnly"
    >
    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED"/>
    <!-- Other permissions... -->
    <application>
        <!-- Other activities, services etc... -->
        <service
            android:name="com.flux.flutter_boot_receiver.BootHandlerService"
            android:exported="false"
            android:permission="android.permission.BIND_JOB_SERVICE"
            />
        <receiver
            android:enabled="true"
            android:exported="true"
            android:name="com.flux.flutter_boot_receiver.BootBroadcastReceiver"
            android:permission="android.permission.RECEIVE_BOOT_COMPLETED">
            <intent-filter>
                <action android:name="android.intent.action.BOOT_COMPLETED" />
                <category android:name="android.intent.category.DEFAULT" />
            </intent-filter>
        </receiver>
    </application>
</manifest>
```

## Usage

```dart
import 'package:flutter_boot_receiver/flutter_boot_receiver.dart';

@pragma('vm:entry-point')
void callback() async {
  // Code here will be executed when the device boots up
}

await BootReceiver.initialize(callback);
```

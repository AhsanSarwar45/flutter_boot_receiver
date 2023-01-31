import 'dart:async';
import 'dart:developer' as developer;
import 'dart:io';
import 'dart:ui';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_boot_listener/broadcast_action.dart';

const String _backgroundName = 'com.example.flutter_boot_listener/background';

// This is the entrypoint for the background isolate. Since we can only enter
// an isolate once, we setup a MethodChannel to listen for method invocations
// from the native portion of the plugin. This allows for the plugin to perform
// any necessary processing in Dart (e.g., populating a custom object) before
// invoking the provided callback.
@pragma('vm:entry-point')
void broadcastListenerCallbackDispatcher() {
  // Initialize state necessary for MethodChannels.
  WidgetsFlutterBinding.ensureInitialized();

  const channel = MethodChannel(_backgroundName, JSONMethodCodec());
  // This is where the magic happens and we handle background events from the
  // native portion of the plugin.
  channel.setMethodCallHandler((MethodCall call) async {
    final dynamic args = call.arguments;
    final handle = CallbackHandle.fromRawHandle(args[0]);

    // PluginUtilities.getCallbackFromHandle performs a lookup based on the
    // callback handle and returns a tear-off of the original callback.
    final closure = PluginUtilities.getCallbackFromHandle(handle);

    if (closure == null) {
      developer.log('Fatal: could not find callback');
      exit(-1);
    }

    // ignore: inference_failure_on_function_return_type
    closure();
  });

  // Once we've finished initializing, let the native portion of the plugin
  // know that it can start scheduling alarms.
  channel.invokeMethod<void>('BroadcastHandlerService.initialized');
}

// A lambda that gets the handle for the given [callback].
typedef _GetCallbackHandle = CallbackHandle? Function(Function callback);

/// A Flutter plugin for registering Dart callbacks with the Android
/// AlarmManager service.
///
/// See the example/ directory in this package for sample usage.
class BroadcastListener {
  static const String _channelName = 'com.example.flutter_boot_listener/main';
  static const MethodChannel _channel =
      MethodChannel(_channelName, JSONMethodCodec());

  // Callback used to get the handle for a callback. It's
  // [PluginUtilities.getCallbackHandle] by default.
  // ignore: prefer_function_declarations_over_variables
  static _GetCallbackHandle _getCallbackHandle =
      (Function callback) => PluginUtilities.getCallbackHandle(callback);

  /// This is exposed for the unit tests. It should not be accessed by users of
  /// the plugin.
  @visibleForTesting
  static void setTestOverrides({
    _GetCallbackHandle? getCallbackHandle,
  }) {
    _getCallbackHandle = (getCallbackHandle ?? _getCallbackHandle);
  }

  /// Starts the [BroadcastListener] service. This must be called before
  /// setting any alarms.
  ///
  /// Returns a [Future] that resolves to `true` on success and `false` on
  /// failure.
  static Future<bool> initialize(void Function() callback) async {
    final callbackDispatcherHandle =
        _getCallbackHandle(broadcastListenerCallbackDispatcher);
    if (callbackDispatcherHandle == null) {
      return false;
    }
    final callbackHandle = _getCallbackHandle(callback);
    final isSuccessful = await _channel.invokeMethod<bool>(
        'BroadcastHandlerService.start', <dynamic>[
      callbackDispatcherHandle.toRawHandle(),
      callbackHandle?.toRawHandle()
    ]);
    return isSuccessful ?? false;
  }
}

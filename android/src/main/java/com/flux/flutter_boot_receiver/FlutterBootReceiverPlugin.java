package com.flux.flutter_boot_receiver;

import android.content.Context;
import android.util.Log;
import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.JSONMethodCodec;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import org.json.JSONArray;
import org.json.JSONException;

/**
 * Flutter plugin for running one-shot and periodic tasks sometime in the future
 * on Android.
 *
 * <p>
 * Plugin initialization goes through these steps:
 *
 * <ol>
 * <li>Flutter app instructs this plugin to initialize() on the Dart side.
 * <li>The Dart side of this plugin sends the Android side a
 * "BootHandlerService.start" message, along
 * with a Dart callback handle for a Dart callback that should be immediately
 * invoked by a
 * background Dart isolate.
 * <li>The Android side of this plugin spins up a background
 * {@link io.flutter.embedding.engine.FlutterEngine}, which
 * includes a background Dart isolate.
 * <li>The Android side of this plugin instructs the new background Dart isolate
 * to execute the
 * callback that was received in the "BootHandlerService.start" message.
 * <li>The Dart side of this plugin, running within the new background isolate,
 * executes the
 * designated callback. This callback prepares the background isolate to then
 * execute any
 * given Dart callback from that point forward. Thus, at this moment the plugin
 * is fully
 * initialized and ready to execute arbitrary Dart tasks in the background. The
 * Dart side of
 * this plugin sends the Android side a "BootHandlerService.initialized"
 * message to signify that the
 * Dart is ready to execute tasks.
 * </ol>
 */
public class FlutterBootReceiverPlugin implements FlutterPlugin, MethodCallHandler {
  private static final String TAG = "FlutterBootReceiverPlugin";
  private Context context;
  private final Object initializationLock = new Object();
  private MethodChannel FlutterBootReceiverPluginChannel;

  @Override
  public void onAttachedToEngine(FlutterPluginBinding binding) {
    onAttachedToEngine(binding.getApplicationContext(), binding.getBinaryMessenger());
  }

  public void onAttachedToEngine(Context applicationContext, BinaryMessenger messenger) {
    synchronized (initializationLock) {
      if (FlutterBootReceiverPluginChannel != null) {
        return;
      }

      Log.i(TAG, "onAttachedToEngine");
      this.context = applicationContext;

      FlutterBootReceiverPluginChannel = new MethodChannel(
              messenger,
              "com.flux.flutter_boot_receiver/main",
              JSONMethodCodec.INSTANCE);

      // Instantiate a new FlutterBootReceiverPlugin and connect the primary
      // method channel for
      // Android/Flutter communication.
      FlutterBootReceiverPluginChannel.setMethodCallHandler(this);
    }
  }

  @Override
  public void onDetachedFromEngine(FlutterPluginBinding binding) {
    Log.i(TAG, "onDetachedFromEngine");
    context = null;
    if (FlutterBootReceiverPluginChannel != null) {
      FlutterBootReceiverPluginChannel.setMethodCallHandler(null);
      FlutterBootReceiverPluginChannel = null;
    }
  }

  public FlutterBootReceiverPlugin() {
  }

  /**
   * Invoked when the Flutter side of this plugin sends a message to the Android
   * side.
   */
  @Override
  public void onMethodCall(MethodCall call, Result result) {
    String method = call.method;
    Object arguments = call.arguments;
    try {
      switch (method) {
        case "BootHandlerService.start":
          // This message is sent when the Dart side of this plugin is told to initialize.
          long callbackDispatcherHandle = ((JSONArray) arguments).getLong(0);
          long callbackHandle = ((JSONArray) arguments).getLong(1);
          // In response, this (native) side of the plugin needs to spin up a background
          // Dart isolate by using the given callbackHandle, and then setup a background
          // method channel to communicate with the new background isolate. Once
          // completed,
          // this onMethodCall() method will receive messages from both the primary and
          // background
          // method channels.
          BootHandlerService.setCallbackHandles(context, callbackDispatcherHandle, callbackHandle);
          BootHandlerService.startBackgroundIsolate(context, callbackDispatcherHandle);
          result.success(true);
          break;
        default:
          result.notImplemented();
          break;
      }
    } catch (JSONException e) {
      result.error("error", "JSON error: " + e.getMessage(), null);
    } catch (Exception e) {
      result.error("error", "FlutterBootReceiver error: " + e.getMessage(), null);
    }
  }
}
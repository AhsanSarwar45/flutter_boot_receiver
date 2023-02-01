package com.flux.flutter_boot_listener;

import android.content.Context;
import android.util.Log;
import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.JSONMethodCodec;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.view.FlutterNativeView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
 * {@link FlutterNativeView}, which
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
public class FlutterBootListenerPlugin implements FlutterPlugin, MethodCallHandler {
  private static final String TAG = "FlutterBootListenerPlugin";
  private Context context;
  private final Object initializationLock = new Object();
  private MethodChannel FlutterBootListenerPluginChannel;

  @Override
  public void onAttachedToEngine(FlutterPluginBinding binding) {
    onAttachedToEngine(binding.getApplicationContext(), binding.getBinaryMessenger());
  }

  public void onAttachedToEngine(Context applicationContext, BinaryMessenger messenger) {
    synchronized (initializationLock) {
      if (FlutterBootListenerPluginChannel != null) {
        return;
      }

      Log.i(TAG, "onAttachedToEngine");
      this.context = applicationContext;

      FlutterBootListenerPluginChannel = new MethodChannel(
          messenger,
          "com.flux.flutter_boot_listener/main",
          JSONMethodCodec.INSTANCE);

      // Instantiate a new FlutterBootListenerPlugin and connect the primary
      // method channel for
      // Android/Flutter communication.
      FlutterBootListenerPluginChannel.setMethodCallHandler(this);
    }
  }

  @Override
  public void onDetachedFromEngine(FlutterPluginBinding binding) {
    Log.i(TAG, "onDetachedFromEngine");
    context = null;
    FlutterBootListenerPluginChannel.setMethodCallHandler(null);
    FlutterBootListenerPluginChannel = null;
  }

  public FlutterBootListenerPlugin() {
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
    } catch (PluginRegistrantException e) {
      result.error("error", "FlutterBootListener error: " + e.getMessage(), null);
    }
  }
}

// import android.content.Context;
// import android.content.Intent;
// import androidx.annotation.NonNull;
// import io.flutter.embedding.engine.plugins.FlutterPlugin;
// import io.flutter.plugin.common.MethodCall;
// import io.flutter.plugin.common.MethodChannel;
// import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
// import io.flutter.plugin.common.MethodChannel.Result;
// import io.flutter.plugin.common.PluginRegistry.Registrar;
// import java.util.ArrayList;
// import io.flutter.view.FlutterCallbackInformation;
// import io.flutter.view.FlutterMain;
// import io.flutter.view.FlutterNativeView;
// import io.flutter.view.FlutterRunArguments;

// public class FlutterBootListener implements FlutterPlugin, MethodCallHandler
// {

// public static final String CALLBACK_HANDLE_KEY = "callback_handle_key";
// public static final String CALLBACK_DISPATCHER_HANDLE_KEY =
// "callback_dispatcher_handle_key";

// private MethodChannel channel;
// private static Context mContext;

// @Override
// public void onAttachedToEngine(@NonNull FlutterPluginBinding
// flutterPluginBinding) {
// channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(),
// "flutter_boot_listener/main");
// channel.setMethodCallHandler(this);
// mContext = flutterPluginBinding.getApplicationContext();
// }

// private static long mCallbackDispatcherHandle;
// private static long mCallbackHandle;
// private static FlutterRunArguments mFlutterRunArguments;
// private static FlutterNativeView mBackgroundFlutterView;

// public static long getCallbackDispatcherHandle() {
// return mCallbackDispatcherHandle;
// }

// public static long getCallbackHandle() {
// return mCallbackHandle;
// }

// public static Context getContext() {
// return mContext;
// }

// public static FlutterRunArguments getFlutterRunArguments() {
// return mFlutterRunArguments;
// }

// public static FlutterNativeView getBackgroundFlutterView() {
// return mBackgroundFlutterView;
// }

// @Override
// public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {

// if (call.method.equals("initialize")) {

// ArrayList args = call.arguments();
// long callBackDispatcherHandle = (long) args.get(0);
// mCallbackDispatcherHandle = callBackDispatcherHandle;

// long callBackHandle = (long) args.get(1);
// mCallbackHandle = callBackHandle;

// FlutterCallbackInformation flutterCallbackInformation =
// FlutterCallbackInformation.lookupCallbackInformation(
// FlutterBootListener.getCallbackDispatcherHandle());

// mFlutterRunArguments = new FlutterRunArguments();
// mFlutterRunArguments.bundlePath = FlutterMain.findAppBundlePath();
// mFlutterRunArguments.entrypoint = flutterCallbackInformation.callbackName;
// mFlutterRunArguments.libraryPath =
// flutterCallbackInformation.callbackLibraryPath;

// mBackgroundFlutterView = new FlutterNativeView(mContext, true);

// result.success(null);
// return;
// }
// // else if (call.method.equals("run")) {
// // Intent intent = new Intent(mContext, MyService.class);
// // intent.putExtra(CALLBACK_HANDLE_KEY, mCallbackHandle);
// // intent.putExtra(CALLBACK_DISPATCHER_HANDLE_KEY,
// // mCallbackDispatcherHandle);
// // mContext.startService(intent);

// // result.success(null);
// // return;
// // }
// result.notImplemented();
// }

// @Override
// public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
// channel.setMethodCallHandler(null);
// }
// }

// // boot receiver

package com.flux.flutter_boot_receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootBroadcastReceiver extends BroadcastReceiver {
  private static final String TAG = "BootBroadcastReceiver";

  @Override
  public void onReceive(Context context, Intent intent) {
    String action = intent.getAction();
    Log.i(TAG, "Received broadcast: " + action);

    if (isBootCompletedAction(action)) {
      Log.i(TAG, "Boot completed received, processing...");
      // Use the new static method to process the intent
      BootHandlerService.processIntent(context, intent);
    } else {
      Log.w(TAG, "Unhandled broadcast action: " + action);
    }
  }

  private boolean isBootCompletedAction(String action) {
    return Intent.ACTION_BOOT_COMPLETED.equals(action) ||
            Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(action) ||
            "android.intent.action.QUICKBOOT_POWERON".equals(action) ||
            "com.htc.intent.action.QUICKBOOT_POWERON".equals(action);
  }
}
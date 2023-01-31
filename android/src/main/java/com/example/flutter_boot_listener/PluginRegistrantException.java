// Copyright 2019 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.example.flutter_boot_listener;

class PluginRegistrantException extends RuntimeException {
  public PluginRegistrantException() {
    super(
        "PluginRegistrantCallback is not set. Did you forget to call "
            + "BroadcastHandlerService.setPluginRegistrant? See the README for instructions.");
  }
}
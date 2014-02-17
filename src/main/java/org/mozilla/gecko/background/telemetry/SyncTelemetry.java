/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.background.telemetry;

import org.mozilla.gecko.background.common.log.Logger;

public class SyncTelemetry {
  private static final String LOG_TAG = SyncTelemetry.class.getSimpleName();

  public static long now() {
    return System.currentTimeMillis();
  }

  // Guarded by SyncTelemetry.class.
  protected static BackgroundTelemetryRecorder telemetryRecorder = null;

  public static synchronized void setTelemetryRecorder(BackgroundTelemetryRecorder recorder) {
    telemetryRecorder = recorder;
  }

  public static void startSyncSession(String sessionName, long timestamp) {
    BackgroundTelemetryRecorder recorder = telemetryRecorder;
    if (recorder == null) {
      Logger.warn(LOG_TAG, "recorder is null; dropping start session.");
      return;
    }
    recorder.startSession(sessionName, timestamp);
  }

  public static void stopSyncSession(String sessionName, String reason, long timestamp) {
    BackgroundTelemetryRecorder recorder = telemetryRecorder;
    if (recorder == null) {
      Logger.warn(LOG_TAG, "recorder is null; dropping stop session.");
      return;
    }
    recorder.stopSession(sessionName, reason, timestamp);
  }

  public static synchronized void sendSyncEvent(String action, String method, long timestamp, String extras) {
    BackgroundTelemetryRecorder recorder = telemetryRecorder;
    if (recorder == null) {
      Logger.warn(LOG_TAG, "recorder is null; dropping event.");
      return;
    }
    recorder.sendEvent(action, method, timestamp, extras);
  }

  public static synchronized void sendSyncEvent(String action, String method, long timestamp) {
    sendSyncEvent(action, method, timestamp, null);
  }

  public static synchronized void sendSyncEvent(String action, String method, String extras) {
    sendSyncEvent(action, method, now(), extras);
  }

  public static synchronized void sendSyncEvent(String action, String method) {
    sendSyncEvent(action, method, now(), null);
  }

  public static synchronized void sendSyncEvent(String action) {
    sendSyncEvent(action, null, now(), null);
  }
}

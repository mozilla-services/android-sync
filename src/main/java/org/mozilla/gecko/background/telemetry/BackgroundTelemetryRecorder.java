/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.background.telemetry;

public interface BackgroundTelemetryRecorder {
  public void startSession(String name, long timestamp);
  public void stopSession(String name, String reason, long timestamp);
  public void sendEvent(String action, String method, long timestamp, String extras);
}

/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.background.telemetry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.mozilla.gecko.background.common.log.Logger;
import org.mozilla.gecko.background.telemetry.BackgroundTelemetryRecorder;

public class JSONBackgroundTelemetryRecorder implements BackgroundTelemetryRecorder {
  private static final String LOG_TAG = JSONBackgroundTelemetryRecorder.class.getCanonicalName();

  /**
   * Map from session names to start times.
   */
  protected final Map<String, Long> activeSessions;

  /**
   * List of events, in advancing chronological order.
   */
  protected final ArrayList<JSONObject> measurements;

  public JSONBackgroundTelemetryRecorder() {
    activeSessions = new HashMap<String, Long>();
    measurements = new ArrayList<JSONObject>();
  }

  public void startSession(String name, long timestamp) {
    if (activeSessions.containsKey(name)) {
      return;
    }
    activeSessions.put(name, timestamp);
  }

  @SuppressWarnings("unchecked")
  public void stopSession(String name, String reason, long timestamp) {
    Long sessionStart = activeSessions.remove(name);
    if (sessionStart == null) {
      Logger.warn(LOG_TAG, "No session '" + name + "' to stop; ignoring.");
      return;
    }
    final JSONObject event = new JSONObject();
    event.put("type", "session");
    event.put("name", name);
    event.put("reason", reason);
    event.put("start", sessionStart.longValue());
    event.put("end", timestamp);
    measurements.add(event);
  }

  @SuppressWarnings("unchecked")
  @Override
  public void sendEvent(String action, String method, long timestamp, String extras) {
    final JSONArray sessions = new JSONArray();
    sessions.addAll(activeSessions.keySet());
    final JSONObject event = new JSONObject();
    event.put("type", "event");
    event.put("action", action);
    event.put("method", method);
    event.put("sessions", sessions);
    event.put("timestamp", timestamp);
    if (extras != null) {
      event.put("extras", extras);
    }
    measurements.add(event);
  }

  public List<JSONObject> getMeasurements() {
    return new ArrayList<JSONObject>(measurements);
  }
}

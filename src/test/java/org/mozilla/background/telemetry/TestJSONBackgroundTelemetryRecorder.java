/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.background.telemetry;

import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.mozilla.gecko.background.telemetry.SyncTelemetry;
import org.mozilla.gecko.sync.ExtendedJSONObject;

public class TestJSONBackgroundTelemetryRecorder {
  protected long now() {
    return System.currentTimeMillis();
  }

  @Test
  public void test() throws Exception {
    JSONBackgroundTelemetryRecorder recorder = new JSONBackgroundTelemetryRecorder();
    SyncTelemetry.setTelemetryRecorder(recorder);

    SyncTelemetry.sendSyncEvent("enone", "method0", now(), null);
    SyncTelemetry.startSyncSession("foo", now());
    SyncTelemetry.sendSyncEvent("efoo", "method1", now(), null);
    SyncTelemetry.startSyncSession("foo", now());
    SyncTelemetry.sendSyncEvent("efoo", "method2", now(), null);
    SyncTelemetry.startSyncSession("bar", now());
    SyncTelemetry.sendSyncEvent("efoobar", "method3", now(), "foobarextras");
    SyncTelemetry.stopSyncSession("foo", "reasonfoo", now());
    SyncTelemetry.sendSyncEvent("ebar", "method4", now(), "barextras");
    SyncTelemetry.stopSyncSession("bar", "reasonbar", now());
    SyncTelemetry.stopSyncSession("bar", "reasonbar2", now());
    SyncTelemetry.sendSyncEvent("enone", "method5", now(), null);

    String[] expectedStrings[] = new String[][] {
        new String[] { "event", "enone", "method0", "", null },
        new String[] { "event", "efoo", "method1", "foo", null },
        new String[] { "event", "efoo", "method2", "foo", null },
        new String[] { "event", "efoobar", "method3", "foo,bar", "foobarextras" },
        new String[] { "session", "foo", "reasonfoo"},
        new String[] { "event", "ebar", "method4", "bar", "barextras" },
        new String[] { "session", "bar", "reasonbar"},
        new String[] { "event", "enone", "method5", "", null },
    };

    List<JSONObject> measurements = recorder.getMeasurements();

    Assert.assertEquals(expectedStrings.length, measurements.size());

    for (int i = 0; i < expectedStrings.length; i++) {
      String[] expectedString = expectedStrings[i];
      ExtendedJSONObject measurement = new ExtendedJSONObject(measurements.get(i));

      Assert.assertEquals(expectedString[0], measurement.getString("type"));

      if (expectedString[0].equals("event")) {
        Assert.assertEquals(expectedString[1], measurement.getString("action"));
        Assert.assertEquals(expectedString[2], measurement.getString("method"));

        String[] expectedSessions = expectedString[3].length() > 0 ? expectedString[3].split(",") : new String[0];
        JSONArray sessions = measurement.getArray("sessions");
        Assert.assertEquals(expectedSessions.length, sessions.size());
        for (String session : expectedSessions) {
          Assert.assertTrue(sessions.contains(session));
        }

        if (expectedString[4] != null) {
          Assert.assertEquals(expectedString[4], measurement.getString("extras"));
        }
      }

      if (expectedString[0].equals("session")) {
        Assert.assertEquals(expectedString[1], measurement.getString("name"));
        Assert.assertEquals(expectedString[2], measurement.getString("reason"));
      }
    }
  }
}

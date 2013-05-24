/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.background.healthreport.test;

import java.io.File;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mozilla.gecko.background.healthreport.HealthReportConstants;
import org.mozilla.gecko.background.healthreport.HealthReportGenerator;
import org.mozilla.gecko.background.healthreport.HealthReportStorage.Field;
import org.mozilla.gecko.background.healthreport.HealthReportStorage.MeasurementFields;
import org.mozilla.gecko.background.healthreport.HealthReportUtils;
import org.mozilla.gecko.background.test.helpers.FakeProfileTestCase;

public class TestHealthReportGenerator extends FakeProfileTestCase {
  public static void testOptObject() throws JSONException {
    JSONObject o = new JSONObject();
    o.put("foo", JSONObject.NULL);
    assertEquals(null, o.optJSONObject("foo"));
  }

  @SuppressWarnings("static-method")
  public void testAppend() throws JSONException {
    JSONObject o = new JSONObject();
    HealthReportUtils.append(o, "yyy", 5);
    assertNotNull(o.getJSONArray("yyy"));
    assertEquals(5, o.getJSONArray("yyy").getInt(0));

    o.put("foo", "noo");
    HealthReportUtils.append(o, "foo", "bar");
    assertNotNull(o.getJSONArray("foo"));
    assertEquals("noo", o.getJSONArray("foo").getString(0));
    assertEquals("bar", o.getJSONArray("foo").getString(1));
  }

  public void testEnvironments() throws JSONException {
    // Hard-coded so you need to update tests!
    // If this is the only thing you need to change when revving a version, you
    // need more test coverage.
    final int expectedVersion = 3;

    MockHealthReportDatabaseStorage storage = new MockHealthReportDatabaseStorage(context, fakeProfileDirectory);
    HealthReportGenerator gen = new HealthReportGenerator(storage);

    final MockDatabaseEnvironment env1 = storage.getEnvironment();
    env1.mockInit("23");
    final String env1Hash = env1.getHash();

    long now = System.currentTimeMillis();
    JSONObject document = gen.generateDocument(0, 0, env1);
    String today = HealthReportUtils.getDateString(now);

    assertFalse(document.has("lastPingDate"));
    document = gen.generateDocument(0, HealthReportConstants.EARLIEST_LAST_PING, env1);
    assertEquals("2013-05-02", document.get("lastPingDate"));

    // True unless test spans midnight...
    assertEquals(today, document.get("thisPingDate"));
    assertEquals(expectedVersion, document.get("version"));

    JSONObject environments = document.getJSONObject("environments");
    JSONObject current = environments.getJSONObject("current");
    assertTrue(current.has("org.mozilla.profile.age"));
    assertTrue(current.has("org.mozilla.sysinfo.sysinfo"));
    assertTrue(current.has("org.mozilla.appInfo.appinfo"));
    assertTrue(current.has("geckoAppInfo"));
    assertTrue(current.has("org.mozilla.addons.active"));
    assertTrue(current.has("org.mozilla.addons.counts"));

    // Make sure we don't get duplicate environments when an environment has
    // been used, and that we get deltas between them.
    env1.register();
    final MockDatabaseEnvironment env2 = storage.getEnvironment();
    env2.mockInit("24");
    final String env2Hash = env2.getHash();
    assertFalse(env2Hash.equals(env1Hash));
    env2.register();
    assertEquals(env2Hash, env2.getHash());

    assertEquals("2013-05-02", document.get("lastPingDate"));

    // True unless test spans midnight...
    assertEquals(today, document.get("thisPingDate"));
    assertEquals(expectedVersion, document.get("version"));
    document = gen.generateDocument(0, HealthReportConstants.EARLIEST_LAST_PING, env2);
    environments = document.getJSONObject("environments");

    // Now we have two: env1, and env2 (as 'current').
    assertTrue(environments.has(env1.getHash()));
    assertTrue(environments.has("current"));
    assertEquals(2, environments.length());

    current = environments.getJSONObject("current");
    assertTrue(current.has("org.mozilla.profile.age"));
    assertTrue(current.has("org.mozilla.sysinfo.sysinfo"));
    assertTrue(current.has("org.mozilla.appInfo.appinfo"));
    assertTrue(current.has("geckoAppInfo"));
    assertTrue(current.has("org.mozilla.addons.active"));
    assertTrue(current.has("org.mozilla.addons.counts"));

    // The diff only contains the changed measurement and fields.
    JSONObject previous = environments.getJSONObject(env1.getHash());
    assertTrue(previous.has("geckoAppInfo"));
    final JSONObject previousAppInfo = previous.getJSONObject("geckoAppInfo");
    assertEquals(2, previousAppInfo.length());
    assertEquals("23", previousAppInfo.getString("version"));
    assertEquals(Integer.valueOf(1), (Integer) previousAppInfo.get("_v"));

    assertFalse(previous.has("org.mozilla.profile.age"));
    assertFalse(previous.has("org.mozilla.sysinfo.sysinfo"));
    assertFalse(previous.has("org.mozilla.appInfo.appinfo"));
    assertFalse(previous.has("org.mozilla.addons.active"));
    assertFalse(previous.has("org.mozilla.addons.counts"));
  }

  public void testInsertedData() throws JSONException {
    MockHealthReportDatabaseStorage storage = new MockHealthReportDatabaseStorage(context, fakeProfileDirectory);
    HealthReportGenerator gen = new HealthReportGenerator(storage);

    storage.beginInitialization();

    final MockDatabaseEnvironment environment = storage.getEnvironment();
    String envHash = environment.getHash();
    int env = environment.mockInit("23").register();

    storage.ensureMeasurementInitialized("org.mozilla.testm5", 1, new MeasurementFields() {
      @Override
      public Iterable<FieldSpec> getFields() {
        ArrayList<FieldSpec> out = new ArrayList<FieldSpec>();
        out.add(new FieldSpec("counter", Field.TYPE_INTEGER_COUNTER));
        out.add(new FieldSpec("discrete_int", Field.TYPE_INTEGER_DISCRETE));
        out.add(new FieldSpec("discrete_str", Field.TYPE_STRING_DISCRETE));
        out.add(new FieldSpec("last_int", Field.TYPE_INTEGER_LAST));
        out.add(new FieldSpec("last_str", Field.TYPE_STRING_LAST));
        return out;
      }
    });

    storage.finishInitialization();

    long now = System.currentTimeMillis();
    int day = storage.getDay(now);
    final String todayString = HealthReportUtils.getDateString(now);

    int counter = storage.getField("org.mozilla.testm5", 1, "counter").getID();
    int discrete_int = storage.getField("org.mozilla.testm5", 1, "discrete_int").getID();
    int discrete_str = storage.getField("org.mozilla.testm5", 1, "discrete_str").getID();
    int last_int = storage.getField("org.mozilla.testm5", 1, "last_int").getID();
    int last_str = storage.getField("org.mozilla.testm5", 1, "last_str").getID();

    storage.incrementDailyCount(env, day, counter, 2);
    storage.incrementDailyCount(env, day, counter, 3);
    storage.recordDailyLast(env, day, last_int, 2);
    storage.recordDailyLast(env, day, last_str, "a");
    storage.recordDailyLast(env, day, last_int, 3);
    storage.recordDailyLast(env, day, last_str, "b");
    storage.recordDailyDiscrete(env, day, discrete_str, "a");
    storage.recordDailyDiscrete(env, day, discrete_str, "b");
    storage.recordDailyDiscrete(env, day, discrete_int, 2);
    storage.recordDailyDiscrete(env, day, discrete_int, 1);
    storage.recordDailyDiscrete(env, day, discrete_int, 3);

    JSONObject document = gen.generateDocument(0, HealthReportConstants.EARLIEST_LAST_PING, environment);
    JSONObject today = document.getJSONObject("data").getJSONObject("days").getJSONObject(todayString);
    assertEquals(1, today.length());
    JSONObject measurement = today.getJSONObject(envHash).getJSONObject("org.mozilla.testm5");
    assertEquals(1, measurement.getInt("_v"));
    assertEquals(5, measurement.getInt("counter"));
    assertEquals(3, measurement.getInt("last_int"));
    assertEquals("b", measurement.getString("last_str"));
    JSONArray discreteInts = measurement.getJSONArray("discrete_int");
    JSONArray discreteStrs = measurement.getJSONArray("discrete_str");
    assertEquals(3, discreteInts.length());
    assertEquals(2, discreteStrs.length());
    assertEquals("a", discreteStrs.get(0));
    assertEquals("b", discreteStrs.get(1));
    assertEquals(Long.valueOf(2), discreteInts.get(0));
    assertEquals(Long.valueOf(1), discreteInts.get(1));
    assertEquals(Long.valueOf(3), discreteInts.get(2));
  }

  @Override
  protected String getCacheSuffix() {
    return File.separator + "health-" + System.currentTimeMillis() + ".profile";
  }
}

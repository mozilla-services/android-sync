/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.gecko.background.healthreport.test;

import java.io.File;
import java.util.ArrayList;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.mozilla.gecko.background.healthreport.HealthReportConstants;
import org.mozilla.gecko.background.healthreport.HealthReportGenerator;
import org.mozilla.gecko.background.healthreport.HealthReportStorage.Field;
import org.mozilla.gecko.background.healthreport.HealthReportStorage.MeasurementFields;
import org.mozilla.gecko.background.healthreport.HealthReportUtils;
import org.mozilla.gecko.sync.ExtendedJSONObject;
import org.mozilla.gecko.sync.NonArrayJSONException;
import org.mozilla.gecko.sync.NonObjectJSONException;

public class TestHealthReportGenerator extends FakeProfileTestCase {
  public void testEnvironments() throws NonObjectJSONException {
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

    assertEquals(null, document.get("lastPingDate"));
    document = gen.generateDocument(0, HealthReportConstants.EARLIEST_LAST_PING, env1);
    assertEquals("2013-05-02", document.get("lastPingDate"));

    // True unless test spans midnight...
    assertEquals(today, document.get("thisPingDate"));
    assertEquals(expectedVersion, document.get("version"));

    // Use EJO to avoid hundreds of casts.
    ExtendedJSONObject environments = new ExtendedJSONObject((JSONObject) document.get("environments"));
    ExtendedJSONObject current = environments.getObject("current");
    assertTrue(current.containsKey("org.mozilla.profile.age"));
    assertTrue(current.containsKey("org.mozilla.sysinfo.sysinfo"));
    assertTrue(current.containsKey("org.mozilla.appInfo.appinfo"));
    assertTrue(current.containsKey("geckoAppInfo"));
    assertTrue(current.containsKey("org.mozilla.addons.active"));
    assertTrue(current.containsKey("org.mozilla.addons.counts"));

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
    environments = new ExtendedJSONObject((JSONObject) document.get("environments"));

    // Now we have two: env1, and env2 (as 'current').
    assertTrue(environments.containsKey(env1.getHash()));
    assertTrue(environments.containsKey("current"));
    assertEquals(2, environments.size());

    current = environments.getObject("current");
    assertTrue(current.containsKey("org.mozilla.profile.age"));
    assertTrue(current.containsKey("org.mozilla.sysinfo.sysinfo"));
    assertTrue(current.containsKey("org.mozilla.appInfo.appinfo"));
    assertTrue(current.containsKey("geckoAppInfo"));
    assertTrue(current.containsKey("org.mozilla.addons.active"));
    assertTrue(current.containsKey("org.mozilla.addons.counts"));

    // The diff only contains the changed measurement and fields.
    ExtendedJSONObject previous = environments.getObject(env1.getHash());
    assertTrue(previous.containsKey("geckoAppInfo"));
    final ExtendedJSONObject previousAppInfo = previous.getObject("geckoAppInfo");
    assertEquals(2, previousAppInfo.size());
    assertEquals("23", previousAppInfo.getString("version"));
    assertEquals(Integer.valueOf(1), (Integer) previousAppInfo.get("_v"));

    assertFalse(previous.containsKey("org.mozilla.profile.age"));
    assertFalse(previous.containsKey("org.mozilla.sysinfo.sysinfo"));
    assertFalse(previous.containsKey("org.mozilla.appInfo.appinfo"));
    assertFalse(previous.containsKey("org.mozilla.addons.active"));
    assertFalse(previous.containsKey("org.mozilla.addons.counts"));
  }

  public void testInsertedData() throws NonObjectJSONException, NonArrayJSONException {
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

    ExtendedJSONObject document = new ExtendedJSONObject(gen.generateDocument(0, HealthReportConstants.EARLIEST_LAST_PING, environment));
    ExtendedJSONObject today = document.getObject("data").getObject("days").getObject(todayString);
    assertEquals(1, today.size());
    ExtendedJSONObject measurement = today.getObject(envHash).getObject("org.mozilla.testm5");
    assertEquals(Integer.valueOf(1), measurement.getIntegerSafely("_v"));
    assertEquals(Integer.valueOf(5), measurement.getIntegerSafely("counter"));
    assertEquals(Integer.valueOf(3), measurement.getIntegerSafely("last_int"));
    assertEquals("b", measurement.getString("last_str"));
    JSONArray discreteInts = measurement.getArray("discrete_int");
    JSONArray discreteStrs = measurement.getArray("discrete_str");
    assertEquals(3, discreteInts.size());
    assertEquals(2, discreteStrs.size());
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

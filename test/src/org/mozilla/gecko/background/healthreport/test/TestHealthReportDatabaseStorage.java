/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.gecko.background.healthreport.test;

import java.io.File;
import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;
import org.mozilla.gecko.background.healthreport.HealthReportConstants;
import org.mozilla.gecko.background.healthreport.HealthReportStorage.Field;
import org.mozilla.gecko.background.healthreport.HealthReportStorage.MeasurementFields;
import org.mozilla.gecko.background.test.helpers.DBHelpers;
import org.mozilla.gecko.background.test.helpers.FakeProfileTestCase;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class TestHealthReportDatabaseStorage extends FakeProfileTestCase {
  @Override
  protected String getCacheSuffix() {
    return File.separator + "health-" + System.currentTimeMillis() + ".profile";
  }

  public static class MockMeasurementFields implements MeasurementFields {
    @Override
    public Iterable<FieldSpec> getFields() {
      ArrayList<FieldSpec> fields = new ArrayList<FieldSpec>();
      fields.add(new FieldSpec("testfield1", Field.TYPE_INTEGER_COUNTER));
      fields.add(new FieldSpec("testfield2", Field.TYPE_INTEGER_COUNTER));
      return fields;
    }
  }

  public void testInitializingProvider() {
    MockHealthReportDatabaseStorage storage = new MockHealthReportDatabaseStorage(context, fakeProfileDirectory);
    storage.beginInitialization();

    // Two providers with the same measurement and field names. Shouldn't conflict.
    storage.ensureMeasurementInitialized("testpA.testm", 1, new MockMeasurementFields());
    storage.ensureMeasurementInitialized("testpB.testm", 2, new MockMeasurementFields());
    storage.finishInitialization();

    // Now make sure our stuff is in the DB.
    SQLiteDatabase db = storage.getDB();
    Cursor c = db.query("measurements", new String[] {"id", "name", "version"}, null, null, null, null, "name");
    assertTrue(c.moveToFirst());
    assertEquals(2, c.getCount());

    Object[][] expected = new Object[][] {
        {null, "testpA.testm", 1},
        {null, "testpB.testm", 2},
    };

    DBHelpers.assertCursorContains(expected, c);
    c.close();
  }

  private static final JSONObject EXAMPLE_ADDONS = safeJSONObject(
            "{ " +
            "\"amznUWL2@amazon.com\": { " +
            "  \"userDisabled\": false, " +
            "  \"appDisabled\": false, " +
            "  \"version\": \"1.10\", " +
            "  \"type\": \"extension\", " +
            "  \"scope\": 1, " +
            "  \"foreignInstall\": false, " +
            "  \"hasBinaryComponents\": false, " +
            "  \"installDay\": 15269, " +
            "  \"updateDay\": 15602 " +
            "}, " +
            "\"jid0-qBnIpLfDFa4LpdrjhAC6vBqN20Q@jetpack\": { " +
            "  \"userDisabled\": false, " +
            "  \"appDisabled\": false, " +
            "  \"version\": \"1.12.1\", " +
            "  \"type\": \"extension\", " +
            "  \"scope\": 1, " +
            "  \"foreignInstall\": false, " +
            "  \"hasBinaryComponents\": false, " +
            "  \"installDay\": 15062, " +
            "  \"updateDay\": 15580 " +
            "} " +
            "} ");

  private static JSONObject safeJSONObject(String s) {
    try {
      return new JSONObject(s);
    } catch (JSONException e) {
      return null;
    }
  }

  public void testEnvironmentsAndFields() throws Exception {
    MockHealthReportDatabaseStorage storage = new MockHealthReportDatabaseStorage(context, fakeProfileDirectory);
    storage.beginInitialization();
    storage.ensureMeasurementInitialized("testpA.testm", 1, new MockMeasurementFields());
    storage.ensureMeasurementInitialized("testpB.testn", 1, new MockMeasurementFields());
    storage.finishInitialization();

    MockDatabaseEnvironment environmentA = storage.getEnvironment();
    environmentA.mockInit("v123");
    environmentA.setJSONForAddons(EXAMPLE_ADDONS);
    final int envA = environmentA.register();
    assertEquals(envA, environmentA.register());

    // getField memoizes.
    assertSame(storage.getField("foo", 2, "bar"),
               storage.getField("foo", 2, "bar"));

    // It throws if you refer to a non-existent field.
    try {
      storage.getField("foo", 2, "bar").getID();
      fail("Should throw.");
    } catch (IllegalStateException ex) {
      // Expected.
    }

    // It returns the field ID for a valid field.
    Field field = storage.getField("testpA.testm", 1, "testfield1");
    assertTrue(field.getID() >= 0);

    // These IDs are stable.
    assertEquals(field.getID(), field.getID());
    int fieldID = field.getID();

    // Before inserting, no events.
    assertFalse(storage.hasEventSince(0));
    assertFalse(storage.hasEventSince(storage.now));

    // Store some data for two environments across two days.
    storage.incrementDailyCount(envA, storage.getYesterday(), fieldID, 4);
    storage.incrementDailyCount(envA, storage.getYesterday(), fieldID, 1);
    storage.incrementDailyCount(envA, storage.getToday(), fieldID, 2);

    // After inserting, we have events.
    assertTrue(storage.hasEventSince(storage.now - HealthReportConstants.MILLISECONDS_PER_DAY));
    assertTrue(storage.hasEventSince(storage.now));
    // But not in the future.
    assertFalse(storage.hasEventSince(storage.now + HealthReportConstants.MILLISECONDS_PER_DAY));

    MockDatabaseEnvironment environmentB = storage.getEnvironment();
    environmentB.mockInit("v234");
    environmentB.setJSONForAddons(EXAMPLE_ADDONS);
    final int envB = environmentB.register();
    assertFalse(envA == envB);

    storage.incrementDailyCount(envB, storage.getToday(), fieldID, 6);
    storage.incrementDailyCount(envB, storage.getToday(), fieldID, 2);

    // Let's make sure everything's there.
    Cursor c = storage.getRawEventsSince(storage.getOneDayAgo());
    try {
      assertTrue(c.moveToFirst());
      assertTrue(assertRowEquals(c, storage.getYesterday(), envA, fieldID, 5));
      assertTrue(assertRowEquals(c, storage.getToday(), envA, fieldID, 2));
      assertFalse(assertRowEquals(c, storage.getToday(), envB, fieldID, 8));
    } finally {
      c.close();
    }

    // The stored environment has the provided JSON add-ons bundle.
    Cursor e = storage.getEnvironmentRecordForID(envA);
    e.moveToFirst();
    assertEquals(EXAMPLE_ADDONS.toString(), e.getString(e.getColumnIndex("addonsBody")));
    e.close();

    e = storage.getEnvironmentRecordForID(envB);
    e.moveToFirst();
    assertEquals(EXAMPLE_ADDONS.toString(), e.getString(e.getColumnIndex("addonsBody")));
    e.close();

    // There's only one add-ons bundle in the DB, despite having two environments.
    Cursor addons = storage.getDB().query("addons", null, null, null, null, null, null);
    assertEquals(1, addons.getCount());
    addons.close();
  }

  /**
   * Asserts validity for a storage cursor. Returns whether there is another row to process.
   */
  private static boolean assertRowEquals(Cursor c, int day, int env, int field, int value) {
    assertEquals(day,   c.getInt(0));
    assertEquals(env,   c.getInt(1));
    assertEquals(field, c.getInt(2));
    assertEquals(value, c.getLong(3));
    return c.moveToNext();
  }

  /**
   * Returns a storage instance prepopulated with dummy data to be used for testing.
   *
   * Note: Editing this data directly will cause tests relying on it to fail. To add additional
   * data, two possiblilites are 1) this method is wrapped and the data is added to the returned
   * object, or 2) this method takes an "version" argument with new data additions running only if
   * the version is greater than some value. Once this is implemented, this comment can be
   * removed.
   *
   * XXX: This is used in lieu of subclassing TestHealthReportDatabaseStorage in an inner class
   * and prepopulating the storage instance in setUp() because the test runner was unable to find
   * the inner class in testing.
   */
  private MockHealthReportDatabaseStorage getPrepopulatedStorage() throws Exception {
    final String[] measurementNames = {"stringMeasurement", "integerMeasurement"};
    final int[] measurementVer = {1, 2};
    final MeasurementFields[] measurementFields = {new MeasurementFields() {
      @Override
      public Iterable<FieldSpec> getFields() {
        ArrayList<FieldSpec> fields = new ArrayList<FieldSpec>();
        fields.add(new FieldSpec("counterField", Field.TYPE_INTEGER_COUNTER));
        fields.add(new FieldSpec("discreteField", Field.TYPE_STRING_DISCRETE));
        fields.add(new FieldSpec("lastField", Field.TYPE_STRING_LAST));
        return fields;
      }
    }, new MeasurementFields() {
      @Override
      public Iterable<FieldSpec> getFields() {
        ArrayList<FieldSpec> fields = new ArrayList<FieldSpec>();
        fields.add(new FieldSpec("counterField", Field.TYPE_INTEGER_COUNTER));
        fields.add(new FieldSpec("discreteField", Field.TYPE_INTEGER_DISCRETE));
        fields.add(new FieldSpec("lastField", Field.TYPE_INTEGER_LAST));
        return fields;
      }
    }};

    final MockHealthReportDatabaseStorage storage = new MockHealthReportDatabaseStorage(context,
        fakeProfileDirectory);
    storage.beginInitialization();
    for (int i = 0; i < measurementNames.length; i++) {
      storage.ensureMeasurementInitialized(measurementNames[i], measurementVer[i],
          measurementFields[i]);
    }
    storage.finishInitialization();

    final MockDatabaseEnvironment environment = storage.getEnvironment();
    environment.mockInit("v123");
    environment.setJSONForAddons(EXAMPLE_ADDONS);
    final int env = environment.register();

    String mName = measurementNames[0];
    int mVer = measurementVer[0];
    int fieldID = storage.getField(mName, mVer, "counterField").getID();
    storage.incrementDailyCount(env, storage.getGivenDaysAgo(7), fieldID, 1);
    storage.incrementDailyCount(env, storage.getGivenDaysAgo(4), fieldID, 2);
    storage.incrementDailyCount(env, storage.getToday(), fieldID, 3);
    fieldID = storage.getField(mName, mVer, "lastField").getID();
    storage.recordDailyLast(env, storage.getGivenDaysAgo(6), fieldID, "six");
    storage.recordDailyLast(env, storage.getGivenDaysAgo(3), fieldID, "three");
    storage.recordDailyLast(env, storage.getToday(), fieldID, "zero");
    fieldID = storage.getField(mName, mVer, "discreteField").getID();
    storage.recordDailyDiscrete(env, storage.getGivenDaysAgo(5), fieldID, "five");
    storage.recordDailyDiscrete(env, storage.getGivenDaysAgo(5), fieldID, "five-two");
    storage.recordDailyDiscrete(env, storage.getGivenDaysAgo(2), fieldID, "two");
    storage.recordDailyDiscrete(env, storage.getToday(), fieldID, "zero");

    mName = measurementNames[1];
    mVer = measurementVer[1];
    fieldID = storage.getField(mName, mVer, "counterField").getID();
    storage.incrementDailyCount(env, storage.getGivenDaysAgo(2), fieldID, 2);
    fieldID = storage.getField(mName, mVer, "lastField").getID();
    storage.recordDailyLast(env, storage.getYesterday(), fieldID, 1);
    fieldID = storage.getField(mName, mVer, "discreteField").getID();
    storage.recordDailyDiscrete(env, storage.getToday(), fieldID, 0);
    storage.recordDailyDiscrete(env, storage.getToday(), fieldID, 1);

    return storage;
  }

  public void testDeleteEventsBefore() throws Exception {
    final MockHealthReportDatabaseStorage storage = getPrepopulatedStorage();
    Cursor c = storage.getEventsSince(0);
    assertEquals(14, c.getCount());
    c.close();

    assertEquals(2, storage.deleteEventsBefore(storage.getGivenDaysAgoMillis(5)));
    c = storage.getEventsSince(0);
    assertEquals(12, c.getCount());
    c.close();

    assertEquals(2, storage.deleteEventsBefore(storage.getGivenDaysAgoMillis(4)));
    c = storage.getEventsSince(0);
    assertEquals(10, c.getCount());
    c.close();

    assertEquals(5, storage.deleteEventsBefore(storage.now));
    c = storage.getEventsSince(0);
    assertEquals(5, c.getCount());
    c.close();
  }
}

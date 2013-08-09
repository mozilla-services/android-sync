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
import org.mozilla.gecko.background.healthreport.test.MockHealthReportDatabaseStorage.PrepopulatedMockHealthReportDatabaseStorage;
import org.mozilla.gecko.background.test.helpers.DBHelpers;
import org.mozilla.gecko.background.test.helpers.FakeProfileTestCase;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;

public class TestHealthReportDatabaseStorage extends FakeProfileTestCase {
  private String[] TABLE_NAMES = {
    "addons",
    "environments",
    "measurements",
    "fields",
    "events_integer",
    "events_textual"
  };

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
   * Test robust insertions. This also acts as a test for the getPrepopulatedStorage method,
   * allowing faster debugging if this fails and other tests relying on getPrepopulatedStorage
   * also fail.
   */
  public void testInsertions() throws Exception {
    final PrepopulatedMockHealthReportDatabaseStorage storage =
        new PrepopulatedMockHealthReportDatabaseStorage(context, fakeProfileDirectory);
    assertNotNull(storage);
  }

  public void testForeignKeyConstraints() throws Exception {
    final PrepopulatedMockHealthReportDatabaseStorage storage =
        new PrepopulatedMockHealthReportDatabaseStorage(context, fakeProfileDirectory);
    final SQLiteDatabase db = storage.getDB();

    final int envID = storage.getEnvironment().register();
    final int counterFieldID = storage.getField(storage.measurementNames[0], storage.measurementVers[0],
        storage.fieldSpecContainers[0].counter.name).getID();
    final int discreteFieldID = storage.getField(storage.measurementNames[0], storage.measurementVers[0],
        storage.fieldSpecContainers[0].discrete.name).getID();

    final int nonExistentEnvID = DBHelpers.getNonExistentID(db, "environments");
    final int nonExistentFieldID = DBHelpers.getNonExistentID(db, "fields");
    final int nonExistentAddonID = DBHelpers.getNonExistentID(db, "addons");
    final int nonExistentMeasurementID = DBHelpers.getNonExistentID(db, "measurements");

    ContentValues v = new ContentValues();
    v.put("field", counterFieldID);
    v.put("env", nonExistentEnvID);
    try {
      db.insertOrThrow("events_integer", null, v);
      fail("Should throw - events_integer(env) is referencing non-existent environments(id)");
    } catch (SQLiteConstraintException e) { }
    v.put("field", discreteFieldID);
    try {
      db.insertOrThrow("events_textual", null, v);
      fail("Should throw - events_textual(env) is referencing non-existent environments(id)");
    } catch (SQLiteConstraintException e) { }

    v.put("field", nonExistentFieldID);
    v.put("env", envID);
    try {
      db.insertOrThrow("events_integer", null, v);
      fail("Should throw - events_integer(field) is referencing non-existent fields(id)");
    } catch (SQLiteConstraintException e) { }
    try {
      db.insertOrThrow("events_textual", null, v);
      fail("Should throw - events_textual(field) is referencing non-existent fields(id)");
    } catch (SQLiteConstraintException e) { }

    v = new ContentValues();
    v.put("addonsID", nonExistentAddonID);
    try {
      db.insertOrThrow("environments", null, v);
      fail("Should throw - environments(addonsID) is referencing non-existent addons(id).");
    } catch (SQLiteConstraintException e) { }

    v = new ContentValues();
    v.put("measurement", nonExistentMeasurementID);
    try {
      db.insertOrThrow("fields", null, v);
      fail("Should throw - fields(measurement) is referencing non-existent measurements(id).");
    } catch (SQLiteConstraintException e) { }
  }

  public void testCascadingDeletions() throws Exception {
    PrepopulatedMockHealthReportDatabaseStorage storage =
        new PrepopulatedMockHealthReportDatabaseStorage(context, fakeProfileDirectory);
    SQLiteDatabase db = storage.getDB();
    db.delete("environments", null, null);
    assertEquals(0, DBHelpers.getRowCount(db, "events_integer"));
    assertEquals(0, DBHelpers.getRowCount(db, "events_textual"));

    storage = new PrepopulatedMockHealthReportDatabaseStorage(context, fakeProfileDirectory);
    db = storage.getDB();
    db.delete("measurements", null, null);
    assertEquals(0, DBHelpers.getRowCount(db, "fields"));
    assertEquals(0, DBHelpers.getRowCount(db, "events_integer"));
    assertEquals(0, DBHelpers.getRowCount(db, "events_textual"));
  }

  public void testRestrictedDeletions() throws Exception {
    final PrepopulatedMockHealthReportDatabaseStorage storage =
        new PrepopulatedMockHealthReportDatabaseStorage(context, fakeProfileDirectory);
    SQLiteDatabase db = storage.getDB();
    try {
      db.delete("addons", null, null);
      fail("Should throw - environment references addons and thus addons cannot be deleted.");
    } catch (SQLiteConstraintException e) { }
  }

  public void testDeleteEverything() throws Exception {
    final PrepopulatedMockHealthReportDatabaseStorage storage =
        new PrepopulatedMockHealthReportDatabaseStorage(context, fakeProfileDirectory);
    storage.deleteEverything();

    final SQLiteDatabase db = storage.getDB();
    for (String table : TABLE_NAMES) {
      if (DBHelpers.getRowCount(db, table) != 0) {
        fail("Not everything has been deleted for table " + table + ".");
      }
    }
  }

  public void testMeasurementRecordingConstraintViolation() throws Exception {
    final PrepopulatedMockHealthReportDatabaseStorage storage =
        new PrepopulatedMockHealthReportDatabaseStorage(context, fakeProfileDirectory);
    final SQLiteDatabase db = storage.getDB();

    final int envID = storage.getEnvironment().register();
    final int counterFieldID = storage.getField(storage.measurementNames[0], storage.measurementVers[0],
        storage.fieldSpecContainers[0].counter.name).getID();
    final int discreteFieldID = storage.getField(storage.measurementNames[0], storage.measurementVers[0],
        storage.fieldSpecContainers[0].discrete.name).getID();

    final int nonExistentEnvID = DBHelpers.getNonExistentID(db, "environments");
    final int nonExistentFieldID = DBHelpers.getNonExistentID(db, "fields");

    try {
      storage.incrementDailyCount(nonExistentEnvID, storage.getToday(), counterFieldID);
      fail("Should throw - event_integer(env) references environments(id), which is given as a non-existent value.");
    } catch (IllegalStateException e) { }
    try {
      storage.recordDailyDiscrete(nonExistentEnvID, storage.getToday(), discreteFieldID, "iu");
      fail("Should throw - event_textual(env) references environments(id), which is given as a non-existent value.");
    } catch (IllegalStateException e) { }
    try {
      storage.recordDailyLast(nonExistentEnvID, storage.getToday(), discreteFieldID, "iu");
      fail("Should throw - event_textual(env) references environments(id), which is given as a non-existent value.");
    } catch (IllegalStateException e) { }

    try {
      storage.incrementDailyCount(envID, storage.getToday(), nonExistentFieldID);
      fail("Should throw - event_integer(field) references fields(id), which is given as a non-existent value.");
    } catch (IllegalStateException e) { }
    try {
      storage.recordDailyDiscrete(envID, storage.getToday(), nonExistentFieldID, "iu");
      fail("Should throw - event_textual(field) references fields(id), which is given as a non-existent value.");
    } catch (IllegalStateException e) { }
    try {
      storage.recordDailyLast(envID, storage.getToday(), nonExistentFieldID, "iu");
      fail("Should throw - event_textual(field) references fields(id), which is given as a non-existent value.");
    } catch (IllegalStateException e) { }
  }
}

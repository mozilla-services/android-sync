/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.gecko.background.healthreport.test;

import java.io.File;
import java.util.ArrayList;

import org.json.JSONObject;
import org.mozilla.gecko.background.healthreport.HealthReportConstants;
import org.mozilla.gecko.background.healthreport.HealthReportDatabaseStorage;
import org.mozilla.gecko.background.healthreport.HealthReportStorage.MeasurementFields;
import org.mozilla.gecko.background.healthreport.HealthReportStorage.MeasurementFields.FieldSpec;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

public class MockHealthReportDatabaseStorage extends HealthReportDatabaseStorage {
  public long now = System.currentTimeMillis();

  public long getOneDayAgo() {
    return now - HealthReportConstants.MILLISECONDS_PER_DAY;
  }

  public int getYesterday() {
    return super.getDay(this.getOneDayAgo());
  }

  public int getToday() {
    return super.getDay(now);
  }

  public int getGivenDaysAgo(int numDays) {
    return super.getDay(this.getGivenDaysAgoMillis(numDays));
  }

  public long getGivenDaysAgoMillis(int numDays) {
    return now - numDays * HealthReportConstants.MILLISECONDS_PER_DAY;
  }

  public MockHealthReportDatabaseStorage(Context context, File fakeProfileDirectory) {
    super(context, fakeProfileDirectory);
  }

  public SQLiteDatabase getDB() {
    return this.helper.getWritableDatabase();
  }

  @Override
  public MockDatabaseEnvironment getEnvironment() {
    return new MockDatabaseEnvironment(this);
  }

  /**
   * A storage instance prepopulated with dummy data to be used for testing.
   *
   * XXX: This is used in lieu of subclassing TestHealthReportDatabaseStorage in an inner class
   * and prepopulating the storage instance in setUp() because the test runner was unable to find
   * the inner class in testing.
   *
   * TODO: Modifying this data directly will cause tests relying on it to fail so a versioned
   * constructor should be added where additional (or entirely different) data is used for each
   * version.
   */
  public static class PrepopulatedMockHealthReportDatabaseStorage extends MockHealthReportDatabaseStorage {
    private final JSONObject addonJSON = new JSONObject(
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

    private static class FieldSpecContainer {
      public final FieldSpec counter;
      public final FieldSpec discrete;
      public final FieldSpec last;

      public FieldSpecContainer(FieldSpec counter, FieldSpec discrete, FieldSpec last) {
        this.counter = counter;
        this.discrete = discrete;
        this.last = last;
      }

      public ArrayList<FieldSpec> asList() {
        final ArrayList<FieldSpec> out = new ArrayList(3);
        out.add(counter);
        out.add(discrete);
        out.add(last);
        return out;
      }
    }

    public PrepopulatedMockHealthReportDatabaseStorage(Context context, File fakeProfileDirectory) throws Exception {
      super(context, fakeProfileDirectory);

      String[] measurementNames = new String[2];
      measurementNames[0] = "a_string_measurement";
      measurementNames[1] = "b_integer_measurement";

      int[] measurementVers = new int[2];
      measurementVers[0] = 1;
      measurementVers[1] = 2;

      final FieldSpecContainer[] fieldSpecContainers = new FieldSpecContainer[2];
      fieldSpecContainers[0] = new FieldSpecContainer(
          new FieldSpec("a_counter_integer_field", Field.TYPE_INTEGER_COUNTER),
          new FieldSpec("a_discrete_string_field", Field.TYPE_STRING_DISCRETE),
          new FieldSpec("a_last_string_field", Field.TYPE_STRING_LAST));
      fieldSpecContainers[1] = new FieldSpecContainer(
          new FieldSpec("b_counter_integer_field", Field.TYPE_INTEGER_COUNTER),
          new FieldSpec("b_discrete_integer_field", Field.TYPE_INTEGER_DISCRETE),
          new FieldSpec("b_last_integer_field", Field.TYPE_INTEGER_LAST));

      final MeasurementFields[] measurementFields =
          new MeasurementFields[fieldSpecContainers.length];
      for (int i = 0; i < fieldSpecContainers.length; i++) {
        final FieldSpecContainer fieldSpecContainer = fieldSpecContainers[i];
        measurementFields[i] = new MeasurementFields() {
          @Override
          public Iterable<FieldSpec> getFields() {
            return fieldSpecContainer.asList();
          }
        };
      }

      this.beginInitialization();
      for (int i = 0; i < measurementNames.length; i++) {
        this.ensureMeasurementInitialized(measurementNames[i], measurementVers[i],
            measurementFields[i]);
      }
      this.finishInitialization();

      final MockDatabaseEnvironment environment = this.getEnvironment();
      environment.mockInit("v123");
      environment.setJSONForAddons(addonJSON);
      final int env = environment.register();

      String mName = measurementNames[0];
      int mVer = measurementVers[0];
      FieldSpecContainer fieldSpecCont = fieldSpecContainers[0];
      int fieldID = this.getField(mName, mVer, fieldSpecCont.counter.name).getID();
      this.incrementDailyCount(env, this.getGivenDaysAgo(7), fieldID, 1);
      this.incrementDailyCount(env, this.getGivenDaysAgo(4), fieldID, 2);
      this.incrementDailyCount(env, this.getToday(), fieldID, 3);
      fieldID = this.getField(mName, mVer, fieldSpecCont.discrete.name).getID();
      this.recordDailyDiscrete(env, this.getGivenDaysAgo(5), fieldID, "five");
      this.recordDailyDiscrete(env, this.getGivenDaysAgo(5), fieldID, "five-two");
      this.recordDailyDiscrete(env, this.getGivenDaysAgo(2), fieldID, "two");
      this.recordDailyDiscrete(env, this.getToday(), fieldID, "zero");
      fieldID = this.getField(mName, mVer, fieldSpecCont.last.name).getID();
      this.recordDailyLast(env, this.getGivenDaysAgo(6), fieldID, "six");
      this.recordDailyLast(env, this.getGivenDaysAgo(3), fieldID, "three");
      this.recordDailyLast(env, this.getToday(), fieldID, "zero");

      mName = measurementNames[1];
      mVer = measurementVers[1];
      fieldSpecCont = fieldSpecContainers[1];
      fieldID = this.getField(mName, mVer, fieldSpecCont.counter.name).getID();
      this.incrementDailyCount(env, this.getGivenDaysAgo(2), fieldID, 2);
      fieldID = this.getField(mName, mVer, fieldSpecCont.discrete.name).getID();
      this.recordDailyDiscrete(env, this.getToday(), fieldID, 0);
      this.recordDailyDiscrete(env, this.getToday(), fieldID, 1);
      fieldID = this.getField(mName, mVer, fieldSpecCont.last.name).getID();
      this.recordDailyLast(env, this.getYesterday(), fieldID, 1);
    }
  }
}

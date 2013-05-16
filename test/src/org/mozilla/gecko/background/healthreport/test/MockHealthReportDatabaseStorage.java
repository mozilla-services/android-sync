/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.gecko.background.healthreport.test;

import java.io.File;

import org.mozilla.gecko.background.healthreport.HealthReportConstants;
import org.mozilla.gecko.background.healthreport.HealthReportDatabaseStorage;

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
}
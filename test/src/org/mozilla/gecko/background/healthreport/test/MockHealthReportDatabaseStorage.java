/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.background.healthreport.test;

import java.io.File;

import org.mozilla.gecko.background.healthreport.HealthReportDatabaseStorage;
import org.mozilla.gecko.background.healthreport.HealthReportUtils;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

public class MockHealthReportDatabaseStorage extends HealthReportDatabaseStorage {
  public long now = System.currentTimeMillis();

  public long getOneDayAgo() {
    return now - HealthReportUtils.MILLISECONDS_PER_DAY;
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
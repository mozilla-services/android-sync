/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.gecko.background.healthreport.test;

import java.io.File;

import org.mozilla.gecko.background.healthreport.HealthReportDatabaseStorage.HealthReportSQLiteOpenHelper;
import org.mozilla.gecko.background.test.helpers.FakeProfileTestCase;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

public class TestHealthReportSQLiteOpenHelper extends FakeProfileTestCase {
  private class TestingHelper extends HealthReportSQLiteOpenHelper {
    public TestingHelper(String name) {
      super(context, fakeProfileDirectory, name);
    }
  }

  @Override
  protected String getCacheSuffix() {
    return File.separator + "testHealth";
  }

  public void testOpening() {
    TestingHelper helper = new TestingHelper("health.db");
    SQLiteDatabase db = helper.getWritableDatabase();
    assertTrue(db.isOpen());
    db.beginTransaction();
    db.setTransactionSuccessful();
    db.endTransaction();
    helper.close();
    assertFalse(db.isOpen());
  }

  private void assertEmptyTable(SQLiteDatabase db, String table, String column) {
    Cursor c = db.query(table, new String[] { column },
                        null, null, null, null, null);
    assertNotNull(c);
    try {
      assertFalse(c.moveToFirst());
    } finally {
      c.close();
    }
  }

  public void testInit() {
    TestingHelper helper = new TestingHelper("health-" + System.currentTimeMillis() + ".db");
    SQLiteDatabase db = helper.getWritableDatabase();
    assertTrue(db.isOpen());

    db.beginTransaction();
    try {
      // DB starts empty with correct tables.
      assertEmptyTable(db, "fields", "name");
      assertEmptyTable(db, "measurements", "name");
      assertEmptyTable(db, "events_textual", "field");
      assertEmptyTable(db, "events_integer", "field");
      assertEmptyTable(db, "events", "field");

      // Throws for tables that don't exist.
      try {
        assertEmptyTable(db, "foobarbaz", "name");
      } catch (SQLiteException e) {
        // Expected.
      }
      db.setTransactionSuccessful();
    } finally {
      db.endTransaction();
    }
  }
}

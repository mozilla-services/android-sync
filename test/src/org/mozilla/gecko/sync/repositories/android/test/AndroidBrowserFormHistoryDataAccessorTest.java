/* Any copyright is dedicated to the Public Domain.
http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.gecko.sync.repositories.android.test;

import org.mozilla.android.sync.test.helpers.FormHistoryHelpers;
import org.mozilla.gecko.db.BrowserContract;
import org.mozilla.gecko.sync.StubActivity;
import org.mozilla.gecko.sync.repositories.NullCursorException;
import org.mozilla.gecko.sync.repositories.android.AndroidBrowserFormHistoryDataAccessor;
import org.mozilla.gecko.sync.repositories.domain.FormHistoryRecord;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.test.ActivityInstrumentationTestCase2;

public class AndroidBrowserFormHistoryDataAccessorTest extends ActivityInstrumentationTestCase2<StubActivity> {

  protected AndroidBrowserFormHistoryDataAccessor accessor = null;
  protected static final String LOG_TAG = "SyncFormHistDAccessTest";

  public AndroidBrowserFormHistoryDataAccessorTest() {
    super(StubActivity.class);
  }

  public Context getApplicationContext() {
    return this.getInstrumentation().getTargetContext().getApplicationContext();
  }

  public void setUp() {
    accessor = new AndroidBrowserFormHistoryDataAccessor(getApplicationContext());
    accessor.wipe();
  }

  public void doFetch(FormHistoryRecord expected) {
    Cursor cur = null;
    try {
      cur = accessor.fetch(new String[] { expected.guid });
      assertEquals(1, cur.getCount());
      cur.moveToFirst();
      assertEquals(expected.fieldName,  cur.getString(cur.getColumnIndexOrThrow(BrowserContract.FormHistory.FIELD_NAME)));
      assertEquals(expected.fieldValue, cur.getString(cur.getColumnIndexOrThrow(BrowserContract.FormHistory.VALUE)));
      assertEquals(expected.fennecTimesUsed, cur.getLong(cur.getColumnIndexOrThrow(BrowserContract.FormHistory.TIMES_USED)));
      assertEquals(expected.fennecFirstUsed, cur.getLong(cur.getColumnIndexOrThrow(BrowserContract.FormHistory.FIRST_USED)));
      assertEquals(expected.fennecLastUsed,  cur.getLong(cur.getColumnIndexOrThrow(BrowserContract.FormHistory.LAST_USED)));
    } catch (NullCursorException e) {
      fail("Caught NullCursorException");
    } finally {
      if (cur != null) {
        cur.close();
      }
    }
  }

  public void testInsert() throws NullCursorException {
    FormHistoryRecord rec = FormHistoryHelpers.createFormHistory1();
    accessor.insert(rec);
    doFetch(rec);

    accessor.insert(FormHistoryHelpers.createFormHistory2());
    Cursor cur = accessor.fetchAll();
    assertEquals(2, cur.getCount());
    cur.close();
  }

  public void doInsertRecords(FormHistoryRecord[] records) {
    for (FormHistoryRecord record : records) {
      accessor.insert(record);
    }
  }

  public void testFetchAll() throws NullCursorException {
    FormHistoryRecord rec1 = FormHistoryHelpers.createFormHistory1();
    FormHistoryRecord rec2 = FormHistoryHelpers.createFormHistory2();
    doInsertRecords(new FormHistoryRecord[]{rec1, rec2});

    Cursor cur = accessor.fetchAll();
    assertEquals(2, cur.getCount());
    cur.close();
  }

  public void testFetch() throws NullCursorException {
    FormHistoryRecord rec1 = FormHistoryHelpers.createFormHistory1();
    FormHistoryRecord rec2 = FormHistoryHelpers.createFormHistory2();
    doInsertRecords(new FormHistoryRecord[]{rec1, rec2});

    doFetch(rec1);
    doFetch(rec2);
  }

  public void testFetchSince() throws NullCursorException {
    FormHistoryRecord rec1 = FormHistoryHelpers.createFormHistory1();
    FormHistoryRecord rec2 = FormHistoryHelpers.createFormHistory2();
    rec1.fennecFirstUsed = 1000;
    rec2.fennecFirstUsed = 2000;
    doInsertRecords(new FormHistoryRecord[]{rec1, rec2});

    Cursor cur = null;
    cur = accessor.fetchSince(0);
    assertEquals(2, cur.getCount());
    cur.close();

    cur = accessor.fetchSince(rec1.fennecFirstUsed + 1);
    assertEquals(1, cur.getCount());
    cur.moveToFirst();
    assertEquals(rec2.guid, cur.getString(cur.getColumnIndexOrThrow(BrowserContract.FormHistory.GUID)));
    cur.close();

    cur = accessor.fetchSince(rec2.fennecFirstUsed + 1);
    assertEquals(0, cur.getCount());
    cur.close();
  }

  public void testUpdate() throws NullCursorException {
    FormHistoryRecord rec1 = FormHistoryHelpers.createFormHistory1();
    rec1.fennecFirstUsed = 1000;
    accessor.insert(rec1);

    FormHistoryRecord rec2 = FormHistoryHelpers.createFormHistory2();
    rec2.fennecFirstUsed = 2000;
    accessor.insert(rec2);

    rec1.fieldName = "newFieldName";
    accessor.update(rec1.guid, rec1);
    doFetch(rec1);
    doFetch(rec2); // Should not be changed.

    rec2.fieldName = "anotherNewFieldName";
    ContentValues cv = new ContentValues();
    cv.put(BrowserContract.FormHistory.FIELD_NAME, rec2.fieldName);
    accessor.updateByGuid(rec2.guid, cv);
    doFetch(rec2);
    doFetch(rec1); // Should not be changed.
  }
}

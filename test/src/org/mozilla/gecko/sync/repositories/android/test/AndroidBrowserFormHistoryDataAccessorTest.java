/* Any copyright is dedicated to the Public Domain.
http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.gecko.sync.repositories.android.test;

import java.util.ArrayList;

import org.mozilla.android.sync.test.helpers.FormHistoryHelpers;
import org.mozilla.gecko.db.BrowserContract;
import org.mozilla.gecko.sync.Logger;
import org.mozilla.gecko.sync.StubActivity;
import org.mozilla.gecko.sync.repositories.NullCursorException;
import org.mozilla.gecko.sync.repositories.android.AndroidBrowserFormHistoryDataAccessor;
import org.mozilla.gecko.sync.repositories.domain.FormHistoryRecord;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;

public class AndroidBrowserFormHistoryDataAccessorTest extends ActivityInstrumentationTestCase2<StubActivity> {
  protected static final String LOG_TAG = "SyncFormHistDAccessTest";

  protected AndroidBrowserFormHistoryDataAccessor accessor = null;

  public AndroidBrowserFormHistoryDataAccessorTest() {
    super(StubActivity.class);
  }

  public Context getApplicationContext() {
    return this.getInstrumentation().getTargetContext().getApplicationContext();
  }

  public void setUp() {
    Logger.debug(LOG_TAG, "FormHistory tables before wipe...");
    accessor = new AndroidBrowserFormHistoryDataAccessor(getApplicationContext());
    accessor.dumpDB();
    Logger.debug(LOG_TAG, "FormHistory tables before wipe... DONE");
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
      // XXX
      // assertEquals(expected.fennecTimesUsed, cur.getLong(cur.getColumnIndexOrThrow(BrowserContract.FormHistory.TIMES_USED)));
      // assertEquals(expected.fennecFirstUsed, cur.getLong(cur.getColumnIndexOrThrow(BrowserContract.FormHistory.FIRST_USED)));
      // assertEquals(expected.fennecLastUsed,  cur.getLong(cur.getColumnIndexOrThrow(BrowserContract.FormHistory.LAST_USED)));
    } catch (NullCursorException e) {
      fail("Caught NullCursorException");
    } finally {
      if (cur != null) {
        cur.close();
      }
    }
  }

  public void testWipe() throws NullCursorException {
    doInsertRecords(new FormHistoryRecord[] {FormHistoryHelpers.createFormHistory1(), FormHistoryHelpers.createFormHistory2()});
    accessor.wipe();
    accessor.dumpDB();

    Cursor cur = accessor.fetchAll();
    assertEquals(0, cur.getCount());
    cur.close();
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
    long after0 = System.currentTimeMillis();

    FormHistoryRecord rec1 = FormHistoryHelpers.createFormHistory1();
    doInsertRecords(new FormHistoryRecord[]{rec1});

    long after1 = System.currentTimeMillis();

    FormHistoryRecord rec2 = FormHistoryHelpers.createFormHistory2();
    doInsertRecords(new FormHistoryRecord[]{rec2});

    long after2 = System.currentTimeMillis();

    Cursor cur = null;
    cur = accessor.fetchSince(after0);
    assertEquals(2, cur.getCount());
    cur.close();

    Log.i(LOG_TAG, "after0: " + after0 + " after1: " + after1 + " after2: " + after2);
    accessor.dumpDB();
    cur = accessor.fetchSince(after1);
    assertEquals(1, cur.getCount());
    cur.moveToFirst();
    assertEquals(rec2.guid, cur.getString(cur.getColumnIndexOrThrow(BrowserContract.FormHistory.GUID)));
    cur.close();

    cur = accessor.fetchSince(after2);
    assertEquals(0, cur.getCount());
    cur.close();
  }

  public void testUpdate() throws NullCursorException {
    FormHistoryRecord rec1 = FormHistoryHelpers.createFormHistory1();
    accessor.insert(rec1);

    FormHistoryRecord rec2 = FormHistoryHelpers.createFormHistory2();
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

  public void testDelete() throws NullCursorException {
    FormHistoryRecord rec1 = FormHistoryHelpers.createFormHistory1();
    accessor.insert(rec1);

    FormHistoryRecord rec2 = FormHistoryHelpers.createFormHistory2();
    accessor.insert(rec2);

    accessor.deleteGuid(rec1.guid);

    // Check that we have the second record, and exactly one record.
    doFetch(rec2);
    Cursor cur = accessor.fetchAll();
    assertEquals(1, cur.getCount());
    cur.close();

    // Check that the deleted record has been deleted (and that's it).
    ArrayList<String> deletedGUIDs = accessor.deletedGuids();
    assertEquals(1, deletedGUIDs.size());
    assertTrue(deletedGUIDs.contains(rec1.guid));

    // Now purge, and check that it's gone.
    accessor.purgeDeleted();
    deletedGUIDs = accessor.deletedGuids();
    assertEquals(0, deletedGUIDs.size());

    // Check that we still have the first record, and exactly one record.
    doFetch(rec2);
    cur = accessor.fetchAll();
    assertEquals(1, cur.getCount());
    cur.close();

    // Now delete multiple records and check we purge them all successfully.
    FormHistoryRecord rec3 = FormHistoryHelpers.createFormHistory3();
    accessor.insert(rec3);
    accessor.deleteGuid(rec2.guid);
    accessor.deleteGuid(rec3.guid);
    cur = accessor.fetchAll();
    assertEquals(0, cur.getCount());
    cur.close();

    deletedGUIDs = accessor.deletedGuids();
    assertEquals(2, deletedGUIDs.size());

    // Now purge again, and check that everything is gone.
    accessor.purgeDeleted();
    deletedGUIDs = accessor.deletedGuids();
    assertEquals(0, deletedGUIDs.size());
  }
}

/* Any copyright is dedicated to the Public Domain.
http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.gecko.sync.repositories.android.test;

import org.mozilla.android.sync.test.helpers.FormHistoryHelpers;
import org.mozilla.gecko.db.BrowserContract;
import org.mozilla.gecko.sync.Logger;
import org.mozilla.gecko.sync.StubActivity;
import org.mozilla.gecko.sync.repositories.NullCursorException;
import org.mozilla.gecko.sync.repositories.android.AndroidBrowserDeletedFormHistoryDataAccessor;
import org.mozilla.gecko.sync.repositories.domain.FormHistoryRecord;

import android.content.Context;
import android.database.Cursor;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;

public class AndroidBrowserDeletedFormHistoryDataAccessorTest extends ActivityInstrumentationTestCase2<StubActivity> {
  protected static final String LOG_TAG = "SyncDelFormHistDAccTest";

  protected AndroidBrowserDeletedFormHistoryDataAccessor accessor = null;

  public AndroidBrowserDeletedFormHistoryDataAccessorTest() {
    super(StubActivity.class);
  }

  public Context getApplicationContext() {
    return this.getInstrumentation().getTargetContext().getApplicationContext();
  }

  public void setUp() {
    Logger.debug(LOG_TAG, "DeletedFormHistory tables before wipe...");
    accessor = new AndroidBrowserDeletedFormHistoryDataAccessor(getApplicationContext());
    accessor.dumpDB();
    Logger.debug(LOG_TAG, "DeletedFormHistory tables before wipe... DONE");
    accessor.wipe();
  }

  public void doFetch(FormHistoryRecord expected) {
    Cursor cur = null;
    try {
      cur = accessor.fetch(new String[] { expected.guid });
      assertEquals(1, cur.getCount());
      cur.moveToFirst();
      assertEquals(expected.guid,  cur.getString(cur.getColumnIndexOrThrow(BrowserContract.FormHistory.GUID)));
    } catch (NullCursorException e) {
      fail("Caught NullCursorException");
    } finally {
      if (cur != null) {
        cur.close();
      }
    }
  }

  public void testWipe() throws NullCursorException {
    doInsertRecords(new FormHistoryRecord[] {FormHistoryHelpers.createDeletedFormHistory1(), FormHistoryHelpers.createDeletedFormHistory2()});
    accessor.wipe();
    accessor.dumpDB();

    Cursor cur = accessor.fetchAll();
    assertEquals(0, cur.getCount());
    cur.close();
  }

  public void testInsert() throws NullCursorException {
    FormHistoryRecord rec = FormHistoryHelpers.createDeletedFormHistory1();
    accessor.insert(rec);
    doFetch(rec);

    accessor.insert(FormHistoryHelpers.createDeletedFormHistory2());
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
    FormHistoryRecord rec1 = FormHistoryHelpers.createDeletedFormHistory1();
    FormHistoryRecord rec2 = FormHistoryHelpers.createDeletedFormHistory2();
    doInsertRecords(new FormHistoryRecord[]{rec1, rec2});

    Cursor cur = accessor.fetchAll();
    assertEquals(2, cur.getCount());
    cur.close();
  }

  public void testFetch() throws NullCursorException {
    FormHistoryRecord rec1 = FormHistoryHelpers.createDeletedFormHistory1();
    FormHistoryRecord rec2 = FormHistoryHelpers.createDeletedFormHistory2();
    doInsertRecords(new FormHistoryRecord[]{rec1, rec2});

    doFetch(rec1);
    doFetch(rec2);
  }

  public void testFetchSince() throws NullCursorException {
    long after0 = System.currentTimeMillis();

    FormHistoryRecord rec1 = FormHistoryHelpers.createDeletedFormHistory1();
    doInsertRecords(new FormHistoryRecord[]{rec1});

    long after1 = System.currentTimeMillis();

    FormHistoryRecord rec2 = FormHistoryHelpers.createDeletedFormHistory2();
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
    FormHistoryRecord rec1 = FormHistoryHelpers.createDeletedFormHistory1();
    accessor.insert(rec1);

    FormHistoryRecord rec2 = FormHistoryHelpers.createDeletedFormHistory2();
    accessor.insert(rec2);

    rec1.fieldName = "newFieldName";
    accessor.update(rec1.guid, rec1);
    doFetch(rec1);
    doFetch(rec2); // Should not be changed.
  }
}

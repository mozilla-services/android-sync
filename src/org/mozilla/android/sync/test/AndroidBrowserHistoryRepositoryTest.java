/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test;

import org.json.simple.JSONObject;
import org.mozilla.android.sync.test.helpers.ExpectFetchDelegate;
import org.mozilla.android.sync.test.helpers.HistoryHelpers;
import org.mozilla.gecko.sync.repositories.NullCursorException;
import org.mozilla.gecko.sync.repositories.android.AndroidBrowserHistoryDataAccessor;
import org.mozilla.gecko.sync.repositories.android.AndroidBrowserHistoryRepository;
import org.mozilla.gecko.sync.repositories.android.AndroidBrowserRepository;
import org.mozilla.gecko.sync.repositories.android.AndroidBrowserRepositoryDataAccessor;
import org.mozilla.gecko.sync.repositories.android.AndroidBrowserRepositorySession;
import org.mozilla.gecko.sync.repositories.android.BrowserContract;
import org.mozilla.gecko.sync.repositories.android.RepoUtils;
import org.mozilla.gecko.sync.repositories.domain.HistoryRecord;
import org.mozilla.gecko.sync.repositories.domain.Record;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

public class AndroidBrowserHistoryRepositoryTest extends AndroidBrowserRepositoryTest {
  
  @Override
  protected AndroidBrowserRepository getRepository() {
    return new AndroidBrowserHistoryRepository();
  }

  @Override
  protected AndroidBrowserRepositoryDataAccessor getDataAccessor() {
    return new AndroidBrowserHistoryDataAccessor(getApplicationContext());
  }

  @Override
  public void testFetchAll() {
    Record[] expected = new Record[2];
    expected[0] = HistoryHelpers.createHistory3();
    expected[1] = HistoryHelpers.createHistory2();
    basicFetchAllTest(expected);
  }

  /*
   * Test storing identical records with different guids.
   * For bookmarks identical is defined by the following fields
   * being the same: title, uri, type, parentName
   */
  @Override
  public void testStoreIdenticalExceptGuid() {
    storeIdenticalExceptGuid(HistoryHelpers.createHistory1());
  }
  
  @Override
  public void testCleanMultipleRecords() {
    cleanMultipleRecords(
        HistoryHelpers.createHistory1(),
        HistoryHelpers.createHistory2(),
        HistoryHelpers.createHistory3(),
        HistoryHelpers.createHistory4(),
        HistoryHelpers.createHistory5()
    );
  } 

  @Override
  public void testGuidsSinceReturnMultipleRecords() {
    HistoryRecord record0 = HistoryHelpers.createHistory1();
    HistoryRecord record1 = HistoryHelpers.createHistory2();
    guidsSinceReturnMultipleRecords(record0, record1);
  }
  
  @Override
  public void testGuidsSinceReturnNoRecords() {
    guidsSinceReturnNoRecords(HistoryHelpers.createHistory3());
  }

  @Override
  public void testFetchSinceOneRecord() {
    fetchSinceOneRecord(HistoryHelpers.createHistory1(),
        HistoryHelpers.createHistory2());
  }
  
  @Override
  public void testFetchSinceReturnNoRecords() {
    fetchSinceReturnNoRecords(HistoryHelpers.createHistory3());
  }
  
  @Override
  public void testFetchOneRecordByGuid() {
    fetchOneRecordByGuid(HistoryHelpers.createHistory1(),
        HistoryHelpers.createHistory2());
  }
  
  @Override
  public void testFetchMultipleRecordsByGuids() {
    HistoryRecord record0 = HistoryHelpers.createHistory1();
    HistoryRecord record1 = HistoryHelpers.createHistory2();
    HistoryRecord record2 = HistoryHelpers.createHistory3();
    fetchMultipleRecordsByGuids(record0, record1, record2);
  }
  
  @Override
  public void testFetchNoRecordByGuid() {
    fetchNoRecordByGuid(HistoryHelpers.createHistory1());
  }
  
  @Override
  public void testWipe() {
    doWipe(HistoryHelpers.createHistory2(), HistoryHelpers.createHistory3());
  }
  
  @Override
  public void testStore() {
    basicStoreTest(HistoryHelpers.createHistory1());
  }
  
  @Override
  public void testRemoteNewerTimeStamp() {
    HistoryRecord local = HistoryHelpers.createHistory1();
    HistoryRecord remote = HistoryHelpers.createHistory2();
    remoteNewerTimeStamp(local, remote);
  }

  @Override
  public void testLocalNewerTimeStamp() {
    HistoryRecord local = HistoryHelpers.createHistory1();
    HistoryRecord remote = HistoryHelpers.createHistory2();
    localNewerTimeStamp(local, remote);
  }
  
  @Override
  public void testDeleteRemoteNewer() {
    HistoryRecord local = HistoryHelpers.createHistory1();
    HistoryRecord remote = HistoryHelpers.createHistory2();
    deleteRemoteNewer(local, remote);
  }
  
  @Override
  public void testDeleteLocalNewer() {
    HistoryRecord local = HistoryHelpers.createHistory1();
    HistoryRecord remote = HistoryHelpers.createHistory2();
    deleteLocalNewer(local, remote);
  }
  
  @Override
  public void testDeleteRemoteLocalNonexistent() {
    deleteRemoteLocalNonexistent(HistoryHelpers.createHistory2());
  }
  
  /*
   * Tests for adding some visits to a history record
   * and doing a fetch.
   */
  @SuppressWarnings("unchecked")
  public void testAddOneVisit() {
    prepSession();
    AndroidBrowserRepositorySession session = getSession();
    
    HistoryRecord record0 = HistoryHelpers.createHistory3();
    performWait(storeRunnable(session, record0));
    
    // Add one visit to the count and put in a new
    // last visited date.
    ContentValues cv = new ContentValues();
    int visits = record0.visits.size() + 1;
    long newVisitTime = System.currentTimeMillis();
    cv.put(BrowserContract.History.VISITS, visits);
    cv.put(BrowserContract.History.DATE_LAST_VISITED, newVisitTime);
    getDataAccessor().updateByGuid(record0.guid, cv);
    
    // Add expected visit to record for verification.
    JSONObject expectedVisit = new JSONObject();
    expectedVisit.put("date", newVisitTime * 1000);    // Microseconds.
    expectedVisit.put("type", 1L);
    record0.visits.add(expectedVisit);
    
    performWait(fetchRunnable(session, new String[] { record0.guid }, new ExpectFetchDelegate(new Record[] { record0 })));
  }
  
  @SuppressWarnings("unchecked")
  public void testAddMultipleVisits() {
    prepSession();
    AndroidBrowserRepositorySession session = getSession();
    
    HistoryRecord record0 = HistoryHelpers.createHistory4();
    performWait(storeRunnable(session, record0));
    
    // Add three visits to the count and put in a new
    // last visited date.
    ContentValues cv = new ContentValues();
    int visits = record0.visits.size() + 3;
    long newVisitTime = System.currentTimeMillis();
    cv.put(BrowserContract.History.VISITS, visits);
    cv.put(BrowserContract.History.DATE_LAST_VISITED, newVisitTime);
    getDataAccessor().updateByGuid(record0.guid, cv);

    // Now shift to microsecond timing for visits.
    long newMicroVisitTime = newVisitTime * 1000;
    
    // Add expected visits to record for verification
    JSONObject expectedVisit = new JSONObject();
    expectedVisit.put("date", newMicroVisitTime);
    expectedVisit.put("type", 1L);
    record0.visits.add(expectedVisit);
    expectedVisit = new JSONObject();
    expectedVisit.put("date", newMicroVisitTime - 1000);
    expectedVisit.put("type", 1L);
    record0.visits.add(expectedVisit);
    expectedVisit = new JSONObject();
    expectedVisit.put("date", newMicroVisitTime - 2000);
    expectedVisit.put("type", 1L);
    record0.visits.add(expectedVisit);
    
    ExpectFetchDelegate delegate = new ExpectFetchDelegate(new Record[] { record0 });
    performWait(fetchRunnable(session, new String[] { record0.guid }, delegate));

    Record fetched = delegate.records.get(0);
    assertTrue(record0.equalPayloads(fetched));
  }

  public void testSqlInjectPurgeDelete() {
    // Some setup.
    prepSession();
    AndroidBrowserRepositoryDataAccessor db = getDataAccessor();

    ContentValues cv = new ContentValues();
    cv.put(BrowserContract.SyncColumns.IS_DELETED, 1);

    // Create and insert 2 history entries, 2nd one is evil (attempts injection).
    HistoryRecord h1 = HistoryHelpers.createHistory1();
    HistoryRecord h2 = HistoryHelpers.createHistory2();
    h2.guid = "' or '1'='1";

    db.insert(h1);
    db.insert(h2);

    // Test 1 - updateByGuid() handles evil history entries correctly.
    db.updateByGuid(h2.guid, cv);

    // Query history table.
    Cursor cur = getAllHistory();
    int numHistory = cur.getCount();

    // Ensure only the evil history entry is marked for deletion.
    try {
      cur.moveToFirst();
      while (!cur.isAfterLast()) {
        String guid = RepoUtils.getStringFromCursor(cur, BrowserContract.SyncColumns.GUID);
        boolean deleted = RepoUtils.getLongFromCursor(cur, BrowserContract.SyncColumns.IS_DELETED) == 1;

        if (guid.equals(h2.guid)) {
          assertTrue(deleted);
        } else {
          assertFalse(deleted);
        }
        cur.moveToNext();
      }
    } finally {
      cur.close();
    }

    // Test 2 - Ensure purgeDelete()'s call to delete() deletes only 1 record.
    try {
      db.purgeDeleted();
    } catch (NullCursorException e) {
      e.printStackTrace();
    }

    cur = getAllHistory();
    int numHistoryAfterDeletion = cur.getCount();

    // Ensure we have only 1 deleted row.
    assertEquals(numHistoryAfterDeletion, numHistory - 1);

    // Ensure only the evil history is deleted.
    try {
      cur.moveToFirst();
      while (!cur.isAfterLast()) {
        String guid = RepoUtils.getStringFromCursor(cur, BrowserContract.SyncColumns.GUID);
        boolean deleted = RepoUtils.getLongFromCursor(cur, BrowserContract.SyncColumns.IS_DELETED) == 1;

        if (guid.equals(h2.guid)) {
          fail("Evil guid was not deleted!");
        } else {
          assertFalse(deleted);
        }
        cur.moveToNext();
      }
    } finally {
      cur.close();
    }
  }

  protected Cursor getAllHistory() {
    Context context = getApplicationContext();
    Cursor cur = context.getContentResolver().query(BrowserContract.History.CONTENT_URI,
        BrowserContract.History.HistoryColumns, null, null, null);
    return cur;
  }
}

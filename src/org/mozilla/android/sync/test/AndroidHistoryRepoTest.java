package org.mozilla.android.sync.test;

import org.mozilla.android.sync.repositories.android.AndroidBrowserHistoryDatabaseHelper;
import org.mozilla.android.sync.repositories.android.AndroidBrowserHistoryRepository;
import org.mozilla.android.sync.repositories.android.AndroidBrowserRepository;
import org.mozilla.android.sync.repositories.android.AndroidBrowserRepositoryDatabaseHelper;
import org.mozilla.android.sync.repositories.domain.HistoryRecord;
import org.mozilla.android.sync.repositories.domain.Record;
import org.mozilla.android.sync.test.helpers.HistoryHelpers;

public class AndroidHistoryRepoTest extends AndroidRepositoryTest {
  
  @Override
  protected AndroidBrowserRepository getRepository() {
    return new AndroidBrowserHistoryRepository();
  }

  @Override
  protected AndroidBrowserRepositoryDatabaseHelper getDatabaseHelper() {
    return new AndroidBrowserHistoryDatabaseHelper(getApplicationContext());
  }

  public void testFetchAll() {
    Record[] expected = new Record[2];
    expected[0] = HistoryHelpers.createHistory3();
    expected[1] = HistoryHelpers.createHistory2();
    basicFetchAllTest(expected);
  }

  /*
   * Tests for fetching GUIDs since a timestamp.
   */
  public void testGuidsSinceReturnMultipleRecords() {
    HistoryRecord record0 = HistoryHelpers.createHistory1();
    HistoryRecord record1 = HistoryHelpers.createHistory2();
    guidsSinceReturnMultipleRecords(record0, record1);
  }
  
  public void testGuidsSinceReturnNoRecords() {
    guidsSinceReturnNoRecords(HistoryHelpers.createHistory3());
  }

  /*
   * Tests for fetchSince
   */  
  public void testFetchSinceOneRecord() {
    fetchSinceOneRecord(HistoryHelpers.createHistory1(),
        HistoryHelpers.createHistory2());
  }
  
  public void testFetchSinceReturnNoRecords() {
    fetchSinceReturnNoRecords(HistoryHelpers.createHistory3());
  }
  
  /*
   * Tests for fetch(guids)
   */
  public void testFetchOneRecordByGuid() {
    fetchOneRecordByGuid(HistoryHelpers.createHistory1(),
        HistoryHelpers.createHistory2());
  }
  
  public void testFetchMultipleRecordsByGuids() {
    HistoryRecord record0 = HistoryHelpers.createHistory1();
    HistoryRecord record1 = HistoryHelpers.createHistory2();
    HistoryRecord record2 = HistoryHelpers.createHistory3();
    fetchMultipleRecordsByGuids(record0, record1, record2);
  }
  
  public void testFetchNoRecordByGuid() {
    fetchNoRecordByGuid(HistoryHelpers.createHistory1());
  }
  
  /*
   * Test wipe
   */
  public void testWipe() {
    doWipe(HistoryHelpers.createHistory2(), HistoryHelpers.createHistory3());
  }
  
  /*
   * Test store
   */
  public void testStoreHistory() {
    basicStoreTest(HistoryHelpers.createHistory1());
  }
  
  /*
   * Test for store conflict resolution
   * NOTE: Must set an android ID for local record for these tests to work
   */
  public void testRemoteNewerTimeStamp() {
    HistoryRecord local = HistoryHelpers.createHistory1();
    HistoryRecord remote = HistoryHelpers.createHistory2();
    local.androidID = 54321;
    remoteNewerTimeStamp(local, remote);
  }

  public void testLocalNewerTimeStamp() {
    HistoryRecord local = HistoryHelpers.createHistory1();
    HistoryRecord remote = HistoryHelpers.createHistory2();
    local.androidID = 54321;
    localNewerTimeStamp(local, remote);
  }
  
  public void testDeleteRemoteNewer() {
    HistoryRecord local = HistoryHelpers.createHistory1();
    HistoryRecord remote = HistoryHelpers.createHistory2();
    local.androidID = 54321;
    deleteRemoteNewer(local, remote);
  }
  
  public void testDeleteLocalNewere() {
    HistoryRecord local = HistoryHelpers.createHistory1();
    HistoryRecord remote = HistoryHelpers.createHistory2();
    local.androidID = 54321;
    deleteLocalNewer(local, remote);
  }
  
  public void testDeleteRemoteLocalNonexistent() {
    HistoryRecord remote = HistoryHelpers.createHistory2();
    deleteRemoteLocalNonexistent(remote);
  }

  /*
   * Helpers
   */
  @Override
  protected void verifyExpectedRecordReturned(Record expected, Record actual) {
    HistoryRecord recExpect = (HistoryRecord) expected;
    HistoryRecord recActual = (HistoryRecord) actual;
    assertEquals(recExpect.guid, recActual.guid);
    assertEquals(recExpect.title, recActual.title);
    assertEquals(recExpect.histURI, recActual.histURI);
    assertEquals(recExpect.transitionType, recActual.transitionType);
    assertEquals(recExpect.androidID, recActual.androidID);
  }
  
}

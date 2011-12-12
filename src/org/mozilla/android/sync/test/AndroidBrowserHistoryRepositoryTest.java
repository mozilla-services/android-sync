/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test;

import org.mozilla.android.sync.test.helpers.HistoryHelpers;
import org.mozilla.gecko.sync.repositories.android.AndroidBrowserHistoryDataAccessor;
import org.mozilla.gecko.sync.repositories.android.AndroidBrowserHistoryRepository;
import org.mozilla.gecko.sync.repositories.android.AndroidBrowserRepository;
import org.mozilla.gecko.sync.repositories.android.AndroidBrowserRepositoryDataAccessor;
import org.mozilla.gecko.sync.repositories.domain.HistoryRecord;
import org.mozilla.gecko.sync.repositories.domain.Record;

public class AndroidBrowserHistoryRepositoryTest /*extends AndroidBrowserRepositoryTest*/ {
  
  /*
   * Comment out while getting bookmars working!
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
    local.androidID = 54321;
    remoteNewerTimeStamp(local, remote);
  }

  @Override
  public void testLocalNewerTimeStamp() {
    HistoryRecord local = HistoryHelpers.createHistory1();
    HistoryRecord remote = HistoryHelpers.createHistory2();
    local.androidID = 54321;
    localNewerTimeStamp(local, remote);
  }
  
  @Override
  public void testDeleteRemoteNewer() {
    HistoryRecord local = HistoryHelpers.createHistory1();
    HistoryRecord remote = HistoryHelpers.createHistory2();
    local.androidID = 54321;
    deleteRemoteNewer(local, remote);
  }
  
  @Override
  public void testDeleteLocalNewer() {
    HistoryRecord local = HistoryHelpers.createHistory1();
    HistoryRecord remote = HistoryHelpers.createHistory2();
    local.androidID = 54321;
    deleteLocalNewer(local, remote);
  }
  
  @Override
  public void testDeleteRemoteLocalNonexistent() {
    HistoryRecord remote = HistoryHelpers.createHistory2();
    deleteRemoteLocalNonexistent(remote);
  }

*/
}

package org.mozilla.android.sync.test;

import org.mozilla.android.sync.repositories.domain.HistoryRecord;
import org.mozilla.android.sync.repositories.domain.Record;
import org.mozilla.android.sync.test.helpers.HistoryHelpers;

public class AndroidHistoryRepoTest extends AndroidHistoryTestBase {
  
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
  

}
